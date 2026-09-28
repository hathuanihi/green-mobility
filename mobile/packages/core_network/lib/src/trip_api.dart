import 'package:core_model/core_model.dart';
import 'api_client.dart';

class TripApi {
  final ApiClient apiClient;

  TripApi({required this.apiClient});

  /// Dự toán cước phí và lượng phát thải CO2 giảm thiểu
  Future<TripEstimateModel> estimateTrip(TripEstimateRequestModel request) async {
    final response = await apiClient.dio.post(
      '/trips/estimate',
      data: request.toJson(),
    );

    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return TripEstimateModel.fromJson(responseData['data'] as Map<String, dynamic>);
    }
    throw Exception('Không thể dự toán cước phí và lộ trình');
  }

  /// Khách hàng tạo yêu cầu đặt xe điện mới (Kích hoạt Matching Engine)
  Future<TripModel> requestTrip(TripRequestModel request) async {
    final response = await apiClient.dio.post(
      '/trips/request',
      data: request.toJson(),
    );

    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return TripModel.fromJson(responseData['data'] as Map<String, dynamic>);
    }
    throw Exception('Không thể tạo yêu cầu đặt xe');
  }

  /// Tra cứu thông tin chi tiết và tiến trình chuyến đi
  Future<TripModel> getTripDetails(String tripId) async {
    final response = await apiClient.dio.get('/trips/$tripId');

    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return TripModel.fromJson(responseData['data'] as Map<String, dynamic>);
    }
    throw Exception('Không tìm thấy thông tin chuyến xe');
  }

  /// Khách hàng hoặc tài xế hủy chuyến xe
  Future<TripModel> cancelTrip(String tripId, {String? reason}) async {
    final response = await apiClient.dio.post(
      '/trips/$tripId/cancel',
      data: {
        'cancelReason': reason ?? 'Người dùng yêu cầu hủy',
      },
    );

    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      return TripModel.fromJson(responseData['data'] as Map<String, dynamic>);
    }
    throw Exception('Hủy chuyến xe thất bại');
  }
}
