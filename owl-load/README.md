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

Environment variables:
```text
OWL_LOAD_USERNAME: Optional username for authenticating the SPARQL endpoint.
OWL_LOAD_PASSWORD: Optional password for authenticating the SPARQL endpoint.
```

Args:
```
-c | --catalog-path path/to/owl/catalog.xml             [Required]
-e | --endpoint-url http://fusekiURL/dataset            [Required]
-f | --file-extensions extension                        [Optional, default: -f ttl -f owl], options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
-i | --iri <IRI>                                        [Required, at least one]
--default                                               [Optional, if specified, load data to the default graph instead of to named graphs]
```
Note: The dataset (database) must have been created prior to execution

## Run as Gradle Task

This is an incremental task; Gradle will determine whether to run this task
if any of the properties changed in values.

There are two ways to use this task:

### OwlLoad depends on Fuseki

This mode is recommended when Fuseki will be managed via the Gradle tasks (start/stop)Fuseki. In this mode, Gradle will take care of starting Fuseki if needed before loading owl files. In particular, if Fuseki was previously stopped, then Gradle will start Fuseki and load owl files.

In the gradle script below, the dependency between load and fuseki is conveyed via the `inferredTaskDependency` property:

```
buildscript {
	repositories {
  		mavenCentral()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-load-gradle:+'
	}
}

task startFuseki(type: io.opencaesar.owl.fuseki.StartFusekiTask) {
	configurationPath = file('path/to/.fuseki.ttl')
	outputFolderPath = file('path/to/output/folder') // with webui, there must be a 'webapp' subfolder for the Fuseki UI
	webui = true
}
task owlLoad(type:io.opencaesar.owl.load.OwlLoadTask) {
	inferredTaskDependency = startFuseki
	catalogPath = file('path/to/catalog.xml') [Required]
	endpointURL = 'url-of-sparql-endpoint' [Required]
	authenticationUsername = '...' [Optional]
	authenticationPassword = '...' [Optional]
    fileExtensions = ['owl', 'ttl'] [Optional, default=['owl', 'ttl'], options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
    iris = ['iri1',...] [One or more]
    loadToDefaultGraph = true|false [Optional, default=false]
}               
```

### OwlLoad is decoupled from Fuseki

This more is useful when Fuseki may be started either via a Gradle task or externally, e.g. with a local installation downloaded from https://jena.apache.org/
Note that in this mode, Gradle will not execute OwlLoad if Fuseki has been restarted because Gradle is unaware of the dependencies between these tasks.

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
	authenticationUsername = '...' [Optional]
	authenticationPassword = '...' [Optional]
    fileExtensions = ['owl', 'ttl'] [Optional, default=['owl', 'ttl'], options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
    iris = ['iri1',...] [One or more]
}               
```
