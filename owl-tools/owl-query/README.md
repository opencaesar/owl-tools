# OWL Query

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
--query, -q path/to/queries/query.sparql (Required)
--endpoint, -e http://fusekiURL/databaseName (Required)
--result, -r path/to/result (Required) (If the result file previously exists, it will be overwritten)
--format, -f xml (Optional; default value is xml)
    - Can select xml, json, csv, n3, ttl, n-triple, or tsv
    - Certain formats are supported based on query type     
```