public class RuleAlreadyExistsException extends Exception {
    public RuleAlreadyExistsException() {
    }

    public RuleAlreadyExistsException(String message) {
        super(message);
    }

    public RuleAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuleAlreadyExistsException(Throwable cause) {
        super(cause);
    }

    public RuleAlreadyExistsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
