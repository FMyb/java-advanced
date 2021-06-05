package info.kgeorgiy.ja.ilyin.bank;

import java.rmi.RemoteException;

public class RemoteAccount extends BaseAccount{

    public RemoteAccount(String subId, String passport) {
        super(subId, passport, 0);
    }
}