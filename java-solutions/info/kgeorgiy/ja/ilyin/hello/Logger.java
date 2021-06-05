package info.kgeorgiy.ja.ilyin.hello;

/**
 * @author Yaroslav Ilin
 * Logger for logging.
 */

public class Logger {
    /**
     * Logging request
     * @param request for request
     */
    public static void request(String request) {
        System.out.println("request sent: " + request);
    }

    /**
     * Logging response
     * @param response for response
     */
    public static void response(String response) {
        System.out.println("response sent: " + response);
    }

    /**
     * Logging exception
     * @param message for message
     * @param exception exception
     */
    public static void exception(String message, Exception exception) {
        System.err.println("Exception: " + message + " " + exception.getMessage());
    }
}
