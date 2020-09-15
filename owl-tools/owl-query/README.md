# OWL Query

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-query/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-query/_latestVersion)

A tool to execute a query on a given SPARQL endpoint. 

## Run as CLI

MacOS/Linux:
```
cd owl-tools
./gradlew owl-query:run --args="..."
```
Windows:
```
cd owl-tools
gradlew.bat owl-query:run --args="..."
```
Args:
```
--endpoint-url | -e http://fusekiURL/databaseName [Required]
--query-path | -q path/to/query.sparql [Required]
    (Accepts either a .sparql file or a directory that will be searched for .sparql files)
--result-path | -r path/to/result/folder [Required]
--format | -f xml [Optional; default value is xml]
    (forats: xml, json, csv, n3, ttl, n-triple, or tsv)
    (Supported formats depend on the query type)
```

## [Run as Gradle Task](../owl-query-gradle/README.md)