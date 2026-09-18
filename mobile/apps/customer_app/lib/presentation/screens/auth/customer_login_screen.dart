import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import '../../../data/repositories/auth_repository.dart';
import '../../../data/repositories/trip_repository.dart';
import '../../../main.dart';
import 'customer_register_screen.dart';

class CustomerLoginScreen extends StatefulWidget {
  final AuthRepository authRepository;
  final TripRepository tripRepository;

  const CustomerLoginScreen({
    super.key,
    required this.authRepository,
    required this.tripRepository,
  });

  @override
  State<CustomerLoginScreen> createState() => _CustomerLoginScreenState();
}

class _CustomerLoginScreenState extends State<CustomerLoginScreen> {
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();

  bool _obscurePassword = true;
  bool _isLoading = false;
  String? _errorMessage;

  @override
  void dispose() {
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    final phone = _phoneController.text.trim();
    final pass = _passwordController.text;

    if (phone.isEmpty || pass.isEmpty) {
      setState(() {
        _errorMessage = 'Vui lòng nhập số điện thoại và mật khẩu.';
      });
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await widget.authRepository.login(
        phoneNumber: phone,
        password: pass,
      );

      if (mounted) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(
            builder: (_) => CustomerHomeScreen(
              tripRepository: widget.tripRepository,
              authRepository: widget.authRepository,
            ),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString().replaceAll('Exception: ', '');
        });
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _openRegister() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => CustomerRegisterScreen(
          authRepository: widget.authRepository,
          tripRepository: widget.tripRepository,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 24.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Logo & Header
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
                          color: GreenColors.primaryEmerald.withValues(alpha: 0.35),
                          blurRadius: 20,
                          offset: const Offset(0, 8),
                        ),
                      ],
                    ),
                    child: const Icon(
                      Icons.eco_rounded,
                      size: 46,
                      color: Colors.white,
                    ),
                  ),
                ),
                const SizedBox(height: 20),
                const Center(
                  child: Text(
                    'Green Mobility',
                    style: TextStyle(
                      fontSize: 28,
                      fontWeight: FontWeight.w900,
                      color: Colors.white,
                      letterSpacing: 0.5,
                    ),
                  ),
                ),
                const SizedBox(height: 6),
                const Center(
                  child: Text(
                    'Nền tảng gọi xe điện giảm phát thải carbon',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 14,
                      color: GreenColors.textSecondary,
                    ),
                  ),
                ),
                const SizedBox(height: 36),

                // Login Form Card
                GreenCard(
                  padding: const EdgeInsets.all(22),
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
                      if (_errorMessage != null) ...[
                        const SizedBox(height: 14),
                        Text(
                          _errorMessage!,
                          style: const TextStyle(color: GreenColors.errorRed, fontSize: 13),
                          textAlign: TextAlign.center,
                        ),
                      ],
                      const SizedBox(height: 24),
                      GreenButton(
                        label: 'Đăng nhập',
                        icon: Icons.login_rounded,
                        isLoading: _isLoading,
                        onPressed: _handleLogin,
                      ),
                    ],
                  ),
                ),

                const SizedBox(height: 24),

                // Register Link
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Text(
                      'Chưa có tài khoản khách hàng? ',
                      style: TextStyle(color: GreenColors.textSecondary, fontSize: 14),
                    ),
                    GestureDetector(
                      onTap: _openRegister,
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
    );
  }
}
