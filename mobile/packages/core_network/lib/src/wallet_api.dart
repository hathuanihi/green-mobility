import 'package:core_model/core_model.dart';
import 'package:dio/dio.dart';
import 'api_client.dart';

class WalletApi {
  final ApiClient apiClient;

  WalletApi({required this.apiClient});

  Future<WalletBalanceModel> getBalance() async {
    final response = await apiClient.dio.get('/wallet');
    final data = response.data['data'] as Map<String, dynamic>;
    return WalletBalanceModel.fromJson(data);
  }

  Future<WalletTransactionsCursorResponse> getTransactions({
    String? type,
    String? cursor,
    int limit = 20,
  }) async {
    final query = <String, dynamic>{'limit': limit};
    if (type != null && type.isNotEmpty) query['type'] = type;
    if (cursor != null && cursor.isNotEmpty) query['cursor'] = cursor;

    final response = await apiClient.dio.get(
      '/wallet/transactions',
      queryParameters: query,
    );
    final data = response.data['data'] as Map<String, dynamic>;
    return WalletTransactionsCursorResponse.fromJson(data);
  }

  Future<TopupInitiateResponse> initiateTopup(
    int amountVnd,
    String gateway, {
    String? returnUrl,
    String? idempotencyKey,
  }) async {
    final req = TopupInitiateRequest(
      amountVnd: amountVnd,
      gateway: gateway,
      returnUrl: returnUrl,
    );
    final key = idempotencyKey ?? ApiClient.generateIdempotencyKey();
    final response = await apiClient.dio.post(
      '/wallet/topup',
      data: req.toJson(),
      options: Options(headers: {'Idempotency-Key': key}),
    );
    final data = response.data['data'] as Map<String, dynamic>;
    return TopupInitiateResponse.fromJson(data);
  }
}
