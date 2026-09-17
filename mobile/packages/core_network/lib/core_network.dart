library core_network;

export 'src/token_storage.dart';
export 'src/api_client.dart';
export 'src/auth_api.dart';
export 'src/driver_api.dart';

class AppConfig {
  static const String defaultApiUrl = 'http://localhost:8080/api/v1';
  static const String defaultWsUrl = 'ws://localhost:8080/api/v1/ws-connect';
}
