import apiClient from "./api";
import { ApiResponse, LiveTripDto, TodayStatsDto, Trip } from "@/types";

export async function fetchLiveTrips(): Promise<LiveTripDto[]> {
  const response = await apiClient.get<ApiResponse<LiveTripDto[]>>("/admin/trips/live");
  if (response.data.success && response.data.data) {
    return response.data.data;
  }
  return [];
}

export async function fetchTodayStats(): Promise<TodayStatsDto | null> {
  const response = await apiClient.get<ApiResponse<TodayStatsDto>>("/admin/trips/stats/today");
  if (response.data.success && response.data.data) {
    return response.data.data;
  }
  return null;
}

export async function fetchAllTrips(): Promise<Trip[]> {
  const response = await apiClient.get<ApiResponse<Trip[]>>("/admin/trips");
  if (response.data.success && response.data.data) {
    return response.data.data;
  }
  return [];
}
