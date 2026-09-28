package com.greenmobility.common.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiện ích mã hóa và giải mã Polyline chuẩn quốc tế (Google / Mapbox / Goong encoded polyline algorithm - 1e5 precision).
 */
public final class PolylineUtil {

    private PolylineUtil() {}

    public record LatLngPoint(double lat, double lng) {}

    /**
     * Mã hóa danh sách tọa độ (List of LatLngPoint) thành chuỗi Polyline chuẩn.
     */
    public static String encode(List<LatLngPoint> points) {
        StringBuilder result = new StringBuilder();
        int lastLat = 0;
        int lastLng = 0;

        for (LatLngPoint point : points) {
            int lat = (int) Math.round(point.lat() * 1e5);
            int lng = (int) Math.round(point.lng() * 1e5);

            int dLat = lat - lastLat;
            int dLng = lng - lastLng;

            encodeChunk(dLat, result);
            encodeChunk(dLng, result);

            lastLat = lat;
            lastLng = lng;
        }

        return result.toString();
    }

    private static void encodeChunk(int v, StringBuilder result) {
        int val = v < 0 ? ~(v << 1) : (v << 1);
        while (val >= 0x20) {
            result.append((char) ((0x20 | (val & 0x1f)) + 63));
            val >>= 5;
        }
        result.append((char) (val + 63));
    }

    /**
     * Giải mã chuỗi Polyline thành danh sách tọa độ.
     */
    public static List<LatLngPoint> decode(String encoded) {
        List<LatLngPoint> points = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return points;
        }

        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                if (index >= len) break;
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                if (index >= len) break;
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            points.add(new LatLngPoint(lat / 1e5, lng / 1e5));
        }

        return points;
    }

    /**
     * Tạo đường Polyline mẫu tự nhiên giữa 2 điểm với các khúc cong đô thị thực tế
     */
    public static String generateUrbanPolyline(double lat1, double lng1, double lat2, double lng2) {
        List<LatLngPoint> points = new ArrayList<>();
        points.add(new LatLngPoint(lat1, lng1));

        // 3 điểm trung gian uốn cong nhẹ dọc trục đường
        double midLat1 = lat1 + (lat2 - lat1) * 0.3 + (lng2 - lng1) * 0.05;
        double midLng1 = lng1 + (lng2 - lng1) * 0.3 - (lat2 - lat1) * 0.05;
        points.add(new LatLngPoint(midLat1, midLng1));

        double midLat2 = lat1 + (lat2 - lat1) * 0.7 - (lng2 - lng1) * 0.03;
        double midLng2 = lng1 + (lng2 - lng1) * 0.7 + (lat2 - lat1) * 0.03;
        points.add(new LatLngPoint(midLat2, midLng2));

        points.add(new LatLngPoint(lat2, lng2));
        return encode(points);
    }
}
