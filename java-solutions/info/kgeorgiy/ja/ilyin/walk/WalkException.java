package info.kgeorgiy.ja.ilyin.walk;

/**
 * @author Yaroslav Ilin
 */

public class WalkException extends Exception {
    public WalkException() {
    }

    public WalkException(String message) {
        super(message);
    }

    public WalkException(String message, Throwable cause) {
        super(message, cause);
    }

    public WalkException(Throwable cause) {
        super(cause);
    }
}
