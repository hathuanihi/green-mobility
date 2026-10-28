import 'package:core_model/core_model.dart';
import 'package:dio/dio.dart';
import 'api_client.dart';

class EarningsApi {
  final ApiClient apiClient;

  EarningsApi({required this.apiClient});

  /// Lấy tổng hợp thu nhập tài xế theo chu kỳ (TODAY, WEEK, MONTH)
  Future<DriverEarningsSummaryModel> getSummary({String period = 'TODAY'}) async {
    final response = await apiClient.dio.get(
      '/driver/earnings/summary',
      queryParameters: {'period': period},
    );
    final resData = response.data;
    if (resData is Map<String, dynamic> && resData['data'] != null) {
      return DriverEarningsSummaryModel.fromJson(resData['data'] as Map<String, dynamic>);
    }
    throw Exception('Không thể tải tổng kết thu nhập');
  }

  /// Danh sách chi tiết thu nhập từng cuốc xe
  Future<List<DriverTripEarningModel>> getTrips({
    String? date,
    int page = 0,
    int size = 20,
  }) async {
    final response = await apiClient.dio.get(
      '/driver/earnings/trips',
      queryParameters: {
        if (date != null) 'date': date,
        'page': page,
        'size': size,
      },
    );
    final resData = response.data;
    if (resData is Map<String, dynamic> && resData['data'] != null) {
      final list = resData['data']['content'] as List<dynamic>?;
      return list
              ?.map((e) => DriverTripEarningModel.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [];
    }
    throw Exception('Không thể tải danh sách cuốc xe');
  }

  /// Gửi yêu cầu rút tiền về tài khoản ngân hàng
  Future<WithdrawalItemModel> requestWithdrawal(
    WithdrawalRequestModel request, {
    String? idempotencyKey,
  }) async {
    final options = Options(
      headers: {
        if (idempotencyKey != null) 'Idempotency-Key': idempotencyKey,
      },
    );
    final response = await apiClient.dio.post(
      '/driver/wallet/withdraw',
      data: request.toJson(),
      options: options,
    );
    final resData = response.data;
    if (resData is Map<String, dynamic> && resData['data'] != null) {
      return WithdrawalItemModel.fromJson(resData['data'] as Map<String, dynamic>);
    }
    throw Exception(resData?['message'] ?? 'Không thể gửi yêu cầu rút tiền');
  }

  /// Lịch sử các yêu cầu rút tiền
  Future<List<WithdrawalItemModel>> getWithdrawals({int page = 0, int size = 20}) async {
    final response = await apiClient.dio.get(
      '/driver/wallet/withdrawals',
      queryParameters: {'page': page, 'size': size},
    );
    final resData = response.data;
    if (resData is Map<String, dynamic> && resData['data'] != null) {
      final list = resData['data']['content'] as List<dynamic>?;
      return list
              ?.map((e) => WithdrawalItemModel.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [];
    }
    throw Exception('Không thể tải lịch sử rút tiền');
  }
}

