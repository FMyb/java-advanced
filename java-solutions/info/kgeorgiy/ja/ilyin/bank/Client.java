package info.kgeorgiy.ja.ilyin.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Objects;

public final class Client {
    /**
     * Utility class.
     */
    private Client() {
    }

    public static void main(final String... args) throws RemoteException {
        if (checkArgs(args)) {
            System.err.println("Invalid arguments. Expected: firstName lastName passport accountId amount");
            return;
        }
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }
        String firstName = args[0];
        String lastName = args[1];
        String passport = args[2];
        String accountId = args[3];
        int amount = Integer.parseInt(args[4]);
        final int PORT = 8080;
        Person person = bank.getPerson(passport, LocalPerson.class);
        if (person == null) {
            System.out.println("Creating person");
            person = bank.createPerson(firstName, lastName, passport);
        } else {
            System.out.println("Person already exists");
        }
        Account account = person.getAccount(accountId);
        if (account == null) {
            System.out.println("Creating account");
            account = person.createAccount(accountId);
        } else {
            System.out.println("Account already exists");
        }
        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        account.setAmount(account.getAmount() + 100);
        System.out.println("Money: " + account.getAmount());
    }

    private static boolean checkArgs(String[] args) {
        return args != null && args.length == 5 && Arrays.stream(args).allMatch(Objects::nonNull);
    }
}
