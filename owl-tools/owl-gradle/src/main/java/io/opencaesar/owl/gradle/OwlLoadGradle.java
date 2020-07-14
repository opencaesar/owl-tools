package io.opencaesar.owl.gradle;


import java.util.ArrayList;
import java.util.List;
import io.opencaesar.owl.load.App; 
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class OwlLoadGradle implements Plugin<Project>{
	@Override
	public void apply(Project project) {
		OwlLoadExtension extension = project.getExtensions().create("owlLoad", OwlLoadExtension.class);
		project.getTasks().create("loadOwl").doLast(new Action<Task>() {
			@Override
			public void execute(Task arg0) {
				// TODO Auto-generated method stub
				//App.main(args.toArray(new String[0]));
				System.out.println("Hello");
				ArrayList<String> args = new ArrayList<String>();
				args.add("-c");
				args.add(project.file(extension.catalogPath).getAbsolutePath());
				args.add("-e");
				args.add(extension.endpoint);
				args.add("-f");
				args.add(extension.fileExt);
				io.opencaesar.owl.load.App.main(args.toArray(new String[0]));
			}
		});
	}
}