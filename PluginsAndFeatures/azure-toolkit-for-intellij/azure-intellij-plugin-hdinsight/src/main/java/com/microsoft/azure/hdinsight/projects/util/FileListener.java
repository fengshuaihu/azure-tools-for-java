package com.microsoft.azure.hdinsight.projects.util;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

public class FileListener  extends FileAlterationListenerAdaptor {
    private FileMonitor monitor;
    private File file;

    private int scanTimes = 0;

    public FileListener(File file) {
        super();
        this.file = file;
    }

    public void setMonitor(FileMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void onStart(FileAlterationObserver observer) {
        super.onStart(observer);
        try {
            if (file.exists()) {
                this.file.delete();
            }
        } catch (Exception e) {

        } finally {
            this.monitor.stop();
        }
    }

    @Override
    public void onStop(FileAlterationObserver observer) {
        super.onStop(observer);
        if (++scanTimes >= 30) {
            this.monitor.stop();
        }
    }

}
