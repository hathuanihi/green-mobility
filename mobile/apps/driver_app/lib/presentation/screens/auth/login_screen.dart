import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../bloc/auth/auth_cubit.dart';
import '../../bloc/auth/auth_state.dart';
import 'register_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _obscurePassword = true;

  @override
  void dispose() {
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _onLogin() {
    final phone = _phoneController.text.trim();
    final pass = _passwordController.text;

    if (phone.isEmpty || pass.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng nhập số điện thoại và mật khẩu'),
          backgroundColor: GreenColors.errorRed,
        ),
      );
      return;
    }

    context.read<AuthCubit>().login(phone, pass);
  }

  @override
  Widget build(BuildContext context) {
    return BlocListener<AuthCubit, AuthState>(
      listener: (context, state) {
        if (state is AuthError) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(state.message),
              backgroundColor: GreenColors.errorRed,
            ),
          );
        }
      },
      child: Scaffold(
        body: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SizedBox(height: 16),
                  // App Brand & Logo
                  Center(
                    child: Container(
                      width: 80,
                      height: 80,
                      decoration: BoxDecoration(
                        gradient: const LinearGradient(
                          colors: [GreenColors.primaryEmerald, GreenColors.electricCyan],
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                        ),
                        borderRadius: BorderRadius.circular(24),
                        boxShadow: [
                          BoxShadow(
                            color: GreenColors.primaryEmerald.withValues(alpha: 0.3),
                            blurRadius: 16,
                            offset: const Offset(0, 8),
                          ),
                        ],
                      ),
                      child: const Icon(
                        Icons.electric_scooter_rounded,
                        size: 42,
                        color: Colors.white,
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),
                  const Center(
                    child: Text(
                      'Green Driver',
                      style: TextStyle(
                        fontSize: 28,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                        letterSpacing: 0.5,
                      ),
                    ),
                  ),
                  const SizedBox(height: 6),
                  const Center(
                    child: Text(
                      'Ứng dụng đối tác tài xế xe điện thông minh',
                      style: TextStyle(
                        fontSize: 14,
                        color: GreenColors.textSecondary,
                      ),
                    ),
                  ),
                  const SizedBox(height: 36),

                  // Login Form Card
                  GreenCard(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      children: [
                        GreenTextField(
                          label: 'Số điện thoại',
                          hint: '09xxxxxxxx',
                          controller: _phoneController,
                          keyboardType: TextInputType.phone,
                          prefixIcon: Icons.phone_android_outlined,
                        ),
                        const SizedBox(height: 16),
                        GreenTextField(
                          label: 'Mật khẩu',
                          hint: '••••••••',
                          controller: _passwordController,
                          obscureText: _obscurePassword,
                          prefixIcon: Icons.lock_outline_rounded,
                          suffixIcon: IconButton(
                            icon: Icon(
                              _obscurePassword ? Icons.visibility_off : Icons.visibility,
                              color: GreenColors.textSecondary,
                              size: 20,
                            ),
                            onPressed: () {
                              setState(() {
                                _obscurePassword = !_obscurePassword;
                              });
                            },
                          ),
                        ),
                        const SizedBox(height: 24),
                        BlocBuilder<AuthCubit, AuthState>(
                          builder: (context, state) {
                            return GreenButton(
                              label: 'Đăng nhập',
                              icon: Icons.login_rounded,
                              isLoading: state is AuthLoading,
                              onPressed: _onLogin,
                            );
                          },
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 32),

                  // Register link
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Text(
                        'Chưa có tài khoản tài xế? ',
                        style: TextStyle(color: GreenColors.textSecondary, fontSize: 14),
                      ),
                      GestureDetector(
                        onTap: () {
                          Navigator.of(context).push(
                            MaterialPageRoute(builder: (_) => const RegisterScreen()),
                          );
                        },
                        child: const Text(
                          'Đăng ký ngay',
                          style: TextStyle(
                            color: GreenColors.electricCyan,
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
