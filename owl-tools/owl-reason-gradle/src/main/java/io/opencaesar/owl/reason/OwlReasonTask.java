package io.opencaesar.owl.reason;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class OwlReasonTask extends DefaultTask {

	public String catalog;

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
		if (catalog != null) {
			args.add("--catalog");
			args.add(catalog);
		}
		if (inputOntologyIris != null) {
			inputOntologyIris.forEach((String iri) -> {
				args.add("--input-iri");
				args.add(iri);
			});
		}
		if (specs != null) {
			specs.forEach((String spec) -> {
				args.add("--spec");
				args.add(spec);
			});
		}
		if (format != null) {
			args.add("--format");
			args.add(format);
		}
		if (removeUnsats) {
			args.add("--remove-unsats");
		}
		if (removeBackbone) {
			args.add("--remove-backbone");
		}
		if (backboneIri != null) {
			args.add("--backbone-iri");
			args.add(backboneIri);
		}
		if (indent != null) {
			args.add("-indent");
			args.add(indent.toString());
		}
		if (debug) {
			args.add("-d");
		}
		OwlReasonApp.main(args.toArray(new String[args.size()]));
	}

}
