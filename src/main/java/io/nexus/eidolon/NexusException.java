package io.nexus.eidolon;

public class NexusException extends Exception {
    public NexusException(String message) {
        super(message);
    }

    public NexusException(String message, Throwable cause) {
        super(message, cause);
    }
}