import 'dart:async';
import 'package:flutter/material.dart';
import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';

class PaymentScreen extends StatefulWidget {
  final String tripId;
  final int amountVnd;
  final PaymentMethod initialMethod;
  final PaymentApi paymentApi;
  final StompService? stompService;
  final VoidCallback onPaymentSuccess;

  const PaymentScreen({
    super.key,
    required this.tripId,
    required this.amountVnd,
    required this.initialMethod,
    required this.paymentApi,
    this.stompService,
    required this.onPaymentSuccess,
  });

  @override
  State<PaymentScreen> createState() => _PaymentScreenState();
}

class _PaymentScreenState extends State<PaymentScreen> with WidgetsBindingObserver {
  late PaymentMethod _method;
  PaymentResponseModel? _paymentResponse;
  bool _isLoading = true;
  String? _errorMessage;
  Timer? _pollingTimer;
  StreamSubscription? _wsSubscription;
  bool _isSuccessHandled = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _method = widget.initialMethod;
    _initiatePayment();
    _subscribeWebSocket();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _pollingTimer?.cancel();
    _wsSubscription?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && _paymentResponse != null) {
      // User returned from VNPay/MoMo app/browser -> poll immediately
      _checkPaymentStatus();
    }
  }

  void _subscribeWebSocket() {
    // Listen for payment updates over WS topic
    if (widget.stompService != null) {
      _wsSubscription = widget.stompService!
          .subscribe('/topic/trip/${widget.tripId}/payment')
          .listen((data) {
        if (data is Map<String, dynamic>) {
          final status = data['status']?.toString();
          if (status == 'PAID') {
            _handleSuccess();
          }
        }
      });
    }
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

      setState(() {
        _paymentResponse = res;
        _isLoading = false;
      });

      if (res.status == PaymentStatus.paid) {
        _handleSuccess();
        return;
      }

      // Start periodic polling fallback
      _startPolling(res.paymentId);
    } catch (e) {
      setState(() {
        _errorMessage = e.toString().replaceFirst('Exception: ', '');
        _isLoading = false;
      });
    }
  }

  void _startPolling(String paymentId) {
    _pollingTimer?.cancel();
    _pollingTimer = Timer.periodic(const Duration(seconds: 3), (timer) async {
      await _checkPaymentStatus();
    });
  }

  Future<void> _checkPaymentStatus() async {
    if (_paymentResponse == null || _isSuccessHandled) return;

    try {
      final statusRes = await widget.paymentApi.getPaymentStatus(_paymentResponse!.paymentId);
      setState(() {
        _paymentResponse = statusRes;
      });

      if (statusRes.status == PaymentStatus.paid) {
        _handleSuccess();
      } else if (statusRes.status == PaymentStatus.failed) {
        _pollingTimer?.cancel();
        setState(() {
          _errorMessage = 'Thanh toán thất bại hoặc hết hạn. Vui lòng thử lại.';
        });
      }
    } catch (_) {
      // Ignore network hiccups during background polling
    }
  }

  void _handleSuccess() {
    if (_isSuccessHandled) return;
    _isSuccessHandled = true;
    _pollingTimer?.cancel();
    widget.onPaymentSuccess();
  }

  String _formatVnd(int amount) {
    return amount.toString().replaceAllMapped(
      RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
      (Match m) => '${m[1]}.',
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text('Thanh toán chuyến đi', style: TextStyle(fontWeight: FontWeight.bold)),
        elevation: 0,
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: SafeArea(
        child: _isLoading
            ? const Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CircularProgressIndicator(color: Color(0xFF10B981)),
                    SizedBox(height: 16),
                    Text('Đang khởi tạo thanh toán...', style: TextStyle(color: Colors.grey)),
                  ],
                ),
              )
            : _errorMessage != null
                ? _buildErrorView()
                : _buildPaymentDetails(),
      ),
    );
  }

  Widget _buildErrorView() {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.error_outline_rounded, color: Colors.redAccent, size: 64),
          const SizedBox(height: 16),
          const Text(
            'Có lỗi xảy ra',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          Text(
            _errorMessage ?? '',
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 14, color: Colors.grey),
          ),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: ElevatedButton(
              onPressed: _initiatePayment,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF10B981),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: const Text('Thử lại', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPaymentDetails() {
    return Padding(
      padding: const EdgeInsets.all(20.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: const Color(0xFF10B981).withOpacity(0.08),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              children: [
                const Text('Tổng tiền cước', style: TextStyle(fontSize: 14, color: Colors.grey)),
                const SizedBox(height: 6),
                Text(
                  '${_formatVnd(widget.amountVnd)} đ',
                  style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: Color(0xFF10B981)),
                ),
                const SizedBox(height: 4),
                Text(
                  'Phương thức: ${_method.value}',
                  style: const TextStyle(fontSize: 13, color: Colors.black54),
                ),
              ],
            ),
          ),
          const SizedBox(height: 32),

          // Instruction or QR Code
          if (_method == PaymentMethod.vnpay || _method == PaymentMethod.momo) ...[
            const Text(
              'Quét mã hoặc mở ứng dụng để thanh toán',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 16),
            Container(
              width: 200,
              height: 200,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey.shade300),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      _method == PaymentMethod.vnpay ? Icons.qr_code_2_rounded : Icons.phone_android_rounded,
                      size: 64,
                      color: const Color(0xFF10B981),
                    ),
                    const SizedBox(height: 8),
                    const Text('Mã QR Thanh toán', style: TextStyle(fontSize: 12, color: Colors.grey)),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),
            if (_paymentResponse?.payUrl != null)
              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton.icon(
                  onPressed: () {
                    // In real app: launchUrlString(_paymentResponse!.payUrl!);
                  },
                  icon: const Icon(Icons.open_in_new, color: Colors.white),
                  label: Text('Mở ứng dụng ${_method.value}', style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white)),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF10B981),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                ),
              ),
          ] else if (_method == PaymentMethod.cash) ...[
            const Icon(Icons.payments_rounded, size: 64, color: Color(0xFF10B981)),
            const SizedBox(height: 16),
            const Text(
              'Thanh toán tiền mặt cho tài xế',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              'Vui lòng gửi ${_formatVnd(widget.amountVnd)} đ trực tiếp cho tài xế khi kết thúc hành trình.',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 14, color: Colors.grey),
            ),
          ],

          const Spacer(),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const SizedBox(
                width: 16,
                height: 16,
                child: CircularProgressIndicator(strokeWidth: 2, color: Color(0xFF10B981)),
              ),
              const SizedBox(width: 10),
              Text(
                'Đang chờ xác nhận thanh toán...',
                style: TextStyle(fontSize: 13, color: Colors.grey.shade600),
              ),
            ],
          ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }
}

