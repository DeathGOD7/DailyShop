package io.github.divios.lib.dLib.log;

import io.github.divios.lib.dLib.log.options.dLogEntry;
import io.github.divios.lib.storage.dataManager;

public class dLog {

    private static final dataManager dManager = dataManager.getInstance();

    /*
    Method to log entries into database. Is a shortcut for dataManager#addLogEntry
     */
    public static void log(dLogEntry entry) {
        dManager.addLogEntry(entry);
    }

}