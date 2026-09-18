import { CARBON_CONSTANTS } from "@/constants";

/**
 * Format currency to Vietnamese Dong string (e.g. 76.000 đ)
 */
export function formatCurrency(amount: number | string | undefined | null): string {
  if (amount === undefined || amount === null) return "0 đ";
  const num = typeof amount === "string" ? Number(amount) : amount;
  return `${num.toLocaleString("vi-VN")} đ`;
}

/**
 * Format date and time string to Vietnamese local format (e.g. 08:30 05/10/2026)
 */
export function formatDateTime(dateInput: string | Date | undefined | null): string {
  if (!dateInput) return "";
  const date = typeof dateInput === "string" ? new Date(dateInput) : dateInput;
  return date.toLocaleString("vi-VN");
}

/**
 * Format time only (e.g. 08:30)
 */
export function formatTime(dateInput: string | Date | undefined | null): string {
  if (!dateInput) return "";
  const date = typeof dateInput === "string" ? new Date(dateInput) : dateInput;
  return date.toLocaleTimeString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * Calculate eco equivalencies from grams of CO2 saved
 */
export function calculateCarbonEquivalents(co2Grams: number = 0) {
  const treeDays = (co2Grams / CARBON_CONSTANTS.TREE_DAILY_ABSORPTION_GRAMS).toFixed(1);
  const ledHours = (co2Grams / CARBON_CONSTANTS.LED_HOURLY_GRAMS).toFixed(1);
  const co2Kg = (co2Grams / 1000.0).toFixed(2);

  return {
    co2Kg,
    treeDays,
    ledHours,
  };
}
