package info.kgeorgiy.ja.ilyin.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

public interface Bank extends Remote {
    Person createPerson(String firstName, String lastName, String passport) throws RemoteException;

    Person getPerson(String passport, Class <? extends Person> personClass) throws RemoteException;

    Set<String> getAccountsByPassport(String passport) throws RemoteException;

    boolean checkPersonData(String firstName, String lastName, String passport) throws RemoteException;

    /**
     * Creates a new account with specified identifier if it is not already exists.
     * @param accountId account id.
     * @param passport person passport.
     * @return created or existing account.
     */
    Account createAccount(String accountId, String passport) throws RemoteException;

    /**
     * Returns account by identifier.
     * @param accountId account id
     * @param passport person passport
     * @return account with specified identifier or {@code null} if such account does not exists.
     */
    Account getAccount(String accountId, String passport) throws RemoteException;
}