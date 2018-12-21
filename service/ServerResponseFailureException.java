package com.cqkct.FunKidII.service;

public class ServerResponseFailureException extends RuntimeException {
    public ServerResponseFailureException() {
    }

    public ServerResponseFailureException(String msg) {
        super(msg);
    }
}