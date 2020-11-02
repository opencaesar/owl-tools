# OWL Shacl Fuseki

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-shacl-fuseki/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-shacl-fuseki/_latestVersion)

A tool to execute a SHACL query on a given Fuseki+SHACL endpoint. 

## Run as CLI

MacOS/Linux:
```
cd owl-tools
./gradlew owl-shacl-fuseki:run --args="..."
```
Windows:
```
cd owl-tools
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
		maven { url 'https://dl.bintray.com/opencaesar/owl-tools' }
  		mavenCentral()
		jcenter()
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