import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';

class AuthRepository {
  final AuthApi authApi;
  final TokenStorage tokenStorage;

  AuthRepository({
    required this.authApi,
    required this.tokenStorage,
  });

  Future<AuthResponseModel> login({
    required String phoneNumber,
    required String password,
  }) async {
    return await authApi.login(
      phoneNumber: phoneNumber,
      password: password,
    );
  }

  Future<AuthResponseModel> register({
    required String phoneNumber,
    required String password,
    required String fullName,
  }) async {
    return await authApi.register(
      phoneNumber: phoneNumber,
      password: password,
      fullName: fullName,
      role: UserRole.driver,
    );
  }

  Future<String?> checkToken() async {
    return await tokenStorage.getToken();
  }

  Future<void> logout() async {
    await authApi.logout();
  }
}
