package info.kgeorgiy.ja.ilyin.bank;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LocalPerson extends BasePerson implements Serializable {
    private final Map<String, Account> personAccounts;

    protected LocalPerson(String firstName, String lastName, String passport, Bank bank) throws RemoteException {
        super(firstName, lastName, passport);
        personAccounts = new HashMap<>();
        for (String accountId : bank.getAccountsByPassport(passport)) {
            personAccounts.put(accountId, new LocalAccount(bank.getAccount(accountId, getPassport())));
        }
    }

    @Override
    public Set<String> getAccounts() {
        return personAccounts.keySet();
    }

    @Override
    public Account getAccount(String accountId) throws RemoteException {
        return personAccounts.get(accountId);
    }

    @Override
    public Account createAccount(String accountId) throws RemoteException {
        personAccounts.putIfAbsent(accountId, new LocalAccount(accountId, getPassport()));
        return personAccounts.get(accountId);
    }
}
