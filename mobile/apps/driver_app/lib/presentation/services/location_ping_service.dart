import 'dart:async';
import 'dart:math';
import 'package:flutter/foundation.dart';
import 'package:core_model/core_model.dart';
import '../../data/repositories/driver_repository.dart';

class LocationPingService {
  final DriverRepository driverRepository;
  Timer? _pingTimer;
  Timer? _dispatchTimer;
  bool _isRunning = false;
  bool _isShowingDispatchModal = false;

  // Starting location: Center of District 1, Ho Chi Minh City
  double _currentLat = 10.776530;
  double _currentLng = 106.700981;
  int _batteryPercent = 94;
  double _heading = 45.0;
  double _speedKmh = 28.5;

  final ValueNotifier<DriverLocationPingModel?> lastPingNotifier = ValueNotifier(null);
  final ValueNotifier<bool> isPingingNotifier = ValueNotifier(false);
  final ValueNotifier<DispatchNotificationModel?> incomingDispatchNotifier = ValueNotifier(null);

  LocationPingService({required this.driverRepository});

  bool get isRunning => _isRunning;
  DriverLocationPingModel? get lastPing => lastPingNotifier.value;

  void setDispatchModalShowing(bool isShowing) {
    _isShowingDispatchModal = isShowing;
  }

  void clearIncomingDispatch() {
    incomingDispatchNotifier.value = null;
  }

  void start() {
    if (_isRunning) return;
    _isRunning = true;
    isPingingNotifier.value = true;

    // Send initial ping immediately
    _sendPing();

    // Ping location every 5 seconds
    _pingTimer = Timer.periodic(const Duration(seconds: 5), (_) {
      _simulateMovement();
      _sendPing();
    });

    // Check for incoming dispatches every 2 seconds when online
    _dispatchTimer = Timer.periodic(const Duration(seconds: 2), (_) {
      _checkForDispatch();
    });
  }

  void stop() {
    _pingTimer?.cancel();
    _pingTimer = null;
    _dispatchTimer?.cancel();
    _dispatchTimer = null;
    _isRunning = false;
    isPingingNotifier.value = false;
    clearIncomingDispatch();
  }

  Future<void> _checkForDispatch() async {
    if (!_isRunning || _isShowingDispatchModal) return;
    try {
      final dispatch = await driverRepository.getPendingDispatch();
      if (dispatch != null && !_isShowingDispatchModal && _isRunning) {
        incomingDispatchNotifier.value = dispatch;
      }
    } catch (_) {
      // Ignore polling errors
    }
  }


  void _simulateMovement() {
    // Add small delta (~20-50 meters)
    final random = Random();
    final latDelta = (random.nextDouble() - 0.5) * 0.0004;
    final lngDelta = (random.nextDouble() - 0.5) * 0.0004;

    _currentLat += latDelta;
    _currentLng += lngDelta;
    _heading = (random.nextDouble() * 360).roundToDouble();
    _speedKmh = 20.0 + random.nextDouble() * 20.0; // 20 - 40 km/h

    // Battery decreases by 1% every 200 ticks
    if (random.nextInt(100) < 5 && _batteryPercent > 15) {
      _batteryPercent -= 1;
    }
  }

  Future<void> _sendPing() async {
    final ping = DriverLocationPingModel(
      lat: double.parse(_currentLat.toStringAsFixed(6)),
      lng: double.parse(_currentLng.toStringAsFixed(6)),
      speedKmh: double.parse(_speedKmh.toStringAsFixed(1)),
      bearing: _heading,
      batteryPercent: _batteryPercent,
    );

    try {
      await driverRepository.pingLocation(ping);
      lastPingNotifier.value = ping;
    } catch (e) {
      // In case of network interruption or server offline, log debug
      debugPrint('Location ping warning: $e');
      lastPingNotifier.value = ping; // Keep last local state
    }
  }

  void dispose() {
    stop();
    lastPingNotifier.dispose();
    isPingingNotifier.dispose();
  }
}
