package io.opencaesar.owl.fuseki;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

public abstract class StopFusekiTask extends DefaultTask {

    private final static Logger LOGGER = Logger.getLogger(StopFusekiTask.class);

    static {
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("stopfuseki.log4j2.properties"));
    }

    private File outputFolderPath;

    @Internal
    public File getOutputFolderPath() { return outputFolderPath; }

    /*
      As a side effect, set the output file property to FusekiApp.STOPPED_FILENAME.
     */
    @SuppressWarnings("unused")
    public void setOutputFolderPath(File path) {
        outputFolderPath = path;
        if (null != getOutputFolderPath()) {
            File stopFile = getOutputFolderPath().toPath().resolve(FusekiApp.STOPPED_FILENAME).toFile();
            LOGGER.info("StopFuseki(" + getName() + ") Configure outputFile = " + stopFile);
            getOutputFile().fileValue(stopFile);
        }
    }

    @Input
    @Optional
    public abstract Property<Boolean> getDebug();

    /**
     * Since this Gradle property is configured as a side effect of configuring the output folder,
     * it is not publicly exposed to users.
     * @return The configured output file.
     */
    @OutputFile
    protected abstract RegularFileProperty getOutputFile();

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @TaskAction
    public void run() {
        final ArrayList<String> args = new ArrayList<>();
        args.add("-c");
        args.add(FusekiApp.Command.stop.toString());
        if (null != outputFolderPath) {
            args.add("-o");
            args.add(outputFolderPath.getAbsolutePath());
        }
        if (getDebug().isPresent() && getDebug().get()) {
            args.add("-d");
        }
        try {
        	FusekiApp.main(args.toArray(new String[0]));
            // Generate a unique output for gradle incremental execution support.
            if (getOutputFile().isPresent()) {
                File output = getOutputFile().get().getAsFile();
                LOGGER.info("StopFuseki("+getName()+") Generate output file: " + output);
                try (PrintStream ps = new PrintStream(new FileOutputStream(output))) {
                    ps.println(getTaskIdentity().uniqueId);
                }
            }
            // Delete the 'fuseki.pid' file to enable startFuseki again.
            if (null != outputFolderPath) {
                File pidFile = outputFolderPath.toPath().resolve(FusekiApp.PID_FILENAME).toFile();
                if (pidFile.exists())
                    pidFile.delete();
            }
        } catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
        }
    }
    
}
