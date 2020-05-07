package io.opencaesar.owl.reason;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.io.CharStreams;

public class App {

	@Parameter(
		names = { "--input", "-i" },
		description = "Path to the OWL catalog 1 file (Required)",
		validateWith = CatalogPath.class,
		required = true,
		order = 1)
	private String catalogPath1 = null;

	@Parameter(
		names = { "--output", "-o" },
		description = "Path to the OWL catalog 2 file (Required)",
		validateWith = CatalogPath.class, 
		required = true,
		order = 2)
	private String catalogPath2 = ".";

	@Parameter(
		names = { "-d", "--debug" },
		description = "Shows debug logging statements",
		order = 3)
	private boolean debug;

	@Parameter(
		names = { "--help", "-h" },
		description = "Displays summary of options",
		help = true,
		order = 4)
	private boolean help;

	private final Logger LOGGER = LogManager.getLogger(App.class);

	public static void main(final String... args) {
		final App app = new App();
		final JCommander builder = JCommander.newBuilder().addObject(app).build();
		builder.parse(args);
		if (app.help) {
			builder.usage();
			return;
		}
		if (app.debug) {
			final Appender appender = LogManager.getRootLogger().getAppender("stdout");
			((AppenderSkeleton) appender).setThreshold(Level.DEBUG);
		}
		try {
			app.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                       OWL Diff " + getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info(("Input OWL Catalog = " + catalogPath1));
		LOGGER.info(("Output Diff Report = " + catalogPath2));


		LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}

	public Collection<File> collectOwlFiles(final File directory) {
		ArrayList<File> omlFiles = new ArrayList<File>();
		for (File file : directory.listFiles()) {
			if (file.isFile()) {
				if (getFileExtension(file).equals("owl")) {
					omlFiles.add(file);
				}
			} else if (file.isDirectory()) {
				omlFiles.addAll(collectOwlFiles(file));
			}
		}
		return omlFiles;
	}

	private String getFileExtension(final File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1)
        	return fileName.substring(fileName.lastIndexOf(".")+1);
        else 
        	return "";
	}

	/**
	 * Get application version id from properties file.
	 * 
	 * @return version string from build.properties or UNKNOWN
	 */
	public String getAppVersion() {
		String version = "UNKNOWN";
		try {
			InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("version.txt");
			InputStreamReader reader = new InputStreamReader(input);
			version = CharStreams.toString(reader);
		} catch (IOException e) {
			String errorMsg = "Could not read version.txt file." + e;
			LOGGER.error(errorMsg, e);
		}
		return version;
	}

	public static class CatalogPath implements IParameterValidator {
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File file = new File(value);
			if (!file.getName().endsWith("catalog.xml")) {
				throw new ParameterException("Parameter " + name + " should be a valid OWL catalog path");
			}
		}
	}

	public static class OutputFilePath implements IParameterValidator {
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File folder = new File(value).getParentFile();
			if (!folder.exists()) {
				folder.mkdir();
			}
		}
	}
}
