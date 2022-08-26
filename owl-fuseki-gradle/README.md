# OWL Fuseki

[![Release](https://img.shields.io/github/v/tag/opencaesar/owl-tools?label=release)](https://github.com/opencaesar/owl-tools/releases/latest)

A Gradle task to start and stop a UI or UI-less Fuseki server with a given configuration file.

## Run as CLI

Outside of Gradle:
- download Apache Jena Fuseki: https://jena.apache.org/download/index.cgi
- start Fuseki: https://jena.apache.org/documentation/fuseki2/fuseki-webapp.html

## Run as Gradle Task

```
repositories {
    mavenLocal() 
    mavenCentral()
}

configurations {
    fuseki
}

dependencies {
	implementation 'io.opencaesar.owl:owl-fuseki-gradle:+'
	 
    fuseki 'org.apache.jena:jena-fuseki-server:4.5.0'
    fuseki 'org.apache.jena:jena-fuseki-war:4.5.0'
}

task startFuseki(type: io.opencaesar.owl.fuseki.StartFusekiTask) {
	configurationPath = file('path/to/.fuseki.ttl')
	port = 3030
	outputFolderPath = file('path/to/output/folder') // with webui, a 'webapp' subfolder will be created
	webUI = true // optional
	maxPings = 10 // optional
}

task stopFuseki(type: io.opencaesar.owl.fuseki.StopFusekiTask) {
	outputFolderPath = file('path/to/output/folder')
}
```
