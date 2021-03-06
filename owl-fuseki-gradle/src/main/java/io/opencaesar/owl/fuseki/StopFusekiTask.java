package io.opencaesar.owl.fuseki;

import java.io.IOException;
import java.util.ArrayList;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

public class StopFusekiTask extends DefaultTask {

    public String outputFolderPath;

    public boolean debug;

    @TaskAction
    public void run() throws IOException {
        final ArrayList<String> args = new ArrayList<String>();
        args.add("-c");
        args.add(FusekiApp.Command.stop.toString());
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
			throw new GradleException(e.getLocalizedMessage(), e);
        }
    }
    
}
