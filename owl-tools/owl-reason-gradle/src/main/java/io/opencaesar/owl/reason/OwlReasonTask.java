package io.opencaesar.owl.reason;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class OwlReasonTask extends DefaultTask {

	public String catalogPath;

	public List<String> inputOntologyIris;

	public List<String> specs;

	public String format;

	public boolean removeUnsats;

	public boolean removeBackbone;

	public String backboneIri;
			
	public Integer indent;

	public boolean debug;

    @TaskAction
    public void run() {
		final ArrayList<String> args = new ArrayList<String>();
		if (catalogPath != null) {
			args.add("-c");
			args.add(catalogPath);
		}
		if (inputOntologyIris != null) {
			inputOntologyIris.forEach((String iri) -> {
				args.add("-i");
				args.add(iri);
			});
		}
		if (specs != null) {
			specs.forEach((String spec) -> {
				args.add("-s");
				args.add(spec);
			});
		}
		if (format != null) {
			args.add("-f");
			args.add(format);
		}
		if (removeUnsats) {
			args.add("-ru");
		}
		if (removeBackbone) {
			args.add("-rb");
		}
		if (backboneIri != null) {
			args.add("-b");
			args.add(backboneIri);
		}
		if (indent != null) {
			args.add("-n");
			args.add(indent.toString());
		}
		if (debug) {
			args.add("-d");
		}
		OwlReasonApp.main(args.toArray(new String[args.size()]));
	}

}
