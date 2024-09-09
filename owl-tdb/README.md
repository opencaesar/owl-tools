# OWL Tdb

[![Release](https://img.shields.io/github/v/tag/opencaesar/owl-tools?label=release)](https://github.com/opencaesar/owl-tools/releases/latest)

A couple of tools to load/save a set of OWL files to/from a TDB dataset.

## Run as CLI
MacOS/Linux:
```
./gradlew owl-tdb:run --args="..."
```
Windows:
```
gradlew.bat owl-tdb:run --args="..."
```

Args:
```
-cm | --command            								[Required, options: load, save]
-ds | --dataset-path  path/to/tdb/dataset/folder         [Required]
-c  | --catalog-path path/to/owl/catalog.xml             [Required]
-e  | --file-extension extension                         [Optional, default: ttl and owl, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]

Args (relevant when command is load)
-i  | --iri <IRI>                                        [Optional, an iri to load]
-p | --iris-path path/to/iris.log                        [Optional, a txt file with iris (one on each line) to load]
-ng | --named-graph false                                [Optional, load to named graphs, default: true]
-dg | --default-graph false                              [Optional, load to default graph, default: true]
```

## Run as Gradle Task

```
buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath 'io.opencaesar.owl:owl-tdb-gradle:+'
	}
}

task owlTdbLoad(type:io.opencaesar.owl.tdb.OwlTdbLoadTask) {
	datasetPath = file('path/to/tdb/dataset/folder') [Required]
	catalogPath = file('path/to/owl/catalog.xml') [Required]
	fileExtensions = ['owl', 'ttl'] [Optional, default='owl'and 'ttl', options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
	iris = ['iri1',...] [Optional]
	irisPath = file('path/to/iris.log') [Optional]
	loadToNamedGraph = true|false [Optional, default=true]
	loadToDefaultGraph = true|false [Optional, default=true]
	incremental = false [Optional, default=true]
}

task owlTdbSave(type:io.opencaesar.owl.tdb.OwlTdbSaveTask) {
	datasetPath = file('path/to/tdb/dataset/folder') [Required]
	catalogPath = file('path/to/owl/catalog.xml') [Required]
	fileExtension = ['ttl'] [Optional, default='ttl', options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
}
```

Setting the owlTdbLoad task's `incremental` flag to `false` causes all OWL files in scope to always load. On the other hand, setting the gradle task's `incremental` flag to `true` causes only the OWL files that have changed to load.

When `iris` and `irisPath` are not specified, the entire catalog is loaded.


