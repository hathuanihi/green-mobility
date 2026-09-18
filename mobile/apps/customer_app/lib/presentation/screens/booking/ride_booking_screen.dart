import 'package:flutter/material.dart';
import 'package:core_map/core_map.dart';
import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';
import '../../../data/repositories/auth_repository.dart';
import '../../../data/repositories/trip_repository.dart';
import '../auth/customer_login_screen.dart';
import 'searching_radar_screen.dart';

class RideBookingScreen extends StatefulWidget {
  final TripRepository tripRepository;
  final AuthRepository? authRepository;

  const RideBookingScreen({
    super.key,
    required this.tripRepository,
    this.authRepository,
  });

  @override
  State<RideBookingScreen> createState() => _RideBookingScreenState();
}

class _RideBookingScreenState extends State<RideBookingScreen> {
  late PresetLocation _selectedPickup;
  late PresetLocation _selectedDropoff;

  String _selectedVehicleType = VehicleTypes.electricMotorbike;
  String _selectedPaymentMethod = PaymentMethod.cash;

  bool _isLoadingEstimate = false;
  bool _isRequestingTrip = false;
  String? _errorMessage;

  TripEstimateModel? _currentEstimate;

  @override
  void initState() {
    super.initState();
    _selectedPickup = PresetLocation.benThanh;
    _selectedDropoff = PresetLocation.landmark81;
    _loadEstimate();
  }

  Future<void> _loadEstimate() async {
    setState(() {
      _isLoadingEstimate = true;
      _errorMessage = null;
    });

    final request = TripEstimateRequestModel(
      pickupAddress: _selectedPickup.address,
      pickupLat: _selectedPickup.lat,
      pickupLng: _selectedPickup.lng,
      dropoffAddress: _selectedDropoff.address,
      dropoffLat: _selectedDropoff.lat,
      dropoffLng: _selectedDropoff.lng,
      vehicleType: _selectedVehicleType,
    );

    try {
      final estimate = await widget.tripRepository.estimateTrip(request);
      if (mounted) {
        setState(() {
          _currentEstimate = estimate;
          _isLoadingEstimate = false;
        });
      }
    } catch (_) {
      // Fallback local calculations using domain models if backend is offline
      if (mounted) {
        setState(() {
          _currentEstimate = _calculateFallbackEstimate(_selectedVehicleType);
          _isLoadingEstimate = false;
        });
      }
    }
  }

  TripEstimateModel _calculateFallbackEstimate(String vehicleType) {
    final opt = VehicleTypes.getOption(vehicleType);
    const double distanceKm = 5.4;
    final fare = opt.calculateFare(distanceKm);
    final co2 = opt.calculateCo2Saved(distanceKm);

    return TripEstimateModel(
      vehicleType: vehicleType,
      distanceMeters: (distanceKm * 1000).toInt(),
      distanceKm: distanceKm,
      durationSeconds: 840,
      durationMinutes: 14,
      fareAmountVnd: fare,
      carbonEstimate: CarbonEstimateModel(
        co2SavedGrams: co2,
        treeAbsorptionDays: co2 / 21.0,
        ledBulbHours: co2 / 10.0,
        baselineGasolineGrams: co2 * 1.5,
        evEmittedGrams: co2 * 0.5,
      ),
    );
  }

  Future<void> _confirmBooking() async {
    if (_isRequestingTrip) return;
    setState(() {
      _isRequestingTrip = true;
      _errorMessage = null;
    });

    // Check if customer is authenticated
    final token = await widget.tripRepository.tripApi.apiClient.tokenStorage.getToken();
    if (token == null || token.isEmpty) {
      setState(() {
        _isRequestingTrip = false;
        _errorMessage = 'Bạn chưa đăng nhập. Vui lòng đăng nhập tài khoản Khách hàng để đặt xe.';
      });
      return;
    }

    final request = TripRequestModel(
      pickupAddress: _selectedPickup.address,
      pickupLat: _selectedPickup.lat,
      pickupLng: _selectedPickup.lng,
      dropoffAddress: _selectedDropoff.address,
      dropoffLat: _selectedDropoff.lat,
      dropoffLng: _selectedDropoff.lng,
      vehicleType: _selectedVehicleType,
      paymentMethod: _selectedPaymentMethod,
    );

    try {
      final trip = await widget.tripRepository.requestTrip(request);
      if (mounted) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(
            builder: (_) => SearchingRadarScreen(
              trip: trip,
              tripRepository: widget.tripRepository,
            ),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        final err = e.toString().replaceAll('Exception: ', '');
        if (err.contains('403') || err.toLowerCase().contains('forbidden')) {
          _errorMessage = 'Phiên làm việc hết hạn hoặc chưa được cấp quyền (403 Forbidden). Vui lòng đăng nhập lại.';
        } else {
          _errorMessage = err;
        }
        setState(() {});
      }
    } finally {
      if (mounted) {
        setState(() {
          _isRequestingTrip = false;
        });
      }
    }
  }


