package io.opencaesar.owl.fuseki;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.resolver.owl.fuseki.ManualRepositorySystemFactory;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.*;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

public class FusekiApp {

    public static final String PID_FILENAME = "fuseki.pid";
    public static final String LOG_FILENAME = "fuseki.log";

    public static final String STOPPED_FILENAME = "fuseki.stopped";

    enum Command {
        start,
        stop
    }

    @Parameter(
            description = "An enumerated command: start or stop (Required)",
            converter = CommandConverter.class,
            required = true,
            order = 1)
    private Command command;

    @Parameter(
            names = {"--fuseki-version"},
            description = "Version of Fuseki, defaults to 4.6.0",
            required = false,
            order = 2)
    private String fusekiVersion = "4.6.1";

    @Parameter(
            names = {"--configurationPath", "-g"},
            description = "A path to a configuration file (Required)",
            order = 3)
    private String configurationPath;

    @Parameter(
            names = {"--outputFolderPath", "-o"},
            description = "A path to an output folder (Required)",
            required = true,
            order = 4)
    private String outputFolderPath;

    @Parameter(
            names = {"--port"},
            description = "Fuseki server port",
            help = true,
            order = 5)
    private int port;

    @Parameter(
            names = {"--webui", "-ui"},
            description = "Starts the Fuseki UI instead of the headless Fuseki server (Optional)",
            order = 6)
    private boolean webui;

    @Parameter(
            names = {"--max-pings", "-p"},
            description = "Maximum number (10 by default) of pings to the server before giving up",
            help = true,
            order = 7)
    private int maxPings = 10;

    @Parameter(
            names = {"--debug", "-d"},
            description = "Shows debug logging statements",
            order = 8)
    private boolean debug;

    @Parameter(
            names = {"--help", "-h"},
            description = "Displays summary of options",
            help = true,
            order = 9)
    private boolean help;


    @Parameter(
            names = {"--remote-repository-url", "-url"},
            description = "URL for a remote repository like Maven Central, defaults to: https://repo.maven.apache.org/maven2/",
            required = false,
            order = 10)
    private String remoteRepositoryURL = "https://repo.maven.apache.org/maven2/";

    private final static Logger LOGGER = Logger.getLogger(FusekiApp.class);

    static {
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
    }

    public static void main(final String... args) throws Exception {
        final FusekiApp app = new FusekiApp();
        final JCommander builder = JCommander.newBuilder().addObject(app).build();
        builder.parse(args);
        if (app.help) {
            builder.usage();
            return;
        }
        if (app.debug) {
            final Appender appender = LogManager.getRootLogger().getAppender("stdout");
            if (appender instanceof AppenderSkeleton)
                ((AppenderSkeleton) appender).setThreshold(Level.DEBUG);
        }

        app.run(app);
    }

