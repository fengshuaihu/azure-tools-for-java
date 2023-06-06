package com.microsoft.azure.hdinsight.projects.util;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

public class FileMonitor {

    private FileAlterationMonitor monitor;
    private FileAlterationListener listener;

    public FileMonitor(long interval) {
        monitor = new FileAlterationMonitor(interval);
    }

    public void monitor(String path, FileAlterationListener listener) {
        this.listener = listener;

        FileAlterationObserver observer = new FileAlterationObserver(new File(path));
        monitor.addObserver(observer);
        observer.addListener(listener);
    }

    public void stop() {
        try {
            monitor.stop();
        } catch (Exception e) {

        }
    }

    public void start() {
        try {
            monitor.start();
        } catch (Exception e) {

        }
    }

}
