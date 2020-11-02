# OWL Load

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-load/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-load/_latestVersion)

A tool to clear and load a Fuseki dataset with the transitive closure of all imported ontologies from a set of Catalog-resolved IRIs.

## Run as CLI
MacOS/Linux:
```
cd owl-tools
./gradlew owl-load:run --args="..."
```
Windows:
```
cd owl-tools
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