import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:core_map/core_map.dart';
import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import '../../../data/repositories/driver_repository.dart';

class DriverActiveTripScreen extends StatefulWidget {
  final DriverTripModel initialTrip;
  final DriverRepository driverRepository;

  const DriverActiveTripScreen({
    super.key,
    required this.initialTrip,
    required this.driverRepository,
  });

  @override
  State<DriverActiveTripScreen> createState() => _DriverActiveTripScreenState();
}

class _DriverActiveTripScreenState extends State<DriverActiveTripScreen> {
  late String _currentStatus;
  bool _isProcessing = false;
  String? _errorMessage;

  // Stomp & Real-time Services
  final StompService _stompService = StompService();
  void Function({Map<String, String>? unsubscribeHeaders})? _tripStatusUnsub;

  // Navigation & Routing State
  RoutingResultModel? _currentRouting;
  int _currentStepIndex = 0;
  String? _encodedPolyline;

  // Coordinates & Driver Tracking State
  late double _driverLat;
  late double _driverLng;
  double _driverBearing = 45.0;
  double _speedKmh = 25.0;
  int _batteryPercent = 92;
  int _remainingDistanceM = 1200;
  int _etaSeconds = 300;
  int _secondsStopped = 0;

  // Waiting Countdown (5 minutes = 300s)
  int _waitingCountdownSeconds = 300;
  Timer? _countdownTimer;

  // GPS Telemetry Streamer Timer
  Timer? _gpsStreamTimer;
  final List<GpsPointModel> _offlineGpsBuffer = [];

  // Completed Summary Data
  TripCompleteSummaryModel? _completeSummary;

  @override
  void initState() {
    super.initState();
    _currentStatus = widget.initialTrip.status;

    // Driver starts near pickup (~500m away)
    _driverLat = widget.initialTrip.pickupLat - 0.0035;
    _driverLng = widget.initialTrip.pickupLng - 0.0035;

    _initializeRealtimeAndTrip();
  }

  Future<void> _initializeRealtimeAndTrip() async {
    setState(() => _isProcessing = true);

    try {
      // 1. Connect WebSocket STOMP with JWT token
      final token = await widget.driverRepository.driverApi.apiClient.tokenStorage.getToken();
      if (token != null && token.isNotEmpty) {
        final wsUrl = ApiClient.defaultBaseUrl.replaceFirst('/api/v1', '').replaceFirst('http', 'ws') + '/ws-connect';
        _stompService.connect(wsUrl, token);

        // Subscribe to trip status events
        _tripStatusUnsub = _stompService.subscribeTripStatus(
          widget.initialTrip.tripId,
          _onServerTripStatusUpdate,
        );
      }

      // 2. Call startArriving API if entering as MATCHED or DRIVER_ARRIVING
      if (_currentStatus == TripStatus.matched || _currentStatus == TripStatus.driverArriving) {
        _currentStatus = TripStatus.driverArriving;
        final arrivingData = await widget.driverRepository.startArriving(widget.initialTrip.tripId);
        if (arrivingData.routing != null) {
          _currentRouting = arrivingData.routing;
          _encodedPolyline = arrivingData.routing!.polyline;
          _remainingDistanceM = arrivingData.routing!.distanceM;
          _etaSeconds = arrivingData.routing!.durationS;
        }
      }

      // 3. Start GPS Telemetry Streaming
      _startGpsStreaming();
    } catch (e) {
      debugPrint('[DriverActiveTrip] Init warning: $e');
    } finally {
      if (mounted) {
        setState(() => _isProcessing = false);
      }
    }
  }

  void _onServerTripStatusUpdate(TripStatusUpdateModel event) {
    if (!mounted) return;
    debugPrint('[DriverActiveTrip] Server status broadcast: ${event.status}');

    if (event.status == TripStatus.arrived && _currentStatus == TripStatus.driverArriving) {
      HapticFeedback.heavyImpact();
      setState(() {
        _currentStatus = TripStatus.arrived;
        _startWaitingCountdown();
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Auto-Geofence: Bạn đã đến điểm đón khách!'),
          backgroundColor: GreenColors.primaryDark,
        ),
      );
    } else if (event.status == TripStatus.cancelled) {
      _handleTripCancelledByCustomer(event.message ?? 'Khách hàng đã hủy chuyến');
    }
  }

