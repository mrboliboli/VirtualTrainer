package fr.pace.ai.openai;

final class OpenAiHttpException extends RuntimeException {
    private final int statusCode;

    OpenAiHttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    int statusCode() {
        return statusCode;
    }
}
