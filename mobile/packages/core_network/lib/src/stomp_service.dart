import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:core_model/core_model.dart';
import 'package:stomp_dart_client/stomp.dart';
import 'package:stomp_dart_client/stomp_config.dart';
import 'package:stomp_dart_client/stomp_frame.dart';

/// Dịch vụ quản lý kết nối thời gian thực WebSocket qua STOMP Protocol
class StompService {
  StompClient? _client;
  bool _isConnected = false;
  int _retryCount = 0;
  static const int maxRetries = 3;
  String? _lastWsUrl;
  String? _lastJwtToken;

  final ValueNotifier<bool> connectionStateNotifier = ValueNotifier<bool>(false);

  bool get isConnected => _isConnected;

  /// Kết nối tới STOMP WebSocket server với JWT Token
  void connect(String wsUrl, String jwtToken) {
    _lastWsUrl = wsUrl;
    _lastJwtToken = jwtToken;
    _retryCount = 0;

    _initAndActivate();
  }

  void _initAndActivate() {
    if (_lastWsUrl == null || _lastJwtToken == null) return;

    final token = _lastJwtToken!.startsWith('Bearer ') ? _lastJwtToken! : 'Bearer $_lastJwtToken';
    final headers = {'Authorization': token};

    _client = StompClient(
      config: StompConfig(
        url: _lastWsUrl!,
        onConnect: _onConnect,
        onWebSocketError: _onWebSocketError,
        onStompError: _onStompError,
        onDisconnect: _onDisconnect,
        stompConnectHeaders: headers,
        webSocketConnectHeaders: headers,
        reconnectDelay: const Duration(seconds: 2),
      ),
    );

    _client?.activate();
  }

  void _onConnect(StompFrame frame) {
    _isConnected = true;
    _retryCount = 0;
    connectionStateNotifier.value = true;
    debugPrint('[StompService] STOMP kết nối thành công.');
  }

  void _onDisconnect(StompFrame frame) {
    _isConnected = false;
    connectionStateNotifier.value = false;
    debugPrint('[StompService] STOMP đã ngắt kết nối.');
  }

  void _onWebSocketError(dynamic error) {
    debugPrint('[StompService] WebSocket Error: $error');
    _handleReconnect();
  }

  void _onStompError(StompFrame frame) {
    debugPrint('[StompService] STOMP Error: ${frame.body}');
  }

  void _handleReconnect() {
    _isConnected = false;
    connectionStateNotifier.value = false;
    if (_retryCount < maxRetries) {
      _retryCount++;
      debugPrint('[StompService] Đang thử kết nối lại lần $_retryCount/$maxRetries sau 2s...');
      Timer(const Duration(seconds: 2), () {
        if (!_isConnected && _lastWsUrl != null) {
          _initAndActivate();
        }
      });
    } else {
      debugPrint('[StompService] Đã đạt giới hạn số lần thử kết nối lại.');
    }
  }

  /// Khách hàng lắng nghe luồng vị trí GPS thời gian thực của tài xế
  void Function({Map<String, String>? unsubscribeHeaders})? subscribeDriverLocation(
    String driverId,
    Function(DriverLocationModel) onMessage,
  ) {
    if (_client == null) return null;
    return _client!.subscribe(
      destination: '/topic/driver-location/$driverId',
      callback: (StompFrame frame) {
        if (frame.body != null) {
          try {
            final data = jsonDecode(frame.body!) as Map<String, dynamic>;
            onMessage(DriverLocationModel.fromJson(data));
          } catch (e) {
            debugPrint('[StompService] Lỗi parse DriverLocationModel: $e');
          }
        }
      },
    );
  }

  /// Lắng nghe cập nhật trạng thái chuyến xe (MATCHED, DRIVER_ARRIVING, ARRIVED, IN_TRIP, COMPLETED, CANCELLED)
  void Function({Map<String, String>? unsubscribeHeaders})? subscribeTripStatus(
    String tripId,
    Function(TripStatusUpdateModel) onMessage,
  ) {
    if (_client == null) return null;
    return _client!.subscribe(
      destination: '/topic/trip/$tripId',
      callback: (StompFrame frame) {
        if (frame.body != null) {
          try {
            final data = jsonDecode(frame.body!) as Map<String, dynamic>;
            onMessage(TripStatusUpdateModel.fromJson(data));
          } catch (e) {
            debugPrint('[StompService] Lỗi parse TripStatusUpdateModel: $e');
          }
        }
      },
    );
  }

  /// Tài xế gửi cập nhật vị trí GPS lên server qua kênh /app/driver/location-update
  void sendLocationUpdate(DriverLocationUpdatePayload data) {
    if (_client == null || !_isConnected) {
      debugPrint('[StompService] Chưa kết nối STOMP, bỏ qua cập nhật vị trí.');
      return;
    }
    _client!.send(
      destination: '/app/driver/location-update',
      body: jsonEncode(data.toJson()),
    );
  }

  /// Ngắt kết nối STOMP
  void disconnect() {
    _client?.deactivate();
    _client = null;
    _isConnected = false;
    connectionStateNotifier.value = false;
    _retryCount = 0;
  }

  void dispose() {
    disconnect();
    connectionStateNotifier.dispose();
  }
}