  void _handleTripCancelledByCustomer(String message) {
    _stopGpsStreaming();
    _countdownTimer?.cancel();
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        backgroundColor: GreenColors.surfaceDark,
        title: const Row(
          children: [
            Icon(Icons.cancel_outlined, color: GreenColors.errorRed),
            SizedBox(width: 8),
            Text('Chuyến đi đã bị hủy', style: TextStyle(color: Colors.white, fontSize: 16)),
          ],
        ),
        content: Text(
          message,
          style: const TextStyle(color: Colors.white70, fontSize: 14),
        ),
        actions: [
          ElevatedButton(
            onPressed: () {
              Navigator.of(ctx).pop();
              Navigator.of(context).pop();
            },
            style: ElevatedButton.styleFrom(backgroundColor: GreenColors.primaryEmerald),
            child: const Text('ĐÓNG', style: TextStyle(color: GreenColors.backgroundDark)),
          ),
        ],
      ),
    );
  }

  // Adaptive GPS Streaming
  void _startGpsStreaming() {
    _gpsStreamTimer?.cancel();
    _scheduleNextGpsTick();
  }

  void _scheduleNextGpsTick() {
    // Adaptive GPS logic: 3s if speed > 15 km/h, 15s if stopped > 30s, 4s otherwise
    int delaySeconds = 4;
    if (_speedKmh > 15.0) {
      delaySeconds = 3;
    } else if (_secondsStopped > 30) {
      delaySeconds = 15;
    }

    _gpsStreamTimer = Timer(Duration(seconds: delaySeconds), () {
      _tickGpsAndSend();
      if (_currentStatus != TripStatus.completed && _currentStatus != TripStatus.cancelled) {
        _scheduleNextGpsTick();
      }
    });
  }

  void _tickGpsAndSend() {
    if (!mounted) return;

    // Simulate progressive movement along path towards target
    final targetLat = (_currentStatus == TripStatus.inTrip)
        ? widget.initialTrip.dropoffLat
        : widget.initialTrip.pickupLat;
    final targetLng = (_currentStatus == TripStatus.inTrip)
        ? widget.initialTrip.dropoffLng
        : widget.initialTrip.pickupLng;

    final distToTarget = _calculateDistance(_driverLat, _driverLng, targetLat, targetLng);

    if (distToTarget > 0.05) {
      // Step ~40 meters towards target
      final angle = math.atan2(targetLat - _driverLat, targetLng - _driverLng);
      final step = 0.00035; // ~38m
      _driverLat += step * math.sin(angle);
      _driverLng += step * math.cos(angle);
      _driverBearing = (angle * 180 / math.pi);
      _speedKmh = 28.0 + math.Random().nextDouble() * 10.0;
      _secondsStopped = 0;

      _remainingDistanceM = math.max((distToTarget * 1000).toInt(), 50);
      _etaSeconds = math.max((_remainingDistanceM / (_speedKmh * 1000 / 3600)).toInt(), 20);
    } else {
      // Arrived near point
      _speedKmh = 0.0;
      _secondsStopped += 3;
      _remainingDistanceM = 0;
      _etaSeconds = 0;
    }

    final point = GpsPointModel(
      lat: double.parse(_driverLat.toStringAsFixed(6)),
      lng: double.parse(_driverLng.toStringAsFixed(6)),
      speedKmh: double.parse(_speedKmh.toStringAsFixed(1)),
      bearing: double.parse(_driverBearing.toStringAsFixed(1)),
      accuracy: 4.5,
      batteryPercent: _batteryPercent,
    );

    _offlineGpsBuffer.add(point);

    // Send location via WebSocket STOMP
    final payload = DriverLocationUpdatePayload(
      tripId: widget.initialTrip.tripId,
      lat: point.lat,
      lng: point.lng,
      speedKmh: point.speedKmh,
      bearing: point.bearing,
      altitude: 12.0,
      accuracy: 4.5,
      batteryPercent: _batteryPercent,
      isMockLocation: false,
    );
    _stompService.sendLocationUpdate(payload);

    setState(() {});
  }

  void _stopGpsStreaming() {
    _gpsStreamTimer?.cancel();
    _gpsStreamTimer = null;
  }

  void _startWaitingCountdown() {
    _waitingCountdownSeconds = 300;
    _countdownTimer?.cancel();
    _countdownTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) return;
      setState(() {
        if (_waitingCountdownSeconds > 0) {
          _waitingCountdownSeconds--;
        } else {
          timer.cancel();
        }
      });
    });
  }

  double _calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    const p = 0.017453292519943295;
    final a = 0.5 -
        math.cos((lat2 - lat1) * p) / 2 +
        math.cos(lat1 * p) * math.cos(lat2 * p) * (1 - math.cos((lon2 - lon1) * p)) / 2;
    return 12742 * math.asin(math.sqrt(a)); // km
  }

  // --- ACTIONS ---

  Future<void> _handleArrivedAtPickup() async {
    setState(() => _isProcessing = true);
    try {
      final res = await widget.driverRepository.arriveAtPickup(widget.initialTrip.tripId);
      setState(() {
        _currentStatus = TripStatus.arrived;
        _startWaitingCountdown();
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Đã đến điểm đón! Đang đếm ngược chờ ${res.customerName.isNotEmpty ? res.customerName : "khách"}.'),
          backgroundColor: GreenColors.primaryEmerald,
        ),
      );
    } catch (e) {
      _showErrorSnackBar('Lỗi xác nhận đã đến: $e');
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  Future<void> _handleStartTrip() async {
    setState(() => _isProcessing = true);
    _countdownTimer?.cancel();

    try {
      final res = await widget.driverRepository.startTrip(widget.initialTrip.tripId);
      setState(() {
        _currentStatus = TripStatus.inTrip;
        if (res.routing != null) {
          _currentRouting = res.routing;
          _encodedPolyline = res.routing!.polyline;
          _remainingDistanceM = res.routing!.distanceM;
          _etaSeconds = res.routing!.durationS;
          _currentStepIndex = 0;
        }
      });
      HapticFeedback.mediumImpact();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Chuyến đi đã bắt đầu! Điều hướng tới điểm trả khách.'),
          backgroundColor: GreenColors.primaryDark,
        ),
      );
    } catch (e) {
      _showErrorSnackBar('Lỗi bắt đầu chuyến: $e');
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  Future<void> _handleCompleteTrip() async {
    setState(() => _isProcessing = true);
    _stopGpsStreaming();

    try {
      // Sync remaining buffered GPS points before finalizing
      if (_offlineGpsBuffer.isNotEmpty) {
        try {
          await widget.driverRepository.syncGpsBatch(widget.initialTrip.tripId, _offlineGpsBuffer);
        } catch (_) {}
      }

      final summary = await widget.driverRepository.completeTrip(widget.initialTrip.tripId);
      setState(() {
        _currentStatus = TripStatus.completed;
        _completeSummary = summary;
      });
      _stompService.disconnect();
      HapticFeedback.heavyImpact();
      _showCompletionDialog(summary);
    } catch (e) {
      _showErrorSnackBar('Lỗi hoàn thành chuyến: $e');
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  Future<void> _handleCancelTrip() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: GreenColors.surfaceDark,
        title: const Text('Xác nhận hủy cuốc xe', style: TextStyle(color: Colors.white, fontSize: 16)),
        content: const Text(
          'Khách hàng không xuất hiện sau 5 phút chờ đợi. Bạn có muốn hủy cuốc xe này không?',
          style: TextStyle(color: Colors.white70, fontSize: 13),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('QUAY LẠI', style: TextStyle(color: Colors.white60)),
          ),
          ElevatedButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            style: ElevatedButton.styleFrom(backgroundColor: GreenColors.errorRed),
            child: const Text('HỦY CUỐC', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      setState(() => _isProcessing = true);
      try {
        await widget.driverRepository.cancelTrip(widget.initialTrip.tripId, reason: 'Khách không đến điểm đón');
        _stopGpsStreaming();
        _stompService.disconnect();
        if (mounted) {
          Navigator.of(context).pop();
        }
      } catch (e) {
        _showErrorSnackBar('Lỗi hủy cuốc: $e');
      } finally {
        if (mounted) setState(() => _isProcessing = false);
      }
    }
  }

  void _showErrorSnackBar(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: GreenColors.errorRed,
      ),
    );
  }

  void _showCompletionDialog(TripCompleteSummaryModel summary) {
    final earnings = summary.driverEarnings?.netEarningsVnd ?? widget.initialTrip.netIncomeVnd;
    final co2 = summary.co2SavedGrams > 0 ? summary.co2SavedGrams : widget.initialTrip.co2SavedGrams;
    final distanceKm = summary.actualDistanceM > 0
        ? (summary.actualDistanceM / 1000.0).toStringAsFixed(1)
        : widget.initialTrip.estimatedDistanceKm.toStringAsFixed(1);
    final durationMins = summary.actualDurationS > 0
        ? (summary.actualDurationS / 60).round()
        : 25;

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        backgroundColor: GreenColors.surfaceDark,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 68,
              height: 68,
              decoration: BoxDecoration(
                color: GreenColors.primaryEmerald.withValues(alpha: 0.15),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.check_circle, color: GreenColors.primaryEmerald, size: 48),
            ),
            const SizedBox(height: 16),
            const Text(
              'CHUYẾN ĐI HOÀN TẤT! 🎉',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
            ),
            const SizedBox(height: 6),
            const Text(
              'Cảm ơn bạn đã đồng hành cùng giao thông xanh!',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 12, color: Colors.white70),
            ),
            const SizedBox(height: 18),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: GreenColors.cardDark,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: GreenColors.cardBorder),
              ),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Thu nhập thực nhận:', style: TextStyle(color: Colors.white70, fontSize: 13)),
                      Text(
                        '+${GreenFormatters.currencyVnd(earnings)}',
                        style: const TextStyle(fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald, fontSize: 16),
                      ),
                    ],
                  ),
                  const Divider(height: 18, color: Colors.white10),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Quãng đường & Thời gian:', style: TextStyle(color: Colors.white70, fontSize: 13)),
                      Text(
                        '$distanceKm km • $durationMins phút',
                        style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white, fontSize: 13),
                      ),
                    ],
                  ),
                  const Divider(height: 18, color: Colors.white10),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Giảm phát thải CO2:', style: TextStyle(color: Colors.white70, fontSize: 13)),
                      Text(
                        '🌱 +${co2.round()}g',
                        style: const TextStyle(fontWeight: FontWeight.bold, color: GreenColors.electricCyan, fontSize: 14),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 22),
            ElevatedButton(
              onPressed: () {
                Navigator.of(ctx).pop();
                Navigator.of(context).pop();
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: GreenColors.primaryEmerald,
                foregroundColor: GreenColors.backgroundDark,
                minimumSize: const Size.fromHeight(48),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
              child: const Text('TIẾP TỤC NHẬN CUỐC', style: TextStyle(fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    _gpsStreamTimer?.cancel();
    _countdownTimer?.cancel();
    _tripStatusUnsub?.call();
    _stompService.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final trip = widget.initialTrip;

    return Scaffold(
      appBar: AppBar(
        title: Text('Cuốc #${trip.tripCode.isNotEmpty ? trip.tripCode : trip.tripId.substring(0, 8).toUpperCase()}'),
        backgroundColor: GreenColors.backgroundDark,
        elevation: 0,
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16.0),
            child: Center(
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: GreenColors.primaryDark.withValues(alpha: 0.3),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: GreenColors.primaryEmerald),
                ),
                child: Text(
                  TripStatus.getLabel(_currentStatus),
                  style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald),
                ),
              ),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // 1. Turn-by-Turn Navigation Instruction Header
              _buildTurnByTurnHeader(),
              const SizedBox(height: 12),

              // 2. Goong Route Map with Real-time Driver Tracking
              GoongRouteMapView(
                pickupLat: trip.pickupLat,
                pickupLng: trip.pickupLng,
                pickupTitle: trip.pickupAddress,
                dropoffLat: trip.dropoffLat,
                dropoffLng: trip.dropoffLng,
                dropoffTitle: trip.dropoffAddress,
                driverLat: _driverLat,
                driverLng: _driverLng,
                driverBearing: _driverBearing,
                encodedPolyline: _encodedPolyline,
                distanceText: '${(_remainingDistanceM / 1000).toStringAsFixed(1)} km',
                durationText: '${math.max((_etaSeconds / 60).round(), 1)} phút',
                height: 220,
              ),
              const SizedBox(height: 14),

              // 3. Customer Profile Card
              _buildCustomerCard(trip),
              const SizedBox(height: 14),

              // 4. Waiting Countdown Timer (When ARRIVED)
              if (_currentStatus == TripStatus.arrived) ...[
                _buildWaitingCountdownCard(),
                const SizedBox(height: 14),
              ],

              // 5. Trip Route & Eco Metrics
              _buildTripMetricsCard(trip),
              const SizedBox(height: 20),

              // 6. Action Button / Swipe-to-Action based on phase
              _buildPhaseActionControls(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTurnByTurnHeader() {
    String instruction = 'Di chuyển đón khách theo định vị';
    IconData maneuverIcon = Icons.navigation;

    if (_currentRouting != null && _currentRouting!.steps.isNotEmpty) {
      final step = _currentRouting!.steps[_currentStepIndex.clamp(0, _currentRouting!.steps.length - 1)];
      instruction = step.instruction;
      final man = step.maneuver?.toLowerCase() ?? '';
      if (man.contains('left')) {
        maneuverIcon = Icons.turn_left_rounded;
      } else if (man.contains('right')) {
        maneuverIcon = Icons.turn_right_rounded;
      } else if (man.contains('u-turn')) {
        maneuverIcon = Icons.u_turn_left_rounded;
      } else {
        maneuverIcon = Icons.straight_rounded;
      }
    } else if (_currentStatus == TripStatus.inTrip) {
      instruction = 'Đang dẫn đường tới điểm trả khách';
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: GreenColors.surfaceDark,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: GreenColors.primaryEmerald.withValues(alpha: 0.3)),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: GreenColors.primaryEmerald.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(maneuverIcon, color: GreenColors.primaryEmerald, size: 24),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  instruction,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: Colors.white),
                ),
                const SizedBox(height: 2),
                Text(
                  'Cách ${_remainingDistanceM}m • ETA: ${math.max((_etaSeconds / 60).round(), 1)} phút',
                  style: const TextStyle(fontSize: 11, color: GreenColors.electricCyan),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCustomerCard(DriverTripModel trip) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: GreenColors.surfaceDark,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: GreenColors.cardBorder),
      ),
      child: Row(
        children: [
          CircleAvatar(
            radius: 24,
            backgroundColor: GreenColors.electricCyan.withValues(alpha: 0.2),
            child: Text(
              trip.customerName.isNotEmpty ? trip.customerName[0].toUpperCase() : 'K',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: GreenColors.electricCyan),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  trip.customerName.isNotEmpty ? trip.customerName : 'Khách hàng Green Mobility',
                  style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white),
                ),
                const SizedBox(height: 2),
                Text(
                  trip.customerPhone.isNotEmpty ? trip.customerPhone : '090 ••• ••••',
                  style: const TextStyle(fontSize: 12, color: GreenColors.textSecondary),
                ),
              ],
            ),
          ),
          IconButton(
            icon: const Icon(Icons.phone, color: GreenColors.primaryEmerald),
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text('Đang gọi cho khách hàng: ${trip.customerPhone}'),
                  backgroundColor: GreenColors.primaryDark,
                ),
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.chat_bubble_outline, color: GreenColors.electricCyan),
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Đang mở hộp thoại tin nhắn với hành khách...')),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _buildWaitingCountdownCard() {
    final mins = (_waitingCountdownSeconds ~/ 60).toString().padLeft(2, '0');
    final secs = (_waitingCountdownSeconds % 60).toString().padLeft(2, '0');
    final isExpired = _waitingCountdownSeconds <= 0;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: isExpired
            ? GreenColors.errorRed.withValues(alpha: 0.15)
            : GreenColors.warningAmber.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: isExpired ? GreenColors.errorRed : GreenColors.warningAmber,
          width: 1.2,
        ),
      ),
      child: Row(
        children: [
          Icon(
            isExpired ? Icons.timer_off : Icons.timer,
            color: isExpired ? GreenColors.errorRed : GreenColors.warningAmber,
            size: 26,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  isExpired ? 'ĐÃ HẾT 5 PHÚT CHỜ' : 'THỜI GIAN CHỜ KHÁCH HÀNG',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    color: isExpired ? GreenColors.errorRed : GreenColors.warningAmber,
                  ),
                ),
                Text(
                  isExpired ? 'Bạn có thể bấm hủy cuốc xe nếu khách không đến' : 'Quy định chờ miễn phí tối đa 5 phút',
                  style: const TextStyle(fontSize: 11, color: Colors.white70),
                ),
              ],
            ),
          ),
          Text(
            '$mins:$secs',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w900,
              fontFamily: 'monospace',
              color: isExpired ? GreenColors.errorRed : Colors.white,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTripMetricsCard(DriverTripModel trip) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF064E3B), Color(0xFF047857)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          Column(
            children: [
              const Text('THU NHẬP DỰ KIẾN', style: TextStyle(fontSize: 10, color: Color(0xFFA7F3D0))),
              const SizedBox(height: 4),
              Text(
                '+${GreenFormatters.currencyVnd(trip.netIncomeVnd)}',
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: Colors.white),
              ),
            ],
          ),
          Container(height: 32, width: 1, color: Colors.white24),
          Column(
            children: [
              const Text('GIẢM PHÁT THẢI CO2', style: TextStyle(fontSize: 10, color: Color(0xFFA7F3D0))),
              const SizedBox(height: 4),
              Row(
                children: [
                  const Icon(Icons.eco, color: Colors.white, size: 16),
                  const SizedBox(width: 4),
                  Text(
                    '+${trip.co2SavedGrams.round()}g',
                    style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: Colors.white),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildPhaseActionControls() {
    if (_currentStatus == TripStatus.driverArriving || _currentStatus == TripStatus.matched) {
      return Column(
        children: [
          ElevatedButton.icon(
            onPressed: _isProcessing ? null : _handleArrivedAtPickup,
            icon: _isProcessing
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: GreenColors.backgroundDark),
                  )
                : const Icon(Icons.location_on, color: GreenColors.backgroundDark),
            label: const Text(
              'TÔI ĐÃ ĐẾN ĐIỂM ĐÓN',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w900, color: GreenColors.backgroundDark),
            ),
            style: ElevatedButton.styleFrom(
              backgroundColor: GreenColors.primaryEmerald,
              minimumSize: const Size.fromHeight(54),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              elevation: 4,
            ),
          ),
        ],
      );
    } else if (_currentStatus == TripStatus.arrived) {
      return Column(
        children: [
          GreenSwipeButton(
            text: 'TRƯỢT ĐỂ BẮT ĐẦU CHUYẾN ĐI',
            icon: Icons.navigation_rounded,
            isLoading: _isProcessing,
            thumbColor: GreenColors.primaryEmerald,
            onSwipeComplete: _handleStartTrip,
          ),
          if (_waitingCountdownSeconds <= 0) ...[
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: _isProcessing ? null : _handleCancelTrip,
              icon: const Icon(Icons.cancel, color: GreenColors.errorRed, size: 18),
              label: const Text(
                'HỦY CUỐC (KHÁCH KHÔNG ĐẾN)',
                style: TextStyle(color: GreenColors.errorRed, fontWeight: FontWeight.bold, fontSize: 13),
              ),
              style: OutlinedButton.styleFrom(
                side: const BorderSide(color: GreenColors.errorRed),
                minimumSize: const Size.fromHeight(46),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
            ),
          ],
        ],
      );
    } else if (_currentStatus == TripStatus.inTrip) {
      return GreenSwipeButton(
        text: 'TRƯỢT ĐỂ HOÀN THÀNH CHUYẾN',
        icon: Icons.check_circle_rounded,
        isLoading: _isProcessing,
        thumbColor: GreenColors.electricCyan,
        onSwipeComplete: _handleCompleteTrip,
      );
    }

    return const SizedBox.shrink();
  }
}
