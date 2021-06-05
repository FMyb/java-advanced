package info.kgeorgiy.ja.ilyin.bank;

import java.rmi.RemoteException;
import java.util.Set;

public class RemotePerson extends BasePerson implements Person{
    private final Bank bank;

    protected RemotePerson(String firstName, String lastName, String passport, Bank bank) {
        super(firstName, lastName, passport);
        this.bank = bank;
    }

    @Override
    public Account getAccount(String accountId) throws RemoteException {
        return bank.getAccount(accountId, getPassport());
    }

    @Override
    public Set<String> getAccounts() throws RemoteException {
        return bank.getAccountsByPassport(getPassport());
    }

    @Override
    public Account createAccount(String accountId) throws RemoteException {
        return bank.createAccount(accountId, getPassport());
    }
}
