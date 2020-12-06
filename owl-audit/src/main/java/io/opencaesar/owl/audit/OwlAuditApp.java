package io.opencaesar.owl.audit;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.util.ClassCache;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Use JRuby's JavaEmbedUtils to initialize the Ruby environment to execute the audit framework.
 * With javax.script.ScriptEngineManager, we cannot initialize the Ruby environment's load paths.
 * @see: https://github.com/jruby/jruby/wiki/DirectJRubyEmbedding
 * @see: https://github.com/jruby/jruby/wiki/Rubygems
 */
public class OwlAuditApp {

    @Parameter(
            names = {"--host"},
            description = "Fuseki host (Required)",
            required = true,
            order = 1)
    private String host;

    @Parameter(
            names = {"--port"},
            description = "Fuseki port (Required)",
            required = true,
            order = 2)
    private int port;

    @Parameter(
            names = {"--dataset"},
            description = "Fuseki dataset (Required)",
            required = true,
            order = 3)
    private String dataset;

    @Parameter(
            names = {"--audit-tree"},
            description = "Directory tree of audits (Required)",
            required = true,
            order = 4)
    private String audit_tree;

    @Parameter(
            names = {"--iri-file"},
            description = "File of ontology IRIs to audit (Required)",
            required = true,
            order = 5)
    private String iri_file;

    @Parameter(
            names = {"--prefix-file"},
            description = "YAML file of namespace prefixes (Required)",
            required = true,
            order = 6)
    private String prefix_file;

    @Parameter(
            names = {"--debug", "-d"},
            description = "Shows debug logging statements",
            order = 7)
    private boolean debug;

    @Parameter(
            names = {"--output-file"},
            description = "File for JUnit Audit results (Required)",
            required = true,
            order = 8)
    private String output_file;

    @Parameter(
            names = {"--help", "-h"},
            description = "Displays summary of options",
            help = true,
            order = 9)
    private boolean help;

    private final static Logger LOGGER = Logger.getLogger(OwlAuditApp.class);

    public static void main(final String... args) throws Exception {
        URL log4jConfig = OwlAuditApp.class.getClassLoader().getResource("log4j.xml");
        if (null != log4jConfig)
            DOMConfigurator.configure(log4jConfig);
        final OwlAuditApp app = new OwlAuditApp();
        final JCommander builder = JCommander.newBuilder().addObject(app).build();
        builder.parse(args);
        if (app.help) {
            builder.usage();
            return;
        }
        if (app.debug) {
            final Logger rootLogger = LogManager.getRootLogger();
            if (null != rootLogger) {
                final Appender appender = rootLogger.getAppender("stdout");
                if (appender instanceof AppenderSkeleton) {
                    ((AppenderSkeleton) appender).setThreshold(Level.DEBUG);
                }
            }
        }
        app.run();
    }

    private static String getResourcePath(String resource) {
        final URL url = OwlAuditApp.class.getResource(resource);
        if (null == url)
            throw new IllegalArgumentException("Cannot find on the classpath the following resource: "+resource);
        return url.getPath();
    }

    private void run() throws Exception {
        LOGGER.info("=================================================================");
        LOGGER.info("                        S T A R T");
        LOGGER.info("                     OWL Audits " + getAppVersion());
        LOGGER.info("=================================================================");

        try {
            final List<String> loadPaths = new ArrayList<>();
            loadPaths.add(getResourcePath("/audit-framework"));
            loadPaths.add(getResourcePath("/rubygems/logger-application-0.0.2/lib"));

            final ClassCache cc = JavaEmbedUtils.createClassCache(this.getClass().getClassLoader());
            final RubyInstanceConfig config = new RubyInstanceConfig();
            config.setClassCache(cc);
            config.setLoader(this.getClass().getClassLoader());
            final Ruby runtime = JavaEmbedUtils.initialize(loadPaths, config);
            final RubyRuntimeAdapter adapter = JavaEmbedUtils.newRuntimeAdapter();
            String script = "require 'tools/run-audits-jena.rb'\n"+
                    "JenaAuditApplication.new('run-audits-jena.rb',\n"+
                    (debug ? "['--debug',\n" : "[\n")+
                    "'--host','"+host+"',\n"+
                    "'--port','"+port+"',\n"+
                    "'--dataset','"+dataset+"',\n"+
                    "'--audit-tree','"+audit_tree+"',\n"+
                    "'--iri-file','"+iri_file+"',\n"+
                    "'--prefix-file','"+prefix_file+"',\n"+
                    "'--output-file','"+output_file+"'\n"+
                    "]).start\n";
            final IRubyObject result = adapter.eval(runtime, script);
            LOGGER.info("Script:\n"+script);
            LOGGER.info("Result:\n"+result);
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            e.printStackTrace();
            System.exit(255);
        }

        LOGGER.info("=================================================================");
        LOGGER.info("                          E N D");
        LOGGER.info("=================================================================");
    }

    private String getAppVersion() {
        String version = "UNKNOWN";
        try {
            InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("version.txt");
            if (null != input) {
                InputStreamReader reader = new InputStreamReader(input);
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[100];
                int nRead;
                while ((nRead = reader.read(buf)) != -1) {
                    sb.append(buf, 0, nRead);
                }
                reader.close();
                version = sb.toString();
            }
        } catch (IOException e) {
            String errorMsg = "Could not read version.txt file." + e;
            LOGGER.error(errorMsg, e);
        }
        return version;
    }
}
