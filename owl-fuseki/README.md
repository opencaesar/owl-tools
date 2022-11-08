# OWL Fuseki

[![Release](https://img.shields.io/github/v/tag/opencaesar/owl-tools?label=release)](https://github.com/opencaesar/owl-tools/releases/latest)

A tool to start and stop a UI or UI-less Fuseki server with a given configuration file

## Run as CLI
MacOS/Linux:
```
./gradlew owl-fuseki:run --args="..."
```
Windows:
```
gradlew.bat owl-fuseki:run --args="..."
```
Args:
```
--command | -c start|stop								[Required]
--configurationPath | -g path/to/.fuseki.ttl			[Required]
--outputFolderPath | -i path/to/output/folder			[Required]
--webui | -ui                                           [Optional]
--max-pings | -p                                        [Optional]
```

## Run as Gradle Task

This is an incremental task; Gradle will determine whether to run this task
if any of the properties changed in values.

Example usage:

```
repositories {
    mavenLocal() 
    mavenCentral()
}

dependencies {
	classpath 'io.opencaesar.owl:owl-fuseki-gradle:3.+'
}

task startFuseki(type: io.opencaesar.owl.fuseki.StartFusekiTask) {
	configurationPath = file('path/to/.fuseki.ttl')
	port = 3030
	outputFolderPath = file('path/to/output/folder') // with webui, a 'webapp' subfolder will be created
	webUI = true // optional, default is false.
	maxPings = 10 // optional
	remoteRepositoryURL = 'https://internal/artifactory/maven-remote-virtual' // if unset, defaults to https://repo.maven.apache.org/maven2/
}

task stopFuseki(type: io.opencaesar.owl.fuseki.StopFusekiTask) {
	outputFolderPath = file('path/to/output/folder')
}
```

Example of `.fuseki.ttl`:

```turtle
@prefix fuseki:  <http://jena.apache.org/fuseki#> .
@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .
@prefix tdb:     <http://jena.hpl.hp.com/2008/tdb#> .
@prefix ja:      <http://jena.hpl.hp.com/2005/11/Assembler#> .
@prefix :        <#> .

[] rdf:type fuseki:Server .

<#service> rdf:type fuseki:Service ;
    rdfs:label          "Oml Template" ;												# Human readable label for dataset
    fuseki:name         "oml-template" ;												# Name of the dataset in the endpoint url
    fuseki:serviceReadWriteGraphStore "data" ;											# SPARQL Graph store protocol (read and write)
    fuseki:endpoint 	[ fuseki:operation fuseki:query ;	fuseki:name "sparql"  ] ;	# SPARQL query service
    fuseki:endpoint 	[ fuseki:operation fuseki:shacl ;	fuseki:name "shacl" ] ;		# SHACL query service
    fuseki:dataset      <#dataset> .

# In memory TDB with union graph.
<#dataset> rdf:type   tdb:DatasetTDB ;
  tdb:location "--mem--" ;
  # Query timeout on this dataset (1s, 1000 milliseconds)
  ja:context [ ja:cxtName "arq:queryTimeout" ; ja:cxtValue "1000" ] ;
  # Make the default graph be the union of all named graphs.
  tdb:unionDefaultGraph true .
```