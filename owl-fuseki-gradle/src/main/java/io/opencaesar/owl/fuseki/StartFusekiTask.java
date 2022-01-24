package io.opencaesar.owl.fuseki;

import java.util.ArrayList;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

public abstract class StartFusekiTask extends DefaultTask {

    @InputFile
    public abstract RegularFileProperty getConfigurationPath();

    @OutputDirectory
    public abstract RegularFileProperty getOutputFolderPath();

    @Input
    @Optional
    public abstract Property<Boolean> getDebug();

    @Input
    @Optional
    public abstract Property<Boolean> getWebUI();

    @TaskAction
    public void run() {
        final ArrayList<String> args = new ArrayList<>();
        args.add("-c");
        args.add(FusekiApp.Command.start.toString());
        if (getConfigurationPath().isPresent()) {
            args.add("-g");
            args.add(getConfigurationPath().get().getAsFile().getAbsolutePath());
        }
        if (getOutputFolderPath() != null) {
            args.add("-o");
            args.add(getOutputFolderPath().get().getAsFile().getAbsolutePath());
        }
        if (getWebUI().isPresent() && getWebUI().get()) {
            args.add("-ui");
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
