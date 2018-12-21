package com.cqkct.FunKidII.service.tlc;

public class NotYetLoginException extends IllegalStateException {
    private static final long serialVersionUID = 1903222667790630714L;

    public NotYetLoginException() {}

    public NotYetLoginException(String message) {
        super(message);
    }
}
