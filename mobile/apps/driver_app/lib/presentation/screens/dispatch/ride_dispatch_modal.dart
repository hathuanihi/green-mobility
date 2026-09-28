import 'dart:async';
import 'package:flutter/material.dart';
import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';
import '../../../data/repositories/driver_repository.dart';

class RideDispatchModal extends StatefulWidget {
  final DispatchNotificationModel dispatch;
  final DriverRepository driverRepository;

  const RideDispatchModal({
    super.key,
    required this.dispatch,
    required this.driverRepository,
  });

  static Future<DriverTripModel?> show(
    BuildContext context, {
    required DispatchNotificationModel dispatch,
    required DriverRepository driverRepository,
  }) {
    return showModalBottomSheet<DriverTripModel>(
      context: context,
      isScrollControlled: true,
      isDismissible: false,
      enableDrag: false,
      backgroundColor: Colors.transparent,
      builder: (_) => RideDispatchModal(
        dispatch: dispatch,
        driverRepository: driverRepository,
      ),
    );
  }

  @override
  State<RideDispatchModal> createState() => _RideDispatchModalState();
}

class _RideDispatchModalState extends State<RideDispatchModal> with SingleTickerProviderStateMixin {
  late int _secondsLeft;
  Timer? _timer;
  bool _isProcessing = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _secondsLeft = widget.dispatch.countdownSeconds > 0 ? widget.dispatch.countdownSeconds : 15;
    _startCountdown();
  }

  void _startCountdown() {
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_secondsLeft <= 1) {
        timer.cancel();
        _handleDecline(isAutoTimeout: true);
      } else {
        setState(() {
          _secondsLeft--;
        });
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Color get _timerColor {
    if (_secondsLeft > 10) return GreenColors.primaryEmerald;
    if (_secondsLeft > 5) return GreenColors.warningAmber;
    return GreenColors.errorRed;
  }

  bool _isValidUuid(String id) {
    final uuidRegex = RegExp(r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$');
    return uuidRegex.hasMatch(id);
  }

  Future<void> _handleAccept() async {
    if (_isProcessing) return;
    _timer?.cancel();

    setState(() {
      _isProcessing = true;
      _errorMessage = null;
    });

    // Check if this is a simulation dispatch (not a real database UUID)
    if (!_isValidUuid(widget.dispatch.tripId)) {
      await Future.delayed(const Duration(milliseconds: 300));
      if (mounted) {
        final mockAcceptedTrip = DriverTripModel(
          tripId: widget.dispatch.tripId,
          tripCode: widget.dispatch.tripCode,
          status: TripStatus.matched,
          customerName: 'Nguyễn Thu Hà',
          customerPhone: '0988 123 456',
          pickupAddress: widget.dispatch.pickupAddress,
          pickupLat: widget.dispatch.pickupLat,
          pickupLng: widget.dispatch.pickupLng,
          dropoffAddress: widget.dispatch.dropoffAddress,
          dropoffLat: widget.dispatch.dropoffLat,
          dropoffLng: widget.dispatch.dropoffLng,
          estimatedDistanceKm: widget.dispatch.tripDistanceKm,
          netIncomeVnd: widget.dispatch.estimatedEarningsVnd,
          co2SavedGrams: widget.dispatch.co2SavedGrams,
          matchedAt: DateTime.now().toIso8601String(),
        );
        Navigator.of(context).pop(mockAcceptedTrip);
      }
      return;
    }

    try {
      final acceptedTrip = await widget.driverRepository.acceptTrip(widget.dispatch.tripId);
      if (mounted) {
        Navigator.of(context).pop(acceptedTrip);
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isProcessing = false;
          _errorMessage = e.toString().replaceAll('Exception: ', '');
        });
      }
    }
  }

  Future<void> _handleDecline({bool isAutoTimeout = false}) async {
    if (_isProcessing) return;
    _timer?.cancel();

    setState(() {
      _isProcessing = true;
    });

    if (!_isValidUuid(widget.dispatch.tripId)) {
      if (mounted) {
        Navigator.of(context).pop(null);
      }
      return;
    }

    try {
      await widget.driverRepository.declineTrip(
        widget.dispatch.tripId,
        reason: isAutoTimeout ? 'Hết thời gian đếm ngược 15s' : 'Tài xế bấm từ chối',
      );
    } catch (_) {
      // Ignore network errors when declining
    }

    if (mounted) {
      Navigator.of(context).pop(null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final dispatch = widget.dispatch;
    final progress = _secondsLeft / 15.0;

    return Container(
      decoration: const BoxDecoration(
        color: GreenColors.surfaceDark,
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
        boxShadow: [
          BoxShadow(
            color: Colors.black54,
            blurRadius: 30,
            offset: Offset(0, -5),
          ),
        ],
      ),
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Top Grab Bar
            Center(
              child: Container(
                width: 48,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.white24,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Header with Circular Countdown Timer & Sound Pulse
            Row(
              children: [
                // Circular Timer
                Stack(
                  alignment: Alignment.center,
                  children: [
                    SizedBox(
                      width: 58,
                      height: 58,
                      child: CircularProgressIndicator(
                        value: progress,
                        strokeWidth: 5,
                        backgroundColor: Colors.white10,
                        valueColor: AlwaysStoppedAnimation<Color>(_timerColor),
                      ),
                    ),
                    Text(
                      '$_secondsLeft',
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                        color: _timerColor,
                      ),
                    ),
                  ],
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Row(
                        children: [
                          Icon(Icons.bolt, color: GreenColors.primaryEmerald, size: 20),
                          SizedBox(width: 4),
                          Text(
                            'CUỐC XE XANH MỚI',
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 0.5,
                              color: GreenColors.primaryEmerald,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Cách điểm đón ${dispatch.distanceToPickupKm > 0 ? dispatch.distanceToPickupKm.toStringAsFixed(1) : "0.8"} km (~3 phút)',
                        style: const TextStyle(
                          fontSize: 13,
                          color: GreenColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                // CO2 Badge
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: GreenColors.primaryDark.withValues(alpha: 0.3),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: GreenColors.primaryEmerald.withValues(alpha: 0.3)),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.eco, color: GreenColors.primaryEmerald, size: 14),
                      const SizedBox(width: 4),
                      Text(
                        '+${dispatch.co2SavedGrams.round()}g',
                        style: const TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: GreenColors.primaryEmerald,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),

            // Earnings Highlight Card
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF064E3B), Color(0xFF065F46)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: GreenColors.primaryEmerald.withValues(alpha: 0.4)),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'THU NHẬP TÀI XẾ (NET)',
                        style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Color(0xFFA7F3D0)),
                      ),
                      SizedBox(height: 2),
                      Text(
                        'Đã trừ 20% phí nền tảng',
                        style: TextStyle(fontSize: 10, color: Colors.white70),
                      ),
                    ],
                  ),
                  Text(
                    '+${GreenFormatters.currencyVnd(dispatch.estimatedEarningsVnd)}',
                    style: const TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.w900,
                      color: Colors.white,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Route Details
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: GreenColors.cardDark,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: GreenColors.cardBorder),
              ),
              child: Column(
                children: [
                  // Pickup row
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(Icons.radio_button_checked, color: GreenColors.primaryEmerald, size: 18),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('ĐIỂM ĐÓN KHÁCH', style: TextStyle(fontSize: 10, color: Colors.white54)),
                            const SizedBox(height: 2),
                            Text(
                              dispatch.pickupAddress,
                              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white),
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const Padding(
                    padding: EdgeInsets.only(left: 8.0, top: 4, bottom: 4),
                    child: Align(
                      alignment: Alignment.centerLeft,
                      child: SizedBox(
                        height: 16,
                        child: VerticalDivider(color: Colors.white24, thickness: 1.5),
                      ),
                    ),
                  ),
                  // Dropoff row
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(Icons.location_on, color: GreenColors.electricCyan, size: 18),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('ĐIỂM ĐẾN', style: TextStyle(fontSize: 10, color: Colors.white54)),
                            const SizedBox(height: 2),
                            Text(
                              dispatch.dropoffAddress,
                              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white),
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.white10,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          '${dispatch.tripDistanceKm.toStringAsFixed(1)} km',
                          style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.white),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),

            if (_errorMessage != null) ...[
              const SizedBox(height: 12),
              Text(
                _errorMessage!,
                style: const TextStyle(color: GreenColors.errorRed, fontSize: 13),
                textAlign: TextAlign.center,
              ),
            ],

            const SizedBox(height: 20),

            // Action Buttons (Decline & Accept)
            Row(
              children: [
                Expanded(
                  flex: 1,
                  child: OutlinedButton(
                    onPressed: _isProcessing ? null : () => _handleDecline(),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.white70,
                      side: const BorderSide(color: Colors.white24),
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                    ),
                    child: const Text('Bỏ qua', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  flex: 2,
                  child: ElevatedButton(
                    onPressed: _isProcessing ? null : _handleAccept,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: GreenColors.primaryEmerald,
                      foregroundColor: GreenColors.backgroundDark,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      elevation: 4,
                      shadowColor: GreenColors.primaryEmerald.withValues(alpha: 0.5),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                    ),
                    child: _isProcessing
                        ? const SizedBox(
                            width: 22,
                            height: 22,
                            child: CircularProgressIndicator(
                              strokeWidth: 2.5,
                              color: GreenColors.backgroundDark,
                            ),
                          )
                        : const Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.check_circle, size: 20, color: GreenColors.backgroundDark),
                              SizedBox(width: 8),
                              Text(
                                'NHẬN CUỐC XE',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w900,
                                  color: GreenColors.backgroundDark,
                                ),
                              ),
                            ],
                          ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
