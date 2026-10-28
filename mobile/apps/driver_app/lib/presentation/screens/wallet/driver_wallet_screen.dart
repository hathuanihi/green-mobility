import 'package:flutter/material.dart';
import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';

class DriverWalletScreen extends StatefulWidget {
  final WalletApi walletApi;
  final EarningsApi earningsApi;

  const DriverWalletScreen({
    super.key,
    required this.walletApi,
    required this.earningsApi,
  });

  @override
  State<DriverWalletScreen> createState() => _DriverWalletScreenState();
}

class _DriverWalletScreenState extends State<DriverWalletScreen> {
  WalletBalanceModel? _balance;
  List<WithdrawalItemModel> _withdrawals = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadAll();
  }

  Future<void> _loadAll() async {
    setState(() => _isLoading = true);
    try {
      final b = await widget.walletApi.getBalance();
      final w = await widget.earningsApi.getWithdrawals();
      setState(() {
        _balance = b;
        _withdrawals = w;
        _isLoading = false;
      });
    } catch (_) {
      setState(() => _isLoading = false);
    }
  }

  String _formatVnd(int amount) {
    return amount.toString().replaceAllMapped(
      RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
      (Match m) => '${m[1]}.',
    );
  }

  void _showWithdrawModal() {
    final formKey = GlobalKey<FormState>();
    final amountController = TextEditingController();
    final bankAccountController = TextEditingController();
    final accountHolderController = TextEditingController();
    String selectedBank = 'VCB'; // Vietcombank
    bool isSubmitting = false;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) {
          return Container(
            decoration: const BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
            ),
            padding: EdgeInsets.fromLTRB(20, 16, 20, MediaQuery.of(context).viewInsets.bottom + 24),
            child: Form(
              key: formKey,
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
                  const Text('Yêu cầu rút tiền về ngân hàng', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 6),
                  Text(
                    'Số dư khả dụng: ${_formatVnd(_balance?.availableBalanceVnd ?? 0)} đ',
                    style: const TextStyle(fontSize: 13, color: Color(0xFF10B981), fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 16),

                  // Bank selector
                  const Text('Ngân hàng thụ hưởng', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.black54)),
                  const SizedBox(height: 6),
                  DropdownButtonFormField<String>(
                    value: selectedBank,
                    decoration: InputDecoration(
                      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                    items: const [
                      DropdownMenuItem(value: 'VCB', child: Text('Vietcombank')),
                      DropdownMenuItem(value: 'TCB', child: Text('Techcombank')),
                      DropdownMenuItem(value: 'MB', child: Text('MBBank')),
                      DropdownMenuItem(value: 'ACB', child: Text('ACB')),
                      DropdownMenuItem(value: 'VPB', child: Text('VPBank')),
                    ],
                    onChanged: (val) {
                      if (val != null) setModalState(() => selectedBank = val);
                    },
                  ),
                  const SizedBox(height: 12),

                  // Account Number
                  const Text('Số tài khoản ngân hàng', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.black54)),
                  const SizedBox(height: 6),
                  TextFormField(
                    controller: bankAccountController,
                    keyboardType: TextInputType.number,
                    decoration: InputDecoration(
                      hintText: 'Nhập số tài khoản',
                      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                    validator: (v) => (v == null || v.trim().length < 6) ? 'Số tài khoản không hợp lệ' : null,
                  ),
                  const SizedBox(height: 12),

                  // Account Holder Name
                  const Text('Tên chủ tài khoản (In hoa không dấu)', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.black54)),
                  const SizedBox(height: 6),
                  TextFormField(
                    controller: accountHolderController,
                    textCapitalization: TextCapitalization.characters,
                    decoration: InputDecoration(
                      hintText: 'NGUYEN VAN A',
                      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                    validator: (v) => (v == null || v.trim().isEmpty) ? 'Vui lòng nhập tên chủ tài khoản' : null,
                  ),
                  const SizedBox(height: 12),

                  // Amount
                  const Text('Số tiền rút (VND, tối thiểu 100.000 đ)', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.black54)),
                  const SizedBox(height: 6),
                  TextFormField(
                    controller: amountController,
                    keyboardType: TextInputType.number,
                    decoration: InputDecoration(
                      hintText: '100000',
                      suffixText: 'đ',
                      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                    validator: (v) {
                      final parsed = int.tryParse(v ?? '');
                      if (parsed == null || parsed < 100000) return 'Tối thiểu 100.000 đ';
                      if (parsed > (_balance?.availableBalanceVnd ?? 0)) return 'Vượt quá số dư khả dụng';
                      return null;
                    },
                  ),
                  const SizedBox(height: 24),

                  SizedBox(
                    width: double.infinity,
                    height: 48,
                    child: ElevatedButton(
                      onPressed: isSubmitting
                          ? null
                          : () async {
                              if (!formKey.currentState!.validate()) return;
                              setModalState(() => isSubmitting = true);
                              try {
                                final req = WithdrawalRequestModel(
                                  amountVnd: int.parse(amountController.text.trim()),
                                  bankCode: selectedBank,
                                  bankAccountNumber: bankAccountController.text.trim(),
                                  accountHolderName: accountHolderController.text.trim().toUpperCase(),
                                );
                                await widget.earningsApi.requestWithdrawal(req);
                                if (mounted) {
                                  Navigator.pop(ctx);
                                  _loadAll();
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(content: Text('Đã gửi yêu cầu rút tiền thành công!')),
                                  );
                                }
                              } catch (e) {
                                setModalState(() => isSubmitting = false);
                                if (mounted) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(content: Text(e.toString()), backgroundColor: Colors.red),
                                  );
                                }
                              }
                            },
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF10B981),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      child: isSubmitting
                          ? const CircularProgressIndicator(color: Colors.white, strokeWidth: 2)
                          : const Text('Xác nhận rút tiền', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final cashDebt = _balance?.cashDebtVnd ?? 0;
    final isCashDebtWarning = cashDebt > 300000;

    return Scaffold(
      backgroundColor: Colors.grey.shade50,
      appBar: AppBar(
        title: const Text('Ví tài xế & Rút tiền', style: TextStyle(fontWeight: FontWeight.bold)),
        elevation: 0,
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
      ),
      body: RefreshIndicator(
        onRefresh: _loadAll,
        color: const Color(0xFF10B981),
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Balance Card
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFF0F766E), Color(0xFF115E59)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Số dư khả dụng để rút', style: TextStyle(color: Colors.white70, fontSize: 13)),
                    const SizedBox(height: 8),
                    Text(
                      '${_formatVnd(_balance?.availableBalanceVnd ?? 0)} đ',
                      style: const TextStyle(color: Colors.white, fontSize: 32, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('Đang giữ rút', style: TextStyle(color: Colors.white60, fontSize: 11)),
                            Text('${_formatVnd(_balance?.pendingHoldVnd ?? 0)} đ', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                          ],
                        ),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('Nợ tiền mặt', style: TextStyle(color: Colors.white60, fontSize: 11)),
                            Text('${_formatVnd(cashDebt)} đ', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                          ],
                        ),
                        ElevatedButton.icon(
                          onPressed: (_balance?.availableBalanceVnd ?? 0) >= 100000 ? _showWithdrawModal : null,
                          icon: const Icon(Icons.arrow_upward_rounded, size: 18, color: Color(0xFF0F766E)),
                          label: const Text('Rút tiền', style: TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF0F766E))),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.white,
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),

              // Cash Debt Warning Banner (R9 / AC-12)
              if (isCashDebtWarning) ...[
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Colors.amber.shade50,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.amber.shade300),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.warning_amber_rounded, color: Colors.amber, size: 28),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Cảnh báo nợ chiết khấu tiền mặt!',
                              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: Colors.brown),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              'Bạn đang nợ ${_formatVnd(cashDebt)} đ phí chiết khấu tiền mặt. Nếu vượt 500.000 đ, hệ thống sẽ tạm ngưng phân bổ các chuyến thanh toán tiền mặt.',
                              style: const TextStyle(fontSize: 12, color: Colors.brown),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ],

              const SizedBox(height: 24),
              const Text('Lịch sử yêu cầu rút tiền', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
              const SizedBox(height: 12),

              if (_withdrawals.isEmpty && !_isLoading)
                Container(
                  padding: const EdgeInsets.symmetric(vertical: 40),
                  child: const Center(
                    child: Text('Chưa có yêu cầu rút tiền nào', style: TextStyle(color: Colors.grey)),
                  ),
                )
              else
                ListView.separated(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: _withdrawals.length,
                  separatorBuilder: (_, __) => const Divider(height: 1),
                  itemBuilder: (context, index) {
                    final item = _withdrawals[index];
                    return Container(
                      color: Colors.white,
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                      child: Row(
                        children: [
                          Container(
                            width: 40,
                            height: 40,
                            decoration: BoxDecoration(
                              color: item.status == 'APPROVED_PAID'
                                  ? const Color(0xFF10B981).withOpacity(0.12)
                                  : item.status == 'REJECTED'
                                      ? Colors.red.withOpacity(0.12)
                                      : Colors.amber.withOpacity(0.12),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Icon(
                              item.status == 'APPROVED_PAID'
                                  ? Icons.check_circle_outline
                                  : item.status == 'REJECTED'
                                      ? Icons.cancel_outlined
                                      : Icons.hourglass_top_rounded,
                              color: item.status == 'APPROVED_PAID'
                                  ? const Color(0xFF10B981)
                                  : item.status == 'REJECTED'
                                      ? Colors.red
                                      : Colors.amber,
                              size: 20,
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  '${item.bankCode} • ****${item.bankAccountLast4}',
                                  style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  item.createdAt.length >= 10 ? item.createdAt.substring(0, 10) : item.createdAt,
                                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                                ),
                              ],
                            ),
                          ),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              Text(
                                '-${_formatVnd(item.amountVnd)} đ',
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                item.status == 'APPROVED_PAID'
                                    ? 'Đã duyệt'
                                    : item.status == 'REJECTED'
                                        ? 'Bị từ chối'
                                        : 'Chờ duyệt',
                                style: TextStyle(
                                  fontSize: 11,
                                  fontWeight: FontWeight.w600,
                                  color: item.status == 'APPROVED_PAID'
                                      ? const Color(0xFF10B981)
                                      : item.status == 'REJECTED'
                                          ? Colors.red
                                          : Colors.amber.shade800,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    );
                  },
                ),
            ],
          ),
        ),
      ),
    );
  }
}

