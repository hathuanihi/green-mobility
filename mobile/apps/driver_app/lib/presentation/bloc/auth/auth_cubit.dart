import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../data/repositories/auth_repository.dart';
import 'auth_state.dart';

class AuthCubit extends Cubit<AuthState> {
  final AuthRepository authRepository;

  AuthCubit({required this.authRepository}) : super(const AuthInitial());

  Future<void> checkAuthStatus() async {
    try {
      final token = await authRepository.checkToken();
      if (token != null && token.isNotEmpty) {
        // Session exists
        emit(const Unauthenticated());
      } else {
        emit(const Unauthenticated());
      }
    } catch (_) {
      emit(const Unauthenticated());
    }
  }

  Future<void> login(String phoneNumber, String password) async {
    emit(const AuthLoading());
    try {
      final user = await authRepository.login(
        phoneNumber: phoneNumber,
        password: password,
      );
      emit(Authenticated(user));
    } catch (e) {
      emit(AuthError(e.toString().replaceAll('Exception: ', '')));
    }
  }

  Future<void> register(String phoneNumber, String password, String fullName) async {
    emit(const AuthLoading());
    try {
      final user = await authRepository.register(
        phoneNumber: phoneNumber,
        password: password,
        fullName: fullName,
      );
      emit(Authenticated(user));
    } catch (e) {
      emit(AuthError(e.toString().replaceAll('Exception: ', '')));
    }
  }

  Future<void> logout() async {
    emit(const AuthLoading());
    await authRepository.logout();
    emit(const Unauthenticated());
  }
}
