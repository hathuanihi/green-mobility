import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../data/repositories/driver_repository.dart';
import '../../bloc/auth/auth_cubit.dart';
import '../../bloc/driver/driver_cubit.dart';
import '../../bloc/driver/driver_state.dart';
import '../../services/location_ping_service.dart';
import '../dispatch/ride_dispatch_modal.dart';
import '../kyc/kyc_wizard_screen.dart';
import '../shift/face_verification_modal.dart';
import '../trip/driver_active_trip_screen.dart';
import 'widgets/kyc_banner.dart';
import 'widgets/shift_toggle_card.dart';
import 'widgets/driver_stats_card.dart';
import 'widgets/vehicle_info_card.dart';

class DriverHomeScreen extends StatefulWidget {
  const DriverHomeScreen({super.key});

  @override
  State<DriverHomeScreen> createState() => _DriverHomeScreenState();
}

class _DriverHomeScreenState extends State<DriverHomeScreen> {
  LocationPingService? _locationPingService;

  @override
  void initState() {
    super.initState();
    final driverRepo = context.read<DriverRepository>();
    _locationPingService = LocationPingService(driverRepository: driverRepo);
    _locationPingService!.incomingDispatchNotifier.addListener(_onIncomingDispatchChanged);
    context.read<DriverCubit>().loadProfile();
  }

  @override
  void dispose() {
    _locationPingService?.incomingDispatchNotifier.removeListener(_onIncomingDispatchChanged);
    _locationPingService?.dispose();
    super.dispose();
  }

