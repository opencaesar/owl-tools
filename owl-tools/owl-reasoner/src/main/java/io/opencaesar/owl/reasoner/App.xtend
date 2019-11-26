package io.opencaesar.owl.reasoner

import com.beust.jcommander.IParameterValidator
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import java.io.File
import java.util.ArrayList
import java.util.List
import java.util.concurrent.locks.ReentrantReadWriteLock
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper
import org.semanticweb.owlapi.util.AutoIRIMapper
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyIRIMapperImpl
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl

class App {

	@Parameter(
		names=#["--satisfiablility"], 
		description="validate satisfiability of all classes",
		order=1
	)
	package boolean validateSatisfiability = false
	
	@Parameter(
		names=#["--indicate-status"], 
		description="indicate validation status via exit value",
		order=2
	)
	package boolean indicateStatus = false

	@Parameter(
		names=#["--location-mapping"], 
		description="location mapping file",
		validateWith = FilePath,
		order=3
	)
	package String locationMapping

	@Parameter(
		names=#["--root-directory"], 
		description="root directory", 
		validateWith = DirectoryPath,
		order=4
	)
	package String rootDirectory

	@Parameter(
		names=#["-d", "--debug"], 
		description="Shows debug logging statements", 
		order=5
	)
	package boolean debug

	@Parameter(
		names=#["--help", "-h"], 
		description="Displays summary of options", 
		help=true, 
		order=5)
	package boolean help

	@Parameter(
		description = "IRIs"
	)
	package List<String> iris = new ArrayList<String>();

	@Parameter(description = "IRIs")
	package List<String> iris = new ArrayList<String>();
	
	val LOGGER = LogManager.getLogger(App)

	def static void main(String ... args) {
		val app = new App
		val builder = JCommander.newBuilder().addObject(app).build()
		builder.parse(args)
		if (app.help) {
			builder.usage()
			return
		}
		if (app.debug) {
			val appender = LogManager.getRootLogger.getAppender("stdout")
			(appender as AppenderSkeleton).setThreshold(Level.DEBUG)
		}
		app.run()
	}

	def void run() {
		LOGGER.info("=================================================================")
		LOGGER.info("                        S T A R T")
		LOGGER.info("=================================================================")
		
		val ontologyManager = new OWLOntologyManagerImpl(new OWLDataFactoryImpl, new ReentrantReadWriteLock)

		val OWLOntologyIRIMapper ontologyIRIMapper =
			if (rootDirectory !== null) {
				val d = new File(rootDirectory)
				new AutoIRIMapper(d, true)
			}
			else
				throw new RuntimeException("no location mapping specified and no root directory specified")
		
		val validator = new OwlValidator(iris, ontologyManager)
		
		validator.run(validateSatisfiability)
		
		LOGGER.info("=================================================================")
		LOGGER.info("                          E N D")
		LOGGER.info("=================================================================")
	}

	static class DirectoryPath implements IParameterValidator {
		override validate(String name, String value) throws ParameterException {
			val file = new File(value)
			if (file.isDirectory) {
				throw new ParameterException("parameter " + name + " is not a valid directory path");
			}
	  	}
	}
	
	static class FilePath implements IParameterValidator {
		override validate(String name, String value) throws ParameterException {
			val file = new File(value)
			if (file.isFile) {
				throw new ParameterException("parameter " + name + " is not a valid file path");
			}
	  	}
	}
}
