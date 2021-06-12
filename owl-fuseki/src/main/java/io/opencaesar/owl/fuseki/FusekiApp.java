package io.opencaesar.owl.fuseki;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.jena.ext.com.google.common.io.CharStreams;
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
            required = false,
            order = 2)
    private String configurationPath;
	
    @Parameter(
            names = {"--outputFolderPath", "-o"},
            description = "A path to an output folder (Required)",
            required = true,
            order = 3)
    private String outputFolderPath;
	
    @Parameter(
            names = {"--debug", "-d"},
            description = "Shows debug logging statements",
            order = 4)
    private boolean debug;

    @Parameter(
            names = {"--help", "-h"},
            description = "Displays summary of options",
            help = true,
            order = 5)
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
            ((AppenderSkeleton) appender).setThreshold(Level.DEBUG);
        }
        app.run();
    }

    private void run() throws Exception {
    	LOGGER.info("=================================================================");
    	LOGGER.info("                        S T A R T");
    	LOGGER.info("                     OWL Fuseki " + getAppVersion());
    	LOGGER.info("=================================================================");
    	LOGGER.info(("Command = " + command));
    	LOGGER.info(("Configuration path = " + configurationPath));
    	LOGGER.info(("Output folder path = " + outputFolderPath));
        
    	if (command == Command.start) {
    		startFuseki(new File(configurationPath), new File(outputFolderPath));
    	} else {
    		stopFuseki(new File(outputFolderPath));
    	}
        
    	LOGGER.info("=================================================================");
    	LOGGER.info("                          E N D");
    	LOGGER.info("=================================================================");
    }

    private String getAppVersion() throws Exception {
		InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("version.txt");
		if (input != null) {
			InputStreamReader reader = new InputStreamReader(input);
			String version = CharStreams.toString(reader);
			if (version != null && !version.isEmpty()) {
				return version;
			}
			throw new IllegalArgumentException("File version.txt is empty");
		}
		throw new FileNotFoundException("version.txt");
    }

    public class CommandConverter implements IStringConverter<Command> {

		@Override
		public Command convert(String value) {
			Command c = Command.valueOf(value);
			if (c == null) {
				throw new ParameterException("Value "+value+" is not a valid (only start or stop)"); 
			}
			return c;
		}
    	
    }
    
    /**
     * Starts a background Fuseki server from a Fuseki configuration file.
     *
     * @param config Absolute path to a Fuseki configuration file.
     * @param outputDirectory Path to an output directory that, if it exists, will be cleaned, and that will have:
     *                        - fuseki.log the combination of standard output and error.
     *                        - fuseki.pid the ID of the fuseki process.
     * @throws IOException if the 'fuseki.pid' file could not be written to 
     * @throws IllegalArgumentException If there exists a process whose ID matches 'fuseki.pid' from the output directory.
     */
    public static void startFuseki(File config, File outputDirectory) throws IOException, URISyntaxException {
        Optional<ProcessHandle> ph = findFusekiProcess(outputDirectory);
        if (ph.isPresent()) {
            throw new IllegalArgumentException("There is already a fuseki server running with pid="+ph.get().pid());
        }
        outputDirectory.mkdirs();
        Path output = outputDirectory.toPath();
        File log = output.resolve("fuseki.log").toFile();
        File pid = output.resolve("fuseki.pid").toFile();

        String java = getJavaCommandPath();
        String jar = findJar("org.apache.jena.fuseki.main.cmds.FusekiMainCmd");
        ProcessBuilder pb = new ProcessBuilder(java, "-jar", jar, "--config=" + config.getAbsolutePath());
        pb.directory(output.toFile());
        pb.redirectErrorStream(true);
        pb.redirectOutput(log);

        Process p = pb.start();

        OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(pid));
        BufferedWriter w = new BufferedWriter(os);
        w.write(Long.toString(p.pid()));
        w.newLine();
        w.close();
        os.close();
    }

    /**
     * Find the process handle of a fuseki server process from a 'fuseki.pid' file, if possible.
     *
     * @param outputDirectory Directory where the 'fuseki.pid' file is located.
     * @return The ProcessHandle retrieved from the process whose ID matches the 'fuseki.pid' file if it exists and is readable.
     * @throws IOException if the 'fuseki.pid' file could be read
     */
    public static Optional<ProcessHandle> findFusekiProcess(File outputDirectory) throws IOException {
        File f = outputDirectory.toPath().resolve("fuseki.pid").toFile();
        if (!f.exists() || !f.canRead())
            return Optional.empty();

        BufferedReader r = new BufferedReader(new FileReader(f));
        String s = r.readLine();
        r.close();
        long pid = Long.parseLong(s);
        return ProcessHandle
                .allProcesses()
                .filter(p -> p.pid() == pid)
                .findFirst();
    }

    /**
     * Stops a background Fuseki server.
     *
     * @param fusekiDir The directory containing the fuseki.pid file with the ID of the Fuseki server process to kill.
     * @throws IOException if the 'fuseki.pid' file could be read
     */
    public static void stopFuseki(File fusekiDir) throws IOException {
        Optional<ProcessHandle> ph = findFusekiProcess(fusekiDir);
        if (ph.isEmpty())
            throw new IllegalArgumentException("Cannot find the 'fuseki.pid' file in the fuseki directory: " + fusekiDir);
        ProcessHandle p = ph.get();
        boolean ok = p.destroyForcibly();
        if (!ok)
            throw new IllegalArgumentException("Failed to kill fuseki process with pid=" + p.pid());
        deleteDirectoryRecursively(fusekiDir);
   }

    /**
     * Find the path of a jar on the classpath that provides a class by its dot-qualified name.
     *
     * @param qualifiedClassName The qualified name of a class from a Jar on the classpath.
     * @return The location of the Jar on the classpath that provides the class.
     * @throws URISyntaxException
     * @throws IOException
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

    public static void deleteDirectoryRecursively(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File aFile : files) {
                    deleteDirectoryRecursively(aFile);
                }
            }
        }
        dir.delete();
    }
}
