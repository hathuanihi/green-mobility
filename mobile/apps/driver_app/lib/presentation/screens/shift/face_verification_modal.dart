import 'dart:async';
import 'dart:io';
import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:image_picker/image_picker.dart';
import '../../bloc/driver/driver_cubit.dart';

class FaceVerificationModal extends StatefulWidget {
  final String driverName;

  const FaceVerificationModal({super.key, required this.driverName});

  @override
  State<FaceVerificationModal> createState() => _FaceVerificationModalState();
}

class _FaceVerificationModalState extends State<FaceVerificationModal>
    with SingleTickerProviderStateMixin {
  late AnimationController _laserController;
  late Animation<double> _laserAnimation;

  final ImagePicker _picker = ImagePicker();
  String? _capturedImagePath;
  bool _isScanning = false;
  FaceVerifyResult? _verificationResult;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    // 1.5 second laser scanning sweep
    _laserController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat(reverse: true);

    _laserAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _laserController, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _laserController.dispose();
    super.dispose();
  }

  Future<void> _captureAndVerify() async {
    try {
      // Pick image via camera (or simulated fallback)
      final photo = await _picker.pickImage(
        source: ImageSource.camera,
        preferredCameraDevice: CameraDevice.front,
        imageQuality: 90,
      );

      if (photo == null) return;

      setState(() {
        _capturedImagePath = photo.path;
        _isScanning = true;
        _errorMessage = null;
        _verificationResult = null;
      });

      // Run 1.5s scanning effect before completing
      await Future.delayed(const Duration(milliseconds: 1500));

      if (!mounted) return;

      final result = await context.read<DriverCubit>().verifyShiftFace(photo.path);

      if (!mounted) return;

      setState(() {
        _isScanning = false;
        _verificationResult = result;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isScanning = false;
        _errorMessage = e.toString().replaceAll('Exception: ', '');
      });
    }
  }

  void _onDone() {
    Navigator.of(context).pop(_verificationResult?.isPassed == true);
  }

  void _onRetry() {
    setState(() {
      _capturedImagePath = null;
      _verificationResult = null;
      _errorMessage = null;
      _isScanning = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final hasResult = _verificationResult != null;
    final isSuccess = _verificationResult?.isPassed == true;

    return Scaffold(
      backgroundColor: Colors.black.withValues(alpha: 0.95),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
          child: Column(
            children: [
              // Header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  IconButton(
                    icon: const Icon(Icons.close, color: Colors.white70),
                    onPressed: () => Navigator.of(context).pop(false),
                  ),
                  const Text(
                    'Xác thực Khuôn mặt Bật ca',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(width: 48), // Balance close button
                ],
              ),
              const SizedBox(height: 12),

              // Instruction text
              Text(
                _isScanning
                    ? 'Đang so khớp sinh trắc học với ảnh KYC...'
                    : (hasResult
                        ? (isSuccess ? 'Xác thực khuôn mặt thành công!' : 'Khuôn mặt không trùng khớp!')
                        : 'Căn chỉnh khuôn mặt vào giữa khung hình và giữ yên'),
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: hasResult
                      ? (isSuccess ? GreenColors.primaryEmerald : GreenColors.errorRed)
                      : GreenColors.textSecondary,
                ),
                textAlign: TextAlign.center,
              ),

              const SizedBox(height: 24),

              // Central Oval Viewfinder
              Expanded(
                child: Center(
                  child: SizedBox(
                    width: 260,
                    height: 350,
                    child: Stack(
                      alignment: Alignment.center,
                      children: [
                        // Viewfinder background (Camera image or placeholder)
                        ClipRRect(
                          borderRadius: const BorderRadius.all(Radius.elliptical(130, 175)),
                          child: Container(
                            width: 260,
                            height: 350,
                            color: GreenColors.surfaceDark,
                            child: _capturedImagePath != null
                                ? Image.file(
                                    File(_capturedImagePath!),
                                    fit: BoxFit.cover,
                                  )
                                : Container(
                                    decoration: BoxDecoration(
                                      gradient: RadialGradient(
                                        colors: [
                                          GreenColors.surfaceDark,
                                          Colors.black.withValues(alpha: 0.8),
                                        ],
                                      ),
                                    ),
                                    child: Center(
                                      child: Column(
                                        mainAxisSize: MainAxisSize.min,
                                        children: [
                                          Icon(
                                            Icons.face_retouching_natural_rounded,
                                            size: 72,
                                            color: GreenColors.electricCyan.withValues(alpha: 0.5),
                                          ),
                                          const SizedBox(height: 12),
                                          Text(
                                            widget.driverName,
                                            style: const TextStyle(
                                              fontSize: 14,
                                              fontWeight: FontWeight.w600,
                                              color: Colors.white70,
                                            ),
                                          ),
                                          const SizedBox(height: 4),
                                          const Text(
                                            'Nhấn nút bên dưới để chụp selfie',
                                            style: TextStyle(fontSize: 11, color: Colors.white38),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                          ),
                        ),

                        // Animated Laser Line (sweeping from top to bottom)
                        if (_isScanning)
                          ClipRRect(
                            borderRadius: const BorderRadius.all(Radius.elliptical(130, 175)),
                            child: AnimatedBuilder(
                              animation: _laserAnimation,
                              builder: (context, child) {
                                return Align(
                                  alignment: Alignment(0, (_laserAnimation.value * 2) - 1),
                                  child: Container(
                                    height: 3,
                                    width: 260,
                                    decoration: BoxDecoration(
                                      color: GreenColors.electricCyan,
                                      boxShadow: [
                                        BoxShadow(
                                          color: GreenColors.electricCyan,
                                          blurRadius: 16,
                                          spreadRadius: 4,
                                        ),
                                        BoxShadow(
                                          color: Colors.white,
                                          blurRadius: 6,
                                          spreadRadius: 1,
                                        ),
                                      ],
                                    ),
                                  ),
                                );
                              },
                            ),
                          ),

                        // Oval Glowing Cyan Border
                        IgnorePointer(
                          child: Container(
                            width: 260,
                            height: 350,
                            decoration: BoxDecoration(
                              borderRadius: const BorderRadius.all(Radius.elliptical(130, 175)),
                              border: Border.all(
                                color: hasResult
                                    ? (isSuccess ? GreenColors.primaryEmerald : GreenColors.errorRed)
                                    : GreenColors.electricCyan,
                                width: 3,
                              ),
                              boxShadow: [
                                BoxShadow(
                                  color: hasResult
                                      ? (isSuccess
                                          ? GreenColors.primaryEmerald.withValues(alpha: 0.5)
                                          : GreenColors.errorRed.withValues(alpha: 0.5))
                                      : GreenColors.electricCyan.withValues(alpha: 0.4),
                                  blurRadius: 24,
                                  spreadRadius: 2,
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),

              const SizedBox(height: 20),

              // Result Card or Action Buttons
              if (hasResult) ...[
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: isSuccess
                        ? GreenColors.primaryEmerald.withValues(alpha: 0.15)
                        : GreenColors.errorRed.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(
                      color: isSuccess ? GreenColors.primaryEmerald : GreenColors.errorRed,
                      width: 1.2,
                    ),
                  ),
                  child: Column(
                    children: [
                      Row(
                        children: [
                          Icon(
                            isSuccess ? Icons.check_circle_rounded : Icons.error_outline_rounded,
                            color: isSuccess ? GreenColors.primaryEmerald : GreenColors.errorRed,
                            size: 24,
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  isSuccess ? 'Xác thực sinh trắc học thành công!' : 'Xác thực không đạt',
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 14,
                                    color: isSuccess ? GreenColors.primaryEmerald : GreenColors.errorRed,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  isSuccess
                                      ? 'Độ tương đồng: ${(_verificationResult!.similarityScore * 100).toStringAsFixed(1)}% (Chuẩn ≥ 75.0%)'
                                      : 'Độ tương đồng: ${(_verificationResult!.similarityScore * 100).toStringAsFixed(1)}% (< 75.0%). Khuôn mặt không khớp ảnh KYC.',
                                  style: const TextStyle(fontSize: 12, color: Colors.white70),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                if (isSuccess)
                  GreenButton(
                    label: 'Bắt đầu Ca Trực Tuyến',
                    icon: Icons.check,
                    onPressed: _onDone,
                  )
                else
                  Row(
                    children: [
                      Expanded(
                        child: GreenButton(
                          label: 'Thoát',
                          variant: GreenButtonVariant.secondary,
                          onPressed: () => Navigator.of(context).pop(false),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: GreenButton(
                          label: 'Chụp lại',
                          icon: Icons.refresh_rounded,
                          variant: GreenButtonVariant.danger,
                          onPressed: _onRetry,
                        ),
                      ),
                    ],
                  ),
              ] else if (_errorMessage != null) ...[
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: GreenColors.errorRed.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: GreenColors.errorRed),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.warning_amber_rounded, color: GreenColors.errorRed),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          _errorMessage!,
                          style: const TextStyle(color: Colors.white, fontSize: 13),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 14),
                GreenButton(
                  label: 'Thử lại',
                  icon: Icons.refresh_rounded,
                  onPressed: _onRetry,
                ),
              ] else ...[
                // Trigger Capture Button
                GreenButton(
                  label: _isScanning ? 'Đang phân tích khuôn mặt...' : 'Chụp ảnh & Xác thực ca',
                  icon: Icons.camera_alt_rounded,
                  isLoading: _isScanning,
                  onPressed: _isScanning ? null : _captureAndVerify,
                ),
              ],
              const SizedBox(height: 12),
            ],
          ),
        ),
      ),
    );
  }
}
