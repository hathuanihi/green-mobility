import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

class KycBanner extends StatelessWidget {
  final String kycStatus;
  final String? rejectionReason;
  final VoidCallback onOpenKycWizard;

  const KycBanner({
    super.key,
    required this.kycStatus,
    this.rejectionReason,
    required this.onOpenKycWizard,
  });

  @override
  Widget build(BuildContext context) {
    if (kycStatus == KycStatus.approved) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: GreenColors.primaryEmerald.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: GreenColors.primaryEmerald.withValues(alpha: 0.3)),
        ),
        child: const Row(
          children: [
            Icon(Icons.verified, color: GreenColors.primaryEmerald, size: 20),
            SizedBox(width: 10),
            Expanded(
              child: Text(
                'Tài xế đã xác thực KYC thành công',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: GreenColors.primaryEmerald,
                ),
              ),
            ),
          ],
        ),
      );
    }

    if (kycStatus == KycStatus.pending) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: GreenColors.warningAmber.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: GreenColors.warningAmber.withValues(alpha: 0.4)),
        ),
        child: const Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(Icons.hourglass_top_rounded, color: GreenColors.warningAmber, size: 22),
            SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Hồ sơ đang chờ xét duyệt',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                      color: GreenColors.warningAmber,
                    ),
                  ),
                  SizedBox(height: 4),
                  Text(
                    'Hồ sơ của bạn đang được quản trị viên Green Mobility đối chiếu giấy tờ và thông số xe điện.',
                    style: TextStyle(fontSize: 12, color: Colors.white70),
                  ),
                ],
              ),
            ),
          ],
        ),
      );
    }

    if (kycStatus == KycStatus.rejected) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: GreenColors.errorRed.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: GreenColors.errorRed.withValues(alpha: 0.4)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.cancel_outlined, color: GreenColors.errorRed, size: 22),
                const SizedBox(width: 10),
                const Text(
                  'Hồ sơ KYC bị từ chối',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: GreenColors.errorRed,
                  ),
                ),
                const Spacer(),
                TextButton(
                  onPressed: onOpenKycWizard,
                  child: const Text(
                    'Nộp lại',
                    style: TextStyle(
                      color: GreenColors.electricCyan,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              'Lý do từ chối: ${rejectionReason ?? "Thông tin giấy tờ chưa hợp lệ"}',
              style: const TextStyle(fontSize: 12, color: Colors.white70),
            ),
          ],
        ),
      );
    }

    // Default: NOT_SUBMITTED
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: GreenColors.surfaceDark,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: GreenColors.electricCyan.withValues(alpha: 0.4)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.assignment_late_outlined, color: GreenColors.electricCyan, size: 20),
              SizedBox(width: 8),
              Text(
                'Yêu cầu hoàn thiện hồ sơ KYC',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          const Text(
            'Nộp ảnh CCCD, GPLX, Cà vẹt xe điện và ảnh chân dung để được cấp quyền bật ca nhận chuyến.',
            style: TextStyle(fontSize: 12, color: Colors.white70),
          ),
          const SizedBox(height: 12),
          GreenButton(
            label: 'Nộp hồ sơ tài xế ngay',
            height: 44,
            icon: Icons.upload_file_rounded,
            onPressed: onOpenKycWizard,
          ),
        ],
      ),
    );
  }
}
