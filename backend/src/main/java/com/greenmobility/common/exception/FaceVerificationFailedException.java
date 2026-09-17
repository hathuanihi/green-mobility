package com.greenmobility.common.exception;

import com.greenmobility.modules.drivervehicle.dto.FaceVerifyResponse;

public class FaceVerificationFailedException extends RuntimeException {

    private final FaceVerifyResponse data;

    public FaceVerificationFailedException(String message, FaceVerifyResponse data) {
        super(message);
        this.data = data;
    }

    public FaceVerifyResponse getData() {
        return data;
    }
}
