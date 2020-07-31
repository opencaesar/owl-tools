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
--catalog-path or -c path/to/owl/catalog.xml
--input-iri or -i input-ontology-iri
--spec or -s 'output-ontology-iri=ALL_SUBCLASS'
--spec or -s 'output-ontology-iri=INVERSE_PROPERTY ALL_SUBPROPERTY'
--spec or -s 'output-ontology-iri=ALL_INSTANCE DATA_PROPERTY_VALUE OBJECT_PROPERTY_VALUE SAME_AS'
--format or -f RDFXML [Optional, default is RDFXML, other values include: TTL, RDF, XML, N3, NTriples]
--remove-unsats or -ru [Optional]
--remove-backbone or -rb [Optional]
--backbone-iri or -b http://opencaesar.io/oml [Optional]
--indent or -n 2 [Optional, default is 2]
--debug or -d [Optional]
--help or -h [Optional]
```

## [Run as Gradle Task](../owl-reason-gradle/README.md)
