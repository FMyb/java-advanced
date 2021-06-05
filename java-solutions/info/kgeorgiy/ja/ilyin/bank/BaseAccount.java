package info.kgeorgiy.ja.ilyin.bank;

import java.rmi.RemoteException;
import java.util.Objects;

public abstract class BaseAccount implements Account {
    private final String subId;
    private final String passport;
    private int amount;

    public BaseAccount(final String subId, final String passport, final int amount) {
        this.subId = subId;
        this.passport = passport;
        this.amount = amount;
    }

    @Override
    public String getId() {
        return passport + ":" + subId;
    }

    @Override
    public String getPassport() throws RemoteException {
        return passport;
    }

    @Override
    public String getSubId() throws RemoteException {
        return subId;
    }

    @Override
    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + getId());
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) {
        System.out.println("Setting amount of money for account " + getId());
        this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseAccount)) return false;
        BaseAccount account = (BaseAccount) o;
        return amount == account.amount && passport.equals(account.passport) && subId.equals(account.subId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(passport, subId, amount);
    }
}
