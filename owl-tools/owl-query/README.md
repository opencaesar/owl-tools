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

## Run as Gradle Task
```
buildscript {
	repositories {
		maven { url 'https://dl.bintray.com/opencaesar/owl-tools' }
  		mavenCentral()
		jcenter()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-query-gradle:+'
	}
}
task owlQuery(type:io.opencaesar.owl.query.OwlQueryTask) {
	endpointURL = 'url-of-sparql-endpoint' [Required]
	queryPath = file('path/to/query.sparql') [Required, path to file or folder]
	resultPath = file('path/to/result/folder') [Required]
    format = 'xml' [Optional, default is xml, other options: json, csv, n3, ttl, n-triple, or tsv]
}               
```