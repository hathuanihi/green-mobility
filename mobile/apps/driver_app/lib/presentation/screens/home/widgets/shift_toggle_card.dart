import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

class ShiftToggleCard extends StatelessWidget {
  final bool isOnline;
  final String kycStatus;
  final ValueChanged<bool> onToggleShift;

  const ShiftToggleCard({
    super.key,
    required this.isOnline,
    required this.kycStatus,
    required this.onToggleShift,
  });

  @override
  Widget build(BuildContext context) {
    final isKycApproved = kycStatus == KycStatus.approved;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: GreenColors.surfaceDark,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: isOnline
              ? GreenColors.primaryEmerald.withValues(alpha: 0.5)
              : Colors.white10,
          width: isOnline ? 1.5 : 1,
        ),
        boxShadow: isOnline
            ? [
                BoxShadow(
                  color: GreenColors.primaryEmerald.withValues(alpha: 0.15),
                  blurRadius: 16,
                  offset: const Offset(0, 4),
                ),
              ]
            : null,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      width: 10,
                      height: 10,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: isOnline ? GreenColors.primaryEmerald : Colors.white38,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      isOnline ? 'ĐANG TRỰC TUYẾN' : 'NGOẠI TUYẾN',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 15,
                        color: isOnline ? GreenColors.primaryEmerald : Colors.white70,
                        letterSpacing: 0.5,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                Text(
                  isOnline
                      ? 'Sẵn sàng nhận cuốc xe • GPS kết nối'
                      : (isKycApproved
                          ? 'Gạt để quét khuôn mặt và bật ca làm việc'
                          : 'Cần phê duyệt KYC trước khi bật ca'),
                  style: const TextStyle(fontSize: 12, color: Colors.white54),
                ),
              ],
            ),
          ),
          Switch(
            value: isOnline,
            activeThumbColor: GreenColors.primaryEmerald,
            activeTrackColor: GreenColors.primaryEmerald.withValues(alpha: 0.3),
            inactiveThumbColor: Colors.white38,
            inactiveTrackColor: Colors.white10,
            onChanged: (val) {
              if (val && !isKycApproved) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(
                      kycStatus == KycStatus.pending
                          ? 'Hồ sơ KYC đang chờ duyệt. Chưa thể bật ca!'
                          : 'Vui lòng hoàn tất nộp hồ sơ KYC để bật ca làm việc.',
                    ),
                    backgroundColor: GreenColors.warningAmber,
                  ),
                );
                return;
              }
              onToggleShift(val);
            },
          ),
        ],
      ),
    );
  }
}
