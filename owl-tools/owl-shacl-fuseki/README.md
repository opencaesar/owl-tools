# OWL Query

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-shacl-fuseki/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-shacl-fuseki/_latestVersion)

A tool to execute a SHACL query on a given Fuseki+SHACL endpoint. 

## Run as CLI

MacOS/Linux:
```
cd owl-tools
./gradlew owl-shacl-query:run --args="..."
```
Windows:
```
cd owl-tools
gradlew.bat owl-shacl-query:run --args="..."
```
Args:
```
--endpoint-url | -e http://fusekiURL/databaseName [Required]
--query-path | -q path/to/query.sparql [Required]
    (Accepts either a .sparql file or a directory that will be searched for .sparql files)
--result-path | -r path/to/result.frame [Required]
--format | -f xml [Optional; default value is xml]
    (forats: xml, json, csv, n3, ttl, n-triple, or tsv)
    (Supported formats depend on the query type)
```

## [Run as Gradle Task](../owl-shacl-query-gradle/README.md)