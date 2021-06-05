package info.kgeorgiy.ja.ilyin.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface Person extends Remote {
    String getFirstName() throws RemoteException;

    String getLastName() throws RemoteException;

    String getPassport() throws RemoteException;

    Account getAccount(String accountId) throws RemoteException;

    Set<String> getAccounts() throws RemoteException;

    Account createAccount(String accountId) throws RemoteException;
}
