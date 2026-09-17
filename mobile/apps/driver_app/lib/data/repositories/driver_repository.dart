import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';

class DriverRepository {
  final DriverApi driverApi;

  DriverRepository({required this.driverApi});

  Future<DriverProfile> getProfile() async {
    return await driverApi.getProfile();
  }

  Future<DriverProfile> submitKyc(KycSubmissionModel submission) async {
    return await driverApi.submitKyc(submission);
  }

  Future<FaceVerifyResult> verifyShiftFace(String selfieFilePath) async {
    return await driverApi.verifyShiftFace(selfieFilePath);
  }
}
