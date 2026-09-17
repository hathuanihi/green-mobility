import 'package:core_model/core_model.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../data/repositories/driver_repository.dart';
import 'driver_state.dart';

class DriverCubit extends Cubit<DriverState> {
  final DriverRepository driverRepository;

  DriverCubit({required this.driverRepository}) : super(const DriverInitial());

  Future<void> loadProfile() async {
    emit(const DriverLoading());
    try {
      final profile = await driverRepository.getProfile();
      emit(DriverLoaded(profile));
    } catch (e) {
      emit(DriverError(e.toString().replaceAll('Exception: ', '')));
    }
  }

  Future<bool> submitKyc(KycSubmissionModel submission) async {
    emit(const DriverKycSubmitting());
    try {
      final profile = await driverRepository.submitKyc(submission);
      emit(DriverKycSuccess(
        profile: profile,
        message: 'Hồ sơ KYC đã được gửi thành công! Quản trị viên sẽ phê duyệt trong 24h.',
      ));
      emit(DriverLoaded(profile));
      return true;
    } catch (e) {
      emit(DriverError(e.toString().replaceAll('Exception: ', '')));
      return false;
    }
  }

  Future<FaceVerifyResult> verifyShiftFace(String selfiePath) async {
    try {
      final result = await driverRepository.verifyShiftFace(selfiePath);
      if (result.isPassed) {
        // Reload driver profile to update active shift state
        final profile = await driverRepository.getProfile();
        emit(DriverLoaded(profile));
      }
      return result;
    } catch (e) {
      return FaceVerifyResult(
        isPassed: false,
        similarityScore: 0.0,
        isActiveShift: false,
        message: e.toString().replaceAll('Exception: ', ''),
      );
    }
  }

  void updateShiftLocally(bool isActive) {
    final currentState = state;
    if (currentState is DriverLoaded) {
      final p = currentState.profile;
      emit(DriverLoaded(DriverProfile(
        driverId: p.driverId,
        userId: p.userId,
        fullName: p.fullName,
        phoneNumber: p.phoneNumber,
        citizenId: p.citizenId,
        driverLicenseNumber: p.driverLicenseNumber,
        licenseClass: p.licenseClass,
        kycStatus: p.kycStatus,
        kycRejectionReason: p.kycRejectionReason,
        citizenCardFrontUrl: p.citizenCardFrontUrl,
        citizenCardBackUrl: p.citizenCardBackUrl,
        driverLicenseImageUrl: p.driverLicenseImageUrl,
        facePortraitUrl: p.facePortraitUrl,
        isActiveShift: isActive,
        ratingAvg: p.ratingAvg,
        totalTripsCompleted: p.totalTripsCompleted,
        totalCo2SavedKg: p.totalCo2SavedKg,
        vehicle: p.vehicle,
      )));
    }
  }
}
