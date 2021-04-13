# OWL Diff

[![Release](https://img.shields.io/github/v/tag/opencaesar/owl-tools?label=release)](https://github.com/opencaesar/owl-tools/releases/latest)

A tool to produce a difference report between two OWL datasets

## Run as CLI

MacOS/Linux:
```
./gradlew owl-diff:run --args="..."
```
Windows:
```
gradlew.bat owl-diff:run --args="..."
```
Args:
```
--catalog1 | -c1 path/to/first/owl/catalog.xml [Required]
--catalog2 | -c2 path/to/second/owl/catalog.xml [Required]
--ignore | -i comma-separated-partial-iris-to-ignore [Optional]
```
