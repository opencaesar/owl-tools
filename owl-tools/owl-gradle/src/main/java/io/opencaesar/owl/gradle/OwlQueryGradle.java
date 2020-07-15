package io.opencaesar.owl.gradle;

import java.util.ArrayList;
import io.opencaesar.owl.query.QueryNoLog; 
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class OwlQueryGradle implements Plugin<Project>{
	@Override
	public void apply(Project project) {
		OwlQueryExtension extension = project.getExtensions().create("owlQuery", OwlQueryExtension.class);
		project.getTasks().create("queryOwl").doLast(new Action<Task>() {
			@Override
			public void execute(Task arg0) {
				//App.main(args.toArray(new String[0]));
				ArrayList<String> args = new ArrayList<String>();
				args.add("-e");
				args.add(extension.endpoint);
				args.add("-q");
				args.add(extension.queriesPath);
				args.add("-r");
				args.add(extension.resultPath);
				args.add("-f");
				args.add(extension.formatType);
				QueryNoLog.execute(args.toArray(new String[0]));
			}
		});
	}
}
