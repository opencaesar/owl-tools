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
--iri | -i <IRI>                                        [Required, at least one]
--catalog-path | -c path/to/owl/catalog.xml             [Required]
--endpoint-url | -e http://fusekiURL/dataset            [Required]
--file-extensions | -f extension                       [Optional, default: -f ttl -f owl]
```
Note: The dataset (database) must have been created prior to execution

## Run as Gradle Task
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
    fileExtensions = ['owl', 'ttl'] [Optional, default is owl]
}               
```