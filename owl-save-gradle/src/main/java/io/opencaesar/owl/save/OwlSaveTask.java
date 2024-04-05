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
package io.opencaesar.owl.save;

import java.io.File;
import java.util.ArrayList;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * A grade task to save files from a SPARQL endpoint to an OWL catalog
 */
public abstract class OwlSaveTask extends DefaultTask {

    /**
     * Creates a new OwlSaveTask object
     */
    public OwlSaveTask() {
    	getOutputs().upToDateWhen(task -> false);
    }

    /**
     * The Fuseki server endpoint URL property (Required)
     *
     * @return String Property
     */
    @Input
    public abstract Property<String> getEndpointURL();

    /**
     * The env var whose value is a username for authenticating to the SPARQL endpoint (Optional)
     *
     * @return String Property
     */
    @Optional
    @Input
    public abstract Property<String> getAuthenticationUsername();

    /**
     * The env var whose value is a password for authenticating to the SPARQL endpoint (Optional)
     *
     * @return String Property
     */
    @Optional
    @Input
    public abstract Property<String> getAuthenticationPassword();

    /**
     * The required OASIS XML catalog path property (Required)
     *
     * @return File Property
     */
    @OutputFile
    public abstract Property<File> getCatalogPath();

	/**
     * The file extension for the saved files (Optional, default is 'ttl', optionsL: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld)
     * 
     * @return String Property
     */
	@Optional
    @Input
    public abstract Property<String> getFileExtension();

    /**
     * The optional gradle task debug property (default is false).
     *
     * @return Boolean Property
     */
    @Optional
    @Input
    public abstract Property<Boolean> getDebug();

    /**
     * The gradle task action logic.
     */
    @TaskAction
    public void run() {
        final ArrayList<String> args = new ArrayList<>();
        
        if (getEndpointURL().isPresent()) {
            args.add("-e");
            args.add(getEndpointURL().get());
        }
        if (getAuthenticationUsername().isPresent()) {
            args.add("-u");
            args.add(getAuthenticationUsername().get());
        }
        if (getAuthenticationPassword().isPresent()) {
            args.add("-p");
            args.add(getAuthenticationPassword().get());
        }
        if (getCatalogPath().isPresent()) {
            args.add("-c");
            args.add(getCatalogPath().get().getAbsolutePath());
        }
        if (getFileExtension().isPresent()) {
            args.add("-f");
            args.add(getFileExtension().get());
        }
        if (getDebug().isPresent() && getDebug().get()) {
            args.add("-d");
        }
        
        try {
            OwlSaveApp.main(args.toArray(new String[0]));
        } catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
        }    
    }
}
