import 'package:core_model/core_model.dart';
import 'api_client.dart';

class AuthApi {
  final ApiClient apiClient;

  AuthApi({required this.apiClient});

  Future<AuthResponseModel> login({
    required String phoneNumber,
    required String password,
  }) async {
    final response = await apiClient.dio.post(
      '/auth/login',
      data: {
        'phoneNumber': phoneNumber.trim(),
        'password': password,
      },
    );

    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      final authData = AuthResponseModel.fromJson(responseData['data']);
      await apiClient.tokenStorage.saveSession(
        token: authData.token,
        userId: authData.userId,
        role: authData.role,
      );
      return authData;
    }
    throw Exception('Dữ liệu phản hồi đăng nhập không hợp lệ');
  }

  Future<AuthResponseModel> register({
    required String phoneNumber,
    required String password,
    required String fullName,
    String role = UserRole.driver,
  }) async {
    final response = await apiClient.dio.post(
      '/auth/register',
      data: {
        'phoneNumber': phoneNumber.trim(),
        'password': password,
        'fullName': fullName.trim(),
        'role': role,
      },
    );

    final responseData = response.data;
    if (responseData is Map<String, dynamic> && responseData['data'] != null) {
      final authData = AuthResponseModel.fromJson(responseData['data']);
      await apiClient.tokenStorage.saveSession(
        token: authData.token,
        userId: authData.userId,
        role: authData.role,
      );
      return authData;
    }
    throw Exception('Dữ liệu phản hồi đăng ký không hợp lệ');
  }

  Future<void> logout() async {
    await apiClient.tokenStorage.clearSession();
  }
}
