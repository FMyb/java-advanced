#!/bin/bash
./compile.sh
cd ../out/production/Bank/
java -jar ../../../lib/junit-platform-console-standalone-1.7.2.jar -cp . -c info.kgeorgiy.ja.ilyin.bank.BankTests