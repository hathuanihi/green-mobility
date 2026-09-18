import 'dart:async';
import 'package:flutter/material.dart';
import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';
import '../../../data/repositories/trip_repository.dart';
import '../trip/customer_active_trip_screen.dart';

class SearchingRadarScreen extends StatefulWidget {
  final TripModel trip;
  final TripRepository tripRepository;

  const SearchingRadarScreen({
    super.key,
    required this.trip,
    required this.tripRepository,
  });

  @override
  State<SearchingRadarScreen> createState() => _SearchingRadarScreenState();
}

class _SearchingRadarScreenState extends State<SearchingRadarScreen> with SingleTickerProviderStateMixin {
  late AnimationController _radarController;
  Timer? _matchTimer;
  int _secondsElapsed = 0;
  bool _isCancelling = false;

  @override
  void initState() {
    super.initState();
    _radarController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    )..repeat();

    // Real-time backend status polling: check trip state every 1.5 seconds
    _matchTimer = Timer.periodic(const Duration(milliseconds: 1500), (timer) async {
      if (!mounted) return;
      setState(() {
        _secondsElapsed = (_secondsElapsed + 1.5).round();
      });

      try {
        final updatedTrip = await widget.tripRepository.getTripDetails(widget.trip.tripId);
        if (!mounted) return;

        if (updatedTrip.status == TripStatus.matched && updatedTrip.driver != null) {
          timer.cancel();
          _navigateToActiveTrip(updatedTrip);
        } else if (updatedTrip.status == TripStatus.cancelled) {
          timer.cancel();
          _handleTripCancelled(updatedTrip.cancelReason);
        }
      } catch (_) {
        // Continue polling on transient errors
      }
    });
  }

  void _navigateToActiveTrip(TripModel matchedTrip) {
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(
        builder: (_) => CustomerActiveTripScreen(
          trip: matchedTrip,
          tripRepository: widget.tripRepository,
        ),
      ),
    );
  }

  void _handleTripCancelled(String? reason) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        backgroundColor: GreenColors.surfaceDark,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text('Cuốc xe đã kết thúc', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        content: Text(
          reason == 'NO_DRIVER_AVAILABLE'
              ? 'Rất tiếc! Hiện không có tài xế xe điện nào rảnh xung quanh điểm đón. Vui lòng thử lại sau ít phút.'
              : (reason ?? 'Cuốc xe đã bị hủy.'),
          style: const TextStyle(color: GreenColors.textSecondary, fontSize: 13),
        ),
        actions: [
          ElevatedButton(
            onPressed: () {
              Navigator.of(ctx).pop();
              Navigator.of(context).pop();
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: GreenColors.primaryEmerald,
              foregroundColor: GreenColors.backgroundDark,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            ),
            child: const Text('QUAY LẠI', style: TextStyle(fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  Future<void> _handleCancel() async {
    if (_isCancelling) return;
    setState(() {
      _isCancelling = true;
    });
    _matchTimer?.cancel();

    try {
      await widget.tripRepository.cancelTrip(widget.trip.tripId, reason: 'Khách hàng hủy tìm kiếm');
    } catch (_) {
      // Allow exiting even if network offline
    }

    if (mounted) {
      Navigator.of(context).pop();
    }
  }

  @override
  void dispose() {
    _radarController.dispose();
    _matchTimer?.cancel();
    super.dispose();
  }


  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 20.0),
          child: Column(
            children: [
              const SizedBox(height: 20),
              // Header
              const Text(
                'TÌM KIẾM TÀI XẾ XANH',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w900,
                  letterSpacing: 1.0,
                  color: GreenColors.primaryEmerald,
                ),
              ),
              const SizedBox(height: 6),
              const Text(
                'Hệ thống đang quét các xe điện trống trong bán kính 3.0 km',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 13, color: GreenColors.textSecondary),
              ),

              const Spacer(),

              // Animated Emerald Radar Waves
              Center(
                child: SizedBox(
                  width: 260,
                  height: 260,
                  child: AnimatedBuilder(
                    animation: _radarController,
                    builder: (context, child) {
                      return Stack(
                        alignment: Alignment.center,
                        children: [
                          // Ripple 1
                          _buildRipple(_radarController.value),
                          // Ripple 2
                          _buildRipple((_radarController.value + 0.33) % 1.0),
                          // Ripple 3
                          _buildRipple((_radarController.value + 0.66) % 1.0),
                          // Center Icon Core
                          Container(
                            width: 84,
                            height: 84,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: GreenColors.surfaceDark,
                              border: Border.all(color: GreenColors.primaryEmerald, width: 3),
                              boxShadow: [
                                BoxShadow(
                                  color: GreenColors.primaryEmerald.withValues(alpha: 0.4),
                                  blurRadius: 24,
                                  spreadRadius: 4,
                                ),
                              ],
                            ),
                            child: const Icon(
                              Icons.electric_car,
                              color: GreenColors.primaryEmerald,
                              size: 44,
                            ),
                          ),
                        ],
                      );
                    },
                  ),
                ),
              ),

              const SizedBox(height: 32),
              Text(
                'Đã tìm kiếm: ${_secondsElapsed}s',
                style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white70),
              ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.eco, color: GreenColors.primaryEmerald, size: 16),
                  const SizedBox(width: 6),
                  Text(
                    'Tiết kiệm ~${widget.trip.co2SavedGrams.round()}g CO2 cho môi trường',
                    style: const TextStyle(fontSize: 12, color: GreenColors.primaryEmerald, fontWeight: FontWeight.w600),
                  ),
                ],
              ),

              const Spacer(),

              // Route Summary Mini Card
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: GreenColors.surfaceDark,
                  borderRadius: BorderRadius.circular(18),
                  border: Border.all(color: GreenColors.cardBorder),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.location_on, color: GreenColors.electricCyan, size: 20),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('ĐẾN', style: TextStyle(fontSize: 10, color: Colors.white54)),
                          Text(
                            widget.trip.dropoffAddress,
                            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: Colors.white),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ),
                    ),
                    Text(
                      '${(widget.trip.finalAmountVnd / 1000).round()}k đ',
                      style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: GreenColors.primaryEmerald),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),

              // Cancel Button
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  onPressed: _isCancelling ? null : _handleCancel,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.white70,
                    side: const BorderSide(color: Colors.white24),
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                  ),
                  child: _isCancelling
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white70),
                        )
                      : const Text(
                          'HỦY TÌM KIẾM',
                          style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, letterSpacing: 0.5),
                        ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildRipple(double value) {
    final size = 84 + (260 - 84) * value;
    final opacity = (1.0 - value).clamp(0.0, 1.0) * 0.5;

    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(
          color: GreenColors.primaryEmerald.withValues(alpha: opacity),
          width: 2.0,
        ),
      ),
    );
  }
}
