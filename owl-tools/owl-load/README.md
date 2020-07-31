# OWL Load

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-load/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-load/_latestVersion)

A tool to load OWL files to a SPARQL end point. 

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
--endpoint-url | -e http://sparqlURL/dataset [Required]
--catalog-path | -c path/to/owl/catalog.xml [Required]
--file-extensions | -f: comma,separated,file,extensions [Optional, default is owl]
```
Note: The dataset (database) must have been created prior to execution

## [Run as Gradle Task](../owl-load-gradle/README.md)

# Running Apache Jena Fuseki
[Download Fuseki](https://jena.apache.org/download/index.cgi)

## Method 1: Running a standalone server 
By default, the server will be available at http://localhost:3030 after running the command
MacOS/Linux: 
```
    cd fuseki-distribution-folder
    ./fuseki-server
```
Windows:
```
    cd fuseki-distribution-folder
    fuseki-server.bat
```    
[More instruction](https://jena.apache.org/documentation/fuseki2/fuseki-run.html#fuseki-standalone-server)
## Method 2: Running as a Web Application
Can use any application that provides Java Servlet 3.1 API. This tutorial will use [Apache Tomcat 8](https://tomcat.apache.org/download-80.cgi)
Follow the setup instructions in the RUNNING.TXT file included in the Tomcat 8 distribution. Then
copy the fuseki.war file from the Fuseki distribution into the webapps directory of Tomcat. The server
will run on default at http://localhost:8080/fuseki after running the command 
MacOS/Linux:
```
    cd tomcat-distribution-folder/bin
    ./startup    
```
Windows:
```
    cd tomcat-distribution-folder/bin
    startup.bat   
```
[More instructions](https://jena.apache.org/documentation/fuseki2/fuseki-quick-start.html)
[Other methods to run Fuseki](https://jena.apache.org/documentation/fuseki2/fuseki-run.html)

# Creating a Fuseki Dataset
These instructions are specific to creating a Fuseki dataset. 

## Method 1: GUI
Enter your Fuseki URL into your web browser and create a dataset in the manage dataset tab.

## Method 2: CLI Curl 
MacOS/Linux:
```
    curl 'http://fusekiURL/$/datasets' -d 'dbName=name&dbType=mem'
```
Windows (Using Powershell):
```
    curl.exe -X POST -d 'dbName=name&dbType=mem' http://fusekiURL/$/datasets
```
