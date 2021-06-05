package info.kgeorgiy.ja.ilyin.bank;

import java.rmi.RemoteException;

public abstract class BasePerson implements Person {
    private final String firstName;
    private final String lastName;
    private final String passport;

    protected BasePerson(final String firstName, final String lastName, final String passport) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passport = passport;
    }

    @Override
    public String getFirstName() throws RemoteException {
        return firstName;
    }

    @Override
    public String getLastName() throws RemoteException {
        return lastName;
    }

    @Override
    public String getPassport() throws RemoteException {
        return passport;
    }

}
