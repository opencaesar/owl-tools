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
-c, --catalog-path PATH				Path/to/owl/catalog.xml [required]
-i, --input-ontology-iri IRI			Iri of the root ontology to analyze [required]
-s, --spec IRI=ALGORITHM|ALGORITHM... 		Iri of an output ontology to hold the inferred entailments. Algorithms: ALL_SUBCLASS, INVERSE_PROPERTY, ALL_SUBPROPERTY, ALL_INSTANCE, DATA_PROPERTY_VALUE, OBJECT_PROPERTY_VALUE, SAME_AS [required, multiple]
-if, --input-file-extension EXTENSION 		Extensions: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss [optional, multiple, default=owl]
-of, --output-file-extension EXTENSION		Extension: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss [optional, default=ttl]
-ef, --explanation-format FORMAT		Format: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss [optional, default=owl]
-ru, --remove-unsats BOOLEAN 			Whether to remove entailments due to unsatisfiability [optional, default=true)
-rb, --remove-backbone BOOLEAN 			Whether to remove axioms on the backhone from entailments [optional, default=true]
-b, --backbone-iri IRI				Iris to ignore [optional, default=http://opencaesar.io/oml]
-n, --indent NUMBER 				Number of spaces to indent by [optional, default=2]
-r, --report-path PATH				Path/to/reasoning.xml [required]
```

Note: the | char separating algorithms in the `spec` argument is not a logical OR; it is just a list delimiter.

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
	catalogPath 		= file('path/to/owl/catalog.xml') [required]
	inputOntologyIri 	= 'root/ontology/iri' [required]
	specs 			= [ 'output/ontology/iri=algorithm1|algorithm2...' ] [required, multiple, algorithms: ALL_SUBCLASS, INVERSE_PROPERTY, ALL_SUBPROPERTY, ALL_INSTANCE, DATA_PROPERTY_VALUE, OBJECT_PROPERTY_VALUE, SAME_AS]
	inputFileExtensions 	= ['extension'] [optional, multiple, default=owl, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
	outputFileExtension 	= 'extension' [optional, default=ttl, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
	explanationFormat 	= 'fss' [optional, default=owl, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
	removeUnsats 		= boolean [optional, default=true]
	removeBackbone 		= boolean [optional, default=true]
	backboneIri 		= 'backbone-iri' [optional, default=http://opencaesar.io/oml]
	indent 			= number [optional, default=2]
	reportPath		= file('path/to/reasoning.xml') [required]
}
```

Note: the | char separating algorithms in the `spec` argument is not a logical OR; it is just a list delimiter.
