# OWL Reason

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-reason-gradle/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-reason-gradle/_latestVersion)

A Gradle task to analyze an OWL dataset for satisfiability and consistency with an OWL2-DL reasoner

## Run as Gradle Task

buildscript {
	repositories {
		maven { url 'https://dl.bintray.com/opencaesar/owl-tools' }
  		mavenCentral()
		jcenter()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-reason-gradle:+'
	}
}
task owlReason(type:io.opencaesar.owl.reason.OwlReasonTask) {
	catalogPath = file('path/to/owl/catalog.xml') [Required]
	inputOntologyIris = ['input-ontology-iri'] [Required]
	specs = [
		'output-ontology-iri=ALL_SUBCLASS',
		'output-ontology-iri=INVERSE_PROPERTY ALL_SUBPROPERTY',
		'output-ontology-iri=ALL_INSTANCE DATA_PROPERTY_VALUE OBJECT_PROPERTY_VALUE SAME_AS'
	] [Required]
	format = TTL [Optional]
	removeUnsats = true [Optional]
	removeBackbone = true [Optional]
	backboneIri = 'http://opencaesar.io/oml' [Optional]
	indent = 2 [Optional]
	debug = true [Optional]
}