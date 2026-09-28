/// Tọa độ điểm trên bản đồ
class MapCoordinate {
  final double latitude;
  final double longitude;

  const MapCoordinate(this.latitude, this.longitude);

  @override
  String toString() => 'MapCoordinate($latitude, $longitude)';
}

/// Tiện ích giải mã Polyline chuẩn của Goong / Google / Mapbox trên Flutter
class GoongPolylineDecoder {
  static List<MapCoordinate> decode(String encoded) {
    List<MapCoordinate> coordinates = [];
    if (encoded.isEmpty) return coordinates;

    // Check if it's the simple fallback format: _lat1=..._lng1=..._lat2=..._lng2=...
    if (encoded.startsWith('_lat1=')) {
      final parts = encoded.split('_');
      double? lat1, lng1, lat2, lng2;
      for (final p in parts) {
        if (p.startsWith('lat1=')) lat1 = double.tryParse(p.substring(5));
        if (p.startsWith('lng1=')) lng1 = double.tryParse(p.substring(5));
        if (p.startsWith('lat2=')) lat2 = double.tryParse(p.substring(5));
        if (p.startsWith('lng2=')) lng2 = double.tryParse(p.substring(5));
      }
      if (lat1 != null && lng1 != null && lat2 != null && lng2 != null) {
        coordinates.add(MapCoordinate(lat1, lng1));
        // Add curve midpoint
        coordinates.add(MapCoordinate(
          lat1 + (lat2 - lat1) * 0.4 + (lng2 - lng1) * 0.04,
          lng1 + (lng2 - lng1) * 0.4 - (lat2 - lat1) * 0.04,
        ));
        coordinates.add(MapCoordinate(
          lat1 + (lat2 - lat1) * 0.7 - (lng2 - lng1) * 0.02,
          lng1 + (lng2 - lng1) * 0.7 + (lat2 - lat1) * 0.02,
        ));
        coordinates.add(MapCoordinate(lat2, lng2));
        return coordinates;
      }
    }

    // Standard Google/Goong polyline algorithm
    int index = 0;
    int len = encoded.length;
    int lat = 0;
    int lng = 0;

    while (index < len) {
      int b;
      int shift = 0;
      int result = 0;
      do {
        if (index >= len) break;
        b = encoded.codeUnitAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);

      int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
      lat += dlat;

      shift = 0;
      result = 0;
      do {
        if (index >= len) break;
        b = encoded.codeUnitAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);

      int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
      lng += dlng;

      coordinates.add(MapCoordinate(lat / 1e5, lng / 1e5));
    }

    return coordinates;
  }
}
