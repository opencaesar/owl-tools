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

It is important to define the Fuseki dependencies on a dedicated Gradle configuration like `fuseki` shown below.

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
	 
    fuseki 'org.apache.jena:jena-fuseki-fulljar:4.6.0'
    fuseki 'org.apache.jena:jena-fuseki-war:4.6.0'
}

task startFuseki(type: io.opencaesar.owl.fuseki.StartFusekiTask) {
	classpath = project.files().from(configurations.getByName("fuseki").resolve().toList())
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
