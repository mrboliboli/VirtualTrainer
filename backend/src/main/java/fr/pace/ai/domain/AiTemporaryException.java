package fr.pace.ai.domain;

public class AiTemporaryException extends AiUnavailableException {
    public AiTemporaryException(String message) { super(message); }
    public AiTemporaryException(String message, Throwable cause) { super(message, cause); }
}
