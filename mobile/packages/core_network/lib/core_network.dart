library core_network;

export 'src/token_storage.dart';
export 'src/api_client.dart';
export 'src/auth_api.dart';
export 'src/driver_api.dart';
export 'src/trip_api.dart';
export 'src/payment_api.dart';
export 'src/wallet_api.dart';
export 'src/earnings_api.dart';
export 'src/stomp_service.dart';

class AppConfig {
  static const String defaultApiUrl = 'http://127.0.0.1:8080/api/v1';
  static const String defaultWsUrl = 'ws://127.0.0.1:8080/api/v1/ws-connect';
}
