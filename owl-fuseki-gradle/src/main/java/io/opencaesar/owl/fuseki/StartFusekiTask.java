package io.opencaesar.owl.fuseki;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.*;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * Gradle task for starting a background Apache Fuseki server that stays running beyond the gradle session.
 */
public abstract class StartFusekiTask extends DefaultTask {

	@InputFile
    public abstract RegularFileProperty getConfigurationPath();

    /**
     * @return The required gradle task fuseki output folder property.
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputFolderPath();

    /**
     * @return The optional gradle task fuseki web ui property (default is false).
     */
    @Optional
    @Input
    public abstract Property<String> getRemoteRepositoryURL();

    @Optional
    @Input
    public abstract Property<String> getFusekiVersion();

    /**
     * @return The optional gradle task fuseki port property (default is 3030).
     */
    @Optional
    @Input
    public abstract Property<Integer> getPort();

    /**
     * @return The optional gradle task fuseki maximum pings property (default is 10).
     */
    @Optional
    @Input
    public abstract Property<Boolean> getWebUI();

    @Optional
    @Input
    public abstract Property<Integer> getMaxPings();

    /**
     * @return The optional gradle task debug property (default is false).
     */
    @Optional
    @Input
    public abstract Property<Boolean> getDebug();

    /**
     * @return The gradle output file, after checking whether the fuseki pid file can be deleted
     *         if the process no longer exists.
     * @throws IOException error
     */
    @OutputFile
    protected Provider<RegularFile> getOutputFile() throws IOException {
        if (getOutputFolderPath().isPresent()) {
            var pidRegularFile = getOutputFolderPath().file(FusekiApp.PID_FILENAME);
            var pidFile = pidRegularFile.get().getAsFile();
            if (pidFile.exists()) {
	        	java.util.Optional<Long> pid = FusekiApp.findFusekiProcessId(pidFile);
	            if (pid.isPresent()) {
	    	        java.util.Optional<ProcessHandle> ph = FusekiApp.findProcess(pid.get());
	    	        if (!ph.isPresent()) {
	    	        	pidFile.delete();
	    	        }
	            }
            }
            return pidRegularFile;
        }
        return null;
    }

    /**
     * The gradle task action logic.
     */
    @TaskAction
    public void run() {
        final ArrayList<String> args = new ArrayList<>();
        args.add(FusekiApp.Command.start.toString());
        if (getFusekiVersion().isPresent()) {
            args.add("--fuseki-version");
            args.add(getFusekiVersion().get());
        }
        if (getRemoteRepositoryURL().isPresent()) {
            args.add("-url");
            args.add(getRemoteRepositoryURL().get());
        }
        if (getConfigurationPath().isPresent()) {
            args.add("-g");
            args.add(getConfigurationPath().get().getAsFile().getAbsolutePath());
        }
        if (getOutputFolderPath().isPresent()) {
            args.add("-o");
            args.add(getOutputFolderPath().get().getAsFile().getAbsolutePath());
        }
        if (getPort().isPresent()) {
        	args.add("--port");
        	args.add(getPort().get().toString());
        }
        if (getWebUI().isPresent()) {
        	if (getWebUI().get()) {
                args.add("-ui");
            }
        }
        if (getMaxPings().isPresent()) {
            args.add("-p");
            args.add(getMaxPings().get().toString());
        }
        if (getDebug().isPresent() && getDebug().get()) {
            args.add("-d");
        }
        try {
            String[] a = args.toArray(new String[0]);
        	FusekiApp.main(a);
        } catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
        }
    }
    
}
