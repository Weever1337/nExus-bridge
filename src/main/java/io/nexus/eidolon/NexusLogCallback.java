package io.nexus.eidolon;

@FunctionalInterface
public interface NexusLogCallback {
    enum LogLevel {
        INFO,
        WARN,
        ERROR
    }
    void onLog(int level, String message);
}