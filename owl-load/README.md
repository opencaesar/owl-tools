# OWL Load

[![Release](https://img.shields.io/github/v/tag/opencaesar/owl-tools?label=release)](https://github.com/opencaesar/owl-tools/releases/latest)

A tool to clear and load a Fuseki dataset with the transitive closure of all imported ontologies from a set of Catalog-resolved IRIs.

## Run as CLI
MacOS/Linux:
```
./gradlew owl-load:run --args="..."
```
Windows:
```
gradlew.bat owl-load:run --args="..."
```
Args:
```
-c | --catalog-path path/to/owl/catalog.xml             [Required]
-e | --endpoint-url http://fusekiURL/dataset            [Required]
-f | --file-extensions extension                        [Optional, default: -f ttl -f owl], options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
-i | --iri <IRI>                                        [Required, at least one]
```
Note: The dataset (database) must have been created prior to execution

## Run as Gradle Task

This is an incremental task; Gradle will determine whether to run this task
if any of the properties changed in values.

```
buildscript {
	repositories {
  		mavenCentral()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-load-gradle:+'
	}
}
task owlLoad(type:io.opencaesar.owl.load.OwlLoadTask) {
	catalogPath = file('path/to/catalog.xml') [Required]
	endpointURL = 'url-of-sparql-endpoint' [Required]
    fileExtensions = ['owl', 'ttl'] [Optional, default=['owl', 'ttl'], options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
    iris = ['iri1',...] [One or more]
}               
```