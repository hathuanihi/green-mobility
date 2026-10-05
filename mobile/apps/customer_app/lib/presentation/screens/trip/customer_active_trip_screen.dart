import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:core_map/core_map.dart';
import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import '../../../data/repositories/trip_repository.dart';

class CustomerActiveTripScreen extends StatefulWidget {
  final TripModel trip;
  final TripRepository tripRepository;

  const CustomerActiveTripScreen({
    super.key,
    required this.trip,
    required this.tripRepository,
  });

  @override
  State<CustomerActiveTripScreen> createState() => _CustomerActiveTripScreenState();
}

class _CustomerActiveTripScreenState extends State<CustomerActiveTripScreen>
    with SingleTickerProviderStateMixin {
  late String _currentStatus;
  bool _isCancelling = false;

  // Stomp & Fallback Polling
  final StompService _stompService = StompService();
  void Function({Map<String, String>? unsubscribeHeaders})? _driverLocationUnsub;
  void Function({Map<String, String>? unsubscribeHeaders})? _tripStatusUnsub;
  Timer? _fallbackPollingTimer;

  // Vehicle Smooth Interpolation (lerp)
  late AnimationController _markerAnimController;
  double _prevLat = 0.0;
  double _prevLng = 0.0;
  double _prevBearing = 0.0;
  double _targetLat = 0.0;
  double _targetLng = 0.0;
  double _targetBearing = 0.0;
  double _driverLat = 0.0;
  double _driverLng = 0.0;
  double _driverBearing = 0.0;

  // ETA & Distance
  int _distanceRemainingM = 1500;
  int _etaSeconds = 360;

  // Polyline & Route
  String? _encodedPolyline;

  // Toast / Notification banner for ARRIVED
  bool _showArrivedBanner = false;

  // Rating in completion modal
  int _ratingStars = 5;
  final Set<String> _selectedCompliments = {'Lái xe an toàn', 'Xe sạch sẽ'};

  @override
  void initState() {
    super.initState();
    _currentStatus = widget.trip.status;
    if (_currentStatus == TripStatus.matched) {
      _currentStatus = TripStatus.driverArriving;
    }

    // Initialize Driver Position (slightly offset from pickup if not provided)
    _driverLat = widget.trip.driver?.currentLat ?? (widget.trip.pickupLat - 0.0035);
    _driverLng = widget.trip.driver?.currentLng ?? (widget.trip.pickupLng - 0.0035);
    _prevLat = _driverLat;
    _prevLng = _driverLng;
    _targetLat = _driverLat;
    _targetLng = _driverLng;

    _markerAnimController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1400),
    )..addListener(_onMarkerAnimTick);

    _initWebSocketAndTracking();
  }

  void _onMarkerAnimTick() {
    final t = _markerAnimController.value;
    setState(() {
      _driverLat = _prevLat + (_targetLat - _prevLat) * t;
      _driverLng = _prevLng + (_targetLng - _prevLng) * t;
      _driverBearing = _prevBearing + (_targetBearing - _prevBearing) * t;
    });
  }

  Future<void> _initWebSocketAndTracking() async {
    try {
      final token = await widget.tripRepository.tripApi.apiClient.tokenStorage.getToken();
      final wsUrl = ApiClient.defaultBaseUrl.replaceFirst('/api/v1', '').replaceFirst('http', 'ws') + '/ws-connect';

      if (token != null && token.isNotEmpty) {
        _stompService.connect(wsUrl, token);

        // 1. Subscribe to Driver Location Stream
        final driverId = widget.trip.driver?.id;
        if (driverId != null && driverId.isNotEmpty) {
          _driverLocationUnsub = _stompService.subscribeDriverLocation(
            driverId,
            _onDriverLocationUpdate,
          );
        }

        // 2. Subscribe to Trip Status Updates
        _tripStatusUnsub = _stompService.subscribeTripStatus(
          widget.trip.tripId,
          _onTripStatusUpdate,
        );
      }

      // 3. Initial tracking fetch (fallback / snapshot)
      _fetchTrackingSnapshot();

      // 4. Fallback interval if STOMP drops
      _fallbackPollingTimer = Timer.periodic(const Duration(seconds: 8), (_) {
        if (!_stompService.isConnected && _currentStatus != TripStatus.completed) {
          _fetchTrackingSnapshot();
        }
      });
    } catch (e) {
      debugPrint('[CustomerActiveTrip] Init warning: $e');
    }
  }

  Future<void> _fetchTrackingSnapshot() async {
    try {
      final tracking = await widget.tripRepository.getTracking(widget.trip.tripId);
      if (!mounted) return;

      if (tracking.status.isNotEmpty && tracking.status != _currentStatus) {
        _updateTripStatus(tracking.status);
      }

      if (tracking.tracking?.driverLat != null && tracking.tracking?.driverLng != null) {
        _animateDriverMarker(
          tracking.tracking!.driverLat!,
          tracking.tracking!.driverLng!,
          tracking.tracking?.bearing ?? _driverBearing,
        );
      }

      if (tracking.tracking?.etaSeconds != null) {
        _etaSeconds = tracking.tracking!.etaSeconds!;
      }
      if (tracking.tracking?.distanceRemainingM != null) {
        _distanceRemainingM = tracking.tracking!.distanceRemainingM!;
      }
      if (tracking.tracking?.routePolyline != null && tracking.tracking!.routePolyline!.isNotEmpty) {
        _encodedPolyline = tracking.tracking!.routePolyline;
      }

      setState(() {});
    } catch (e) {
      debugPrint('[CustomerActiveTrip] Tracking snapshot error: $e');
    }
  }

  void _onDriverLocationUpdate(DriverLocationModel loc) {
    if (!mounted) return;
    _animateDriverMarker(loc.lat, loc.lng, loc.bearing ?? _driverBearing);

    if (loc.etaSeconds != null) _etaSeconds = loc.etaSeconds!;
    if (loc.distanceRemainingM != null) _distanceRemainingM = loc.distanceRemainingM!;

    setState(() {});
  }

  void _animateDriverMarker(double targetLat, double targetLng, double targetBearing) {
    _prevLat = _driverLat;
    _prevLng = _driverLng;
    _prevBearing = _driverBearing;
    _targetLat = targetLat;
    _targetLng = targetLng;
    _targetBearing = targetBearing;

    _markerAnimController.forward(from: 0.0);
  }

  void _onTripStatusUpdate(TripStatusUpdateModel event) {
    if (!mounted) return;
    debugPrint('[CustomerActiveTrip] Status update received: ${event.status}');
    _updateTripStatus(event.status, message: event.message);
  }

  void _updateTripStatus(String newStatus, {String? message}) {
    if (_currentStatus == newStatus) return;

    setState(() {
      _currentStatus = newStatus;
    });

    if (newStatus == TripStatus.arrived) {
      HapticFeedback.heavyImpact();
      setState(() => _showArrivedBanner = true);
      Future.delayed(const Duration(seconds: 6), () {
        if (mounted) setState(() => _showArrivedBanner = false);
      });
    } else if (newStatus == TripStatus.inTrip) {
      HapticFeedback.mediumImpact();
      setState(() => _showArrivedBanner = false);
    } else if (newStatus == TripStatus.completed) {
      HapticFeedback.heavyImpact();
      _showCompletionDialog();
    } else if (newStatus == TripStatus.cancelled) {
      _showCancellationDialog(message ?? 'Chuyến đi đã bị hủy');
    }
  }

  Future<void> _handleCancelTrip() async {
    final reason = await showDialog<String>(
      context: context,
      builder: (ctx) {
        String selectedReason = 'Đổi ý không muốn đi nữa';
        return StatefulBuilder(
          builder: (context, setModalState) => AlertDialog(
            backgroundColor: GreenColors.surfaceDark,
            title: const Text('Hủy chuyến xe', style: TextStyle(color: Colors.white, fontSize: 16)),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Vui lòng chọn lý do hủy chuyến:', style: TextStyle(color: Colors.white70, fontSize: 13)),
                const SizedBox(height: 12),
                ...['Đổi ý không muốn đi nữa', 'Thời gian chờ tài xế quá lâu', 'Đặt nhầm điểm đón/điểm đến'].map((r) =>
                    RadioListTile<String>(
                      title: Text(r, style: const TextStyle(color: Colors.white, fontSize: 13)),
                      value: r,
                      groupValue: selectedReason,
                      dense: true,
                      activeColor: GreenColors.primaryEmerald,
                      onChanged: (val) {
                        if (val != null) setModalState(() => selectedReason = val);
                      },
                    )),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(ctx).pop(null),
                child: const Text('QUAY LẠI', style: TextStyle(color: Colors.white60)),
              ),
              ElevatedButton(
                onPressed: () => Navigator.of(ctx).pop(selectedReason),
                style: ElevatedButton.styleFrom(backgroundColor: GreenColors.errorRed),
                child: const Text('XÁC NHẬN HỦY', style: TextStyle(color: Colors.white)),
              ),
            ],
          ),
        );
      },
    );

    if (reason != null && mounted) {
      setState(() => _isCancelling = true);
      try {
        await widget.tripRepository.cancelTrip(widget.trip.tripId, reason: reason);
        if (mounted) {
          Navigator.of(context).pop();
        }
      } catch (e) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Lỗi hủy chuyến: $e'), backgroundColor: GreenColors.errorRed),
        );
      } finally {
        if (mounted) setState(() => _isCancelling = false);
      }
    }
  }

  void _showCancellationDialog(String message) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        backgroundColor: GreenColors.surfaceDark,
        title: const Text('Chuyến đi đã bị hủy', style: TextStyle(color: GreenColors.errorRed)),
        content: Text(message, style: const TextStyle(color: Colors.white70, fontSize: 14)),
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

  void _showCompletionDialog() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setSheetState) {
          return ConfettiCelebration(
            isPlaying: true,
            child: Container(
              padding: const EdgeInsets.all(24),
              decoration: const BoxDecoration(
                color: GreenColors.surfaceDark,
                borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
              ),
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 48,
                      height: 5,
                      decoration: BoxDecoration(
                        color: Colors.white24,
                        borderRadius: BorderRadius.circular(10),
                      ),
                    ),
                    const SizedBox(height: 20),
                    Container(
                      width: 68,
                      height: 68,
                      decoration: BoxDecoration(
                        color: GreenColors.primaryEmerald.withValues(alpha: 0.15),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(Icons.eco_rounded, color: GreenColors.primaryEmerald, size: 44),
                    ),
                    const SizedBox(height: 14),
                    const Text(
                      'CHUYẾN ĐI HOÀN TẤT!',
                      style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900, color: Colors.white),
                    ),
                    const SizedBox(height: 6),
                    const Text(
                      'Cảm ơn bạn đã lựa chọn di chuyển xanh bảo vệ môi trường!',
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 12, color: Colors.white70),
                    ),
                    const SizedBox(height: 20),

                    // Metrics Card
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: GreenColors.cardDark,
                        borderRadius: BorderRadius.circular(18),
                        border: Border.all(color: GreenColors.cardBorder),
                      ),
                      child: Column(
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Text('Cước phí chuyến đi:', style: TextStyle(color: Colors.white70, fontSize: 13)),
                              Text(
                                GreenFormatters.currencyVnd(widget.trip.finalAmountVnd),
                                style: const TextStyle(fontWeight: FontWeight.w900, color: Colors.white, fontSize: 17),
                              ),
                            ],
                          ),
                          const Divider(height: 20, color: Colors.white10),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Text('Lượng CO2 đã giảm:', style: TextStyle(color: Colors.white70, fontSize: 13)),
                              Text(
                                '🌱 -${widget.trip.co2SavedGrams.round()}g',
                                style: const TextStyle(fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald, fontSize: 15),
                              ),
                            ],
                          ),
                          const Divider(height: 20, color: Colors.white10),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Text('Tín chỉ Carbon (PCC):', style: TextStyle(color: Colors.white70, fontSize: 13)),
                              Text(
                                '+${(widget.trip.co2SavedGrams / 1000).toStringAsFixed(3)} PCC',
                                style: const TextStyle(fontWeight: FontWeight.bold, color: GreenColors.electricCyan, fontSize: 14),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 20),

                    // 5-Star Driver Rating
                    const Text(
                      'ĐÁNH GIÁ CHUYẾN ĐI',
                      style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 0.5, color: Colors.white60),
                    ),
                    const SizedBox(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: List.generate(5, (index) {
                        final star = index + 1;
                        return IconButton(
                          icon: Icon(
                            star <= _ratingStars ? Icons.star_rounded : Icons.star_outline_rounded,
                            color: GreenColors.accentGold,
                            size: 38,
                          ),
                          onPressed: () {
                            setSheetState(() => _ratingStars = star);
                          },
                        );
                      }),
                    ),
                    const SizedBox(height: 12),

                    // Compliment Tags
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        'Lái xe an toàn',
                        'Xe sạch sẽ',
                        'Thân thiện',
                        'Đúng giờ',
                        'Nhiệt tình hỗ trợ',
                      ].map((tag) {
                        final isSelected = _selectedCompliments.contains(tag);
                        return FilterChip(
                          label: Text(tag, style: TextStyle(color: isSelected ? GreenColors.primaryEmerald : Colors.white70, fontSize: 11)),
                          selected: isSelected,
                          selectedColor: GreenColors.primaryEmerald.withValues(alpha: 0.2),
                          backgroundColor: GreenColors.cardDark,
                          checkmarkColor: GreenColors.primaryEmerald,
                          side: BorderSide(color: isSelected ? GreenColors.primaryEmerald : GreenColors.cardBorder),
                          onSelected: (selected) {
                            setSheetState(() {
                              if (selected) {
                                _selectedCompliments.add(tag);
                              } else {
                                _selectedCompliments.remove(tag);
                              }
                            });
                          },
                        );
                      }).toList(),
                    ),
                    const SizedBox(height: 24),

                    ElevatedButton(
                      onPressed: () {
                        Navigator.of(ctx).pop();
                        Navigator.of(context).pop();
                      },
                      style: ElevatedButton.styleFrom(
                        backgroundColor: GreenColors.primaryEmerald,
                        foregroundColor: GreenColors.backgroundDark,
                        minimumSize: const Size.fromHeight(50),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                      ),
                      child: const Text('VỀ TRANG CHỦ', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  @override
  void dispose() {
    _fallbackPollingTimer?.cancel();
    _markerAnimController.dispose();
    _driverLocationUnsub?.call();
    _tripStatusUnsub?.call();
    _stompService.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final trip = widget.trip;
    final driver = trip.driver;

    String statusText = 'Tài xế đang di chuyển đến điểm đón (${math.max((_etaSeconds / 60).round(), 1)} phút)';
    Color statusColor = GreenColors.primaryEmerald;
    if (_currentStatus == TripStatus.arrived) {
      statusText = 'Tài xế đã đến điểm đón! Vui lòng ra xe.';
      statusColor = GreenColors.electricCyan;
    } else if (_currentStatus == TripStatus.inTrip) {
      statusText = 'Đang di chuyển đến điểm trả khách an toàn...';
      statusColor = GreenColors.accentGold;
    } else if (_currentStatus == TripStatus.completed) {
      statusText = 'Chuyến đi đã hoàn thành!';
      statusColor = GreenColors.primaryEmerald;
    }

    return Scaffold(
      appBar: AppBar(
        title: Text('Chuyến #${trip.tripCode.isNotEmpty ? trip.tripCode : "GM-TRIP"}'),
        backgroundColor: GreenColors.backgroundDark,
        elevation: 0,
        automaticallyImplyLeading: false,
        actions: [
          IconButton(
            icon: const Icon(Icons.help_outline, color: Colors.white70),
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Trung tâm hỗ trợ khách hàng Green Mobility: 1900 6868')),
              );
            },
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // 1. ARRIVED Pop-up Banner Toast
              if (_showArrivedBanner)
                Container(
                  margin: const EdgeInsets.only(bottom: 12),
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                  decoration: BoxDecoration(
                    color: GreenColors.electricCyan.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: GreenColors.electricCyan, width: 1.5),
                  ),
                  child: const Row(
                    children: [
                      Icon(Icons.notifications_active, color: GreenColors.electricCyan, size: 26),
                      SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'Tài xế đã đến điểm đón! Vui lòng ra xe để bắt đầu chuyến đi.',
                          style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
                        ),
                      ),
                    ],
                  ),
                ),

              // 2. Real-time Status Card
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                decoration: BoxDecoration(
                  color: statusColor.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: statusColor.withValues(alpha: 0.4)),
                ),
                child: Row(
                  children: [
                    Icon(Icons.directions_car, color: statusColor, size: 22),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        statusText,
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: statusColor,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),

              // 3. Goong Live Route Map with Smooth Lerp Vehicle Marker
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
                distanceText: '${(_distanceRemainingM / 1000).toStringAsFixed(1)} km',
                durationText: '${math.max((_etaSeconds / 60).round(), 1)} phút',
                height: 220,
              ),
              const SizedBox(height: 14),

              // 4. Driver & Vehicle Information Card
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: GreenColors.surfaceDark,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: GreenColors.cardBorder),
                ),
                child: Column(
                  children: [
                    Row(
                      children: [
                        CircleAvatar(
                          radius: 26,
                          backgroundColor: GreenColors.primaryEmerald.withValues(alpha: 0.2),
                          child: Text(
                            driver?.fullName.isNotEmpty == true ? driver!.fullName[0].toUpperCase() : 'H',
                            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                driver?.fullName ?? 'Nguyễn Văn Hùng',
                                style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white),
                              ),
                              const SizedBox(height: 3),
                              Row(
                                children: [
                                  const Icon(Icons.star, color: GreenColors.accentGold, size: 15),
                                  const SizedBox(width: 4),
                                  Text(
                                    '${driver?.ratingAvg ?? 4.95}',
                                    style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.white),
                                  ),
                                  const SizedBox(width: 6),
                                  const Text('• Tài xế 5 sao', style: TextStyle(fontSize: 11, color: Colors.white54)),
                                ],
                              ),
                            ],
                          ),
                        ),
                        IconButton(
                          icon: const Icon(Icons.phone, color: GreenColors.primaryEmerald),
                          onPressed: () {
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(content: Text('Đang gọi cho tài xế: ${driver?.phoneNumber ?? "0912 345 678"}')),
                            );
                          },
                        ),
                        IconButton(
                          icon: const Icon(Icons.chat_bubble_outline, color: GreenColors.electricCyan),
                          onPressed: () {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('Đang mở hộp thoại chat với tài xế...')),
                            );
                          },
                        ),
                      ],
                    ),
                    const Divider(height: 20, color: Colors.white10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('PHƯƠNG TIỆN', style: TextStyle(fontSize: 10, color: Colors.white54)),
                            const SizedBox(height: 2),
                            Text(
                              driver?.vehicleModel ?? 'VinFast VF e34 (Xanh Lục)',
                              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: Colors.white),
                            ),
                          ],
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                          decoration: BoxDecoration(
                            color: Colors.white10,
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: Colors.white24),
                          ),
                          child: Text(
                            driver?.licensePlate ?? '51K-987.65',
                            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, letterSpacing: 0.8, color: Colors.white),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),

              // 5. Route & Environmental Badge Card
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: GreenColors.cardDark,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: GreenColors.cardBorder),
                ),
                child: Column(
                  children: [
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Icon(Icons.radio_button_checked, color: GreenColors.primaryEmerald, size: 16),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Điểm đón', style: TextStyle(fontSize: 10, color: Colors.white54)),
                              Text(trip.pickupAddress, style: const TextStyle(fontSize: 12, color: Colors.white)),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const Padding(
                      padding: EdgeInsets.only(left: 7.0, top: 4, bottom: 4),
                      child: Align(
                        alignment: Alignment.centerLeft,
                        child: SizedBox(height: 14, child: VerticalDivider(color: Colors.white24, thickness: 1.5)),
                      ),
                    ),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Icon(Icons.location_on, color: GreenColors.electricCyan, size: 16),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Điểm đến', style: TextStyle(fontSize: 10, color: Colors.white54)),
                              Text(trip.dropoffAddress, style: const TextStyle(fontSize: 12, color: Colors.white)),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const Divider(height: 20, color: Colors.white10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('CƯỚC PHÍ', style: TextStyle(fontSize: 10, color: Colors.white54)),
                            Text(
                              GreenFormatters.currencyVnd(trip.finalAmountVnd),
                              style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald),
                            ),
                          ],
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                          decoration: BoxDecoration(
                            color: GreenColors.primaryDark.withValues(alpha: 0.3),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.eco, color: GreenColors.primaryEmerald, size: 14),
                              const SizedBox(width: 4),
                              Text(
                                '-${trip.co2SavedGrams.round()}g CO2',
                                style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // 6. Cancel Trip Button (Visible during DRIVER_ARRIVING & ARRIVED)
              if (_currentStatus == TripStatus.driverArriving || _currentStatus == TripStatus.arrived)
                Center(
                  child: TextButton.icon(
                    onPressed: _isCancelling ? null : _handleCancelTrip,
                    icon: _isCancelling
                        ? const SizedBox(
                            width: 14,
                            height: 14,
                            child: CircularProgressIndicator(strokeWidth: 2, color: GreenColors.errorRed),
                          )
                        : const Icon(Icons.close, color: GreenColors.errorRed, size: 16),
                    label: const Text(
                      'HỦY CUỐC XE',
                      style: TextStyle(color: GreenColors.errorRed, fontSize: 13, fontWeight: FontWeight.bold),
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
