# OWL Load

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-load-gradle/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-load-gradle/_latestVersion)

A tool to load OWL files to a SPARQL end point. 

## Run as Gradle Task

```
buildscript {
	repositories {
		maven { url 'https://dl.bintray.com/opencaesar/owl-tools' }
  		mavenCentral()
		jcenter()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-load-gradle:+'
	}
}
task owlLoad(type:io.opencaesar.owl.load.OwlLoadTask) {
	catalogPath = file('path/to/catalog.xml') [Required]
	endpointURL = 'url-of-sparql-endpoint' [Required]
    fileExtensions = ['owl', 'ttl'] [Optional, default is owl]
}               
```