import 'package:flutter_test/flutter_test.dart';
import 'package:driver_app/presentation/screens/auth/login_screen.dart';
import 'package:driver_app/data/repositories/auth_repository.dart';
import 'package:driver_app/presentation/bloc/auth/auth_cubit.dart';
import 'package:core_network/core_network.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

void main() {
  testWidgets('LoginScreen displays Green Driver branding and login button', (WidgetTester tester) async {
    final tokenStorage = TokenStorage();
    final apiClient = ApiClient(tokenStorage: tokenStorage);
    final authApi = AuthApi(apiClient: apiClient);
    final authRepo = AuthRepository(authApi: authApi, tokenStorage: tokenStorage);

    await tester.pumpWidget(
      MaterialApp(
        home: BlocProvider(
          create: (_) => AuthCubit(authRepository: authRepo),
          child: const LoginScreen(),
        ),
      ),
    );

    expect(find.text('Green Driver'), findsOneWidget);
    expect(find.text('Đăng nhập'), findsOneWidget);
    expect(find.text('Số điện thoại'), findsOneWidget);
  });
}
