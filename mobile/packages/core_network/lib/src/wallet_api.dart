import 'package:core_model/core_model.dart';
import 'package:dio/dio.dart';
import 'api_client.dart';

class WalletApi {
  final ApiClient apiClient;

  WalletApi({required this.apiClient});

  /// Lấy số dư ví (Balance, Available, Pending hold, Cash debt)
  Future<WalletBalanceModel> getBalance() async {
    final response = await apiClient.dio.get('/wallet/balance');
    final resData = response.data;
    if (resData is Map<String, dynamic> && resData['data'] != null) {
      return WalletBalanceModel.fromJson(resData['data'] as Map<String, dynamic>);
    }
    throw Exception('Không thể tải số dư Ví Xanh');
  }

  /// Lịch sử biến động số dư theo Cursor pagination
  Future<WalletTransactionsCursorResponse> getTransactions({
    String? cursor,
    int limit = 20,
  }) async {
    final queryParams = {
      if (cursor != null) 'cursor': cursor,
      'limit': limit,
    };
    final response = await apiClient.dio.get(
      '/wallet/transactions',
      queryParameters: queryParams,
    );
    final resData = response.data;
    if (resData is Map<String, dynamic> && resData['data'] != null) {
      return WalletTransactionsCursorResponse.fromJson(resData['data'] as Map<String, dynamic>);
    }
    throw Exception('Không thể tải lịch sử giao dịch');
  }

  /// Khởi tạo nạp tiền vào Ví Xanh
  Future<TopupInitiateResponse> initiateTopup(
    int amountVnd,
    String gateway, {
    String? returnUrl,
    String? idempotencyKey,
  }) async {
    final options = Options(
      headers: {
        if (idempotencyKey != null) 'Idempotency-Key': idempotencyKey,
      },
    );
    final response = await apiClient.dio.post(
      '/wallet/topup/initiate',
      data: {
        'amountVnd': amountVnd,
        'gateway': gateway,
        if (returnUrl != null) 'returnUrl': returnUrl,
      },
      options: options,
    );
    final resData = response.data;
    if (resData is Map<String, dynamic> && resData['data'] != null) {
      return TopupInitiateResponse.fromJson(resData['data'] as Map<String, dynamic>);
    }
    throw Exception(resData?['message'] ?? 'Không thể khởi tạo yêu cầu nạp tiền');
  }
}

