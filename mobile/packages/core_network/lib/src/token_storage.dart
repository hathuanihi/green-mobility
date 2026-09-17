import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TokenStorage {
  static const String _tokenKey = 'green_jwt_token';
  static const String _userIdKey = 'green_user_id';
  static const String _roleKey = 'green_user_role';

  final FlutterSecureStorage _storage;

  TokenStorage({FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage();

  Future<void> saveSession({
    required String token,
    required String userId,
    required String role,
  }) async {
    await _storage.write(key: _tokenKey, value: token);
    await _storage.write(key: _userIdKey, value: userId);
    await _storage.write(key: _roleKey, value: role);
  }

  Future<String?> getToken() async {
    return await _storage.read(key: _tokenKey);
  }

  Future<String?> getUserId() async {
    return await _storage.read(key: _userIdKey);
  }

  Future<String?> getRole() async {
    return await _storage.read(key: _roleKey);
  }

  Future<void> clearSession() async {
    await _storage.delete(key: _tokenKey);
    await _storage.delete(key: _userIdKey);
    await _storage.delete(key: _roleKey);
  }
}
