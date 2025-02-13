# OWL Shacl

[![Release](https://img.shields.io/github/v/tag/opencaesar/owl-tools?label=release)](https://github.com/opencaesar/owl-tools/releases/latest)

A tool to execute SHACL rules on a given dataset. 

## Run as CLI

MacOS/Linux:
```
./gradlew owl-shacl:run --args="..."
```
Windows:
```
gradlew.bat owl-shacl:run --args="..."
```
Args:
```
-e | --endpoint-url http://URL/databaseName [Required]
-s | --shacl-service shacl [Optional, default is 'shacl')
-q | --query-path path/to/queries.shacl [Required]
       (Accepts either a .shacl file or a directory that will be searched for .shacl files)
-r | --result-path path/to/result/folder [Required]
    (Supported formats depend on the query type)
```

## Run as Gradle Task

This is an incremental task; Gradle will determine whether to run this task
if any of the properties changed in values.

```
buildscript {
	repositories {
  		mavenCentral()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-shacl-gradle:+'
	}
}
task owlShacl(type:io.opencaesar.owl.query.OwlShaclßTask) {
	endpointURL = 'url-of-sparql-endpoint' [Required]
	shaclService = 'shacl' [Optional, default='shacl']
	queryPath = file('path/to/queries.shacl') [Required, path to file or folder]
	resultPath = file('path/to/result/folder') [Required]
}               
```
