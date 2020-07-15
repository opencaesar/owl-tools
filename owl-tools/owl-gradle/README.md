# Run from Gradle
Install owl tools into your local maven repo
MacOS/Linux:
```
    cd owl-tools
    ./gradlew install
```
Windows:
```
    cd owl-tools
    gradlew.bat install
```
In a build.gralde script add the following: 
```
buildscript {
	repositories {
		mavenLocal()
		jcenter()
	}
	dependencies {
		classpath("io.opencaesar.owl:owl-gradle:+")
	}
}

apply plugin: 'io.opencaesar.owl.load'
apply plugin: 'io.opencaesar.owl.query'

owlLoad {
	endpoint = 'http://sparqlURL/datasetName'
	catalogPath = 'path/to/owl/catalog.xml'
	fileExt = 'desiredFileExt,Separated,by,commas'
}

owlQuery {
	endpoint = 'http://sparqlURL/datasetName'
	queriesPath = 'queries/file.sparql'
	resultPath = 'result/directory'
	formatType = 'xml'
}
```
### To execute the plugins 
MacOS/Linux:
```
    ./gradlew loadOwl 
    ./gradlew queryOwl
```
Windows:
```
    gradlew.bat loadOwl
    gradlew.bat queryOwl
```
### Adding it to a gradle task example
In a build.gradle script: 
```
task execute {
	dependsOn loadOwl
	dependsOn queryOwl
}
```
MacOS/Linux:
```
    ./gradlew execute
```
Windows:
```
    gradlew.bat execute
```