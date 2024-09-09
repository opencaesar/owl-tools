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
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

/**
 * Gradle task to load Owl files to a TDB dataset
 */
public abstract class OwlTdbLoadTask extends DefaultTask {

	/**
	 * Creates a new OwlTdbLoadTask object
	 */
	public OwlTdbLoadTask() {
		getOutputs().upToDateWhen(task -> {
			boolean incremental = getIncremental().isPresent() ? getIncremental().get() : true;
			if (incremental) {
				getInputFolder().set(getCatalogPath().get().getParentFile());
			}
			return incremental;
		});
	}

	/**
	 * Path to a folder representing the TDB dataset (Required).
	 *
	 * @return String Property
	 */
	@OutputDirectory 
	public abstract Property<File> getDatasetPath();

	/**
	 * Path to an Apache catalog for OWL files (Required).
	 *
	 * @return File Property
	 */
	@InputFile
	public abstract Property<File> getCatalogPath();

	/**
	 * File extensions for the loaded Owl files (optional, default is both owl and ttl, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss).
	 *
	 * @return List of Strings Property
	 */
	@Optional
	@Input
	public abstract ListProperty<String> getFileExtensions();

	/**
	 * A list of ontology IRIs to load (Optional).
	 * 
	 * @return List of Strings
	 */
	@Optional
	@Input
	public abstract ListProperty<String> getIris();

	/**
	 * A text file listing all ontology IRIs (one per line) to load (Optional).
	 * 
	 * @return RegularFile Property
	 */
	@Optional
	@InputFile
	public abstract Property<File> getIrisPath();

	/**
	 * Whether to load to named graphs (Optional, default is true).
	 *
	 * @return Boolean Property
	 */
	@Optional
	@Input
	public abstract Property<Boolean> getLoadToNamedGraphs();

	/**
	 * Whether to load to the default graph (Optional, default is true).
	 *
	 * @return Boolean Property
	 */
	@Optional
	@Input
	public abstract Property<Boolean> getLoadToDefaultGraph();

	/**
	 * Whether to load the dataset incrementally
	 * 
	 * @return Boolean property
	 */
	@Optional
	@Input
	public abstract Property<Boolean> getIncremental();

	/**
	 * The optional gradle task debug property (default is false).
	 *
	 * @return Boolean Property
	 */
	@Optional
	@Input
	public abstract Property<Boolean> getDebug();

	/**
	 * The folder of input files for incremental load
	 * 
	 * @return DirectoryProperty
	 */
	@Incremental
	@InputDirectory
	@Optional
	protected abstract DirectoryProperty getInputFolder();

	/**
	 * The gradle task action logic.
	 * 
	 * @param inputChanges The input changes
	 */
	@TaskAction
	public void run(InputChanges inputChanges) {
		final ArrayList<String> args = new ArrayList<>();
		args.add("-cm");
		args.add(OwlTdbApp.Command.load.toString());
		
		if (getDatasetPath().isPresent()) {
			args.add("-ds");
			args.add(getDatasetPath().get().getAbsolutePath());
		}
		if (getCatalogPath().isPresent()) {
			args.add("-c");
			args.add(getCatalogPath().get().getAbsolutePath());
		}
		if (getFileExtensions().isPresent()) {
			getFileExtensions().get().forEach(ext -> {
				args.add("-e");
				args.add(ext);
			});
		}
		if (getIris().isPresent()) {
			getIris().get().forEach(iri -> {
				args.add("-i");
				args.add(iri);
			});
		}
		if (getIrisPath().isPresent()) {
			args.add("-p");
			args.add(getIrisPath().get().getAbsolutePath());
		}
		if (getLoadToNamedGraphs().isPresent()) {
			args.add("-ng");
			args.add(getLoadToNamedGraphs().get() ? "true" : "false");
		}
		if (getLoadToDefaultGraph().isPresent()) {
			args.add("-dg");
			args.add(getLoadToDefaultGraph().get() ? "true" : "false");
		}
		if (getDebug().isPresent()) {
			if ( getDebug().get()) {
				args.add("-d");
			}
		}
		
		try {
			if (getIncremental().isPresent() && !getIncremental().get()) {
				OwlTdbApp.main(args.toArray(new String[0]));
			} else { // run incrementally by default
				final Set<File> deltas = new HashSet<>();
				inputChanges.getFileChanges(getInputFolder()).forEach(f -> deltas.add(f.getFile()));
				OwlTdbApp.mainWithDeltas(deltas, args.toArray(new String[0]));
			}
		} catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
		}	
	}
}
