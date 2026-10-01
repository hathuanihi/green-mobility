import 'dart:io';
import 'package:core_model/core_model.dart';
import 'package:dio/dio.dart';
import 'api_client.dart';

class DriverApi {
  final ApiClient apiClient;

  DriverApi({required this.apiClient});

  /// Lấy thông tin hồ sơ tài xế và trạng thái KYC hiện tại
  Future<DriverProfile> getProfile() async {
    final response = await apiClient.dio.get('/driver/profile');
    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return DriverProfile.fromJson(responseData['data']);
    }
    throw Exception('Không thể tải thông tin hồ sơ tài xế');
  }

  /// Nộp hồ sơ KYC kèm thông tin phương tiện xe điện và 5 ảnh minh chứng
  Future<DriverProfile> submitKyc(KycSubmissionModel submission) async {
    final Map<String, dynamic> map = {
      'citizenId': submission.citizenId.trim(),
      'licenseNumber': submission.licenseNumber.trim(),
      'licenseClass': submission.licenseClass,
      'vehicleType': submission.vehicleType,
      'make': submission.make.trim(),
      'model': submission.model.trim(),
      'licensePlate': submission.licensePlate.trim().toUpperCase(),
      'color': submission.color.trim(),
      'batteryCapacityKwh': submission.batteryCapacityKwh.toString(),
      'rangePerChargeKm': submission.rangePerChargeKm.toString(),
      'inspectionExpiryDate': submission.inspectionExpiryDate,
    };

    // Attach 5 files as MultipartFile
    if (submission.citizenFrontPath.isNotEmpty && File(submission.citizenFrontPath).existsSync()) {
      map['citizenFrontImage'] = await MultipartFile.fromFile(
        submission.citizenFrontPath,
        filename: 'citizen_front.jpg',
      );
    }
    if (submission.citizenBackPath.isNotEmpty && File(submission.citizenBackPath).existsSync()) {
      map['citizenBackImage'] = await MultipartFile.fromFile(
        submission.citizenBackPath,
        filename: 'citizen_back.jpg',
      );
    }
    if (submission.licenseImagePath.isNotEmpty && File(submission.licenseImagePath).existsSync()) {
      map['licenseImage'] = await MultipartFile.fromFile(
        submission.licenseImagePath,
        filename: 'license.jpg',
      );
    }
    if (submission.vehicleRegistrationPath.isNotEmpty && File(submission.vehicleRegistrationPath).existsSync()) {
      map['vehicleRegistrationImage'] = await MultipartFile.fromFile(
        submission.vehicleRegistrationPath,
        filename: 'registration.jpg',
      );
    }
    if (submission.facePortraitPath.isNotEmpty && File(submission.facePortraitPath).existsSync()) {
      map['facePortraitImage'] = await MultipartFile.fromFile(
        submission.facePortraitPath,
        filename: 'face_portrait.jpg',
      );
    }

    final formData = FormData.fromMap(map);
    final response = await apiClient.dio.post(
      '/driver/kyc/submit',
      data: formData,
      options: Options(
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      ),
    );

    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['success'] == true) {
      // Reload profile to return updated KYC status
      return await getProfile();
    }
    throw Exception('Gửi hồ sơ KYC thất bại');
  }

  /// Xác thực khuôn mặt bật ca làm việc qua Face Biometrics
  Future<FaceVerifyResult> verifyShiftFace(String selfieFilePath) async {
    final file = File(selfieFilePath);
    if (!file.existsSync()) {
      throw Exception('Tệp ảnh selfie không tồn tại');
    }

    final formData = FormData.fromMap({
      'selfieImage': await MultipartFile.fromFile(
        selfieFilePath,
        filename: 'shift_selfie.jpg',
      ),
    });

    final response = await apiClient.dio.post(
      '/driver/shift/face-verify',
      data: formData,
      options: Options(
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      ),
    );

    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return FaceVerifyResult.fromJson(
        responseData['data'],
        defaultMessage: responseData['message']?.toString(),
      );
    }
    throw Exception('Xác thực khuôn mặt thất bại');
  }

  /// Đồng bộ vị trí GPS và % pin xe điện lên hệ thống Matching
  Future<void> pingLocation(DriverLocationPingModel ping) async {
    await apiClient.dio.post(
      '/driver/location/ping',
      data: ping.toJson(),
    );
  }

  /// Tài xế chấp nhận cuốc xe trong vòng 15 giây đếm ngược
  Future<DriverTripModel> acceptTrip(String tripId) async {
    final response = await apiClient.dio.post('/driver/trips/$tripId/accept');
    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return DriverTripModel.fromJson(responseData['data'] as Map<String, dynamic>);
    }
    throw Exception('Không thể nhận cuốc xe');
  }

  /// Tài xế từ chối cuốc xe
  Future<void> declineTrip(String tripId, {String? reason}) async {
    await apiClient.dio.post(
      '/driver/trips/$tripId/decline',
      data: {
        'cancelReason': reason ?? 'Tài xế bận',
      },
    );
  }

  /// Lấy cuốc xe đang thực hiện của tài xế
  Future<TripModel?> getCurrentTrip() async {
    final response = await apiClient.dio.get('/driver/trips/current');
    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return TripModel.fromJson(responseData['data'] as Map<String, dynamic>);
    }
    return null;
  }

  /// Kiểm tra xem có cuốc xe nào đang được Matching Engine điều phối tới tài xế không
  Future<DispatchNotificationModel?> getPendingDispatch() async {
    try {
      final response = await apiClient.dio.get('/driver/trips/dispatch/pending');
      final responseData = response.data;
      if (responseData is Map<String, dynamic> && responseData['data'] != null) {
        return DispatchNotificationModel.fromJson(responseData['data'] as Map<String, dynamic>);
      }
    } catch (_) {
      // Return null if no pending dispatch or network error
    }
    return null;
  }

  /// Tắt ca làm việc của tài xế và xóa khỏi Redis GEO
  Future<void> endShift() async {
    await apiClient.dio.post('/driver/shift/end');
  }

  /// Tài xế bắt đầu di chuyển đón khách
  Future<DriverArrivingModel> startArriving(String tripId) async {
    final response = await apiClient.dio.post('/driver/trips/$tripId/start-arriving');
    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return DriverArrivingModel.fromJson(responseData['data'] as Map<String, dynamic>);
    }
    throw Exception('Không thể bắt đầu di chuyển đón khách');
  }

  /// Tài xế xác nhận đã đến điểm đón (Arrived at Pickup)
  Future<DriverArriveModel> arriveAtPickup(String tripId) async {
    final response = await apiClient.dio.post('/driver/trips/$tripId/arrive');
    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return DriverArriveModel.fromJson(responseData['data'] as Map<String, dynamic>);
    }
    throw Exception('Không thể xác nhận đã đến điểm đón');
  }

  /// Tài xế bắt đầu chuyến đi khi khách đã lên xe
  Future<DriverStartTripModel> startTrip(String tripId) async {
    final response = await apiClient.dio.post('/driver/trips/$tripId/start-trip');
    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return DriverStartTripModel.fromJson(responseData['data'] as Map<String, dynamic>);
    }
    throw Exception('Không thể bắt đầu chuyến đi');
  }

  /// Tài xế hoàn thành chuyến đi tại điểm trả
  Future<TripCompleteSummaryModel> completeTrip(String tripId) async {
    final response = await apiClient.dio.post('/driver/trips/$tripId/complete');
    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return TripCompleteSummaryModel.fromJson(responseData['data'] as Map<String, dynamic>);
    }
    throw Exception('Không thể hoàn thành chuyến đi');
  }

  /// Tài xế hủy chuyến xe đang đón/chờ khách
  Future<DriverCancelModel> cancelTrip(String tripId, {String? reason}) async {
    final response = await apiClient.dio.post(
      '/driver/trips/$tripId/cancel',
      data: {
        'cancelReason': reason ?? 'Tài xế hủy cuốc',
      },
    );
    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return DriverCancelModel.fromJson(responseData['data'] as Map<String, dynamic>);
    }
    throw Exception('Không thể hủy chuyến đi');
  }

  /// Đồng bộ offline batch các điểm GPS lên hệ thống telemetry
  Future<Map<String, dynamic>> syncGpsBatch(String tripId, List<GpsPointModel> points) async {
    final response = await apiClient.dio.post(
      '/driver/trips/$tripId/sync-gps-batch',
      data: {
        'points': points.map((p) => p.toJson()).toList(),
      },
    );
    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return responseData['data'] as Map<String, dynamic>;
    }
    return {'syncedPoints': points.length};
  }
}

