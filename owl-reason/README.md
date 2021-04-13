# OWL Reason

[![Release](https://img.shields.io/github/v/tag/opencaesar/owl-tools?label=release)](https://github.com/opencaesar/owl-tools/releases/latest)

A tool to analyze an OWL dataset for satisfiability and consistency with an OWL2-DL reasoner

## Run as CLI

MacOS/Linux:
```
./gradlew owl-reason:run --args="..."
```
Windows:
```
gradlew.bat owl-reason:run --args="..."
```
Args:
```
--catalog-path | -c path/to/owl/catalog.xml
--input-ontology-iri | -i iri
--spec | -s 'output-ontology-iri=ALL_SUBCLASS'
--spec | -s 'output-ontology-iri=INVERSE_PROPERTY | ALL_SUBPROPERTY'
--spec | -s 'output-ontology-iri=ALL_INSTANCE | DATA_PROPERTY_VALUE | OBJECT_PROPERTY_VALUE | SAME_AS'
--format | -f ttl [Optional, default is ttl, other options: rdf, owl, nt, n3]
--remove-unsats | -ru [Optional]
--remove-backbone | -rb [Optional]
--backbone-iri | -b http://opencaesar.io/oml [Optional]
--indent | -n 2 [Optional, default is 2]
```

## Run as Gradle Task
```
buildscript {
	repositories {
  		mavenCentral()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-reason-gradle:+'
	}
}
task owlReason(type:io.opencaesar.owl.reason.OwlReasonTask) {
	catalogPath = file('path/to/owl/catalog.xml') [Required]
	inputOntologyIri = 'iri' [Required]
	specs = [
		'output-ontology-iri=ALL_SUBCLASS',
		'output-ontology-iri=INVERSE_PROPERTY ALL_SUBPROPERTY',
		'output-ontology-iri=ALL_INSTANCE DATA_PROPERTY_VALUE OBJECT_PROPERTY_VALUE SAME_AS'
	] [Required]
	format = 'ttl' [Optional, default is ttl, other options: rdf, owl, nt, n3]
	removeUnsats = true [Optional]
	removeBackbone = true [Optional]
	backboneIri = 'http://opencaesar.io/oml' [Optional]
	indent = 2 [Optional, default is 2]
}
```
