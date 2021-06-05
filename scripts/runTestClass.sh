#!/bin/bash
./compile.sh
cd ../out/production/Bank/
cp ../../../lib/junit-platform-console-standalone-1.7.2.jar .
jar xf junit-platform-console-standalone-1.7.2.jar
java info.kgeorgiy.ja.ilyin.bank.RunTests