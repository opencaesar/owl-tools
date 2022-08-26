package io.opencaesar.owl.fuseki;

import java.io.File;
import java.io.IOException;

import groovy.lang.Closure;
import org.gradle.api.*;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;

public abstract class StopFusekiTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getOutputFolderPath();

    @Optional
    @Input
    public abstract Property<Boolean> getDebug();

    @Override
    public Task configure(Closure closure) {
        Task t = super.configure(closure);
        t.setOnlyIf(new Closure<Boolean>(null) {
            public Boolean doCall(Task ignore) {
                final File pidFile = getOutputFolderPath().file(StartFusekiTask.PID_FILENAME).get().getAsFile();
                if (pidFile.exists()) {
                    try {
                        java.util.Optional<Long> pid = StartFusekiTask.findFusekiProcessId(pidFile);
                        return pid.isEmpty();
                    } catch (IOException e) {
                        return false;
                    }
                } else
                    return true;
            }
        });

        return t;
    }

    @Override
    public Task doLast(Action<? super Task> action) {
        Task t = super.doLast(action);
        final File pidFile = getOutputFolderPath().file(StartFusekiTask.PID_FILENAME).get().getAsFile();
        if (pidFile.exists()) {
            try {
                java.util.Optional<Long> pid = StartFusekiTask.findFusekiProcessId(pidFile);
                if (!pid.isEmpty()) {
                    java.util.Optional<ProcessHandle> ph = findProcess(pid.get());
                    if (ph.isPresent()) {
                        if (!ph.get().destroyForcibly()) {
                            throw new IllegalArgumentException("Failed to kill a Fuseki server process with pid=" + pid.get());
                        } else {
                            System.out.println("Fuseki server with pid=" + pid.get() + " has been stopped");
                        }
                    }
                    pidFile.delete();
                }
            } catch (IOException e) {
                throw new GradleException("StopFusekiTask: " + e.getLocalizedMessage(), e);
            }
        }
        return t;
    }

    /**
     * Find a process handle given a process id.
     *
     * @param pid The process id
     * @return The process handle of the given process id
     */
    public static java.util.Optional<ProcessHandle> findProcess(long pid) {
        return ProcessHandle
                .allProcesses()
                .filter(p -> p.pid() == pid)
                .findFirst();
    }
}
