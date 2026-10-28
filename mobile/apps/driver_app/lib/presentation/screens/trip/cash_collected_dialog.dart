import 'package:flutter/material.dart';
import 'package:core_network/core_network.dart';

class CashCollectedDialog extends StatefulWidget {
  final String paymentId;
  final String tripCode;
  final int totalAmountVnd;
  final int commissionVnd;
  final int driverAmountVnd;
  final PaymentApi paymentApi;
  final VoidCallback onConfirmed;

  const CashCollectedDialog({
    super.key,
    required this.paymentId,
    required this.tripCode,
    required this.totalAmountVnd,
    required this.commissionVnd,
    required this.driverAmountVnd,
    required this.paymentApi,
    required this.onConfirmed,
  });

  static Future<void> show(
    BuildContext context, {
    required String paymentId,
    required String tripCode,
    required int totalAmountVnd,
    required int commissionVnd,
    required int driverAmountVnd,
    required PaymentApi paymentApi,
    required VoidCallback onConfirmed,
  }) {
    return showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => CashCollectedDialog(
        paymentId: paymentId,
        tripCode: tripCode,
        totalAmountVnd: totalAmountVnd,
        commissionVnd: commissionVnd,
        driverAmountVnd: driverAmountVnd,
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

  String _formatVnd(int amount) {
    return amount.toString().replaceAllMapped(
      RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
      (Match m) => '${m[1]}.',
    );
  }

  Future<void> _handleConfirm() async {
    setState(() {
      _isConfirming = true;
      _errorMessage = null;
    });

    try {
      await widget.paymentApi.confirmCashCollected(widget.paymentId);
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
    return AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      title: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: Colors.amber.withOpacity(0.15),
              borderRadius: BorderRadius.circular(10),
            ),
            child: const Icon(Icons.payments_rounded, color: Colors.amber, size: 28),
          ),
          const SizedBox(width: 12),
          const Expanded(
            child: Text(
              'Thu tiền mặt chuyến đi',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
          ),
        ],
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Mã cuốc xe: #${widget.tripCode}',
            style: const TextStyle(fontSize: 13, color: Colors.grey),
          ),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: Colors.grey.shade50,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: Colors.grey.shade200),
            ),
            child: Column(
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.between,
                  children: [
                    const Text('Số tiền cần thu khách:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                    Text(
                      '${_formatVnd(widget.totalAmountVnd)} đ',
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Color(0xFF10B981)),
                    ),
                  ],
                ),
                const Divider(height: 20),
                Row(
                  mainAxisAlignment: MainAxisAlignment.between,
                  children: [
                    const Text('Phí nền tảng (20%):', style: TextStyle(fontSize: 13, color: Colors.black54)),
                    Text('-${_formatVnd(widget.commissionVnd)} đ', style: const TextStyle(fontSize: 13, color: Colors.red)),
                  ],
                ),
                const SizedBox(height: 4),
                Row(
                  mainAxisAlignment: MainAxisAlignment.between,
                  children: [
                    const Text('Thu nhập thực nhận:', style: TextStyle(fontSize: 13, color: Colors.black54)),
                    Text('${_formatVnd(widget.driverAmountVnd)} đ', style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          const Text(
            'Lưu ý: Phí nền tảng 20% sẽ được ghi nhận vào nợ tiền mặt và tự động cấn trừ vào số dư Ví Xanh của bạn.',
            style: TextStyle(fontSize: 12, color: Colors.grey, fontStyle: FontStyle.italic),
          ),
          if (_errorMessage != null) ...[
            const SizedBox(height: 12),
            Text(
              _errorMessage!,
              style: const TextStyle(color: Colors.red, fontSize: 13),
            ),
          ],
        ],
      ),
      actions: [
        SizedBox(
          width: double.infinity,
          height: 44,
          child: ElevatedButton(
            onPressed: _isConfirming ? null : _handleConfirm,
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF10B981),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            ),
            child: _isConfirming
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                  )
                : const Text('Đã thu đủ tiền', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15)),
          ),
        ),
      ],
    );
  }
}

