# OWL Shacl Fuseki

[![Release](https://img.shields.io/github/v/tag/opencaesar/owl-tools?label=release)](https://github.com/opencaesar/owl-tools/releases/latest)

A tool to execute a SHACL query on a given Fuseki+SHACL endpoint. 

## Run as CLI

MacOS/Linux:
```
./gradlew owl-shacl-fuseki:run --args="..."
```
Windows:
```
gradlew.bat owl-shacl-fuseki:run --args="..."
```
Args:
```
--endpoint-url | -e http://fusekiURL/databaseName [Required]
--query-path | -q path/to/queries.shacl [Required]
    (Accepts either a .shacl file or a directory that will be searched for .shacl files)
--result-path | -r path/to/result/folder [Required]
--format | -f xml [Optional; default value is xml]
    (forats: xml, json, csv, n3, ttl, n-triple, or tsv)
    (Supported formats depend on the query type)
```

## Run as Gradle Task
```
buildscript {
	repositories {
  		mavenCentral()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-shacl-fuseki-gradle:+'
	}
}
task owlShaclFuseki(type:io.opencaesar.owl.query.OwlShaclFusekiTask) {
	endpointURL = 'url-of-sparql-endpoint' [Required]
	queryPath = file('path/to/queries.shacl') [Required, path to file or folder]
	resultPath = file('path/to/result/folder') [Required]
}               
```