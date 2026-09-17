import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../bloc/auth/auth_cubit.dart';
import '../../bloc/auth/auth_state.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _fullNameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  bool _obscurePassword = true;

  @override
  void dispose() {
    _fullNameController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  void _onRegister() {
    final fullName = _fullNameController.text.trim();
    final phone = _phoneController.text.trim();
    final pass = _passwordController.text;
    final confirmPass = _confirmPasswordController.text;

    if (fullName.isEmpty || phone.isEmpty || pass.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng điền đầy đủ các thông tin bắt buộc'),
          backgroundColor: GreenColors.errorRed,
        ),
      );
      return;
    }

    if (pass != confirmPass) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Mật khẩu xác nhận không khớp'),
          backgroundColor: GreenColors.errorRed,
        ),
      );
      return;
    }

    if (pass.length < 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Mật khẩu phải từ 6 ký tự trở lên'),
          backgroundColor: GreenColors.errorRed,
        ),
      );
      return;
    }

    context.read<AuthCubit>().register(phone, pass, fullName);
  }

  @override
  Widget build(BuildContext context) {
    return BlocListener<AuthCubit, AuthState>(
      listener: (context, state) {
        if (state is Authenticated) {
          Navigator.of(context).pop(); // Back to main wrapper which will route to home
        } else if (state is AuthError) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(state.message),
              backgroundColor: GreenColors.errorRed,
            ),
          );
        }
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Đăng ký Tài xế'),
        ),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Tham gia mạng lưới Giao thông Xanh',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(height: 6),
                const Text(
                  'Đăng ký tài khoản để bắt đầu quy trình nộp hồ sơ KYC và vận hành xe điện',
                  style: TextStyle(fontSize: 13, color: GreenColors.textSecondary),
                ),
                const SizedBox(height: 24),
                GreenCard(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    children: [
                      GreenTextField(
                        label: 'Họ và tên',
                        hint: 'Nguyễn Văn A',
                        controller: _fullNameController,
                        prefixIcon: Icons.person_outline_rounded,
                      ),
                      const SizedBox(height: 16),
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
                        hint: 'Tối thiểu 6 ký tự',
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
                      const SizedBox(height: 16),
                      GreenTextField(
                        label: 'Xác nhận mật khẩu',
                        hint: 'Nhập lại mật khẩu',
                        controller: _confirmPasswordController,
                        obscureText: _obscurePassword,
                        prefixIcon: Icons.lock_clock_outlined,
                      ),
                      const SizedBox(height: 24),
                      BlocBuilder<AuthCubit, AuthState>(
                        builder: (context, state) {
                          return GreenButton(
                            label: 'Hoàn tất Đăng ký',
                            icon: Icons.app_registration_rounded,
                            isLoading: state is AuthLoading,
                            onPressed: _onRegister,
                          );
                        },
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
