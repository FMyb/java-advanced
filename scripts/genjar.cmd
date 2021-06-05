MD info.kgeorgiy.ja.ilyin.implementor\info\kgeorgiy\ja\ilyin\implementor
Xcopy ..\java-solutions\info\kgeorgiy\ja\ilyin\implementor\Implementor.java info.kgeorgiy.ja.ilyin.implementor\info\kgeorgiy\ja\ilyin\implementor
Xcopy ..\java-solutions\info\kgeorgiy\ja\ilyin\implementor\module-info.java info.kgeorgiy.ja.ilyin.implementor
javac -d . -p ..\..\java-advanced-2021\artifacts\;..\..\java-advanced-2021\lib\ --module-source-path .;..\..\java-advanced-2021\modules --module info.kgeorgiy.ja.ilyin.implementor
cd info.kgeorgiy.ja.ilyin.implementor
jar -c -f Implementor.jar -m ..\MANIFEST.MF .
copy Implementor.jar ..
cd ..
rmdir /s info.kgeorgiy.ja.ilyin.implementor