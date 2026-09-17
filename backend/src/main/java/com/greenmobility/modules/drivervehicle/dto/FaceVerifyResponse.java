package com.greenmobility.modules.drivervehicle.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class FaceVerifyResponse {

    private boolean isPassed;
    private BigDecimal similarityScore;
    private boolean isActiveShift;
    private Instant verifiedAt;

    public FaceVerifyResponse() {}

    public FaceVerifyResponse(boolean isPassed, BigDecimal similarityScore, boolean isActiveShift, Instant verifiedAt) {
        this.isPassed = isPassed;
        this.similarityScore = similarityScore;
        this.isActiveShift = isActiveShift;
        this.verifiedAt = verifiedAt;
    }

    public boolean isPassed() { return isPassed; }
    public void setPassed(boolean passed) { isPassed = passed; }

    public BigDecimal getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(BigDecimal similarityScore) { this.similarityScore = similarityScore; }

    public boolean isActiveShift() { return isActiveShift; }
    public void setActiveShift(boolean activeShift) { isActiveShift = activeShift; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
}
