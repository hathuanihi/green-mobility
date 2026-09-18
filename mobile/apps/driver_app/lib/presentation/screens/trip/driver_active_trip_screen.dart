import 'package:flutter/material.dart';
import 'package:core_model/core_model.dart';
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
  bool _isUpdating = false;

  @override
  void initState() {
    super.initState();
    _currentStatus = widget.initialTrip.status;
    if (_currentStatus == TripStatus.matched) {
      _currentStatus = TripStatus.driverArriving;
    }
  }

  void _nextStep() {
    setState(() {
      _isUpdating = true;
    });

    Future.delayed(const Duration(milliseconds: 600), () {
      if (!mounted) return;
      setState(() {
        _isUpdating = false;
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
              width: 64,
              height: 64,
              decoration: BoxDecoration(
                color: GreenColors.primaryEmerald.withValues(alpha: 0.15),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.check_circle, color: GreenColors.primaryEmerald, size: 44),
            ),
            const SizedBox(height: 16),
            const Text(
              'CHUYẾN ĐI HOÀN TẤT! 🎉',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
            ),
            const SizedBox(height: 8),
            const Text(
              'Cảm ơn bạn đã đồng hành cùng giao thông xanh!',
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
                      const Text('Thu nhập thực nhận:', style: TextStyle(color: Colors.white70)),
                      Text(
                        '+${GreenFormatters.currencyVnd(widget.initialTrip.netIncomeVnd)}',
                        style: const TextStyle(fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald, fontSize: 16),
                      ),
                    ],
                  ),
                  const Divider(height: 20, color: Colors.white10),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Lượng CO2 đã giảm:', style: TextStyle(color: Colors.white70)),
                      Text(
                        '🌱 +${widget.initialTrip.co2SavedGrams.round()}g',
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
    final trip = widget.initialTrip;

    String stepButtonText = 'ĐÃ ĐẾN ĐIỂM ĐÓN';
    IconData stepButtonIcon = Icons.location_on;
    if (_currentStatus == TripStatus.arrived) {
      stepButtonText = 'BẮT ĐẦU CHỞ KHÁCH';
      stepButtonIcon = Icons.navigation;
    } else if (_currentStatus == TripStatus.inTrip) {
      stepButtonText = 'HOÀN THÀNH CHUYẾN ĐI';
      stepButtonIcon = Icons.check_circle_outline;
    }

    return Scaffold(
      appBar: AppBar(
        title: Text('Cuốc xe #${trip.tripCode.isNotEmpty ? trip.tripCode : trip.tripId.substring(0, 8).toUpperCase()}'),
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
                  style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald),
                ),
              ),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Customer Profile Card
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: GreenColors.surfaceDark,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: GreenColors.cardBorder),
                ),
                child: Row(
                  children: [
                    CircleAvatar(
                      radius: 26,
                      backgroundColor: GreenColors.electricCyan.withValues(alpha: 0.2),
                      child: Text(
                        trip.customerName.isNotEmpty ? trip.customerName[0].toUpperCase() : 'K',
                        style: const TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                          color: GreenColors.electricCyan,
                        ),
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            trip.customerName.isNotEmpty ? trip.customerName : 'Khách hàng Green Mobility',
                            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            trip.customerPhone.isNotEmpty ? trip.customerPhone : '090 ••• ••••',
                            style: const TextStyle(fontSize: 13, color: GreenColors.textSecondary),
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
                          const SnackBar(
                            content: Text('Đang kết nối tin nhắn với hành khách...'),
                            backgroundColor: GreenColors.surfaceDark,
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // Earnings & Green Eco Metrics Card
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFF064E3B), Color(0xFF047857)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: [
                    Column(
                      children: [
                        const Text('THU NHẬP DỰ KIẾN', style: TextStyle(fontSize: 11, color: Color(0xFFA7F3D0))),
                        const SizedBox(height: 6),
                        Text(
                          '+${GreenFormatters.currencyVnd(trip.netIncomeVnd)}',
                          style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900, color: Colors.white),
                        ),
                      ],
                    ),
                    Container(height: 36, width: 1, color: Colors.white24),
                    Column(
                      children: [
                        const Text('GIẢM PHÁT THẢI CO2', style: TextStyle(fontSize: 11, color: Color(0xFFA7F3D0))),
                        const SizedBox(height: 6),
                        Row(
                          children: [
                            const Icon(Icons.eco, color: Colors.white, size: 18),
                            const SizedBox(width: 4),
                            Text(
                              '+${trip.co2SavedGrams.round()}g',
                              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900, color: Colors.white),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),

              // Route Timeline Card
              Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: GreenColors.cardDark,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: GreenColors.cardBorder),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'LỘ TRÌNH DI CHUYỂN',
                      style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, letterSpacing: 0.5, color: Colors.white60),
                    ),
                    const SizedBox(height: 14),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Icon(Icons.trip_origin, color: GreenColors.primaryEmerald, size: 20),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Điểm đón', style: TextStyle(fontSize: 11, color: Colors.white54)),
                              const SizedBox(height: 2),
                              Text(
                                trip.pickupAddress,
                                style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const Padding(
                      padding: EdgeInsets.only(left: 9.0, top: 4, bottom: 4),
                      child: SizedBox(
                        height: 22,
                        child: VerticalDivider(color: Colors.white24, thickness: 1.5),
                      ),
                    ),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Icon(Icons.location_on, color: GreenColors.electricCyan, size: 20),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Điểm đến', style: TextStyle(fontSize: 11, color: Colors.white54)),
                              const SizedBox(height: 2),
                              Text(
                                trip.dropoffAddress,
                                style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 32),

              // Progression Action Button
              ElevatedButton.icon(
                onPressed: _isUpdating ? null : _nextStep,
                icon: _isUpdating
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2, color: GreenColors.backgroundDark),
                      )
                    : Icon(stepButtonIcon, color: GreenColors.backgroundDark),
                label: Text(
                  stepButtonText,
                  style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: GreenColors.backgroundDark),
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: GreenColors.primaryEmerald,
                  padding: const EdgeInsets.symmetric(vertical: 18),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  elevation: 4,
                  shadowColor: GreenColors.primaryEmerald.withValues(alpha: 0.4),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
