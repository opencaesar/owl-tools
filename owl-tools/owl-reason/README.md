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
--catalog path/to/owl/catalog.xml
--input-iri of-a-box-ontology-in-catalog
--spec 'output_iri=ALL_SUBCLASS'
--spec 'output_iri=INVERSE_PROPERTY ALL_SUBPROPERTY'
--spec 'output_iri=ALL_INSTANCE DATA_PROPERTY_VALUE OBJECT_PROPERTY_VALUE SAME_AS'
```
