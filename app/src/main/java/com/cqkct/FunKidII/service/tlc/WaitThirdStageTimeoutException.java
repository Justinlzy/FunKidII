package com.cqkct.FunKidII.service.tlc;

public class WaitThirdStageTimeoutException extends Exception {
    private static final long serialVersionUID = 1900122667790630714L;

    public WaitThirdStageTimeoutException() {}

    public WaitThirdStageTimeoutException(String message) {
        super(message);
    }
}
