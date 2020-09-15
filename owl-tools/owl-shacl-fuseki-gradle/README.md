# OWL Shacl

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-shacl-fuseki-gradle/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-shacl-fuseki-gradle/_latestVersion)

A tool to execute a SHACL query on a given Fuseki+SHACL endpoint. 

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