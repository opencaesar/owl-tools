# OWL Load

[![Release](https://img.shields.io/github/v/tag/opencaesar/owl-tools?label=release)](https://github.com/opencaesar/owl-tools/releases/latest)

A tool to load a dataset to a SPARQL endpoint from a set of Catalog-resolved IRIs.

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
-e  | --endpoint-url http://fusekiURL/dataset            [Required]
-q  | --query-service sparql                             [Optional, default is 'sparql')
-u  | --username <env-var>                               [Optional, used for authentication to endpoint]
-p  | --password <env-var>                               [Optional, used for authentication to endpoint]
-c  | --catalog-path path/to/owl/catalog.xml             [Required]
-f  | --file-extensions extension                        [Optional, default: -f ttl -f owl], options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
-i  | --iri <IRI>                                        [Required only if '-ip` is not used]
-ip | --iris-path path/to/iris.log                       [Required onlyy if '-i' is not used]
-df | --default                                          [Optional, if specified, load data to the default graph instead of to named graphs]
```
Note: The dataset (database) must have been created in the server prior to executing OwlLoad

Note: `-ip` is preferred to `-i` because it avoids `OwlLoad` loading the dataset just to calculate the used iri closure. The `-ip` file can be produced by the `OwlReason` task.

Note: `-u` and `-p` should be names of env vars with credentials to access the endpoint.

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
	endpointURL = 'url-of-sparql-endpoint' [Required]
	queryService = 'sparql' [Optional, default='sparql']
	authenticationUsername = '...' [Optional]
	authenticationPassword = '...' [Optional]
	catalogPath = file('path/to/catalog.xml') [Required]
    fileExtensions = ['owl', 'ttl'] [Optional, default=['owl', 'ttl'], options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
    iris = ['iri1',...] [Required only if 'irisPath' is not set]
    irisPath = file('path/to/iris.log') [Required only if 'iris' is not set]
    loadToDefaultGraph = true|false [Optional, default=false]
    // This is a gradle task only flag
    incremental = true [Optional, default=false]
}               
```

Note: the `incremental` flag makes the task only loading files if it determined them to be changed. 

### OwlLoad relation to StartFuseki

While `OwlLoad` is independent of `StartFuseki` tasks, the user may find it
beneficial to make `OwlLoad`'s inputs depend on `StartFuseki`'s outputs, especially
when `incremental` mode is enabled.  This will cause `OwlLoad` to detect a Fuseki 
restart and fully rerun trying to load all files. See below how to set this up.

```
buildscript {
	repositories {
  		mavenCentral()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-load-gradle:+'
		classpath 'io.opencaesar.owl:owl-fuseki-gradle:+'
	}
}
task owlLoad(type:io.opencaesar.owl.load.OwlLoadTask) {
	inputs.files(startFuseki.output.files) // rerun when fuseki restarts
	....
	incremental = true
}               
```
