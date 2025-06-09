package com.github.gabrycoder.lagmonitor.storage;

import com.github.gabrycoder.lagmonitor.task.TPSHistoryTask;

public class TPSSaveTask implements Runnable {

    private final TPSHistoryTask tpsHistoryTask;
    private final Storage storage;

    public TPSSaveTask(TPSHistoryTask tpsHistoryTask, Storage storage) {
        this.tpsHistoryTask = tpsHistoryTask;
        this.storage = storage;
    }

    @Override
    public void run() {
        float lastSample = tpsHistoryTask.getLastSample();
        if (lastSample > 0 && lastSample < 50) {
            storage.saveTps(lastSample);
        }
    }
}
