import 'package:flutter/material.dart';
import 'package:core_model/core_model.dart';
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

class _CustomerActiveTripScreenState extends State<CustomerActiveTripScreen> {
  late String _currentStatus;
  bool _isProcessing = false;

  @override
  void initState() {
    super.initState();
    _currentStatus = widget.trip.status;
    if (_currentStatus == TripStatus.matched) {
      _currentStatus = TripStatus.driverArriving;
    }
  }

  void _progressTripSimulated() {
    setState(() {
      _isProcessing = true;
    });

    Future.delayed(const Duration(milliseconds: 600), () {
      if (!mounted) return;
      setState(() {
        _isProcessing = false;
        if (_currentStatus == TripStatus.driverArriving || _currentStatus == TripStatus.matched) {
          _currentStatus = TripStatus.arrived;
        } else if (_currentStatus == TripStatus.arrived) {
          _currentStatus = TripStatus.inTrip;
        } else if (_currentStatus == TripStatus.inTrip) {
          _currentStatus = TripStatus.completed;
          _showCompletionDialog();
        }
      });
    });
  }

  void _showCompletionDialog() {
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
              width: 70,
              height: 70,
              decoration: BoxDecoration(
                color: GreenColors.primaryEmerald.withValues(alpha: 0.15),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.eco, color: GreenColors.primaryEmerald, size: 44),
            ),
            const SizedBox(height: 16),
            const Text(
              'CHUYẾN ĐI HOÀN TẤT!',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
            ),
            const SizedBox(height: 8),
            const Text(
              'Cảm ơn bạn đã lựa chọn di chuyển phát thải thấp cùng Green Mobility.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 13, color: Colors.white70),
            ),
            const SizedBox(height: 20),
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
                      const Text('CO2 đã giảm thiểu:', style: TextStyle(color: Colors.white70)),
                      Text(
                        '🌱 -${widget.trip.co2SavedGrams.round()}g',
                        style: const TextStyle(fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald, fontSize: 16),
                      ),
                    ],
                  ),
                  const Divider(height: 20, color: Colors.white10),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Tín chỉ Carbon (PCC):', style: TextStyle(color: Colors.white70)),
                      Text(
                        '+${(widget.trip.co2SavedGrams / 1000).toStringAsFixed(3)} PCC',
                        style: const TextStyle(fontWeight: FontWeight.bold, color: GreenColors.electricCyan, fontSize: 15),
                      ),
                    ],
                  ),
                ],
              ),
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
                minimumSize: const Size.fromHeight(48),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
              child: const Text('VỀ TRANG CHỦ', style: TextStyle(fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final trip = widget.trip;
    final driver = trip.driver;

    String statusText = 'Tài xế đang di chuyển đến điểm đón (khoảng 3 phút)';
    Color statusColor = GreenColors.primaryEmerald;
    if (_currentStatus == TripStatus.arrived) {
      statusText = 'Tài xế đã đến điểm đón! Vui lòng ra xe.';
      statusColor = GreenColors.electricCyan;
    } else if (_currentStatus == TripStatus.inTrip) {
      statusText = 'Đang di chuyển đến điểm trả khách an toàn...';
      statusColor = GreenColors.accentGold;
    } else if (_currentStatus == TripStatus.completed) {
      statusText = 'Chuyến đi đã hoàn thành';
      statusColor = GreenColors.primaryEmerald;
    }

    return Scaffold(
      appBar: AppBar(
        title: Text('Chuyến đi #${trip.tripCode.isNotEmpty ? trip.tripCode : "GM-TRIP"}'),
        backgroundColor: GreenColors.backgroundDark,
        elevation: 0,
        automaticallyImplyLeading: false,
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Live Status Header
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
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
              const SizedBox(height: 16),

              // Driver & Vehicle Card
              Container(
                padding: const EdgeInsets.all(18),
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
                          radius: 28,
                          backgroundColor: GreenColors.primaryEmerald.withValues(alpha: 0.2),
                          child: Text(
                            driver?.fullName.isNotEmpty == true ? driver!.fullName[0].toUpperCase() : 'H',
                            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald),
                          ),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                driver?.fullName ?? 'Nguyễn Văn Hùng',
                                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white),
                              ),
                              const SizedBox(height: 4),
                              Row(
                                children: [
                                  const Icon(Icons.star, color: GreenColors.accentGold, size: 16),
                                  const SizedBox(width: 4),
                                  Text(
                                    '${driver?.ratingAvg ?? 4.95}',
                                    style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: Colors.white),
                                  ),
                                  const SizedBox(width: 8),
                                  const Text('• Tài xế 5 sao', style: TextStyle(fontSize: 12, color: Colors.white54)),
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
                      ],
                    ),
                    const Divider(height: 24, color: Colors.white10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('XE ĐIỆN', style: TextStyle(fontSize: 10, color: Colors.white54)),
                            const SizedBox(height: 2),
                            Text(
                              driver?.vehicleModel ?? 'VinFast VF e34 (Xanh Lục)',
                              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white),
                            ),
                          ],
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                          decoration: BoxDecoration(
                            color: Colors.white10,
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: Colors.white24),
                          ),
                          child: Text(
                            driver?.licensePlate ?? '51K-987.65',
                            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, letterSpacing: 1.0, color: Colors.white),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // Route & Environmental Badge Card
              Container(
                padding: const EdgeInsets.all(18),
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
                        const Icon(Icons.radio_button_checked, color: GreenColors.primaryEmerald, size: 18),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Điểm đón', style: TextStyle(fontSize: 10, color: Colors.white54)),
                              Text(trip.pickupAddress, style: const TextStyle(fontSize: 13, color: Colors.white)),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const Padding(
                      padding: EdgeInsets.only(left: 8.0, top: 4, bottom: 4),
                      child: Align(
                        alignment: Alignment.centerLeft,
                        child: SizedBox(height: 16, child: VerticalDivider(color: Colors.white24, thickness: 1.5)),
                      ),
                    ),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Icon(Icons.location_on, color: GreenColors.electricCyan, size: 18),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Điểm đến', style: TextStyle(fontSize: 10, color: Colors.white54)),
                              Text(trip.dropoffAddress, style: const TextStyle(fontSize: 13, color: Colors.white)),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const Divider(height: 24, color: Colors.white10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('CƯỚC PHÍ', style: TextStyle(fontSize: 10, color: Colors.white54)),
                            Text(
                              GreenFormatters.currencyVnd(trip.finalAmountVnd),
                              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald),
                            ),
                          ],
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                          decoration: BoxDecoration(
                            color: GreenColors.primaryDark.withValues(alpha: 0.3),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.eco, color: GreenColors.primaryEmerald, size: 14),
                              const SizedBox(width: 4),
                              Text(
                                '-${trip.co2SavedGrams.round()}g CO2',
                                style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),

              // Simulation Progression Button
              ElevatedButton.icon(
                onPressed: _isProcessing ? null : _progressTripSimulated,
                icon: _isProcessing
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2, color: GreenColors.backgroundDark),
                      )
                    : const Icon(Icons.fast_forward, color: GreenColors.backgroundDark),
                label: Text(
                  _currentStatus == TripStatus.inTrip ? 'HOÀN THÀNH CHUYẾN ĐI' : 'MÔ PHỎNG TIẾN TRÌNH XE ĐẾN',
                  style: const TextStyle(fontWeight: FontWeight.bold, color: GreenColors.backgroundDark),
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: GreenColors.primaryEmerald,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
