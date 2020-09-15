# OWL Query

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-query-gradle/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-query-gradle/_latestVersion)

A tool to execute a query on a given SPARQL endpoint. 

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