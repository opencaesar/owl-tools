package io.opencaesar.owl.fuseki;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
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

public class FusekiApp {

    public static final String PID_FILENAME = "fuseki.pid";
    public static final String LOG_FILENAME = "fuseki.log";

    public static final String STOPPED_FILENAME = "fuseki.stopped";

	enum Command {
		start,
		stop
	}
	
    @Parameter(
            names = {"--command", "-c"},
            description = "An enumerated command: start or stop (Required)",
            converter = CommandConverter.class,
            required = true,
            order = 1)
    private Command command;
	
    @Parameter(
            names = {"--configurationPath", "-g"},
            description = "A path to a configuration file (Required)",
            order = 2)
    private String configurationPath;
	
    @Parameter(
            names = {"--outputFolderPath", "-o"},
            description = "A path to an output folder (Required)",
            required = true,
            order = 3)
    private String outputFolderPath;

    @Parameter(
            names = {"--webui", "-ui"},
            description = "Starts the Fuseki UI instead of the headless Fuseki server (Optional)",
            order = 4)
    private boolean webui;

    @Parameter(
            names = {"--debug", "-d"},
            description = "Shows debug logging statements",
            order = 5)
    private boolean debug;

    @Parameter(
            names = {"--help", "-h"},
            description = "Displays summary of options",
            help = true,
            order = 6)
    private boolean help;
	
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
    	LOGGER.info(("Configuration path = " + configurationPath));
    	LOGGER.info(("Output folder path = " + outputFolderPath));

    	if (command == Command.start) {
          if (app.webui) {
              final File webappFolder = new File(outputFolderPath).toPath().resolve("webapp").toFile();
              webappFolder.mkdirs();
              final URL fusekiWarPomURL = FusekiApp.class.getClassLoader().getResource("META-INF/maven/org.apache.jena/jena-fuseki-war/pom.xml");
              if (null != fusekiWarPomURL && "jar".equals(fusekiWarPomURL.getProtocol())) {
                final JarURLConnection connection = (JarURLConnection) fusekiWarPomURL.openConnection();
                final URL jarURL = connection.getJarFileURL();
                  try (InputStream is = jarURL.openStream()) {
                      unzip(is, webappFolder);
                  }
              }
              LOGGER.info("fusekiWarPomURL="+fusekiWarPomURL);
              startFuseki(new File(configurationPath), new File(outputFolderPath), "org.apache.jena.fuseki.cmd.FusekiCmd", app, "--localhost");
          } else {
              startFuseki(new File(configurationPath), new File(outputFolderPath), "org.apache.jena.fuseki.main.cmds.FusekiMainCmd", app);
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
                    LOGGER.info("Extracting: " + nextFile);
                    copy(zipStream, targetStream);
                }
            }
        }
    }

    /**
     * Starts a background Fuseki server from a Fuseki configuration file.
     *
     * @param config Absolute path to a Fuseki configuration file.
     * @param fusekiDir Path to an output directory that, if it exists, will be cleaned, and that will have:
     *                        - fuseki.log the combination of standard output and error.
     *                        - fuseki.pid the ID of the fuseki process.
     * @param clazz Qualified name of the Fuseki server application (with or without Web UI)
     * @param app Fuseki application
     * @param argv Additional arguments
     * @throws IOException if the 'fuseki.pid' file could not be written to
     * @throws URISyntaxException If there is a problem retrieving the location of the fuseki jar.
     */
    public static void startFuseki(File config, File fusekiDir, String clazz, FusekiApp app, String... argv) throws IOException, URISyntaxException {
        Path output = fusekiDir.toPath();
        File pidFile = output.resolve(PID_FILENAME).toFile();
        File logFile = output.resolve(LOG_FILENAME).toFile();
       
        Optional<Long> pid = findFusekiProcessId(pidFile);
        if (pid.isPresent()) {
	        Optional<ProcessHandle> ph = findProcess(pid.get());
	        if (ph.isPresent()) {
	        	System.out.print("Found a Fuseki server already running with pid="+ph.get().pid());
	        	return;
	        }
	        pidFile.delete();
        }
        
        fusekiDir.mkdirs();

        String java = getJavaCommandPath();
        String jar = findJar(clazz);
        int argCount = 5 + argv.length + (app.webui ? 1 : 0);
        String[] args = new String[argCount];
        int pos = 0;
        args[pos++] = java;
        if (app.webui) {
            args[pos++] = "-Dlog4j.configurationFile=webapp/log4j2.properties";
        }
        args[pos++] = "-jar";
        args[pos++] = jar;
        args[pos++] = "--ping";
        args[pos++] = "--config=" + config.getAbsolutePath();
        System.arraycopy(argv, 0, args, pos, argv.length);
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(output.toFile());
        pb.redirectErrorStream(true);
        pb.redirectOutput(logFile);

        Process p = pb.start();
        try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// do nothing
		}
        
        if (!p.isAlive()) {
            throw new IllegalArgumentException("Fuseki server failed to start and returned error code: " + p.exitValue() + ". See "+logFile+" for more details.");
        } else {
        	if (pingServer()) {
        		System.out.print("Started a Fuseki server with pid="+p.pid());
        	} else {
                p.destroyForcibly();
                try {
        			Thread.sleep(2000);
        		} catch (InterruptedException e) {
        			// do nothing
        		}
                throw new IllegalArgumentException("Fuseki server failed to start and returned error code: " + p.exitValue() + ". See "+logFile+" for more details.");
        	}
        }
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
                	System.out.println("Stopped a Fuseki server process with pid=" + pid.get());
                }
            }
	        pidFile.delete();
        }
    }
    
    private static boolean pingServer() throws IOException {
        URL url = new URL("http://localhost:3030/$/ping");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int responseCode = HttpURLConnection.HTTP_NOT_FOUND;
        try {
	        con.setRequestMethod("GET");
	        con.setConnectTimeout(5000);
	        con.setReadTimeout(5000);
	        responseCode = con.getResponseCode();
        } catch (Exception e) {
        	LOGGER.error(e);
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
				throw new ParameterException("Value "+value+" is not a valid (only start or stop)");
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
     * Find the path of a jar on the classpath that provides a class by its dot-qualified name.
     *
     * @param qualifiedClassName The qualified name of a class from a Jar on the classpath.
     * @return The location of the Jar on the classpath that provides the class.
     * @throws URISyntaxException if there is a problem retrieving the location of the fuseki jar.
     * @throws IOException if there is a problem retrieving the location of the fuseki jar.
     * @see ClassLoader#getResource(String) about using '/' as a separator for resource paths.
     */
    public static String findJar(String qualifiedClassName) throws URISyntaxException, IOException {
        String resourceName = qualifiedClassName.replaceAll("\\.", "/") + ".class";
        URL classURL = FusekiApp.class.getClassLoader().getResource(resourceName);
        if (null == classURL)
            throw new IllegalArgumentException("Cannot find " + qualifiedClassName + " on the classpath.");
        JarURLConnection c = (JarURLConnection) classURL.openConnection();
        URL jarURL = c.getJarFileURL();
        Path jarPath = Paths.get(jarURL.toURI());
        File f = jarPath.toFile();
        if (!f.exists() || !f.canRead())
            throw new IllegalArgumentException("Cannot find jar of " + qualifiedClassName + " at: " + f);
        return f.getAbsolutePath();
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

}
