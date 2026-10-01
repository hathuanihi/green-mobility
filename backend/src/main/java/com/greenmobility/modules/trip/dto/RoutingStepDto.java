package com.greenmobility.modules.trip.dto;

/**
 * Individual navigation step (turn-by-turn instruction).
 */
public class RoutingStepDto {
    private String instruction;
    private Integer distanceM;
    private Integer durationS;
    private String maneuver;
    private double[] startLocation;

    public RoutingStepDto() {}

    public RoutingStepDto(String instruction, Integer distanceM, Integer durationS, String maneuver) {
        this.instruction = instruction;
        this.distanceM = distanceM;
        this.durationS = durationS;
        this.maneuver = maneuver;
    }

    public RoutingStepDto(String instruction, Integer distanceM, Integer durationS, String maneuver, double[] startLocation) {
        this.instruction = instruction;
        this.distanceM = distanceM;
        this.durationS = durationS;
        this.maneuver = maneuver;
        this.startLocation = startLocation;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public Integer getDistanceM() {
        return distanceM;
    }

    public void setDistanceM(Integer distanceM) {
        this.distanceM = distanceM;
    }

    public Integer getDurationS() {
        return durationS;
    }

    public void setDurationS(Integer durationS) {
        this.durationS = durationS;
    }

    public String getManeuver() {
        return maneuver;
    }

    public void setManeuver(String maneuver) {
        this.maneuver = maneuver;
    }

    public double[] getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(double[] startLocation) {
        this.startLocation = startLocation;
    }
}
