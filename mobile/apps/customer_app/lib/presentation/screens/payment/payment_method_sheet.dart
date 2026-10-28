import 'package:flutter/material.dart';
import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';

class PaymentMethodSheet extends StatelessWidget {
  final PaymentMethod selectedMethod;
  final int? walletBalance;
  final int requiredAmount;
  final ValueChanged<PaymentMethod> onSelect;

  const PaymentMethodSheet({
    super.key,
    required this.selectedMethod,
    this.walletBalance,
    required this.requiredAmount,
    required this.onSelect,
  });

  static Future<PaymentMethod?> show(
    BuildContext context, {
    required PaymentMethod currentMethod,
    int? walletBalance,
    required int requiredAmount,
  }) {
    return showModalBottomSheet<PaymentMethod>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => PaymentMethodSheet(
        selectedMethod: currentMethod,
        walletBalance: walletBalance,
        requiredAmount: requiredAmount,
        onSelect: (m) => Navigator.pop(ctx, m),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final hasEnoughWallet = (walletBalance ?? 0) >= requiredAmount;

    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.grey.shade300,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 16),
          const Text(
            'Chọn phương thức thanh toán',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Số tiền thanh toán: ${requiredAmount.toString().replaceAllMapped(RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'), (m) => '${m[1]}.')} đ',
            style: const TextStyle(fontSize: 14, color: Colors.grey),
          ),
          const SizedBox(height: 16),

          // 1. Green Wallet
          _buildMethodItem(
            context,
            method: PaymentMethod.greenWallet,
            title: 'Ví Xanh (Green Wallet)',
            subtitle: walletBalance != null
                ? 'Số dư: ${walletBalance.toString().replaceAllMapped(RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'), (m) => '${m[1]}.')} đ ${hasEnoughWallet ? '' : '(Không đủ số dư)'}'
                : 'Thanh toán tức thì không phí',
            icon: Icons.account_balance_wallet_rounded,
            iconColor: const Color(0xFF10B981),
            isDisabled: !hasEnoughWallet && walletBalance != null,
          ),
          const SizedBox(height: 12),

          // 2. VNPay
          _buildMethodItem(
            context,
            method: PaymentMethod.vnpay,
            title: 'VNPay',
            subtitle: 'Thẻ ATM nội địa, Thẻ quốc tế, VNPay-QR',
            icon: Icons.credit_card_rounded,
            iconColor: const Color(0xFF005BAA),
          ),
          const SizedBox(height: 12),

          // 3. MoMo
          _buildMethodItem(
            context,
            method: PaymentMethod.momo,
            title: 'Ví MoMo',
            subtitle: 'Thanh toán qua ứng dụng MoMo',
            icon: Icons.phone_android_rounded,
            iconColor: const Color(0xFFA50064),
          ),
          const SizedBox(height: 12),

          // 4. Cash
          _buildMethodItem(
            context,
            method: PaymentMethod.cash,
            title: 'Tiền mặt (CASH)',
            subtitle: 'Thanh toán trực tiếp cho tài xế khi kết thúc',
            icon: Icons.payments_rounded,
            iconColor: const Color(0xFF059669),
          ),
        ],
      ),
    );
  }

  Widget _buildMethodItem(
    BuildContext context, {
    required PaymentMethod method,
    required String title,
    required String subtitle,
    required IconData icon,
    required Color iconColor,
    bool isDisabled = false,
  }) {
    final isSelected = selectedMethod == method;

    return InkWell(
      onTap: isDisabled ? null : () => onSelect(method),
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: isSelected
                ? const Color(0xFF10B981)
                : (isDisabled ? Colors.grey.shade200 : Colors.grey.shade300),
            width: isSelected ? 2 : 1,
          ),
          color: isSelected
              ? const Color(0xFF10B981).withOpacity(0.05)
              : (isDisabled ? Colors.grey.shade50 : Colors.white),
        ),
        child: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: iconColor.withOpacity(0.12),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, color: iconColor, size: 24),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: isDisabled ? Colors.grey : Colors.black87,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    style: TextStyle(
                      fontSize: 12,
                      color: isDisabled ? Colors.red.shade400 : Colors.grey.shade600,
                    ),
                  ),
                ],
              ),
            ),
            if (isSelected)
              const Icon(Icons.check_circle_rounded, color: Color(0xFF10B981), size: 22),
          ],
        ),
      ),
    );
  }
}

