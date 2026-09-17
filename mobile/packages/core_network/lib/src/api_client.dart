import 'dart:io' show Platform;
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:dio/dio.dart';
import 'token_storage.dart';

class ApiClient {
  static String get defaultBaseUrl {
    if (kIsWeb) {
      return 'http://localhost:8080/api/v1';
    }
    try {
      if (Platform.isAndroid) {
        return 'http://10.0.2.2:8080/api/v1';
      }
    } catch (_) {
      // Platform check may fail on Web
    }
    return 'http://localhost:8080/api/v1';
  }

  final Dio dio;
  final TokenStorage tokenStorage;

  ApiClient({
    String? baseUrl,
    TokenStorage? tokenStorage,
  })  : tokenStorage = tokenStorage ?? TokenStorage(),
        dio = Dio(BaseOptions(
          baseUrl: baseUrl ?? defaultBaseUrl,
          connectTimeout: const Duration(seconds: 15),
          receiveTimeout: const Duration(seconds: 20),
          sendTimeout: const Duration(seconds: 30),
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          },
        )) {
    _initInterceptors();
  }

  void _initInterceptors() {
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await tokenStorage.getToken();
          if (token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          return handler.next(options);
        },
        onError: (DioException error, handler) {
          // Extract backend ApiResponse message if available
          String errorMessage = 'Đã có lỗi xảy ra. Vui lòng thử lại.';
          if (error.response?.data != null) {
            final data = error.response!.data;
            if (data is Map<String, dynamic>) {
              if (data['message'] != null && data['message'].toString().isNotEmpty) {
                errorMessage = data['message'].toString();
              } else if (data['error'] != null) {
                errorMessage = data['error'].toString();
              }
            }
          } else if (error.type == DioExceptionType.connectionTimeout ||
              error.type == DioExceptionType.connectionError) {
            errorMessage = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra mạng.';
          }

          final customError = DioException(
            requestOptions: error.requestOptions,
            response: error.response,
            type: error.type,
            error: errorMessage,
            message: errorMessage,
          );
          return handler.next(customError);
        },
      ),
    );
  }
}
