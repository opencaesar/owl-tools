plugins {
	application
	id("com.github.johnrengelman.shadow") version "6.1.0"
}
ext["title"] = "OWL Audit"
description = "A tool to execute a set of SPARQL-based Audit rules on a given SPARQL dataset"


application {
	mainClassName = "io.opencaesar.owl.audit.OwlAuditApp"
	applicationName = "owl-audit"
}

val jCommanderVersion: String by rootProject
val internal by configurations.creating

tasks {
	shadowJar {
		archiveClassifier.set("")
		configurations = listOf(internal)
	}
}

val shadowedJars by configurations.creating {
	isCanBeConsumed = true
	isCanBeResolved = false
}

artifacts {
	add("shadowedJars", tasks.shadowJar)
}

dependencies {
	internal(group = "org.jruby", name = "jruby-complete", version = "1.7.27")
	internal(group = "commons-codec", name = "commons-codec", version = "1.6")
	internal(group = "org.apache.httpcomponents", name = "httpclient", version = "4.2.3")
	internal(group = "org.apache.httpcomponents", name = "httpcore", version = "4.2.2")
	internal(group = "org.slf4j", name = "jcl-over-slf4j", version = "1.6.4")
	internal(group = "org.apache.jena", name = "jena-arq", version = "2.10.1")
	internal(group = "org.apache.jena", name = "jena-core", version = "2.10.1")
	internal(group = "org.apache.jena", name = "jena-iri", version = "0.9.6")
	internal(group = "org.apache.jena", name = "jena-tdb", version = "0.10.1")
	internal(group = "org.slf4j", name = "slf4j-api", version = "1.6.4")
	internal(group = "org.slf4j", name = "slf4j-log4j12", version = "1.6.4")
	internal(group = "xerces", name = "xercesImpl", version = "2.11.0")
	internal(group = "xml-apis", name = "xml-apis", version = "1.4.01")
	internal(group = "log4j", name = "log4j", version = "1.2.16")
	internal(group = "com.beust", name = "jcommander", version = jCommanderVersion)
	// The implementation dependencies include the internal dependencies.
	implementation(internal)
}
