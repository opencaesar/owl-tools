ext["title"] = "OWL Audit Task"
description = "A gradle task to invoke owl audit"

dependencies {
	implementation(gradleApi())
    implementation(project(":owl-audit"))
    implementation(group = "org.jruby", name = "jruby-complete", version = "1.7.27")
}
