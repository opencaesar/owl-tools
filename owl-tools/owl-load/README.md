# OWL Load

A tool to load RDF files into a Fuseki end point. 

## Run as CLI

MacOS/Linux:
```
    cd owl-tools
    ./gradlew owl-load:run --args="..."
```
Windows:
```
    cd owl-tools
    gradlew.bat owl-load:run --args="..."
```
Args:
```
--catalog path/to/owl/catalog.xml
--endpoint http://sparqlURL/
--dataset-name nameOfDataset
--file-extensions: comma,separated,file,extensions (Optional, default is owl)
```
