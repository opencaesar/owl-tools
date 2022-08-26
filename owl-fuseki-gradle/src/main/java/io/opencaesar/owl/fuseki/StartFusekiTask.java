package io.opencaesar.owl.fuseki;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.*;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

public abstract class StartFusekiTask extends DefaultTask {

    public static final String PID_FILENAME = "fuseki.pid";
    public static final String LOG_FILENAME = "fuseki.log";

    @InputFile
    public abstract RegularFileProperty getConfigurationPath();

    @OutputDirectory
    public abstract DirectoryProperty getOutputFolderPath();

    @Optional
    @Input
    public abstract Property<Boolean> getWebUI();

    @Input
    public abstract Property<Integer> getPort();

    @Optional
    @Input
    public abstract Property<Integer> getMaxPings();

    @Optional
    @Input
    public abstract Property<Boolean> getDebug();

    @OutputFile
    protected Provider<RegularFile> getOutputFile() {
        if (getOutputFolderPath().isPresent()) {
            return getOutputFolderPath().file(PID_FILENAME);
        }
        return null;
    }

    @Override
    public Task configure(Closure closure) {
        Task t = super.configure(closure);
        getLogger().info("StartFusekiTask.configure");
        t.setOnlyIf(new Closure<Boolean>(null) {
            public Boolean doCall(Task ignore) {
                final File pidFile = getOutputFile().get().getAsFile();
                Boolean ok = true;
                if (pidFile.exists()) {
                    try {
                        java.util.Optional<Long> pid = findFusekiProcessId(pidFile);
                        ok = pid.isEmpty();
                    } catch (IOException e) {
                        ok = true;
                    }
                }
                getLogger().info("StartFusekiTask.configure="+ok);
                return ok;
            }
        });

        return t;
    }


