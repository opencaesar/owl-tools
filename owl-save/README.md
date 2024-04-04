# OWL Save

[![Release](https://img.shields.io/github/v/tag/opencaesar/owl-tools?label=release)](https://github.com/opencaesar/owl-tools/releases/latest)

A tool to save a dataset from a SPARQL endpoint to an OWL catalog.

## Run as CLI
MacOS/Linux:
```
./gradlew owl-save:run --args="..."
```
Windows:
```
gradlew.bat owl-save:run --args="..."
```

Args:
```
-e  | --endpoint-url http://fusekiURL/dataset            [Required]
-u  | --username <env-var>                               [Optional, used for authentication to endpoint]
-p  | --password <env-var>                               [Optional, used for authentication to endpoint]
-c  | --catalog-path path/to/owl/catalog.xml             [Required]
-f  | --file-extension                        			 [Optional, default: ttl, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld]
```
Note: The dataset (database) must exist at the endpoint prior to executing OwlSave

Note: `-u` and `-p` should be names of env vars with credentials to access the endpoint.

## Run as Gradle Task

```
buildscript {
	repositories {
  		mavenCentral()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-save-gradle:+'
	}
}

task owlSave(type:io.opencaesar.owl.save.OwlSaveTask) {
	endpointURL = 'url-of-sparql-endpoint' [Required]
	authenticationUsername = '...' [Optional]
	authenticationPassword = '...' [Optional]
	catalogPath = file('path/to/catalog.xml') [Required]
 	fileExtension = 'ttl' [Optional, default= 'ttl', options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld]
}               
```