package io.opencaesar.owl.fuseki;

import java.util.ArrayList;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

/**
 * Gradle task for stopping an Apache Fuseki server by killing the process based on the fuseki pid file.
 */
public abstract class StopFusekiTask extends DefaultTask {

    /**
     * @return The required gradle task output folder property.
     */
    @InputDirectory
    public abstract DirectoryProperty getOutputFolderPath();

    /**
     * @return The optional gradle task debug property (default is false).
     */
    @Optional
    @Input
    public abstract Property<Boolean> getDebug();

    /**
     * The gradle task action logic.
     */
    @SuppressWarnings({ "deprecation" })
    @TaskAction
    public void run() {
        final ArrayList<String> args = new ArrayList<>();
        args.add(FusekiApp.Command.stop.toString());
        if (getOutputFolderPath().isPresent()) {
            args.add("-o");
            args.add(getOutputFolderPath().get().getAsFile().getAbsolutePath());
        }
        if (getDebug().isPresent() && getDebug().get()) {
            args.add("-d");
        }
        try {
        	FusekiApp.main(args.toArray(new String[0]));
        } catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
        }
    }
    
}
