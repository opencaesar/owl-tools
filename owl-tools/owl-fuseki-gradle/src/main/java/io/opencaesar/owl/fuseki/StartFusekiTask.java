package io.opencaesar.owl.fuseki;

import java.io.IOException;
import java.util.ArrayList;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

public class StartFusekiTask extends DefaultTask {

    public String configurationPath;

    public String outputFolderPath;

    public boolean debug;

    @TaskAction
    public void run() throws IOException {
        final ArrayList<String> args = new ArrayList<String>();
        args.add("-c");
        args.add(FusekiApp.Command.start.toString());
        if (configurationPath != null) {
            args.add("-g");
            args.add(configurationPath);
        }
        if (outputFolderPath != null) {
            args.add("-o");
            args.add(outputFolderPath);
        }
        if (debug) {
            args.add("-d");
        }
        try {
        	FusekiApp.main(args.toArray(new String[args.size()]));
        } catch (Exception e) {
            throw new TaskExecutionException(this, e);
        }
    }
    
}
