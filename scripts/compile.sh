#! /bin/bash
cd ..
#cd ..
rm -r out 2>/dev/null
mkdir out
cd out
mkdir production
cd production
mkdir Bank
cd Bank
cd ../../../
cd java-solutions/info/kgeorgiy/ja/ilyin/bank
javac -classpath ../../../../../../lib/junit-platform-console-standalone-1.7.2.jar -d ../../../../../../out/production/Bank/ *.java