    @TaskAction
    public void run() {
        try {
            getLogger().info("=================================================================");
            getLogger().info("                        S T A R T");
            getLogger().info("                     OWL Fuseki " + getAppVersion());
            getLogger().info("=================================================================");
            final Boolean webUI = getWebUI().getOrElse(false);
            final int port = getPort().get();
            final int maxPings = getMaxPings().getOrElse(1);
            final File pidFile = getOutputFile().get().getAsFile();
            final File outputFolderPath = getOutputFolderPath().getAsFile().get();
            final File logFile = getOutputFolderPath().file(LOG_FILENAME).get().getAsFile();

            final Set<File> files = getProject()
                    .getConfigurations()
                    .getByName("fuseki")
                    .resolve();

            URL[] urls = files
                    .stream()
                    .map(f -> {
                        try {
                            return f.toURI().toURL();
                        } catch (MalformedURLException e) {
                            throw new GradleException(e.getLocalizedMessage(), e);
                        }
                    })
                    .toArray(URL[]::new);

            String[] jars = files
                    .stream()
                    .map(File::getAbsolutePath)
                    .toArray(String[]::new);

            if (webUI) {
                final File webappFolder = outputFolderPath.toPath().resolve("webapp").toFile();
                webappFolder.mkdirs();
                URLClassLoader cl = new URLClassLoader(urls);
                final URL fusekiWarPomURL = cl.getResource("META-INF/maven/org.apache.jena/jena-fuseki-war/pom.xml");
                if (null != fusekiWarPomURL && "jar".equals(fusekiWarPomURL.getProtocol())) {
                    final JarURLConnection connection = (JarURLConnection) fusekiWarPomURL.openConnection();
                    final URL jarURL = connection.getJarFileURL();
                    try (InputStream is = jarURL.openStream()) {
                        unzip(is, webappFolder);
                    }
                }
                getLogger().info("fusekiWarPomURL=" + fusekiWarPomURL);
            }
            final String clazz =
                    (webUI) ? "org.apache.jena.fuseki.cmd.FusekiCmd"
                            : "org.apache.jena.fuseki.main.cmds.FusekiMainCmd";
            int argCount = 7 + (webUI ? 2 : 0);
            String[] args = new String[argCount];
            int pos = 0;
            args[pos++] = getJavaCommandPath();
            if (webUI) {
                args[pos++] = "-Dlog4j.configurationFile=webapp/log4j2.properties";
            }
            args[pos++] = "-cp";

            args[pos++] = String.join(File.pathSeparator, jars);
            args[pos++] = clazz;
            args[pos++] = "--port";
            args[pos++] = Integer.toString(port);
            if (!webUI) {
                args[pos++] = "--ping";
            }
            args[pos++] = "--config=" + getConfigurationPath().get().getAsFile().getAbsolutePath();

            getLogger().info("Process args: "+Arrays.toString(args));
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(getOutputFolderPath().get().getAsFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(logFile);

            Process p = pb.start();
            try {
                Thread.sleep(2000); // give server a bit of time to start
            } catch (InterruptedException e) {
                // do nothing
            }

            // Check that the newly started server is alive
            if (!p.isAlive()) {
                throw new GradleException("Fuseki server has failed to start with exit code: " + p.exitValue() + ". See " + logFile + " for more details.");
            } else if (maxPings > 0) {
                // Ping the server a max number of times until it responds
                int ping = 0;
                while (++ping <= maxPings) {
                    if (pingServer(port)) {
                        getLogger().info("Fuseki server has now successfully started with pid=" + p.pid() + ", listening on http://localhost:" + port);
                        break;
                    }
                    try {
                        Thread.sleep(2000); // wait before the next ping
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }
                // if the server has failed to respond, kill it
                if (ping > maxPings) {
                    try {
                        p.destroyForcibly().waitFor();
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                    throw new GradleException("Fuseki server has failed to respond after " + maxPings + " pings and now been killed");
                }
            }

            // Create the pid file and record the pid of the server in it
            OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(pidFile));
            BufferedWriter w = new BufferedWriter(os);
            w.write(Long.toString(p.pid()));
            w.newLine();
            w.close();
            os.close();

            getLogger().info("=================================================================");
            getLogger().info("                          E N D");
            getLogger().info("=================================================================");
        } catch (IOException e) {
            throw new GradleException("StartFuseki failed: "+e.getLocalizedMessage(), e);
        }
    }

    private boolean pingServer(int port) throws IOException {
        URL url = new URL("http://localhost:" + port + "/$/ping");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int responseCode = HttpURLConnection.HTTP_NOT_FOUND;
        try {
            con.setRequestMethod("GET");
            responseCode = con.getResponseCode();
        } catch (Exception e) {
            getLogger().error("Fuseki server has not yet responded to ping");
        } finally {
            con.disconnect();
        }
        return responseCode == HttpURLConnection.HTTP_OK;
    }

    public static java.util.Optional<Long> findFusekiProcessId(File f) throws IOException {
        if (!f.exists() || !f.canRead())
            return java.util.Optional.empty();
        BufferedReader r = new BufferedReader(new FileReader(f));
        String s = r.readLine();
        r.close();
        long pid = Long.parseLong(s);
        return java.util.Optional.of(pid);
    }

    /**
     * Find the path to the java executable (java.exe on windows, java elsewhere).
     *
     * @return The absolute path to the java executable.
     */
    public static String getJavaCommandPath() {
        String javaHome = System.getProperty("java.home");
        String osName = System.getProperty("os.name");
        boolean isWindows = (null != osName) && osName.startsWith("Windows");

        String javaExe = javaHome + File.separator + "bin" + File.separator + "java" + (isWindows ? ".exe" : "");
        File java = new File(javaExe);
        if (java.exists() && java.canExecute())
            return javaExe;
        else
            throw new RuntimeException("Cannot find java executable at: " + javaExe);
    }


    public static void unzip(InputStream source, File target) throws IOException {
        final ZipInputStream zipStream = new ZipInputStream(source);
        ZipEntry nextEntry;
        while ((nextEntry = zipStream.getNextEntry()) != null) {
            final String name = nextEntry.getName();
            // only extract files
            if (!name.endsWith("/")) {
                final File nextFile = new File(target, name);

                // create directories
                final File parent = nextFile.getParentFile();
                if (parent != null) {
                    //noinspection ResultOfMethodCallIgnored
                    parent.mkdirs();
                }

                // write file
                // Skipping writing the file if it exists turns out to be a bad idea.
                // - changes in the version of Fuseki could cause problems.
                // - without changes, the webapp ui is not displaying properly.
                // So it is better to always overwrite existing files.
                try (OutputStream targetStream = new FileOutputStream(nextFile)) {
                    copy(zipStream, targetStream);
                }
            }
        }
    }

    private static void copy(final InputStream source, final OutputStream target) throws IOException {
        final int bufferSize = 4 * 1024;
        final byte[] buffer = new byte[bufferSize];

        int nextCount;
        while ((nextCount = source.read(buffer)) >= 0) {
            target.write(buffer, 0, nextCount);
        }
    }

    private String getAppVersion() {
        var version = this.getClass().getPackage().getImplementationVersion();
        return (version != null) ? version : "<SNAPSHOT>";
    }
}
