import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../bloc/auth/auth_cubit.dart';
import '../../bloc/driver/driver_cubit.dart';
import '../../bloc/driver/driver_state.dart';
import '../kyc/kyc_wizard_screen.dart';
import '../shift/face_verification_modal.dart';
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
  @override
  void initState() {
    super.initState();
    context.read<DriverCubit>().loadProfile();
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
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Xác thực sinh trắc học thành công! Bạn đã TRỰC TUYẾN.'),
            backgroundColor: GreenColors.primaryEmerald,
          ),
        );
      }
    } else {
      // Turn shift off
      context.read<DriverCubit>().updateShiftLocally(false);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Đã tắt ca làm việc (Ngoại tuyến).'),
          backgroundColor: Colors.white24,
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
                  const SizedBox(height: 20),

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
