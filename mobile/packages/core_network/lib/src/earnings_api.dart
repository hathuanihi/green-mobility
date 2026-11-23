import 'package:core_model/core_model.dart';
import 'package:dio/dio.dart';
import 'api_client.dart';

class EarningsApi {
  final ApiClient apiClient;

  EarningsApi({required this.apiClient});

  Future<DriverEarningsSummaryModel> getSummary({String period = 'TODAY'}) async {
    final response = await apiClient.dio.get(
      '/driver/earnings/summary',
      queryParameters: {'range': period},
    );
    final data = response.data['data'] as Map<String, dynamic>;
    return DriverEarningsSummaryModel.fromJson(data);
  }

  Future<List<DriverTripEarningModel>> getTrips({
    String? cursor,
    int limit = 20,
  }) async {
    final query = <String, dynamic>{'limit': limit};
    if (cursor != null && cursor.isNotEmpty) query['cursor'] = cursor;

    final response = await apiClient.dio.get(
      '/driver/earnings/trips',
      queryParameters: query,
    );
    final data = response.data['data'];
    final List<dynamic> list = data is List
        ? data
        : (data is Map<String, dynamic> && data['items'] is List
            ? data['items'] as List<dynamic>
            : (data is Map<String, dynamic> && data['content'] is List
                ? data['content'] as List<dynamic>
                : []));
    return list
        .map((e) => DriverTripEarningModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<WithdrawalItemModel> requestWithdrawal(
    WithdrawalRequestModel request, {
    String? idempotencyKey,
  }) async {
    final key = idempotencyKey ?? ApiClient.generateIdempotencyKey();
    final response = await apiClient.dio.post(
      '/driver/withdrawals',
      data: request.toJson(),
      options: Options(headers: {'Idempotency-Key': key}),
    );
    final data = response.data['data'] as Map<String, dynamic>;
    return WithdrawalItemModel.fromJson(data);
  }

  Future<List<WithdrawalItemModel>> getWithdrawals() async {
    final response = await apiClient.dio.get('/driver/withdrawals');
    final data = response.data['data'];
    final List<dynamic> list = data is List
        ? data
        : (data is Map<String, dynamic> && data['content'] is List
            ? data['content'] as List<dynamic>
            : []);
    return list
        .map((e) => WithdrawalItemModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> cancelWithdrawal(String withdrawalId) async {
    await apiClient.dio.delete('/driver/withdrawals/$withdrawalId');
  }
}
