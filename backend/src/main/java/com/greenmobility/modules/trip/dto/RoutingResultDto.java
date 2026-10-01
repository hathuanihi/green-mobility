package com.greenmobility.modules.trip.dto;

import java.util.List;

/**
 * DTO wrapping the routing result from Goong/OSRM.
 * Used by start-arriving and start-trip responses to provide navigation data.
 */
public class RoutingResultDto {

    private Integer distanceM;
    private Integer durationS;
    private String polyline;
    private List<RoutingStepDto> steps;

    public RoutingResultDto() {}

    public RoutingResultDto(int distanceM, int durationS, String polyline, List<RoutingStepDto> steps) {
        this.distanceM = distanceM;
        this.durationS = durationS;
        this.polyline = polyline;
        this.steps = steps;
    }

    public Integer getDistanceM() { return distanceM; }
    public void setDistanceM(Integer distanceM) { this.distanceM = distanceM; }

    public Integer getDurationS() { return durationS; }
    public void setDurationS(Integer durationS) { this.durationS = durationS; }

    public String getPolyline() { return polyline; }
    public void setPolyline(String polyline) { this.polyline = polyline; }

    public List<RoutingStepDto> getSteps() { return steps; }
    public void setSteps(List<RoutingStepDto> steps) { this.steps = steps; }
}
