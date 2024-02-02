package org.openclover.idea.coverage;

import java.io.File;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.openclover.util.Maps.newHashMap;

public class SnapshotFileMutexService implements SnapshotFileMutex {
    private final Map<File, CountedLock> locks = newHashMap();

    private static class CountedLock {
        private final Lock lock = new ReentrantLock();
        private int count = 1;
    }


    @Override
    public void lockFile(File file) {
        CountedLock countedLock;
        synchronized (locks) {
            countedLock = locks.get(file);
            if (countedLock == null) {
                countedLock = new CountedLock();
                locks.put(file, countedLock);
            } else {
                ++ countedLock.count;
            }
        }
        countedLock.lock.lock();
    }

    @Override
    public void releaseFile(File file) {
        final CountedLock countedLock;
        synchronized (locks) {
            countedLock = locks.get(file);
            if (countedLock == null) {
                throw new IllegalMonitorStateException("Unlocking not locked file " + file.getAbsolutePath());
            }
            if (-- countedLock.count == 0) {
                locks.remove(file);
            }

        }
        countedLock.lock.unlock();
    }

    int mapSize() {
        return locks.size();
    }
}
