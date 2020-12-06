package io.opencaesar.owl.audit;

import io.opencaesar.owl.audit.util.ChildFirstURLClassLoader;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class OwlAuditTask extends DefaultTask {

    public String host;

    public int port;

    public String dataset;

    public String audit_tree;

    public String iri_file;

    public String prefix_file;

    public String output_file;

    public boolean debug;

    @TaskAction
    public void run() {
        final ArrayList<String> args = new ArrayList<>();
        args.add("--host");
        args.add(host);
        args.add("--port");
        args.add(Integer.toString(port));
        args.add("--dataset");
        args.add(dataset);
        args.add("--audit-tree");
        args.add(audit_tree);
        args.add("--iri-file");
        args.add(iri_file);
        args.add("--prefix-file");
        args.add(prefix_file);
        args.add("--output-file");
        args.add(output_file);
        if (debug)
            args.add("--debug");

        try {
            URL jarURL = this.getClass().getResource("/owl-audit.jar");
            if (null == jarURL) {
                System.err.println("The OwlAuditTask jar is missing the resource 'owl-audit.jar'");
                System.exit(255);
            }

            final File jarFile = extractJarFile(jarURL);
            final URL[] urls = new URL[1];
            urls[0] = jarFile.toURI().toURL();
            final ChildFirstURLClassLoader cl = new ChildFirstURLClassLoader(urls, this.getClass().getClassLoader());
            Class<?> app = cl.loadClass("io.opencaesar.owl.audit.OwlAuditApp");
            Method main = app.getMethod("main", String[].class);
            String[] params = args.toArray(new String[args.size()]);
            main.invoke(null, new Object[] { params });
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            e.printStackTrace(System.err);
            System.exit(255);
        }
    }

    private File extractJarFile(URL jarURL) throws Exception {
        Path dir = Files.createTempDirectory("audit-framework-");
        dir.toFile().deleteOnExit();

        File jarFile = Files.createTempFile(dir,"owl-audit-", ".jar").toFile();
        jarFile.deleteOnExit();

        final byte[] buffer = new byte[4096];

        final InputStream is = jarURL.openStream();
        final FileOutputStream fos = new FileOutputStream(jarFile);
        int len;
        while ((len = is.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }
        fos.close();
        is.close();

        return jarFile;
    }
}
