package fr.pace.ai.domain;

public class AiInvalidResponseException extends AiUnavailableException {
    public AiInvalidResponseException(String message) { super(message); }
    public AiInvalidResponseException(String message, Throwable cause) { super(message, cause); }
}
