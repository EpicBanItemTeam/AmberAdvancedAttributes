package io.izzel.aaa.api.data;

public class UnreachableSlotDataException extends Exception {
    private static final long serialVersionUID = 6222090226224399350L;

    public UnreachableSlotDataException(String message) {
        super(message);
    }

    public UnreachableSlotDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