  void _openKycWizard(DriverProfile? profile) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => KycWizardScreen(initialProfile: profile),
      ),
    );
  }

  Future<void> _handleToggleShift(bool targetState, DriverProfile profile) async {
    if (targetState) {
      // Need face verification modal
      final passed = await Navigator.of(context).push<bool>(
        MaterialPageRoute(
          builder: (_) => FaceVerificationModal(driverName: profile.fullName),
          fullscreenDialog: true,
        ),
      );

      if (passed == true && mounted) {
        context.read<DriverCubit>().updateShiftLocally(true);
        _locationPingService?.start();
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Xác thực sinh trắc học thành công! Bạn đã TRỰC TUYẾN và đang phát GPS.'),
            backgroundColor: GreenColors.primaryEmerald,
          ),
        );
      }
    } else {
      // Turn shift off
      try {
        await context.read<DriverRepository>().endShift();
      } catch (e) {
        debugPrint('Lỗi tắt ca trên backend: $e');
      }
      if (mounted) {
        context.read<DriverCubit>().updateShiftLocally(false);
        _locationPingService?.stop();
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Đã tắt ca làm việc (Ngoại tuyến). Dừng phát tín hiệu GPS và đã xóa vị trí khỏi hệ thống.'),
            backgroundColor: Colors.white24,
          ),
        );
      }
    }
  }

  Future<void> _onIncomingDispatchChanged() async {
    final dispatch = _locationPingService?.incomingDispatchNotifier.value;
    if (dispatch == null || !mounted) return;

    _locationPingService?.setDispatchModalShowing(true);
    final driverRepo = context.read<DriverRepository>();

    final acceptedTrip = await RideDispatchModal.show(
      context,
      dispatch: dispatch,
      driverRepository: driverRepo,
    );

    _locationPingService?.clearIncomingDispatch();
    _locationPingService?.setDispatchModalShowing(false);

    if (acceptedTrip != null && mounted) {
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => DriverActiveTripScreen(
            initialTrip: acceptedTrip,
            driverRepository: driverRepo,
          ),
        ),
      );
    }
  }


  @override
  Widget build(BuildContext context) {
    return BlocConsumer<DriverCubit, DriverState>(
      listener: (context, state) {
        if (state is DriverError) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(state.message),
              backgroundColor: GreenColors.errorRed,
            ),
          );
        }
      },
      builder: (context, state) {
        DriverProfile? profile;
        if (state is DriverLoaded) {
          profile = state.profile;
        }

        final fullName = profile?.fullName ?? 'Tài xế';
        final kycStatus = profile?.kycStatus ?? KycStatus.notSubmitted;
        final isOnline = profile?.isActiveShift ?? false;

        return Scaffold(
          appBar: AppBar(
            title: Row(
              children: [
                CircleAvatar(
                  radius: 18,
                  backgroundColor: GreenColors.primaryEmerald.withValues(alpha: 0.2),
                  child: Text(
                    fullName.isNotEmpty ? fullName[0].toUpperCase() : 'D',
                    style: const TextStyle(
                      color: GreenColors.primaryEmerald,
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        fullName,
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                      Text(
                        profile?.phoneNumber ?? '',
                        style: const TextStyle(fontSize: 11, color: GreenColors.textSecondary),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            actions: [
              Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: StatusBadge(status: kycStatus),
              ),
              IconButton(
                icon: const Icon(Icons.logout_rounded, color: Colors.white60, size: 20),
                tooltip: 'Đăng xuất',
                onPressed: () {
                  context.read<AuthCubit>().logout();
                },
              ),
            ],
          ),
          body: RefreshIndicator(
            onRefresh: () => context.read<DriverCubit>().loadProfile(),
            color: GreenColors.primaryEmerald,
            backgroundColor: GreenColors.surfaceDark,
            child: SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // KYC Status Banner
                  KycBanner(
                    kycStatus: kycStatus,
                    rejectionReason: profile?.kycRejectionReason,
                    onOpenKycWizard: () => _openKycWizard(profile),
                  ),
                  const SizedBox(height: 20),

                  // Shift Toggle Card
                  ShiftToggleCard(
                    isOnline: isOnline,
                    kycStatus: kycStatus,
                    onToggleShift: (val) {
                      if (profile != null) {
                        _handleToggleShift(val, profile);
                      }
                    },
                  ),
                  const SizedBox(height: 16),

                  // GPS & Battery Live Ping Status (When Online)
                  if (isOnline && _locationPingService != null) ...[
                    ValueListenableBuilder<DriverLocationPingModel?>(
                      valueListenable: _locationPingService!.lastPingNotifier,
                      builder: (context, ping, _) {
                        return Container(
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: GreenColors.surfaceDark,
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(color: GreenColors.primaryEmerald.withValues(alpha: 0.3)),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Row(
                                children: [
                                  Icon(Icons.radar, color: GreenColors.primaryEmerald, size: 18),
                                  SizedBox(width: 8),
                                  Text(
                                    'TRẠNG THÁI GPS & PIN TRỰC TUYẾN',
                                    style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 0.5, color: GreenColors.primaryEmerald),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 10),
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      const Text('Tọa độ hiện tại:', style: TextStyle(fontSize: 10, color: Colors.white54)),
                                      const SizedBox(height: 2),
                                      Text(
                                        ping != null ? '${ping.lat}, ${ping.lng}' : '10.776530, 106.700981',
                                        style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: Colors.white),
                                      ),
                                    ],
                                  ),
                                  Row(
                                    children: [
                                      const Icon(Icons.battery_charging_full, color: GreenColors.electricCyan, size: 18),
                                      const SizedBox(width: 4),
                                      Text(
                                        '${ping?.batteryPercent ?? 94}% Pin',
                                        style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: GreenColors.electricCyan),
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                              const SizedBox(height: 12),
                              Container(
                                width: double.infinity,
                                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                                decoration: BoxDecoration(
                                  color: GreenColors.primaryEmerald.withValues(alpha: 0.1),
                                  borderRadius: BorderRadius.circular(12),
                                  border: Border.all(color: GreenColors.primaryEmerald.withValues(alpha: 0.3)),
                                ),
                                child: const Row(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    SizedBox(
                                      width: 12,
                                      height: 12,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                        valueColor: AlwaysStoppedAnimation<Color>(GreenColors.primaryEmerald),
                                      ),
                                    ),
                                    SizedBox(width: 10),
                                    Text(
                                      'Đang sẵn sàng đón khách & chờ điều phối...',
                                      style: TextStyle(
                                        fontSize: 12,
                                        fontWeight: FontWeight.w600,
                                        color: GreenColors.primaryEmerald,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        );
                      },
                    ),
                    const SizedBox(height: 16),
                  ],

                  // Driver Stats Card (CO2 & Rating & Trips)
                  DriverStatsCard(
                    totalCo2SavedKg: profile?.totalCo2SavedKg ?? 0.0,
                    totalTrips: profile?.totalTripsCompleted ?? 0,
                    ratingAvg: profile?.ratingAvg ?? 5.0,
                  ),
                  const SizedBox(height: 20),

                  // Vehicle Specs Card
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Phương tiện xe điện',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                      if (kycStatus == KycStatus.approved || kycStatus == KycStatus.rejected)
                        TextButton(
                          onPressed: () => _openKycWizard(profile),
                          child: const Text('Cập nhật', style: TextStyle(color: GreenColors.electricCyan)),
                        ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  VehicleInfoCard(
                    vehicle: profile?.vehicle,
                    onEditVehicle: () => _openKycWizard(profile),
                  ),
                  const SizedBox(height: 24),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
