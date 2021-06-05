package info.kgeorgiy.ja.ilyin.bank;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalAccount extends BaseAccount implements Serializable {
    public LocalAccount(Account account) throws RemoteException {
        super(account.getSubId(), account.getPassport(), account.getAmount());
    }

    public LocalAccount(String subId, String passport) {
        super(subId, passport, 0);
    }
}
