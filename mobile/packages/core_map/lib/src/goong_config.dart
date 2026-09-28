/// Cấu hình tích hợp Goong Map cho Mobile Apps (Customer & Driver)
///
/// Keys được inject lúc build qua --dart-define:
///   flutter run --dart-define=GOONG_MAPTILES_KEY=xxx --dart-define=GOONG_API_KEY=yyy
///
/// Hoặc dùng file --dart-define-from-file=.env (Flutter >= 3.7):
///   flutter run --dart-define-from-file=../../.env
class GoongMapConfig {
  /// Khóa MapTiles dùng để render Vector/Raster Tiles
  static const String maptilesKey = String.fromEnvironment('GOONG_MAPTILES_KEY');

  /// Khóa REST API (Direction, Geocode, Places Autocomplete)
  static const String apiKey = String.fromEnvironment('GOONG_API_KEY');

  /// Kiểm tra keys có được cấu hình không
  static bool get hasMaptilesKey => maptilesKey.isNotEmpty;
  static bool get hasApiKey => apiKey.isNotEmpty;

  /// URLs Style Vector Mapbox GL / MapLibre của Goong
  static String getWebStyleUrl([String? key]) =>
      'https://tiles.goong.io/assets/goong_map_web.json?api_key=${key ?? maptilesKey}';

  static String getDarkStyleUrl([String? key]) =>
      'https://tiles.goong.io/assets/goong_map_dark.json?api_key=${key ?? maptilesKey}';

  static String getNavNightStyleUrl([String? key]) =>
      'https://tiles.goong.io/assets/navigation_night.json?api_key=${key ?? maptilesKey}';

  static String getNavDayStyleUrl([String? key]) =>
      'https://tiles.goong.io/assets/navigation_day.json?api_key=${key ?? maptilesKey}';

  /// REST APIs
  static const String directionUrl = 'https://rsapi.goong.io/Direction';
  static const String placeAutocompleteUrl = 'https://rsapi.goong.io/Place/AutoComplete';
  static const String placeDetailUrl = 'https://rsapi.goong.io/Place/Detail';
  static const String geocodeUrl = 'https://rsapi.goong.io/geocode';

  /// Tọa độ trung tâm mặc định: TP. Hồ Chí Minh
  static const double defaultLat = 10.776530;
  static const double defaultLng = 106.700981;
  static const double defaultZoom = 15.0;
}
