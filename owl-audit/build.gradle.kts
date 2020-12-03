import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation

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

val jenaVersion: String by rootProject

val jena by configurations.creating

tasks {
	// For development, shadow only, no relocation.
	val shadowDevJar by registering(ShadowJar::class) {
		group = "build"
		archiveClassifier.set("dev")
		configurations = listOf(jena)
		from(sourceSets.main.get().output)
	}

	// This creates build/productionGems, which has a copy of src/main/resources/
	// where the java_import statements in the Ruby scripts have been relocated.
	val processProductionResources by registering(Copy::class) {
		from(sourceSets.main.get().resources)
		destinationDir = File(project.buildDir, "productionGems")
		filteringCharset = "UTF-8"
		eachFile {
			this.filter { line -> line.replace("java_import ", "java_import io.opencaesar.owl.audit.") }
		}
	}

	// This relocates all the dependencies in the jena configuration
	// such that all class files have been relocated w/ a prefix
	// to avoid classloader conflicts with other uses of different versions of libraries in other gradle tasks.
	register("relocateShadowedDependenciesJar", ConfigureShadowRelocation::class) {
		target = shadowJar.get()
		prefix = "io.opencaesar.owl.audit"
	}

	// For production, shadow + relocate.
	shadowJar {
		archiveClassifier.set("")
		configurations = listOf(jena)

		// This should use the build/productionGems instead of the src/main/resources.
		// Unfortunately, this does not seem to work as documented.
//		from(files(File(project.buildDir, "productionGems")))
//		dependsOn("processProductionResources", "relocateShadowedDependenciesJar")
	}
}

dependencies {
	implementation(group = "org.jruby", name = "jruby-complete", version = "1.7.27")
	jena("commons-codec:commons-codec:1.6")
	jena("org.apache.httpcomponents:httpclient:4.2.3") {
		exclude(group = "log4j", module = "log4j")
	}
	jena("org.apache.httpcomponents:httpcore:4.2.2") {
		exclude(group = "log4j", module = "log4j")
	}
	jena("org.slf4j:jcl-over-slf4j:1.6.4") {
		exclude(group = "log4j", module = "log4j")
	}
	jena("org.apache.jena:jena-arq:2.10.1") {
		exclude(group = "log4j", module = "log4j")
	}
	jena("org.apache.jena:jena-core:2.10.1") {
		exclude(group = "log4j", module = "log4j")
	}
	jena("org.apache.jena:jena-iri:0.9.6") {
		exclude(group = "log4j", module = "log4j")
	}
	jena("org.apache.jena:jena-tdb:0.10.1") {
		exclude(group = "log4j", module = "log4j")
	}
	jena("org.slf4j:slf4j-api:1.6.4")
	jena("org.slf4j:slf4j-log4j12:1.6.4") {
		exclude(group = "log4j", module = "log4j")
	}
	jena("xerces:xercesImpl:2.11.0")
	jena("xml-apis:xml-apis:1.4.01")

	// This library cannot be shaddowed because it internally uses Java reflection to create an instance of log4j Appender.
	implementation("log4j:log4j:1.2.16")

	// The implementation dependencies include the jena dependencies.
	implementation(jena)
}
