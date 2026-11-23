import 'package:core_model/core_model.dart';
import 'package:dio/dio.dart';
import 'api_client.dart';

class PaymentApi {
  final ApiClient apiClient;

  PaymentApi({required this.apiClient});

  Future<List<Map<String, dynamic>>> getAvailableMethods() async {
    final response = await apiClient.dio.get('/payments/methods');
    final data = response.data['data'] as List<dynamic>? ?? [];
    return data.map((e) => e as Map<String, dynamic>).toList();
  }

  Future<PaymentResponseModel> getPaymentByTrip(String tripId) async {
    final response = await apiClient.dio.get('/payments/trips/$tripId');
    final data = response.data['data'] as Map<String, dynamic>;
    return PaymentResponseModel.fromJson(data);
  }

  Future<PaymentResponseModel> settlePayment(
    String tripId,
    PaymentMethod method, {
    String? returnUrl,
    String? idempotencyKey,
  }) async {
    final key = idempotencyKey ?? ApiClient.generateIdempotencyKey();
    final response = await apiClient.dio.post(
      '/payments/trips/$tripId/checkout',
      data: {
        'method': method.value,
        if (returnUrl != null) 'returnUrl': returnUrl,
      },
      options: Options(headers: {'Idempotency-Key': key}),
    );
    final data = response.data['data'] as Map<String, dynamic>;
    return PaymentResponseModel.fromJson(data);
  }

  Future<PaymentResponseModel> getPaymentStatus(String paymentId) async {
    final response = await apiClient.dio.get('/payments/$paymentId');
    final data = response.data['data'] as Map<String, dynamic>;
    return PaymentResponseModel.fromJson(data);
  }

  Future<void> confirmCashCollected(String tripId, {String? idempotencyKey}) async {
    final key = idempotencyKey ?? ApiClient.generateIdempotencyKey();
    await apiClient.dio.post(
      '/driver/trips/$tripId/confirm-cash',
      options: Options(headers: {'Idempotency-Key': key}),
    );
  }
}
