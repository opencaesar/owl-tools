plugins {
    application
}

application {
    mainClassName = "io.opencaesar.owl.audit.OwlAuditTask"
    applicationName = "owl-audit"
}

ext["title"] = "OWL Audit Task"
description = "A gradle task to invoke owl audit"


val hidden by configurations.creating

dependencies {
	implementation(gradleApi())
    hidden(project(mapOf(
            "path" to ":owl-audit",
            "configuration" to "shadowedJars")))

}

tasks.register<Copy>("copyHidden") {
    from(hidden)
    destinationDir = File(project.buildDir, "hiddenDependencies")
    rename { filename ->
        filename.replace(Regex("-([0-9]+.)+"),".")
    }
}

tasks.jar {
    from(File(project.buildDir, "hiddenDependencies"))
    dependsOn("copyHidden")
}
