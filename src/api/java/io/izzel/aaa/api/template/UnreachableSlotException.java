package io.izzel.aaa.api.template;

public class UnreachableSlotException extends Exception {
    private static final long serialVersionUID = 6222090226224399350L;

    public UnreachableSlotException(String message) {
        super(message);
    }

    public UnreachableSlotException(String message, Throwable cause) {
        super(message, cause);
    }
}
