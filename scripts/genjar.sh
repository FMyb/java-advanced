#!/bin/bash

mkdir tmp
cd tmp
mkdir -p info.kgeorgiy.ja.ilyin.implementor/info/kgeorgiy/ja/ilyin/implementor
cp ../../java-solutions/info/kgeorgiy/ja/ilyin/implementor/Implementor.java \
 info.kgeorgiy.ja.ilyin.implementor/info/kgeorgiy/ja/ilyin/implementor
cp ../module-info.java info.kgeorgiy.ja.ilyin.implementor
# :NOTE: clean up after you are done, this creates a ton of unneeded (later) files
javac -d . -p ../../../java-advanced-2021/artifacts:../../../java-advanced-2021/lib --module-source-path ./:../../../java-advanced-2021/modules \
--module info.kgeorgiy.ja.ilyin.implementor
cd info.kgeorgiy.ja.ilyin.implementor
jar cfm Implementor.jar ../../MANIFEST.MF . ../info.kgeorgiy.java.advanced.base ../info.kgeorgiy.java.advanced.implementor
cp Implementor.jar ../..
cd ../..
rm -r tmp
