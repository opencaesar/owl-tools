package io.opencaesar.owl.doc;

import java.util.ArrayList;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

public class OwlDocTask extends DefaultTask {

	public String endpointURL;

	public String documentIRI;

	public String outputPath;

	public boolean debug;
    
    @TaskAction
    public void run() {
		final ArrayList<String> args = new ArrayList<String>();
		if (endpointURL != null) {
			args.add("-e");
			args.add(endpointURL);
		}
		if (documentIRI != null) {
			args.add("-i");
			args.add(documentIRI);
		}
		if (outputPath != null) {
			args.add("-o");
			args.add(outputPath);
		}
		if (debug) {
			args.add("-d");
		}
		try {
			OwlDocApp.main(args.toArray(new String[args.size()]));
		} catch (Exception e) {
			throw new TaskExecutionException(this, e);
		}
    }
}