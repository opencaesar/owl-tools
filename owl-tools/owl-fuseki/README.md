# OWL Load

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-fuseki/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-fuseki/_latestVersion)

A tool to start and stop a UU-less Fuseki server with a given configuration file

## Run as CLI
MacOS/Linux:
```
cd owl-tools
./gradlew owl-fuseki:run --args="..."
```
Windows:
```
cd owl-tools
gradlew.bat owl-fuseki:run --args="..."
```
Args:
```
--command | -c start|stop                               [Required]
--configurationPath | -g path/to/.fuseki.ttl            [Required]
--outputFolderPath | -i path/to/output/folder  		    [Required]
```

## Run as Gradle Task
```
buildscript {
	repositories {
		maven { url 'https://dl.bintray.com/opencaesar/owl-tools' }
  		mavenCentral()
		jcenter()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-fuseki-gradle:+'
	}
}
task startFuseki(type: io.opencaesar.owl.fuseki.StartFusekiTask) {
	configurationPath = file('path/to/.fuseki.ttl')
	outputFolderPath = file('path/to/output/folder')
}
task stopFuseki(type: io.opencaesar.owl.fuseki.StopFusekiTask) {
	outputFolderPath = file('path/to/output/folder')
}

```