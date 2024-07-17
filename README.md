# OWL Tools

[![Build Status](https://github.com/opencaesar/owl-tools/actions/workflows/ci.yml/badge.svg)](https://github.com/opencaesar/owl-tools/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/opencaesar/owl-tools?label=Release)](https://github.com/opencaesar/owl-tools/releases/latest)

A set of OWL based analysis tools

## Clone
```
    git clone https://github.com/opencaesar/owl-tools.git
    cd owl-tools
```
      
## Build
Requirements: JDK 17+
```
    ./gradlew build
```

## [OWL Close World](owl-close-world/README.md)

A library of different algorithms to close the world on OWL ontologies

## [OWL Diff](owl-diff/README.md)

A tool to produce a difference report between two OWL datasets

## [OWL Doc](owl-doc/README.md)

A tool to generate documentation for an OWL dataset.

## [OWL Fuseki](owl-fuseki/README.md)

A tool to start and stop a UI-less Fuseki server with a given configuration file

## [OWL Reason](owl-reason/README.md)

A tool to analyze an OWL dataset for satisfiability and consistency with an OWL2-DL reasoner

## [OWL Load](owl-load/README.md)

A tool to load a catalog of OWL ontologies to a database endpoint

## [OWL Query](owl-query/README.md)

A tool to send a SPARQL query to a database endpoint and save the result

## [OWL Shacl Fuseki](owl-shacl-fuseki/README.md)

A tool to send SHACL queries to a Fuseki database endpoint and save the result
