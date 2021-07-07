package io.opencaesar.owl.shacl.fuseki;

import java.util.ArrayList;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

public class OwlShaclFusekiTask extends DefaultTask {

	public String endpointURL;

	public String queryPath;

	public String resultPath;

	public boolean debug;
    
    @TaskAction
    public void run() {
		final ArrayList<String> args = new ArrayList<>();
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
		if (debug) {
			args.add("-d");
		}
		try {
			OwlShaclFusekiApp.main(args.toArray(new String[args.size()]));
		} catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
		}
    }
}