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
--webui                                                 [Optional]
```

## Run as Gradle Task
```
buildscript {
	repositories {
  		mavenCentral()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-fuseki-gradle:+'
	}
}
task startFuseki(type: io.opencaesar.owl.fuseki.StartFusekiTask) {
	configurationPath = file('path/to/.fuseki.ttl')
	outputFolderPath = file('path/to/output/folder') // with webui, there must be a 'webapp' subfolder for the Fuseki UI
	webui = true
}
task stopFuseki(type: io.opencaesar.owl.fuseki.StopFusekiTask) {
	outputFolderPath = file('path/to/output/folder')
}

```

For starting Fuseki with the Web UI, the output folder must have a proper 'webapp' subfolder.
The simplest way to get such a folder is to 
[download the Fuseki UI](https://jena.apache.org/documentation/fuseki2/#download-fuseki) 
application and copy its 'webapp' folder to the desired output folder.
