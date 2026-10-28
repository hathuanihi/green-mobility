import 'package:core_model/core_model.dart';
import 'package:dio/dio.dart';
import 'api_client.dart';

class PaymentApi {
  final ApiClient apiClient;

  PaymentApi({required this.apiClient});

  /// Khởi tạo thanh toán cuốc xe (Ví Xanh, MoMo, VNPay, Tiền mặt)
  Future<PaymentResponseModel> settlePayment(
    String tripId,
    PaymentMethod method, {
    String? returnUrl,
    String? idempotencyKey,
  }) async {
    final payload = SettlePaymentRequest(
      tripId: tripId,
      method: method,
      returnUrl: returnUrl,
    );

    final options = Options(
      headers: {
        if (idempotencyKey != null) 'Idempotency-Key': idempotencyKey,
      },
    );

    final response = await apiClient.dio.post(
      '/payments/settle',
      data: payload.toJson(),
      options: options,
    );

    final resData = response.data;
    if (resData is Map<String, dynamic> && resData['data'] != null) {
      return PaymentResponseModel.fromJson(resData['data'] as Map<String, dynamic>);
    }
    throw Exception(resData?['message'] ?? 'Không thể khởi tạo thanh toán');
  }

  /// Tra cứu trạng thái thanh toán của cuốc xe
  Future<PaymentResponseModel> getPaymentStatus(String paymentId) async {
    final response = await apiClient.dio.get('/payments/$paymentId');
    final resData = response.data;
    if (resData is Map<String, dynamic> && resData['data'] != null) {
      return PaymentResponseModel.fromJson(resData['data'] as Map<String, dynamic>);
    }
    throw Exception('Không tìm thấy thông tin giao dịch thanh toán');
  }

  /// Tra cứu giao dịch thanh toán theo tripId
  Future<PaymentResponseModel?> getPaymentByTrip(String tripId) async {
    try {
      final response = await apiClient.dio.get('/payments/by-trip/$tripId');
      final resData = response.data;
      if (resData is Map<String, dynamic> && resData['data'] != null) {
        return PaymentResponseModel.fromJson(resData['data'] as Map<String, dynamic>);
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  /// Driver xác nhận đã thu tiền mặt của khách (Cash Collected)
  Future<void> confirmCashCollected(String paymentId) async {
    final response = await apiClient.dio.post('/payments/$paymentId/cash-confirm');
    final resData = response.data;
    if (resData is Map<String, dynamic> && resData['success'] == true) {
      return;
    }
    throw Exception(resData?['message'] ?? 'Không thể xác nhận thu tiền mặt');
  }
}

