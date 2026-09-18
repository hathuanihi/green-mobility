import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import '../../../data/repositories/auth_repository.dart';
import '../../../data/repositories/trip_repository.dart';
import '../../../main.dart';

class CustomerRegisterScreen extends StatefulWidget {
  final AuthRepository authRepository;
  final TripRepository tripRepository;

  const CustomerRegisterScreen({
    super.key,
    required this.authRepository,
    required this.tripRepository,
  });

  @override
  State<CustomerRegisterScreen> createState() => _CustomerRegisterScreenState();
}

class _CustomerRegisterScreenState extends State<CustomerRegisterScreen> {
  final _fullNameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();

  bool _obscurePassword = true;
  bool _isLoading = false;
  String? _errorMessage;

  @override
  void dispose() {
    _fullNameController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _handleRegister() async {
    final name = _fullNameController.text.trim();
    final phone = _phoneController.text.trim();
    final pass = _passwordController.text;
    final confirmPass = _confirmPasswordController.text;

    if (name.isEmpty || phone.isEmpty || pass.isEmpty) {
      setState(() {
        _errorMessage = 'Vui lòng điền đầy đủ họ tên, số điện thoại và mật khẩu.';
      });
      return;
    }

    if (pass.length < 6) {
      setState(() {
        _errorMessage = 'Mật khẩu phải có độ dài tối thiểu 6 ký tự.';
      });
      return;
    }

    if (pass != confirmPass) {
      setState(() {
        _errorMessage = 'Mật khẩu xác nhận không trùng khớp.';
      });
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await widget.authRepository.register(
        phoneNumber: phone,
        password: pass,
        fullName: name,
      );

      if (mounted) {
        Navigator.of(context).pushAndRemoveUntil(
          MaterialPageRoute(
            builder: (_) => CustomerHomeScreen(
              tripRepository: widget.tripRepository,
              authRepository: widget.authRepository,
            ),
          ),
          (route) => false,
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      appBar: AppBar(
        title: const Text('Đăng ký Khách hàng', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        backgroundColor: GreenColors.backgroundDark,
        elevation: 0,
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Center(
                  child: Container(
                    width: 68,
                    height: 68,
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [GreenColors.primaryEmerald, GreenColors.electricCyan],
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                      ),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: const Icon(Icons.person_add_alt_1_rounded, size: 36, color: Colors.white),
                  ),
                ),
                const SizedBox(height: 16),
                const Center(
                  child: Text(
                    'Tạo tài khoản Green Mobility',
                    style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.white),
                  ),
                ),
                const SizedBox(height: 6),
                const Center(
                  child: Text(
                    'Tham gia mạng lưới di chuyển xanh và nhận thưởng carbon',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 13, color: GreenColors.textSecondary),
                  ),
                ),
                const SizedBox(height: 28),

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
                      const SizedBox(height: 14),
                      GreenTextField(
                        label: 'Số điện thoại',
                        hint: '09xxxxxxxx',
                        controller: _phoneController,
                        keyboardType: TextInputType.phone,
                        prefixIcon: Icons.phone_android_outlined,
                      ),
                      const SizedBox(height: 14),
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
                      const SizedBox(height: 14),
                      GreenTextField(
                        label: 'Xác nhận mật khẩu',
                        hint: '••••••••',
                        controller: _confirmPasswordController,
                        obscureText: _obscurePassword,
                        prefixIcon: Icons.lock_clock_outlined,
                      ),
                      if (_errorMessage != null) ...[
                        const SizedBox(height: 14),
                        Text(
                          _errorMessage!,
                          style: const TextStyle(color: GreenColors.errorRed, fontSize: 13),
                          textAlign: TextAlign.center,
                        ),
                      ],
                      const SizedBox(height: 22),
                      GreenButton(
                        label: 'Hoàn tất Đăng ký',
                        icon: Icons.check_circle_outline,
                        isLoading: _isLoading,
                        onPressed: _handleRegister,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),

                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Text('Đã có tài khoản? ', style: TextStyle(color: GreenColors.textSecondary, fontSize: 14)),
                    GestureDetector(
                      onTap: () => Navigator.of(context).pop(),
                      child: const Text(
                        'Đăng nhập ngay',
                        style: TextStyle(color: GreenColors.electricCyan, fontSize: 14, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
