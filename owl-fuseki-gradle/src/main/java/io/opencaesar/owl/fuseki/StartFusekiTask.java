package io.opencaesar.owl.fuseki;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class StartFusekiTask extends DefaultTask {

    private final static Logger LOGGER = Logger.getLogger(StartFusekiTask.class);

    static {
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("startfuseki.log4j2.properties"));
    }

    @InputFile
    public abstract RegularFileProperty getConfigurationPath();

    // contributes to the output file property
    public File outputFolderPath;

    /*
      As a side effect, set the outputFile property to FusekiApp.PID_FILENAME.
     */
    @SuppressWarnings({ "unused", "deprecation" })
    public void setOutputFolderPath(File path) {
        outputFolderPath = path;
        if (null != outputFolderPath) {
            File pidFile = outputFolderPath.toPath().resolve(FusekiApp.PID_FILENAME).toFile();
            LOGGER.info("StartFuseki(" + getName() + ") Configure outputFile = " + pidFile);
            getOutputFile().fileValue(pidFile);
        }
    }

    /**
     * Since this Gradle property is configured as a side effect of configuring the output folder, it is not publicly exposed to users.
     * @return The configured output file.
     */
    @OutputFile
    protected abstract RegularFileProperty getOutputFile();

    @Input
    @Optional
    public abstract Property<Boolean> getWebUI();

    @Input
    @Optional
    public abstract Property<Boolean> getDebug();

    @TaskAction
    public void run() {
        final ArrayList<String> args = new ArrayList<>();
        args.add("-c");
        args.add(FusekiApp.Command.start.toString());
        if (getConfigurationPath().isPresent()) {
            args.add("-g");
            args.add(getConfigurationPath().get().getAsFile().getAbsolutePath());
        }
        if (null != outputFolderPath) {
            args.add("-o");
            args.add(outputFolderPath.getAbsolutePath());
        }
        if (getWebUI().isPresent() && getWebUI().get()) {
            args.add("-ui");
        }
        if (getDebug().isPresent() && getDebug().get()) {
            args.add("-d");
        }
        try {
        	FusekiApp.main(args.toArray(new String[0]));

            // Delete the 'fuseki.stopped' file to enable stopFuseki again.
            if (null != outputFolderPath) {
                File stoppedFile = outputFolderPath.toPath().resolve(FusekiApp.STOPPED_FILENAME).toFile();
                if (stoppedFile.exists())
                    stoppedFile.delete();
            }
        } catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
        }
    }
    
}
