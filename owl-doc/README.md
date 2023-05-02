# OWL Doc

[![Release](https://img.shields.io/github/v/tag/opencaesar/owl-tools?label=release)](https://github.com/opencaesar/owl-tools/releases/latest)

A tool to generate documentation for an OWL dataset

## Run as CLI

MacOS/Linux:

```
./gradlew owl-doc:run --args="..."
```
Windows:

```
gradlew.bat owl-doc:run --args="..."
```
Args:

```
  * --input-catalog-path, -c
      path to the input OWL catalog
    --input-catalog-title, -t
      Title of OML input catalog
      Default: OWL Ontology Index
    --input-catalog-version, -v
      Version of OML input catalog
      Default: <empty string>
  * --input-ontology-iri, -i
      iri of input OWL ontology
      Default: []
    --input-file-extension, -e
      input file extension (options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss)
      Default: [owl, ttl]
  * --output-folder-path, -o
      Path of Bikeshed output folder
      Default: .
    --output-case-sensitive, -s
      Whether output paths are case sensitive
    --debug, -d
      Shows debug logging statements
      Default: false
    --help, -h
      Displays summary of options
```

## Run as Gradle Task

```
buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-doc-gradle:+'
	}
}
task owlDoc(type:io.opencaesar.owl.doc.OwlDocTask) {
	inputCatalogPath = file('path/to/input/oml/catalog') [Required]
	inputCatalogTitle = project.title [Optional]
	inputCatalogVersion = project.version [Optional]
	inputOntologyIris = [ 'http://root/ontology/classes', .. ]  [Required]
	inputFileExtensions = [ 'owl', 'ttl' ] [Optional, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]	
	outputFolderPath = file('path/to/output/bikeshed/folder') [Required]
	outputCaseSensitive = org.gradle.internal.os.OperatingSystem.current().isLinux() [Optional]
}
```