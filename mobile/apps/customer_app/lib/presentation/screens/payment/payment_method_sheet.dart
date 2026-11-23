import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

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
        color: GreenColors.surfaceDark,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        border: Border(top: BorderSide(color: GreenColors.cardBorder)),
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
              decoration: BoxDecoration(color: Colors.white24, borderRadius: BorderRadius.circular(2)),
            ),
          ),
          const SizedBox(height: 16),
          const Text(
            'Chọn phương thức thanh toán',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
          ),
          const SizedBox(height: 6),
          Text(
            'Số tiền thanh toán: ${GreenFormatters.currencyVnd(requiredAmount.toDouble())}',
            style: const TextStyle(fontSize: 14, color: GreenColors.textSecondary),
          ),
          const SizedBox(height: 16),
          _buildMethodItem(
            context,
            method: PaymentMethod.greenWallet,
            title: 'Ví Xanh (Green Wallet)',
            subtitle: walletBalance != null
                ? 'Số dư: ${GreenFormatters.currencyVnd(walletBalance!.toDouble())} ${hasEnoughWallet ? '' : '(Không đủ)'}'
                : 'Thanh toán tức thì không phí',
            icon: Icons.account_balance_wallet_rounded,
            iconColor: GreenColors.primaryEmerald,
            isDisabled: !hasEnoughWallet && walletBalance != null,
          ),
          const SizedBox(height: 12),
          _buildMethodItem(
            context,
            method: PaymentMethod.vnpay,
            title: 'VNPay QR / Thẻ ATM',
            subtitle: 'Thẻ ATM nội địa, Quốc tế, QR',
            icon: Icons.credit_card_rounded,
            iconColor: GreenColors.electricCyan,
          ),
          const SizedBox(height: 12),
          _buildMethodItem(
            context,
            method: PaymentMethod.momo,
            title: 'Ví MoMo',
            subtitle: 'Thanh toán nhanh qua ví điện tử',
            icon: Icons.qr_code_scanner_rounded,
            iconColor: const Color(0xFFF472B6),
          ),
          const SizedBox(height: 12),
          _buildMethodItem(
            context,
            method: PaymentMethod.cash,
            title: 'Tiền mặt',
            subtitle: 'Thanh toán trực tiếp cho tài xế',
            icon: Icons.payments_rounded,
            iconColor: GreenColors.accentGold,
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
    return GestureDetector(
      onTap: isDisabled ? null : () => onSelect(method),
      child: Opacity(
        opacity: isDisabled ? 0.45 : 1.0,
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: isSelected ? GreenColors.primaryEmerald.withValues(alpha: 0.14) : GreenColors.cardDark,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(
              color: isSelected ? GreenColors.primaryEmerald : GreenColors.cardBorder,
            ),
          ),
          child: Row(
            children: [
              Icon(icon, color: iconColor, size: 24),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
                    Text(subtitle, style: const TextStyle(color: GreenColors.textSecondary, fontSize: 12)),
                  ],
                ),
              ),
              if (isSelected)
                const Icon(Icons.check_circle_rounded, color: GreenColors.primaryEmerald),
            ],
          ),
        ),
      ),
    );
  }
}
