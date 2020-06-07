# OWL Diff

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-diff/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-diff/_latestVersion)

A tool to produce a difference report between two OWL datasets

## Run as CLI

MacOS/Linux:
```
    cd owl-adapter
    ./gradlew owl-diff:run --args="..."
```
Windows:
```
    cd owl-adapter
    gradlew.bat owl-diff:run --args="..."
```
Args:
```
-c1 path/to/first/owl/catalog.xml 
-c2 path/to/second/owl/catalog.xml
-n comma-separated-partial-iris-to-ignore
```
