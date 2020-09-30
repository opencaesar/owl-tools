package io.opencaesar.owl.fuseki;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

public class FusekiApp {

    /**
     * Starts a background Fuseki server from a Fuseki configuration file.
     *
     * @param config Absolute path to a Fuseki configuration file.
     * @param outputDirectory Path to an output directory that, if it exists, will be cleaned, and that will have:
     *                        - fuseki.log the combination of standard output and error.
     *                        - fuseki.pid the ID of the fuseki process.
     * @throws IOException
     */
    public static void startFuseki(File config, File outputDirectory) throws IOException {
        String java = getJavaCommandPath();
        String jar = findJar("org.apache.jena.fuseki.main.cmds.FusekiMainCmd");
        outputDirectory.mkdirs();
        deleteDirectoryRecursively(outputDirectory);
        outputDirectory.mkdir();
        Path output = outputDirectory.toPath();
        File log = output.resolve("fuseki.log").toFile();
        File pid = output.resolve("fuseki.pid").toFile();

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
     * Stops a background Fuseki server.
     *
     * @param fusekiDir The directory containing the fuseki.pid file with the ID of the Fuseki server process to kill.
     * @throws IOException
     */
    public static void stopFuseki(File fusekiDir) throws IOException {
        File f = fusekiDir.toPath().resolve("fuseki.pid").toFile();
        if (!f.exists() || !f.canRead())
            throw new IllegalArgumentException("Cannot find the 'fuseki.pid' in the fuseki directory at: " + f);
        BufferedReader r = new BufferedReader(new FileReader(f));
        String s = r.readLine();
        r.close();
        long pid = Long.parseLong(s);

        Optional<ProcessHandle> ph = ProcessHandle
                .allProcesses()
                .filter(p -> p.pid() == pid)
                .findFirst();

        if (ph.isPresent()) {
            ProcessHandle p = ph.get();
            boolean ok = p.destroyForcibly();
            if (!ok)
                throw new IllegalArgumentException("Failed to kill fuseki process with pid=" + pid);

        } else
            throw new IllegalArgumentException("Cannot find fuseki process with pid=" + pid);
    }

    /**
     * Find the path of a jar on the classpath that provides a class by its dot-qualified name.
     *
     * @param qualifiedClassName The qualified name of a class from a Jar on the classpath.
     * @return The location of the Jar on the classpath that provides the class.
     * @see ClassLoader#getResource(String) about using '/' as a separator for resource paths.
     */
    public static String findJar(String qualifiedClassName) {
        String resourceName = qualifiedClassName.replaceAll("\\.", "/") + ".class";
        URL classURL = FusekiApp.class.getClassLoader().getResource(resourceName);
        if (null == classURL)
            throw new IllegalArgumentException("Cannot find " + qualifiedClassName + " on the classpath.");
        String path = classURL.getPath().replaceFirst("file:", "");
        String jar = path.substring(0, path.indexOf('!'));
        File f = new File(jar);
        if (!f.exists() || !f.canRead())
            throw new IllegalArgumentException("Cannot find jar of " + qualifiedClassName + " at: " + jar);
        return jar;
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