    private void run(FusekiApp app) throws Exception {
        LOGGER.info("=================================================================");
        LOGGER.info("                        S T A R T");
        LOGGER.info("                     OWL Fuseki " + getAppVersion());
        LOGGER.info("=================================================================");
        LOGGER.info(("Command = " + command));
        LOGGER.info(("Fuseki version = " + fusekiVersion));
        LOGGER.info(("Output folder path = " + outputFolderPath));

        if (command == Command.start) {
             RepositorySystem repositorySystem = ManualRepositorySystemFactory.newRepositorySystem();
            DefaultRepositorySystemSession session = newRepositorySystemSession(repositorySystem);
            List<RemoteRepository> repositories = newRepositories(remoteRepositoryURL);
            final List<String> deps = new ArrayList<>();
            collectDependencies(repositorySystem, session, repositories, newFusekiServerArtifact(fusekiVersion), deps);

            if (app.webui) {
                collectDependencies(repositorySystem, session, repositories, newFusekiWebAppArtifact(fusekiVersion), deps);

                final File webappFolder = new File(outputFolderPath).toPath().resolve("webapp").toFile();
                webappFolder.mkdirs();
                ArtifactResult fusekiWar = resolveArtifact(
                        repositorySystem,
                        session,
                        repositories,
                        newFusekiWarArtifact(fusekiVersion));
                if (!fusekiWar.isResolved())
                    throw new IllegalArgumentException("Failed to resolve Fuseki War version "+fusekiVersion);
                final File fusekiWarFile = fusekiWar.getArtifact().getFile();
                try (InputStream is = new FileInputStream(fusekiWarFile)) {
                    unzip(is, webappFolder);
                }

                String[] cpEntries = deps.toArray(new String[0]);
                startFuseki(cpEntries, new File(configurationPath), new File(outputFolderPath), app.port, false, app.maxPings, "org.apache.jena.fuseki.cmd.FusekiCmd", app, "--localhost");
            } else {
                String[] cpEntries = deps.toArray(new String[0]);
                startFuseki(cpEntries, new File(configurationPath), new File(outputFolderPath), app.port, true, app.maxPings, "org.apache.jena.fuseki.main.cmds.FusekiMainCmd", app);
            }
        } else {
            stopFuseki(new File(outputFolderPath));
        }

        LOGGER.info("=================================================================");
        LOGGER.info("                          E N D");
        LOGGER.info("=================================================================");
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

    /**
     * Starts a background Fuseki server from a Fuseki configuration file.
     *
     * @param cpEntries Classpath entries
     * @param config    Absolute path to a Fuseki configuration file.
     * @param fusekiDir Path to an output directory that, if it exists, will be cleaned, and that will have:
     *                  - fuseki.log the combination of standard output and error.
     *                  - fuseki.pid the ID of the fuseki process.
     * @param port      The port that the Fuseki server will be listening on.
     * @param pingArg   Whether to add --ping to the command
     * @param maxPings  The maximum number of pings to try before giving up
     * @param clazz     Qualified name of the Fuseki server application (with or without Web UI)
     * @param app       Fuseki application
     * @param argv      Additional arguments
     * @throws IOException        if the 'fuseki.pid' file could not be written to
     * @throws URISyntaxException If there is a problem retrieving the location of the fuseki jar.
     */
    public static void startFuseki(
            String[] cpEntries,
            File config,
            File fusekiDir,
            int port,
            boolean pingArg,
            int maxPings,
            String clazz,
            FusekiApp app,
            String... argv) throws IOException, URISyntaxException {
        Path output = fusekiDir.toPath();
        File pidFile = output.resolve(PID_FILENAME).toFile();
        File logFile = output.resolve(LOG_FILENAME).toFile();

        // Check if the server is already running
        Optional<Long> pid = findFusekiProcessId(pidFile);
        if (pid.isPresent()) {
            Optional<ProcessHandle> ph = findProcess(pid.get());
            if (ph.isPresent()) {
                LOGGER.warn("Fuseki server is already running with pid=" + ph.get().pid());
                return;
            }
            pidFile.delete();
        }

        // Start the server
        fusekiDir.mkdirs();

        String java = getJavaCommandPath();
        int argCount = 7 + argv.length + (pingArg ? 1 : 0);
        String[] args = new String[argCount];
        int pos = 0;
        args[pos++] = java;
        args[pos++] = "-cp";
        args[pos++] = String.join(File.pathSeparator, cpEntries);
        args[pos++] = clazz;
        args[pos++] = "--port";
        args[pos++] = Integer.toString(port);
        if (pingArg) {
            args[pos++] = "--ping";
        }
        args[pos++] = "--config=" + config.getAbsolutePath();

        System.arraycopy(argv, 0, args, pos, argv.length);
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(output.toFile());
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
            throw new RuntimeException("Fuseki server has failed to start with exit code: " + p.exitValue() + ". See " + logFile + " for more details.");
        } else if (maxPings > 0) {
            // Ping the server a max number of times until it responds
            int ping = 0;
            while (++ping <= maxPings) {
                if (pingServer(port)) {
                    LOGGER.warn("Fuseki server has now successfully started with pid=" + p.pid() + ", listening on http://localhost:" + port);
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
                throw new IllegalArgumentException("Fuseki server has failed to respond after " + maxPings + " pings and now been killed");
            }
        }

        // Create the pid file and record the pid of the server in it
        OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(pidFile));
        BufferedWriter w = new BufferedWriter(os);
        w.write(Long.toString(p.pid()));
        w.newLine();
        w.close();
        os.close();
    }

    /**
     * Stops a background Fuseki server.
     *
     * @param fusekiDir The directory containing the fuseki.pid file with the ID of the Fuseki server process to kill.
     * @throws IOException if the 'fuseki.pid' file could be read
     */
    public static void stopFuseki(File fusekiDir) throws IOException {
        File pidFile = fusekiDir.toPath().resolve(PID_FILENAME).toFile();
        Optional<Long> pid = findFusekiProcessId(pidFile);
        if (!pid.isEmpty()) {
            Optional<ProcessHandle> ph = findProcess(pid.get());
            if (ph.isPresent()) {
                if (!ph.get().destroyForcibly()) {
                    throw new IllegalArgumentException("Failed to kill a Fuseki server process with pid=" + pid.get());
                } else {
                    LOGGER.warn("Fuseki server with pid=" + pid.get() + " has been stopped");
                }
            }
            pidFile.delete();
        }
    }

