import 'package:core_model/core_model.dart';
import 'api_client.dart';

class CarbonApi {
  final ApiClient apiClient;

  CarbonApi({required this.apiClient});

  Future<UserCarbonSummaryModel> getUserSummary() async {
    final response = await apiClient.dio.get('/carbon/user/summary');
    final data = response.data['data'] as Map<String, dynamic>;
    return UserCarbonSummaryModel.fromJson(data);
  }

  Future<TripImpactReceiptModel> getReceiptByTripId(String tripId) async {
    final response = await apiClient.dio.get('/carbon/receipts/trip/$tripId');
    final data = response.data['data'] as Map<String, dynamic>;
    return TripImpactReceiptModel.fromJson(data);
  }

  Future<TripImpactReceiptModel> getReceiptBySlug(String slug) async {
    final response = await apiClient.dio.get('/carbon/receipts/share/$slug');
    final data = response.data['data'] as Map<String, dynamic>;
    return TripImpactReceiptModel.fromJson(data);
  }
}
