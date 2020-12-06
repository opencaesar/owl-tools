# OWL Tools

[![Build Status](https://travis-ci.org/opencaesar/owl-tools.svg?branch=master)](https://travis-ci.org/opencaesar/owl-tools)

A set of OWL based analysis tools

## Clone
```
    git clone https://github.com/opencaesar/owl-tools.git
    cd owl-tools
```
      
## Build
Requirements: java 11
```
    cd owl-tools
    ./gradlew build
```

## [OWL Audit](owl-audit) [ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-audit/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-audit/_latestVersion)

A library check OWL ontologies with respect to audits written in a Ruby-based DSL.

## [OWL Close World](owl-close-world) [ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-close-world/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-close-world/_latestVersion)

A library of different algorithms to close the world on OWL ontologies.

## [OWL Diff](owl-diff) [ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-diff/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-diff/_latestVersion)

A tool to produce a difference report between two OWL datasets.

## [OWL Fuseki](owl-fuseki) [ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-fuseki/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-fuseki/_latestVersion)

A tool to start and stop a UI-less Fuseki server with a given configuration file.

## [OWL Reason](owl-reason) [ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-reason/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-reason/_latestVersion)

A tool to analyze an OWL dataset for satisfiability and consistency with an OWL2-DL reasoner.

## [OWL Load](owl-load) [ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-load/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-load/_latestVersion)

A tool to load a catalog of OWL ontologies to a database endpoint.

## [OWL Query](owl-query) [ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-query/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-query/_latestVersion)

A tool to send a SPARQL query to a database endpoint and save the result.

## [OWL Shacl Fuseki](owl-shacl-fuseki) [ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-shacl-fuseki/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-shacl-fuseki/_latestVersion)

A tool to send SHACL queries to a Fuseki database endpoint and save the result.

## Classloader conflicts with Gradle scripts

When a gradle build script declares multiple plugin dependencies, 
there is a risk for classloader conflicts due to the plugin transitive dependencies.

For example, the [owl-audit](owl-audit) tool depends on different versions of the Apache Jena libraries
than the [owl-reason](owl-reason) and [owl-fuseki](owl-fuseki) tools. 
Classloader conflicts would occur if the corresponding gradle tasks 
exposed these tools' dependencies to Gradle in the normal way; that is, where the Gradle task declares
a classloader dependency on the corresponding tool. In such circumstances, the Gradle classloader
will have to resolve the conflicting dependencies of multiple tools loaded from multiple Gradle tasks.
Although developing Gradle tasks in this normal way is simple for developers, it creates a significant
risk that users of these Gradle tasks will experience in their Gradle script classloader conflicts
that may be very difficult to diagnose because the errors may manifest in other Gradle task plugins that,
in isolation, work fine but do not work when used with certain other Gradle task plugins. Troubleshooting
these problems can be a very time consuming process.

Fortunately, it is possible to prevent these classloader conflicts by designing Gradle tasks to execute
tools in an isolated classloader. For custom Gradle task development involving a Gradle [WorkQueue](https://docs.gradle.org/current/javadoc/org/gradle/workers/WorkQueue.html) for asychronous execution, 
Gradle provides support for so-called [Isolation modes](https://docs.gradle.org/current/userguide/custom_tasks.html#isolation-modes):
> [WorkerExecutor.noIsolation()](https://docs.gradle.org/current/javadoc/org/gradle/workers/WorkerExecutor.html#noIsolation--)

    This states that the work should be run in a thread with a minimum of isolation. For instance, it will share the same classloader that the task is loaded from. This is the fastest level of isolation.
> [WorkerExecutor.classLoaderIsolation()](https://docs.gradle.org/current/javadoc/org/gradle/workers/WorkerExecutor.html#classLoaderIsolation-org.gradle.api.Action-)

    This states that the work should be run in a thread with an isolated classloader. The classloader will have the classpath from the classloader that the unit of work implementation class was loaded from as well as any additional classpath entries added through ClassLoaderWorkerSpec.getClasspath().

> [WorkerExecutor.processIsolation()](https://docs.gradle.org/current/javadoc/org/gradle/workers/WorkerExecutor.html#processIsolation-org.gradle.api.Action-)

    This states that the work should be run with a maximum level of isolation by executing the work in a separate process. The classloader of the process will use the classpath from the classloader that the unit of work was loaded from as well as any additional classpath entries added through ClassLoaderWorkerSpec.getClasspath(). Furthermore, the process will be a Worker Daemon which will stay alive and can be reused for future work items that may have the same requirements. This process can be configured with different settings than the Gradle JVM using ProcessWorkerSpec.forkOptions(org.gradle.api.Action).

For simple Gradle tasks that **synchronously** execute a tool, Gradle does not currently provide support for classloader isolation. However, this can be easily with the help of Apache Spark 3.0.1 [ChildFirstURLClassLOader](https://github.com/apache/spark/blob/v3.0.1/core/src/main/java/org/apache/spark/util/ChildFirstURLClassLoader.java)
as demonstrated in the [owl-audit-gradle](owl-audit-gradle) task.

The following summarizes the development steps involved to apply this technique:

1) For the tool, e.g., [owl-audit](owl-audit), apply the Gradle [shadow plugin](https://imperceptiblethoughts.com/shadow/) to package the tool and its dependencies transitively into a single jar file.

   - Create a Gradle configuration for the tool's dependencies:

     ```kotlin
     val internal by configurations.creating
     ```
     
   - Define the dependencies using this configuration:
   
     ```kotlin
     dependencies {
       internal(group = "org.jruby", name = "jruby-complete", version = "1.7.27")
       // ...
       internal(group = "org.apache.jena", name = "jena-arq", version = "2.10.1")
       internal(group = "org.apache.jena", name = "jena-core", version = "2.10.1")
       internal(group = "org.apache.jena", name = "jena-iri", version = "0.9.6")
       // ...
       // The implementation dependencies include the internal dependencies.
       implementation(internal)
     }
     ```

   - Add the [shadow plugin](https://imperceptiblethoughts.com/shadow/)
   
     ```kotlin
     plugins {
        application
        id("com.github.johnrengelman.shadow") version "6.1.0"
     }
     ```
    
   - Configure the `shadowJar` task to include all the configured dependencies:
   
     ```kotlin
     tasks {
       shadowJar {
         archiveClassifier.set("")
         configurations = listOf(internal)
       }
     }
     ```
   
    - Finally, expose a new configuration with the shadowed jar such that downstream
      Gradle projects can use it:
   
      ```kotlin
      val shadowedJars by configurations.creating {
        isCanBeConsumed = true
        isCanBeResolved = false
      }
      ```
      
    - Note that for the tool itself, it is important to ensure that any classloader
      created uses the context classloader instead of the default application classloader.
      See for example the configuration of the JRuby interpreter in [OwlAuditApp]([owl-audit-gradle](owl-audit-gradle)):
      
      ```java
              try {
               final List<String> loadPaths = new ArrayList<>();
               loadPaths.add(getResourcePath("/audit-framework"));
               loadPaths.add(getResourcePath("/rubygems/logger-application-0.0.2/lib"));
   
               final ClassCache cc = JavaEmbedUtils.createClassCache(this.getClass().getClassLoader());
               final RubyInstanceConfig config = new RubyInstanceConfig();
               config.setClassCache(cc);
               config.setLoader(this.getClass().getClassLoader());
               final Ruby runtime = JavaEmbedUtils.initialize(loadPaths, config);
               final RubyRuntimeAdapter adapter = JavaEmbedUtils.newRuntimeAdapter();
               String script = "...";
               final IRubyObject result = adapter.eval(runtime, script);
               // ...
               } catch (...) { ... }
       ```
      
2) For the Gradle task, e.g., [owl-audit-gradle](owl-audit-gradle), consume the exposed
shadowed jar from the tool project as a hidden dependency that the task will extract 
to a temporary file for execution using a child-first classloader.
   
   - In the Gradle script, declare a `hidden` configuration for the dependency 
     on the shadowed jar exposed by the tool project:
     
     ```kotlin
     val hidden by configurations.creating
      
     dependencies {
       implementation(gradleApi())
       hidden(project(mapOf(
         "path" to ":owl-audit",
         "configuration" to "shadowedJars"))) 
       }
     ```
   
   - To facilitate finding the hidden shadowed jar, rename its filename to
     remove the version suffix such that the task's jar will include the tool jar
     as a resource without the version suffix.
     
     ```kotlin
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
     ```

   - In the Gradle task itself, instead of executing the tool via a normal Java
     import dependency and instatiating the tool's class, extract the tool's shadowed
     jar to a temporary file and create a child-first classloader with a URL of this
     temporary file.
     
     For example, instead of the following:
   
     ```java
     
     import io.opencaesar.owl.audit.OwlAuditApp;
     
     public class OwlAuditTask extends DefaultTask {

       // ...
 
       @TaskAction
       public void run() {
         final ArrayList<String> args = new ArrayList<String>();
         args.add("...");
         
         try { 
           OwlAuditApp.main(args.toArray(new String[args.size()]));
         } catch (Exception e) { 
           throw new TaskExecutionException(this, e);
         }
       }
     }
     ```

     Write instead the following:
   
     ```java
     public class OwlAuditTask extends DefaultTask {

       // ...

       @TaskAction
       public void run() {
         final ArrayList<String> args = new ArrayList<>();
         args.add("...");

         try {
           URL jarURL = this.getClass().getResource("/owl-audit.jar");
           if (null == jarURL) {
             System.err.println("The OwlAuditTask jar is missing the resource 'owl-audit.jar'");
             System.exit(255);
           }

           final File jarFile = extractJarFile(jarURL);
           final URL[] urls = new URL[1];
           urls[0] = jarFile.toURI().toURL();
           final ChildFirstURLClassLoader cl = new ChildFirstURLClassLoader(urls, this.getClass().getClassLoader());
           Class<?> app = cl.loadClass("io.opencaesar.owl.audit.OwlAuditApp");
           Method main = app.getMethod("main", String[].class);
           String[] params = args.toArray(new String[args.size()]);
           main.invoke(null, new Object[] { params });
         } catch (Exception e) {
           System.err.println(e.getLocalizedMessage());
           e.printStackTrace(System.err);
           System.exit(255);
         }
       }

       private File extractJarFile(URL jarURL) throws Exception {
         Path dir = Files.createTempDirectory("audit-framework-");
         dir.toFile().deleteOnExit();

         File jarFile = Files.createTempFile(dir,"owl-audit-", ".jar").toFile();
         jarFile.deleteOnExit();

         final byte[] buffer = new byte[4096];
         final InputStream is = jarURL.openStream();
         final FileOutputStream fos = new FileOutputStream(jarFile);
         int len;
         while ((len = is.read(buffer)) > 0) { 
           fos.write(buffer, 0, len);
         }
         fos.close();
         is.close();

         return jarFile;
       }
     }
     ```
