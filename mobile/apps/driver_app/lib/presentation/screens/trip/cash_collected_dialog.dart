import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

class CashCollectedDialog extends StatefulWidget {
  final String tripId;
  final int amountVnd;
  final String customerName;
  final PaymentApi paymentApi;
  final VoidCallback onConfirmed;

  const CashCollectedDialog({
    super.key,
    required this.tripId,
    required this.amountVnd,
    this.customerName = 'Khách hàng',
    required this.paymentApi,
    required this.onConfirmed,
  });

  static Future<void> show(
    BuildContext context, {
    required String tripId,
    required int amountVnd,
    String customerName = 'Khách hàng',
    required PaymentApi paymentApi,
    required VoidCallback onConfirmed,
  }) {
    return showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => CashCollectedDialog(
        tripId: tripId,
        amountVnd: amountVnd,
        customerName: customerName,
        paymentApi: paymentApi,
        onConfirmed: onConfirmed,
      ),
    );
  }

  @override
  State<CashCollectedDialog> createState() => _CashCollectedDialogState();
}

class _CashCollectedDialogState extends State<CashCollectedDialog> {
  bool _isConfirming = false;
  String? _errorMessage;

  Future<void> _handleConfirm() async {
    setState(() {
      _isConfirming = true;
      _errorMessage = null;
    });
    try {
      await widget.paymentApi.confirmCashCollected(widget.tripId);
      if (mounted) {
        Navigator.pop(context);
        widget.onConfirmed();
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString().replaceFirst('Exception: ', '');
          _isConfirming = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: GreenColors.surfaceDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: GreenColors.accentGold.withValues(alpha: 0.16),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.payments_rounded, color: GreenColors.accentGold, size: 34),
            ),
            const SizedBox(height: 14),
            const Text('Thu tiền mặt từ khách hàng',
                style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Text('Xác nhận đã thu đủ tiền mặt từ ${widget.customerName}:',
                textAlign: TextAlign.center, style: const TextStyle(color: GreenColors.textSecondary, fontSize: 13)),
            const SizedBox(height: 12),
            Text(
              GreenFormatters.currencyVnd(widget.amountVnd.toDouble()),
              style: const TextStyle(color: GreenColors.primaryEmerald, fontSize: 28, fontWeight: FontWeight.w900),
            ),
            if (_errorMessage != null) ...[
              const SizedBox(height: 12),
              Text(_errorMessage!, style: const TextStyle(color: GreenColors.errorRed, fontSize: 12)),
            ],
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              height: 46,
              child: ElevatedButton(
                onPressed: _isConfirming ? null : _handleConfirm,
                style: ElevatedButton.styleFrom(
                  backgroundColor: GreenColors.primaryEmerald,
                  foregroundColor: GreenColors.backgroundDark,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: Text(_isConfirming ? 'Đang xác nhận...' : 'ĐÃ THU TIỀN MẶT',
                    style: const TextStyle(fontWeight: FontWeight.bold)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
