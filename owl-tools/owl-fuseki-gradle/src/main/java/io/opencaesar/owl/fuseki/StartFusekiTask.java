package io.opencaesar.owl.fuseki;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

public class StartFusekiTask extends DefaultTask {

    public File config;

    public File outputDirectory;

    @TaskAction
    public void run() throws IOException {
        FusekiApp.startFuseki(config, outputDirectory);
    }
    
}
