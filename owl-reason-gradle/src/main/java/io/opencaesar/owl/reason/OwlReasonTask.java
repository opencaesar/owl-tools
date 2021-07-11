package io.opencaesar.owl.reason;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

public class OwlReasonTask extends DefaultTask {

	public String catalogPath;

	public String inputOntologyIri;

	public List<String> specs;
	
	public String reportPath;

	public List<String> inputFileExtensions;

	public String outputFileExtension;

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
		if (inputOntologyIri != null) {
			args.add("-i");
			args.add(inputOntologyIri);
		}
		if (specs != null) {
			specs.forEach((String spec) -> {
				args.add("-s");
				args.add(spec);
			});
		}
		if (reportPath != null) {
			args.add("-r");
			args.add(reportPath);
		}
		if (inputFileExtensions != null) {
            inputFileExtensions.forEach((String ext) -> {
                args.add("-if");
                args.add(ext);
            });
		}
		if (outputFileExtension != null) {
			args.add("-of");
			args.add(outputFileExtension);
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
		try {
			OwlReasonApp.main(args.toArray(new String[args.size()]));
		} catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
		}
	}

}
