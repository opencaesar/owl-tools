# OWL Reason

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-reason/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-reason/_latestVersion)

A tool to analyze an OWL dataset for satisfiability and consistency with an OWL2-DL reasoner

## Run as CLI

MacOS/Linux:
```
cd owl-adapter
./gradlew owl-reason:run --args="..."
```
Windows:
```
cd owl-adapter
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

## [Run as Gradle Task](../owl-reason-gradle/README.md)
