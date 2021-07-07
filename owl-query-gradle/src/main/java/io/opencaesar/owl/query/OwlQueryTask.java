package io.opencaesar.owl.query;

import java.util.ArrayList;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

public class OwlQueryTask extends DefaultTask {

	public String endpointURL;

	public String queryPath;

	public String resultPath;

	public String format;

	public boolean debug;
    
    @TaskAction
    public void run() {
		final ArrayList<String> args = new ArrayList<String>();
		if (endpointURL != null) {
			args.add("-e");
			args.add(endpointURL);
		}
		if (queryPath != null) {
			args.add("-q");
			args.add(queryPath);
		}
		if (resultPath != null) {
			args.add("-r");
			args.add(resultPath);
		}
		if (format != null) {
			args.add("-f");
			args.add(format);
		}
		if (debug) {
			args.add("-d");
		}
		try {
			OwlQueryApp.main(args.toArray(new String[args.size()]));
		} catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
		}
    }
}