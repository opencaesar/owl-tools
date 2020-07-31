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
--endpoint-url or -e http://fusekiURL/databaseName [Required]
--query-path or -q path/to/query.sparql [Required]
--result-path or -r path/to/result.frame [Required]
--format or -f xml [Optional; default value is xml]
    - forats: xml, json, csv, n3, ttl, n-triple, or tsv
    - Supported formats depend on the query type
--debug or -d
--help or -h
```

## [Run as Gradle Task](../owl-query-gradle/README.md)