    private static boolean pingServer(int port) throws IOException {
        URL url = new URL("http://localhost:" + port + "/$/ping");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int responseCode = HttpURLConnection.HTTP_NOT_FOUND;
        try {
            con.setRequestMethod("GET");
            responseCode = con.getResponseCode();
        } catch (Exception e) {
            LOGGER.error("Fuseki server has not yet responded to ping");
        } finally {
            con.disconnect();
        }
        return responseCode == HttpURLConnection.HTTP_OK;
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

    public static class CommandConverter implements IStringConverter<Command> {

        @Override
        public Command convert(String value) {
            try {
                return Command.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new ParameterException("Value " + value + " is not a valid (only start or stop)");
            }
        }

    }

    /**
     * Find the fuseki process id from the '.fuseki.pid' file in the given directory
     *
     * @param f The 'fuseki.pid' file.
     * @return An optional containing the process id
     * @throws IOException if the 'fuseki.pid' file could not be read
     */
    public static Optional<Long> findFusekiProcessId(File f) throws IOException {
        if (!f.exists() || !f.canRead())
            return Optional.empty();
        BufferedReader r = new BufferedReader(new FileReader(f));
        String s = r.readLine();
        r.close();
        long pid = Long.parseLong(s);
        return Optional.of(pid);
    }

    /**
     * Find a process handle given a process id.
     *
     * @param pid The process id
     * @return The process handle of the given process id
     */
    public static Optional<ProcessHandle> findProcess(long pid) {
        return ProcessHandle
                .allProcesses()
                .filter(p -> p.pid() == pid)
                .findFirst();
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

    public static Artifact newFusekiServerArtifact(String version) {
        return new DefaultArtifact("org.apache.jena:jena-fuseki-server:"+version);
    }

    public static Artifact newFusekiWebAppArtifact(String version) {
        return new DefaultArtifact("org.apache.jena:jena-fuseki-webapp:"+version);
    }

    public static Artifact newFusekiWarArtifact(String version) {
        return new DefaultArtifact("org.apache.jena:jena-fuseki-war:war:"+version);
    }

    private static void collectDependencies(RepositorySystem system,
                                            DefaultRepositorySystemSession session,
                                            List<RemoteRepository> repositories,
                                            Artifact artifact,
                                            List<String> deps) throws DependencyCollectionException {
        CollectResult fusekiDeps = resolveDependencies(
                system,
                session,
                repositories,
                artifact);

        fusekiDeps.getRoot().accept(new DependencyVisitor() {
            @Override
            public boolean visitEnter(DependencyNode node) {
                Artifact a = node.getArtifact();
                try {
                    ArtifactResult r = resolveArtifact(system, session, repositories, a);
                    deps.add(r.getArtifact().getFile().getAbsolutePath());
                } catch (ArtifactResolutionException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
                return true;
            }
        });
    }
    private static CollectResult resolveDependencies(RepositorySystem system,
                                                     DefaultRepositorySystemSession session,
                                                     List<RemoteRepository> repositories,
                                                     Artifact artifact) throws DependencyCollectionException {
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, ""));
        collectRequest.setRepositories(repositories);
        CollectResult result = system.collectDependencies(session, collectRequest);
        return result;
    }

    private static ArtifactResult resolveArtifact(RepositorySystem system,
                                                  DefaultRepositorySystemSession session,
                                                  List<RemoteRepository> repositories,
                                                  Artifact artifact) throws ArtifactResolutionException {
        ArtifactRequest req = new ArtifactRequest();
        req.setArtifact(artifact);
        req.setRepositories(repositories);
        ArtifactResult res = system.resolveArtifact( session, req);
        return res;
    }

    /*
     * Similar to https://github.com/apache/maven-resolver/blob/0572277e23f5c2f2643dacf8bc8b8ea1ba031dea/maven-resolver-demos/maven-resolver-demo-snippets/src/main/java/org/apache/maven/resolver/examples/util/Booter.java#L71
     * The difference is that the local repository is ~/.m2/repository
     */
    private static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) throws IOException {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        File home = new File(System.getProperty("user.home"));
        if (!home.isDirectory() || !home.canExecute())
            throw new IllegalArgumentException("user.home is not a directory: "+home);
        File m2 = home.toPath().resolve(".m2").toFile();
        if (!m2.exists())
            m2.mkdirs();
        if (!m2.exists())
            throw new IllegalArgumentException("Cannot create ~/.m2");
        File local = m2.toPath().resolve("repository").toFile();
        if (!local.exists())
            local.mkdirs();
        if (!local.exists())
            throw new IllegalArgumentException("Cannot create ~/.m2/repository");

        LocalRepository localRepo = new LocalRepository( local );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

        session.setTransferListener( new ConsoleTransferListener() );
        session.setRepositoryListener( new ConsoleRepositoryListener() );

        return session;
    }

    private static List<RemoteRepository> newRepositories(String mavenCentralURL)
    {
        return new ArrayList<>( Collections.singletonList( newCentralRepository(mavenCentralURL) ) );
    }

    private static RemoteRepository newCentralRepository(String mavenCentralURL)
    {
        return new RemoteRepository.Builder( "central", "default", mavenCentralURL).build();
    }
}