  IconData _getVehicleIcon(String iconCode) {
    switch (iconCode) {
      case 'two_wheeler':
        return Icons.two_wheeler;
      case 'airport_shuttle':
        return Icons.airport_shuttle;
      case 'directions_car':
      default:
        return Icons.directions_car;
    }
  }

  @override
  Widget build(BuildContext context) {
    final fareDisplay = _currentEstimate != null
        ? GreenFormatters.currencyVnd(_currentEstimate!.fareAmountVnd)
        : '76.000 đ';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Đặt xe điện Green Mobility', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17)),
        backgroundColor: GreenColors.backgroundDark,
        elevation: 0,
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Origin & Destination Inputs Card
              Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: GreenColors.surfaceDark,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: GreenColors.cardBorder),
                ),
                child: Column(
                  children: [
                    // Pickup selector
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Padding(
                          padding: EdgeInsets.only(top: 8.0),
                          child: Icon(Icons.radio_button_checked, color: GreenColors.primaryEmerald, size: 20),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('ĐIỂM ĐÓN', style: TextStyle(fontSize: 10, color: Colors.white54, fontWeight: FontWeight.bold)),
                              const SizedBox(height: 2),
                              DropdownButton<PresetLocation>(
                                value: _selectedPickup,
                                isExpanded: true,
                                underline: const SizedBox(),
                                dropdownColor: GreenColors.surfaceDark,
                                items: PresetLocation.all.map((loc) {
                                  return DropdownMenuItem(
                                    value: loc,
                                    child: Text(
                                      loc.title,
                                      style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
                                    ),
                                  );
                                }).toList(),
                                onChanged: (val) {
                                  if (val != null) {
                                    setState(() {
                                      _selectedPickup = val;
                                    });
                                    _loadEstimate();
                                  }
                                },
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const Divider(height: 20, color: Colors.white12),
                    // Dropoff selector
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Padding(
                          padding: EdgeInsets.only(top: 8.0),
                          child: Icon(Icons.location_on, color: GreenColors.electricCyan, size: 20),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('ĐIỂM ĐẾN', style: TextStyle(fontSize: 10, color: Colors.white54, fontWeight: FontWeight.bold)),
                              const SizedBox(height: 2),
                              DropdownButton<PresetLocation>(
                                value: _selectedDropoff,
                                isExpanded: true,
                                underline: const SizedBox(),
                                dropdownColor: GreenColors.surfaceDark,
                                items: PresetLocation.all.map((loc) {
                                  return DropdownMenuItem(
                                    value: loc,
                                    child: Text(
                                      loc.title,
                                      style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w600),
                                    ),
                                  );
                                }).toList(),
                                onChanged: (val) {
                                  if (val != null) {
                                    setState(() {
                                      _selectedDropoff = val;
                                    });
                                    _loadEstimate();
                                  }
                                },
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),

              // EV Tier Selection Section (DRY mapping from VehicleTypes.options)
              const Text(
                'Chọn dòng phương tiện xanh',
                style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white),
              ),
              const SizedBox(height: 12),

              ...VehicleTypes.options.map((opt) {
                final isSelected = _selectedVehicleType == opt.code;
                final fareEst = opt.calculateFare(5.4);
                final co2Est = opt.calculateCo2Saved(5.4);

                return Padding(
                  padding: const EdgeInsets.only(bottom: 10.0),
                  child: GestureDetector(
                    onTap: () {
                      setState(() {
                        _selectedVehicleType = opt.code;
                      });
                      _loadEstimate();
                    },
                    child: Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: isSelected ? GreenColors.primaryDark.withValues(alpha: 0.2) : GreenColors.cardDark,
                        borderRadius: BorderRadius.circular(18),
                        border: Border.all(
                          color: isSelected ? GreenColors.primaryEmerald : GreenColors.cardBorder,
                          width: isSelected ? 2.0 : 1.0,
                        ),
                      ),
                      child: Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: isSelected ? GreenColors.primaryEmerald : Colors.white10,
                              borderRadius: BorderRadius.circular(14),
                            ),
                            child: Icon(
                              _getVehicleIcon(opt.icon),
                              color: isSelected ? GreenColors.backgroundDark : Colors.white,
                              size: 26,
                            ),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  opt.label,
                                  style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Colors.white),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  opt.subtitle,
                                  style: const TextStyle(fontSize: 11, color: GreenColors.textSecondary),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  '🌱 ${GreenFormatters.co2(co2Est)}',
                                  style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald),
                                ),
                              ],
                            ),
                          ),
                          Text(
                            GreenFormatters.currencyVnd(fareEst),
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w900,
                              color: isSelected ? GreenColors.primaryEmerald : Colors.white,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              }),

              const SizedBox(height: 10),

              // Green Impact & Environmental Badge
              Container(
                padding: const EdgeInsets.all(16),
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
                  children: [
                    const Icon(Icons.eco, color: GreenColors.primaryEmerald, size: 28),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Tác động Môi trường Chuyến đi',
                            style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: Colors.white),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            _currentEstimate != null
                                ? 'Tiết kiệm ~${GreenFormatters.co2(_currentEstimate!.carbonEstimate?.co2SavedGrams ?? 817)} so với xe xăng'
                                : 'Giảm phát thải khí nhà kính trực tiếp trên từng km',
                            style: const TextStyle(fontSize: 11, color: Color(0xFFA7F3D0)),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),

              // Payment Method Picker
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                decoration: BoxDecoration(
                  color: GreenColors.cardDark,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: GreenColors.cardBorder),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.payment, color: GreenColors.electricCyan, size: 20),
                    const SizedBox(width: 12),
                    const Text('Thanh toán:', style: TextStyle(fontSize: 13, color: Colors.white70)),
                    const Spacer(),
                    DropdownButton<String>(
                      value: _selectedPaymentMethod,
                      underline: const SizedBox(),
                      dropdownColor: GreenColors.surfaceDark,
                      items: [
                        DropdownMenuItem(
                          value: PaymentMethod.cash,
                          child: Text(PaymentMethod.getLabel(PaymentMethod.cash), style: const TextStyle(color: Colors.white)),
                        ),
                        DropdownMenuItem(
                          value: PaymentMethod.wallet,
                          child: Text(PaymentMethod.getLabel(PaymentMethod.wallet), style: const TextStyle(color: Colors.white)),
                        ),
                        DropdownMenuItem(
                          value: PaymentMethod.card,
                          child: Text(PaymentMethod.getLabel(PaymentMethod.card), style: const TextStyle(color: Colors.white)),
                        ),
                      ],
                      onChanged: (val) {
                        if (val != null) {
                          setState(() {
                            _selectedPaymentMethod = val;
                          });
                        }
                      },
                    ),
                  ],
                ),
              ),

              if (_errorMessage != null) ...[
                const SizedBox(height: 14),
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: GreenColors.errorRed.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: GreenColors.errorRed.withValues(alpha: 0.4)),
                  ),
                  child: Column(
                    children: [
                      Row(
                        children: [
                          const Icon(Icons.error_outline, color: GreenColors.errorRed, size: 20),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              _errorMessage!,
                              style: const TextStyle(color: Colors.white, fontSize: 13),
                            ),
                          ),
                        ],
                      ),
                      if (widget.authRepository != null &&
                          (_errorMessage!.contains('đăng nhập') || _errorMessage!.contains('403'))) ...[
                        const SizedBox(height: 10),
                        SizedBox(
                          width: double.infinity,
                          child: ElevatedButton(
                            onPressed: () {
                              Navigator.of(context).push(
                                MaterialPageRoute(
                                  builder: (_) => CustomerLoginScreen(
                                    authRepository: widget.authRepository!,
                                    tripRepository: widget.tripRepository,
                                  ),
                                ),
                              );
                            },
                            style: ElevatedButton.styleFrom(
                              backgroundColor: GreenColors.primaryEmerald,
                              foregroundColor: GreenColors.backgroundDark,
                              padding: const EdgeInsets.symmetric(vertical: 10),
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                            ),
                            child: const Text('ĐĂNG NHẬP TÀI KHOẢN KHÁCH HÀNG', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ],


              const SizedBox(height: 24),

              // Submit Button
              ElevatedButton(
                onPressed: _isRequestingTrip ? null : _confirmBooking,
                style: ElevatedButton.styleFrom(
                  backgroundColor: GreenColors.primaryEmerald,
                  foregroundColor: GreenColors.backgroundDark,
                  padding: const EdgeInsets.symmetric(vertical: 18),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  elevation: 4,
                  shadowColor: GreenColors.primaryEmerald.withValues(alpha: 0.5),
                ),
                child: _isRequestingTrip
                    ? const SizedBox(
                        width: 22,
                        height: 22,
                        child: CircularProgressIndicator(strokeWidth: 2.5, color: GreenColors.backgroundDark),
                      )
                    : Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Icon(Icons.bolt, color: GreenColors.backgroundDark, size: 22),
                          const SizedBox(width: 8),
                          Text(
                            _isLoadingEstimate
                                ? 'Đang tính toán cước phí...'
                                : 'XÁC NHẬN ĐẶT XE XANH • $fareDisplay',
                            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w900, color: GreenColors.backgroundDark),
                          ),
                        ],
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
