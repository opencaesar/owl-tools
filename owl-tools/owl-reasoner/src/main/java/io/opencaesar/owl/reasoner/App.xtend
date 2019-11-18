package io.opencaesar.owl.reasoner

import com.beust.jcommander.IParameterValidator
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import java.io.File
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import io.opencaesar.owl.reasoner.OwlValidator
import java.util.List
import java.util.ArrayList

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
		names=#["-d", "--debug"], 
		description="Shows debug logging statements", 
		order=4
	)
	package boolean debug

	@Parameter(
		names=#["--help", "-h"], 
		description="Displays summary of options", 
		help=true, 
		order=4
	)
	package boolean help

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
		
		val validator = new OwlValidator(
			validateSatisfiability,
			indicateStatus,
			locationMapping,
			iris
		)
		
		validator.run()
		
		LOGGER.info("=================================================================")
		LOGGER.info("                          E N D")
		LOGGER.info("=================================================================")
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
