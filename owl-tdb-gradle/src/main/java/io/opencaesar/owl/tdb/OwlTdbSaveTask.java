/**
 * Copyright 2024 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opencaesar.owl.tdb;

import java.io.File;
import java.util.ArrayList;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

/**
 * Gradle task to save Owl files from a TDB dataset
 */
public abstract class OwlTdbSaveTask extends DefaultTask {

	/**
	 * Creates a new OwlTdbSaveTask object
	 */
	public OwlTdbSaveTask() {
    	getOutputs().upToDateWhen(task -> false);
	}


	/**
	 * Path to a folder representing the TDB dataset (Required).
	 *
	 * @return String Property
	 */
	@InputDirectory 
	public abstract Property<File> getDatasetPath();

	/**
	 * Path to an Apache catalog for OWL files (Required).
	 *
	 * @return File Property
	 */
    @OutputFile
	public abstract Property<File> getCatalogPath();

	/**
	 * File extension for the saved Owl files (optional, default is ttl, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss).
	 *
	 * @return List of Strings Property
	 */
	@Optional
	@Input
	public abstract Property<String> getFileExtension();

	/**
	 * The optional debug property (default is false).
	 *
	 * @return Boolean Property
	 */
	@Optional
	@Input
	public abstract Property<Boolean> getDebug();

	/**
	 * The gradle task action logic.
	 * 
	 * @param inputChanges The input changes
	 */
	@TaskAction
	public void run(InputChanges inputChanges) {
		final ArrayList<String> args = new ArrayList<>();
		args.add("-cm");
		args.add(OwlTdbApp.Command.save.toString());
		
		if (getDatasetPath().isPresent()) {
			args.add("-ds");
			args.add(getDatasetPath().get().getAbsolutePath());
		}
		if (getCatalogPath().isPresent()) {
			args.add("-c");
			args.add(getCatalogPath().get().getAbsolutePath());
		}
		if (getFileExtension().isPresent()) {
			args.add("-e");
			args.add(getFileExtension().get());
		}
		if (getDebug().isPresent()) {
			if ( getDebug().get()) {
				args.add("-d");
			}
		}
		
		try {
			OwlTdbApp.main(args.toArray(new String[0]));
		} catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
		}	
	}
}
