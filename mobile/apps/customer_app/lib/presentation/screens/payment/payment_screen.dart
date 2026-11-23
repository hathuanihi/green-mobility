import 'dart:async';
import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

class PaymentScreen extends StatefulWidget {
  final String tripId;
  final int amountVnd;
  final PaymentMethod initialMethod;
  final PaymentApi paymentApi;
  final VoidCallback onPaymentSuccess;

  const PaymentScreen({
    super.key,
    required this.tripId,
    required this.amountVnd,
    this.initialMethod = PaymentMethod.vnpay,
    required this.paymentApi,
    required this.onPaymentSuccess,
  });

  @override
  State<PaymentScreen> createState() => _PaymentScreenState();
}

class _PaymentScreenState extends State<PaymentScreen> {
  late PaymentMethod _method;
  PaymentResponseModel? _paymentResponse;
  bool _isLoading = true;
  String? _errorMessage;
  Timer? _pollingTimer;
  bool _isSuccessHandled = false;

  @override
  void initState() {
    super.initState();
    _method = widget.initialMethod;
    _initiatePayment();
  }

  @override
  void dispose() {
    _pollingTimer?.cancel();
    super.dispose();
  }

  Future<void> _initiatePayment() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final res = await widget.paymentApi.settlePayment(
        widget.tripId,
        _method,
        returnUrl: 'greenmobility://payment/result?tripId=${widget.tripId}',
      );
      if (mounted) {
        setState(() {
          _paymentResponse = res;
          _isLoading = false;
        });
        if (res.status == PaymentStatus.paid) {
          _handleSuccess();
          return;
        }
        _pollingTimer?.cancel();
        _pollingTimer = Timer.periodic(const Duration(seconds: 3), (_) => _checkPaymentStatus());
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = e.toString().replaceFirst('Exception: ', '');
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _checkPaymentStatus() async {
    if (_paymentResponse == null || _isSuccessHandled) return;
    try {
      final statusRes = await widget.paymentApi.getPaymentStatus(_paymentResponse!.paymentId);
      if (!mounted) return;
      setState(() => _paymentResponse = statusRes);
      if (statusRes.status == PaymentStatus.paid) {
        _handleSuccess();
      } else if (statusRes.status == PaymentStatus.failed) {
        _pollingTimer?.cancel();
        setState(() => _errorMessage = 'Thanh toán thất bại hoặc hết hạn.');
      }
    } catch (_) {}
  }

  void _handleSuccess() {
    if (_isSuccessHandled) return;
    _isSuccessHandled = true;
    _pollingTimer?.cancel();
    widget.onPaymentSuccess();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      appBar: AppBar(
        backgroundColor: GreenColors.backgroundDark,
        title: const Text('Thanh toán chuyến đi'),
        leading: IconButton(icon: const Icon(Icons.close), onPressed: () => Navigator.pop(context)),
      ),
      body: SafeArea(
        child: _isLoading
            ? const Center(child: CircularProgressIndicator(color: GreenColors.primaryEmerald))
            : _errorMessage != null
                ? Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.error_outline_rounded, color: GreenColors.errorRed, size: 56),
                        const SizedBox(height: 16),
                        Text(_errorMessage!, textAlign: TextAlign.center, style: const TextStyle(color: Colors.white70)),
                        const SizedBox(height: 20),
                        ElevatedButton(
                          onPressed: _initiatePayment,
                          style: ElevatedButton.styleFrom(backgroundColor: GreenColors.primaryEmerald, foregroundColor: GreenColors.backgroundDark),
                          child: const Text('Thử lại', style: TextStyle(fontWeight: FontWeight.bold)),
                        ),
                      ],
                    ),
                  )
                : Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Container(
                          padding: const EdgeInsets.all(20),
                          decoration: BoxDecoration(
                            color: GreenColors.surfaceDark,
                            borderRadius: BorderRadius.circular(20),
                            border: Border.all(color: GreenColors.cardBorder),
                          ),
                          child: Column(
                            children: [
                              Text(PaymentMethod.getLabel(_method.value), style: const TextStyle(color: GreenColors.textSecondary)),
                              const SizedBox(height: 8),
                              Text(
                                GreenFormatters.currencyVnd((_paymentResponse?.amountVnd ?? widget.amountVnd).toDouble()),
                                style: const TextStyle(color: GreenColors.primaryEmerald, fontSize: 30, fontWeight: FontWeight.w900),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                'Trạng thái: ${_paymentResponse?.status.value ?? 'PENDING'}',
                                style: const TextStyle(color: GreenColors.electricCyan, fontWeight: FontWeight.w600),
                              ),
                            ],
                          ),
                        ),
                        if (_paymentResponse?.payUrl != null) ...[
                          const SizedBox(height: 16),
                          Container(
                            padding: const EdgeInsets.all(14),
                            decoration: BoxDecoration(
                              color: GreenColors.cardDark,
                              borderRadius: BorderRadius.circular(14),
                              border: Border.all(color: GreenColors.cardBorder),
                            ),
                            child: Text(
                              'Liên kết cổng thanh toán:\n${_paymentResponse!.payUrl}',
                              style: const TextStyle(color: Colors.white70, fontSize: 12),
                            ),
                          ),
                        ],
                        const Spacer(),
                        ElevatedButton(
                          onPressed: _checkPaymentStatus,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: GreenColors.primaryEmerald,
                            foregroundColor: GreenColors.backgroundDark,
                            padding: const EdgeInsets.symmetric(vertical: 16),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                          ),
                          child: const Text('KIỂM TRA TRẠNG THÁI THANH TOÁN', style: TextStyle(fontWeight: FontWeight.bold)),
                        ),
                      ],
                    ),
                  ),
      ),
    );
  }
}
