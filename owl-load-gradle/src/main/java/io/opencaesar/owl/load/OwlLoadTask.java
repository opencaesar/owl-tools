package io.opencaesar.owl.load;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import java.util.ArrayList;
import java.util.List;

public class OwlLoadTask extends DefaultTask {

    public List<String> iris;

    public String catalogPath;

    public String endpointURL;

    public List<String> fileExtensions;

    public boolean debug;

    @TaskAction
    public void run() {
        final ArrayList<String> args = new ArrayList<String>();
        if (null != iris) {
            iris.forEach(iri -> {
                args.add("-i");
                args.add(iri);
            });
        }
        if (catalogPath != null) {
            args.add("-c");
            args.add(catalogPath);
        }
        if (endpointURL != null) {
            args.add("-e");
            args.add(endpointURL);
        }
        if (fileExtensions != null) {
            fileExtensions.forEach((String ext) -> {
                args.add("-f");
                args.add(ext);
            });
        }
        if (debug) {
            args.add("-d");
        }
        try {
            OwlLoadApp.main(args.toArray(new String[args.size()]));
        } catch (Exception e) {
            throw new TaskExecutionException(this, e);
        }
    }
}