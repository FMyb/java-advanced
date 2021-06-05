package info.kgeorgiy.ja.ilyin.bank;

import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// :NOTE: маловато будет
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BankTests {
    private static Bank bank;
    private final static int PORT = 32381;

    @BeforeAll
    public static void prepare() throws RemoteException {
        final Registry registry = LocateRegistry.createRegistry(PORT);
        bank = new RemoteBank(PORT);
        UnicastRemoteObject.exportObject(bank, PORT);
        registry.rebind("//localhost/bank", bank);
        try {
            bank = (Bank) registry.lookup("//localhost/bank");
        } catch (final NotBoundException ignored) {
        }
    }

    @Test
    public void test_01_createPerson() throws RemoteException {
        Person person = bank.createPerson("Yaroslav", "Ilin", "YaroslavIlin");
        Person copy = bank.createPerson("Yaroslav", "Ilin", "YaroslavIlin");
        assertEquals(person, copy);
        Person fake = bank.createPerson("Yaroslav", "Iiin", "YaroslavIlin");
        assertNull(fake);
    }

    @Test
    public void test_02_checkPersonData() throws RemoteException {
        Person person = bank.createPerson("Georgiy", "Korneev", "kgeorgiy");
        assertTrue(bank.checkPersonData("Georgiy", "Korneev", "kgeorgiy"));
        assertFalse(bank.checkPersonData("Yaroslav", "Ilin", "kgeorgiy"));
        assertEquals(person, bank.getPerson("kgeorgiy", RemotePerson.class));
    }

    @Test
    public void test_03_remoteAccount() throws RemoteException {
        bank.createPerson("Yaroslav", "Ilin", "YI");
        Account account = bank.createAccount("01", "YI");
        assertEquals(0, account.getAmount());
        account.setAmount(300);
        assertEquals(300, bank.getAccount("01", "YI").getAmount());
        bank.getAccount("01", "YI").setAmount(200);
        assertEquals(200, account.getAmount());
    }

    @Test
    public void test_04_createAccounts() throws RemoteException {
        Person person = bank.createPerson("Dmitry", "Nagiev", "DN");
        bank.createAccount("DN1", "DN");
        bank.createAccount("DN2", "DN");
        person.createAccount("DN3");
        assertEquals(person.createAccount("DN4"), bank.createAccount("DN4", "DN"));
        assertEquals(4, bank.getAccountsByPassport("DN").size());
    }

    private void testSet(final String passport, final String[] accountIds) throws RemoteException {
        final Set<String> accounts = new HashSet<>();
        for (final String accountId : accountIds) {
            accounts.add(accountId);
            bank.createAccount(accountId, passport);
        }
        assertEquals(accounts, bank.getAccountsByPassport(passport));
    }

    @Test
    public void test_05_personTest() throws RemoteException {
        bank.createPerson("Mat", "Throw", "MT");
        testSet("MT", new String[]{"dom", "gfgerger", "qerwgqre"});
        final LocalPerson localPerson = (LocalPerson) bank.getPerson("MT", LocalPerson.class);
        final RemotePerson remotePerson = (RemotePerson) bank.getPerson("MT", RemotePerson.class);
        assertEquals(localPerson.getPassport(), "MT");
        assertEquals(3, localPerson.getAccounts().size());
        bank.createAccount("reger", "MT");
        assertEquals(3, localPerson.getAccounts().size());
        assertEquals(4, remotePerson.getAccounts().size());
        localPerson.createAccount("qwe");
        assertNotEquals(remotePerson.getAccounts(), localPerson.getAccounts());
    }

    @Test
    public void test_06_multiThreads() throws InterruptedException, RemoteException {
        final ExecutorService checkers = Executors.newCachedThreadPool();
        checkers.submit(() -> {
            try {
                bank.createPerson("Don", "Yagon", "T1");
                bank.createAccount("acc4", "T1");
                bank.createAccount("acc5", "T1");
                bank.createAccount("acc6", "T1");
            } catch (final RemoteException ignored) {
            }
        });
        checkers.submit(() -> {
            try {
                bank.createPerson("Mal", "Milk", "T2");
                bank.createAccount("acc1", "T2");
                bank.createAccount("acc2", "T2");
                bank.createAccount("acc3", "T2");
            } catch (final RemoteException ignored) {
            }
        });
        checkers.shutdown();
        checkers.awaitTermination(60, TimeUnit.SECONDS);
        assertNull(bank.getPerson("T3", RemotePerson.class));
        assertTrue(bank.checkPersonData("Don", "Yagon", "T1"));
        assertEquals(3, bank.getAccountsByPassport("T2").size());
        assertEquals("T1:acc4", bank.getAccount("acc4", "T1").getId());
    }

    @Test
    public void test_07_manyThreads() throws InterruptedException {
        final ExecutorService checkers = Executors.newCachedThreadPool();
        for (int thread = 0; thread < 20; thread++) {
            final int numOfThread = thread;
            checkers.submit(() -> {
                try {
                    final String passportId = "passport_Id_" + numOfThread;
                    bank.createPerson("firstName_" + numOfThread, "lastName_" + numOfThread, passportId);
                    for (int account = 0; account < 20; account++) {
                        bank.createAccount(passportId, Integer.toString(account)).setAmount(account);
                    }
                    for (int account = 0; account < 20; account++) {
                        assertEquals(account, bank.getAccount(passportId, Integer.toString(account)).getAmount());
                    }
                } catch (final RemoteException ignored) {
                }
            });
        }
        checkers.shutdown();
        checkers.awaitTermination(60, TimeUnit.SECONDS);
    }

    @Test
    public void test_08_localAccountTest() throws RemoteException {
        bank.createPerson("NAME!", "LASTNAME", "NLA");
        final Account remoteAccount = bank.createAccount("qwer", "NLA");
        remoteAccount.setAmount(100);
        final LocalPerson localPerson = (LocalPerson) bank.getPerson("NLA", LocalPerson.class);
        final Account localAccount = localPerson.getAccount("qwer");
        assertEquals(100, localAccount.getAmount());
        remoteAccount.setAmount(-200);
        assertEquals(100, localAccount.getAmount());
        assertEquals(-200, remoteAccount.getAmount());
        localAccount.setAmount(300);
        assertEquals(300, localAccount.getAmount());
        assertEquals(-200, remoteAccount.getAmount());
    }
}