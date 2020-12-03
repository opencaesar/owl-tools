package io.opencaesar.owl.audit;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import java.util.ArrayList;

public class OwlAuditTask extends DefaultTask {

    public String host;

    public int port;

    public String dataset;

    public String audit_tree;

    public String iri_file;

    public String prefix_file;

    public boolean debug;

    @TaskAction
    public void run() {
        final ArrayList<String> args = new ArrayList<String>();
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
        if (debug)
            args.add("--debug");

        try {
            AuditApp.main(args.toArray(new String[args.size()]));
        } catch (Exception e) {
            throw new TaskExecutionException(this, e);
        }
    }
}
