# OWL Doc

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-doc/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-doc/_latestVersion)

Extracts a document specified with the `http://opencaesar.io/document` ontology
from a SPARQL endpoint and produces a JSON representation of the binding tree.



## Run as CLI

MacOS/Linux:
```
cd owl-tools
./gradlew owl-doc:run --args="..."
```
Windows:
```
cd owl-tools
gradlew.bat owl-doc:run --args="..."
```
Args:
```
--endpoint-url | -e http://fusekiURL/databaseName [Required]
--document-iri | -q http://example.com/document/iri#Document [Required]
--output-path | -r path/to/output.json [Required]
--debug | -d [Optional]
```

## Run as Gradle Task
```
buildscript {
	repositories {
		maven { url 'https://dl.bintray.com/opencaesar/owl-tools' }
  		mavenCentral()
		jcenter()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-doc-gradle:+'
	}
}
task owlDoc(type:io.opencaesar.owl.query.OwlDocTask) {
	endpointURL = 'url-of-sparql-endpoint' [Required]
	documentIRI = 'http://example.com/document/iri#Document' [Required, path to file or folder]
	outputPath = file('path/to/output.json') [Required]
}               
```

## Output JSON

The output JSON is a tree of Node objects, with the root being the document
Node produced from the given document IRI. In TypeScript syntax, this looks like:

```typescript

// The document root and all child nodes
interface Node {

	// The IRI of the Element this node is derived from
	iri: string

	// All type IRIs of the source Element, where each type is a subtype of
	// <http://opencaesar.io/document#Element>
	types: string[]

	// An object where the key is the binding name and the value is the BindingValue
	bindings: {[bindingName:string]:BindingValue}

	// Array of child nodes, derived from <http://opencaesar.io/document#hasElement>
	// relation
	children: Node[]
}

// Node bindings, modeled after the SPARQL JSON result binding format.
interface BindingValue {

	// The type of value
	type: "uri"|"literal"|"bnode"

	// The URI or literal value
	value?: string
	
	// The datatype of the literal, e.g. "http://www.w3.org/2001/XMLSchema#string"
	datatype?: string
	
	// If specified, the language of a literal value, e.g. "en"
	"xml:lang"?: string
}
```