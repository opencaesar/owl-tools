/**
 * 
 * Copyright 2019-2021 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package io.opencaesar.owl.doc;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.work.Incremental;

/**
 * A gradle task to invoke the owl doc tool 
 */
public abstract class OwlDocTask extends DefaultTask {
	
	/**
	 * Creates a new OwlDocTask object
	 */
	public OwlDocTask() {
	}

	/**
	 * Path of the input OWL catalog.
	 * 
	 * @return File Property
	 */
	@Input
    public abstract Property<File> getInputCatalogPath();

	/**
	 * Title of OML input catalog (Optional).
	 * 
	 * @return String Property
	 */
    @Optional
	@Input
    public abstract Property<String> getInputCatalogTitle();

	/**
	 * Version of OML input catalog (Optional).
	 * 
	 * @return String Property
	 */
    @Optional
	@Input
    public abstract Property<String> getInputCatalogVersion();

    /**
	 * Iris of input OWL ontologies.
	 * 
	 * @return List of Strings Property
	 */
    @Optional
    @Input
    public abstract ListProperty<String> getInputOntologyIris();

	/**
	 * Extensions for the input OWL files (default=["owl", "ttl"], options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss).
	 * 
	 * @return List of Strings Property
	 */
    @Optional
    @Input
    public abstract ListProperty<String> getInputFileExtensions();

    /**
	 * Path of the output folder.
	 * 
	 * @return File Property
	 */
    @OutputDirectory
 	public abstract DirectoryProperty getOutputFolderPath();

	/**
	 * Whether output paths are case sensitive.
	 * 
	 * @return Boolean Property
	 */
    @Optional
    @Input
    public abstract Property<Boolean> getOutputCaseSensitive();

	/**
	 * The debug flag
	 * 
	 * @return Boolean Property
	 */
    @Optional
    @Input
    public abstract Property<Boolean> getDebug();

	/**
	 * The collection of input OML files referenced by the input Oml catalog
	 * 
     * @return ConfigurableFileCollection
     * @throws IOException error
     * @throws URISyntaxException error
	 */
	@Incremental
	@InputFiles
    protected ConfigurableFileCollection getInputFiles() throws IOException, URISyntaxException {
		if (getInputCatalogPath().isPresent() && getInputCatalogPath().get().exists()) {
			final var catalogURI = getInputCatalogPath().get().toURI();
			final var inputCatalog = OwlCatalog.create(catalogURI);

			final var inputFileExtensions = !getInputFileExtensions().get().isEmpty() ? getInputFileExtensions().get() : Arrays.asList(OwlDocApp.DEFAULT_EXTENSIONS);
			final var inputFiles = inputCatalog.getFileUriMap(inputFileExtensions).values().stream().map(f-> new File(f)).collect(Collectors.toList());

			return getProject().files(inputFiles);
        }
		return getProject().files(Collections.EMPTY_LIST);
    }
        
   /**
    * The gradle task action logic.
    */
    @TaskAction
    public void run() {
        List<String> args = new ArrayList<>();
        if (getInputCatalogPath().isPresent()) {
		    args.add("-c");
		    args.add(getInputCatalogPath().get().getAbsolutePath());
        }
        if (getInputCatalogTitle().isPresent()) {
		    args.add("-t");
		    args.add(getInputCatalogTitle().get());
        }
        if (getInputCatalogVersion().isPresent()) {
		    args.add("-v");
		    args.add(getInputCatalogVersion().get());
        }
        if (getInputOntologyIris().isPresent()) {
        	getInputOntologyIris().get().forEach(iri -> {
            	args.add("-i");
            	args.add(iri);
            });
		}
        if (getInputFileExtensions().isPresent()) {
        	getInputFileExtensions().get().forEach(ext -> {
                args.add("-e");
                args.add(ext);
            });
        }
		if (getOutputFolderPath().isPresent()) {
			args.add("-o");
			args.add(getOutputFolderPath().get().getAsFile().getAbsolutePath());
		}
	    if (getOutputCaseSensitive().isPresent()) {
	    	if (getOutputCaseSensitive().get()) {
	    		args.add("-s");
	    	}
	    }
		if (getDebug().isPresent() && getDebug().get()) {
		    args.add("-d");
	    }
	    try {
    		OwlDocApp.main(args.toArray(new String[0]));
		} catch (Exception e) {
			throw new TaskExecutionException(this, e);
		}
   	}
    
}