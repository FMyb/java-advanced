package info.kgeorgiy.ja.ilyin.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, Map<String, Account>> personsAccounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Person> persons = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public Person createPerson(String firstName, String lastName, String passport) throws RemoteException {
        Person person = new RemotePerson(firstName, lastName, passport, this);
        if (persons.putIfAbsent(passport, person) == null) {
            System.out.println("Creating person: passport = " + passport);
            personsAccounts.put(passport, new ConcurrentHashMap<>());
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            if (checkPersonData(firstName, lastName, passport)) {
                return getPerson(passport, RemotePerson.class);
            } else {
                return null;
            }
        }
    }

    @Override
    public Person getPerson(String passport, Class<? extends Person> personClass) throws RemoteException {
        System.out.println("Get person: passport = " + passport + " class = " + personClass.getSimpleName());
        Person person = persons.get(passport);
        if (person == null) {
            System.err.println("Don't have person in bank (passport = " + passport + ")");
            return null;
        } else if (personClass == RemotePerson.class) {
            return person;
        } else {
            return new LocalPerson(person.getFirstName(), person.getLastName(), passport, this);
        }
    }

    @Override
    public Set<String> getAccountsByPassport(String passport) throws RemoteException {
        System.out.println("Get accounts by passport: passport = " + passport);
        return Set.copyOf(personsAccounts.getOrDefault(passport, new HashMap<>()).keySet());
    }

    @Override
    public boolean checkPersonData(String firstName, String lastName, String passport) throws RemoteException {
        System.out.println("Check person data: passport = " + passport + " firstName = " + firstName + " lastName = " + lastName);
        Person person = persons.get(passport);
        return person != null && person.getFirstName().equals(firstName) && person.getLastName().equals(lastName);
    }

    @Override
    public Account createAccount(String accountId, String passport) throws RemoteException {
        Person person = persons.get(passport);
        if (person == null) {
            System.err.println("Don't find person: passport = " + passport);
            return null;
        }
        Map<String, Account> accounts = personsAccounts.get(passport);
        Account account = new RemoteAccount(accountId, passport);
        if (accounts.putIfAbsent(accountId, account) == null) {
            System.out.println("Creating account: id =  " + passport + ":" + accountId);
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccount(accountId, passport);
        }
    }

    @Override
    public Account getAccount(String accountId, String passport) throws RemoteException {
        System.out.println("Get account: accountId = " + accountId + " passport = " + passport);
        return personsAccounts.getOrDefault(passport, Map.of()).get(accountId);
    }
}