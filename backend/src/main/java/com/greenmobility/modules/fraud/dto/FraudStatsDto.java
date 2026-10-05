package com.greenmobility.modules.fraud.dto;

public class FraudStatsDto {
    private long totalAlerts;
    private long pendingAlerts;
    private long resolvedAlerts;
    private double highRiskRatio;

    public FraudStatsDto() {}

    public FraudStatsDto(long totalAlerts, long pendingAlerts, long resolvedAlerts, double highRiskRatio) {
        this.totalAlerts = totalAlerts;
        this.pendingAlerts = pendingAlerts;
        this.resolvedAlerts = resolvedAlerts;
        this.highRiskRatio = highRiskRatio;
    }

    public long getTotalAlerts() { return totalAlerts; }
    public void setTotalAlerts(long totalAlerts) { this.totalAlerts = totalAlerts; }

    public long getPendingAlerts() { return pendingAlerts; }
    public void setPendingAlerts(long pendingAlerts) { this.pendingAlerts = pendingAlerts; }

    public long getResolvedAlerts() { return resolvedAlerts; }
    public void setResolvedAlerts(long resolvedAlerts) { this.resolvedAlerts = resolvedAlerts; }

    public double getHighRiskRatio() { return highRiskRatio; }
    public void setHighRiskRatio(double highRiskRatio) { this.highRiskRatio = highRiskRatio; }
}
