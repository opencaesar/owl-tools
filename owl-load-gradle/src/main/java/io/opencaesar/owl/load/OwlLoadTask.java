package io.opencaesar.owl.load;

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
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

/**
 * Gradle incremental build support is available for an internal input file collection
 * derived from the catalogPath and fileExtension properties.
 */
public abstract class OwlLoadTask extends DefaultTask {

    /**
     * Creates a new OwlLoadTask object
     */
    public OwlLoadTask() {
    	getOutputs().upToDateWhen(task -> {
            boolean incremental = getIncremental().isPresent() ? getIncremental().get() : true;
            if (incremental) {
    			getInputFolder().set(getCatalogPath().get().getParentFile());
            }
            return incremental;
        });
    }

    /**
     * The required gradle task Fuseki server endpoint URL property.
     *
     * @return String Property
     */
    @Input
    public abstract Property<String> getEndpointURL();

    /**
     * The short name of the query service (optional, default is 'sparql').
     *
     * @return String Property
     */
    @Optional
    @Input
    public abstract Property<String> getQueryService();

    /**
     * The env var whose value is a username for authenticating to the SPARQL endpoint. (Optional).
     *
     * @return String Property
     */
    @Optional
    @Input
    public abstract Property<String> getAuthenticationUsername();

    /**
     * The env var whose value is a password for authenticating to the SPARQL endpoint. (Optional).
     *
     * @return String Property
     */
    @Optional
    @Input
    public abstract Property<String> getAuthenticationPassword();

    /**
     * The required gradle task OASIS XML catalog path property.
     *
     * @return File Property
     */
    @Input
    public abstract Property<File> getCatalogPath();

	/**
     * The gradle task list of ontology IRIs property (Required if 'irisPath' is not set).
     * 
     * @return List of Strings
     */
	@Optional
    @Input
    public abstract ListProperty<String> getIris();

	/**
	 * The txt file listing all ontology IRIs (one per line) (Required if 'iris' is not set).
	 * 
	 * @return RegularFile Property
	 */
	@Optional
	@InputFile
	public abstract Property<File> getIrisPath();

	/**
     * The gradle task ontology file extensions property (optional and default is both owl and ttl).
     *
     * @return List of Strings Property
     */
    @Optional
    @Input
    public abstract ListProperty<String> getFileExtensions();

    /**
     * Whether to load to the default graph (Optional, default is false).
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
        
        if (getEndpointURL().isPresent()) {
            args.add("-e");
            args.add(getEndpointURL().get());
        }
        if (getQueryService().isPresent()) {
            args.add("-q");
            args.add(getQueryService().get());
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
        if (getIris().isPresent()) {
	        getIris().get().forEach(iri -> {
	            args.add("-i");
	            args.add(iri);
	        });
        }
		if (getIrisPath().isPresent()) {
			args.add("-ip");
			args.add(getIrisPath().get().getAbsolutePath());
		}
        if (getFileExtensions().isPresent()) {
            getFileExtensions().get().forEach(ext -> {
                args.add("-f");
                args.add(ext);
            });
        }
        if (getLoadToDefaultGraph().isPresent() && getLoadToDefaultGraph().get()) {
            args.add("-df");
        }
        if (getDebug().isPresent() && getDebug().get()) {
            args.add("-d");
        }
        
        try {
        	if (getIncremental().isPresent() && !getIncremental().get()) {
	            OwlLoadApp.main(args.toArray(new String[0]));
        	} else { // run incrementally by default
        		final Set<File> deltas = new HashSet<>();
	        	inputChanges.getFileChanges(getInputFolder()).forEach(f -> deltas.add(f.getFile()));
	            OwlLoadApp.mainWithDeltas(deltas, args.toArray(new String[0]));
        	}
        } catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
        }    
    }
}
