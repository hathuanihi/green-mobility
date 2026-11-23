import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

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
      final results = await Future.wait([
        widget.walletApi.getBalance(),
        widget.earningsApi.getWithdrawals(),
      ]);
      if (mounted) {
        setState(() {
          _balance = results[0] as WalletBalanceModel;
          _withdrawals = results[1] as List<WithdrawalItemModel>;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showWithdrawModal() {
    final amountCtrl = TextEditingController();
    final accNoCtrl = TextEditingController();
    final holderCtrl = TextEditingController();
    String selectedBank = 'VCB';
    bool isSubmitting = false;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: GreenColors.surfaceDark,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => Padding(
          padding: EdgeInsets.fromLTRB(20, 20, 20, MediaQuery.of(ctx).viewInsets.bottom + 24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Yêu cầu Rút tiền Về Ngân hàng', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
              const SizedBox(height: 14),
              DropdownButtonFormField<String>(
                value: selectedBank,
                dropdownColor: GreenColors.surfaceDark,
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(
                  labelText: 'Ngân hàng',
                  labelStyle: const TextStyle(color: GreenColors.textSecondary),
                  filled: true,
                  fillColor: GreenColors.cardDark,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                ),
                items: const [
                  DropdownMenuItem(value: 'VCB', child: Text('Vietcombank (VCB)')),
                  DropdownMenuItem(value: 'TCB', child: Text('Techcombank (TCB)')),
                  DropdownMenuItem(value: 'MBB', child: Text('MB Bank (MBB)')),
                  DropdownMenuItem(value: 'ACB', child: Text('ACB')),
                ],
                onChanged: (v) {
                  if (v != null) setModalState(() => selectedBank = v);
                },
              ),
              const SizedBox(height: 12),
              TextField(
                controller: amountCtrl,
                keyboardType: TextInputType.number,
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(
                  labelText: 'Số tiền rút (VNĐ)',
                  labelStyle: const TextStyle(color: GreenColors.textSecondary),
                  filled: true,
                  fillColor: GreenColors.cardDark,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: accNoCtrl,
                keyboardType: TextInputType.number,
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(
                  labelText: 'Số tài khoản ngân hàng',
                  labelStyle: const TextStyle(color: GreenColors.textSecondary),
                  filled: true,
                  fillColor: GreenColors.cardDark,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: holderCtrl,
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(
                  labelText: 'Tên chủ tài khoản (Không dấu)',
                  labelStyle: const TextStyle(color: GreenColors.textSecondary),
                  filled: true,
                  fillColor: GreenColors.cardDark,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                ),
              ),
              const SizedBox(height: 18),
              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton(
                  onPressed: isSubmitting
                      ? null
                      : () async {
                          final amount = int.tryParse(amountCtrl.text.trim()) ?? 0;
                          if (amount < 50000 || accNoCtrl.text.trim().isEmpty || holderCtrl.text.trim().isEmpty) {
                            return;
                          }
                          setModalState(() => isSubmitting = true);
                          try {
                            await widget.earningsApi.requestWithdrawal(WithdrawalRequestModel(
                              amountVnd: amount,
                              bankCode: selectedBank,
                              bankAccountNumber: accNoCtrl.text.trim(),
                              accountHolderName: holderCtrl.text.trim().toUpperCase(),
                            ));
                            if (mounted) {
                              Navigator.pop(ctx);
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(content: Text('Đã gửi yêu cầu rút tiền!')),
                              );
                              await _loadAll();
                            }
                          } catch (e) {
                            setModalState(() => isSubmitting = false);
                            if (mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(content: Text(e.toString().replaceFirst('Exception: ', '')), backgroundColor: GreenColors.errorRed),
                              );
                            }
                          }
                        },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: GreenColors.primaryEmerald,
                    foregroundColor: GreenColors.backgroundDark,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  child: Text(isSubmitting ? 'Đang gửi...' : 'Xác nhận rút tiền', style: const TextStyle(fontWeight: FontWeight.bold)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final earnBal = _balance?.earningsBalanceVnd ?? _balance?.availableBalanceVnd ?? 0;
    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      appBar: AppBar(
        backgroundColor: GreenColors.backgroundDark,
        title: const Text('Ví Tài xế & Rút tiền'),
        actions: [
          IconButton(onPressed: _loadAll, icon: const Icon(Icons.refresh_rounded)),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadAll,
        child: _isLoading
            ? const Center(child: CircularProgressIndicator(color: GreenColors.primaryEmerald))
            : ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: GreenColors.surfaceDark,
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(color: GreenColors.primaryEmerald.withValues(alpha: 0.4)),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Số dư thu nhập khả dụng', style: TextStyle(color: GreenColors.textSecondary, fontSize: 12)),
                        const SizedBox(height: 6),
                        Text(
                          GreenFormatters.currencyVnd(earnBal.toDouble()),
                          style: const TextStyle(color: GreenColors.primaryEmerald, fontSize: 28, fontWeight: FontWeight.w900),
                        ),
                        const SizedBox(height: 10),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text('Đang giữ chờ duyệt: ${GreenFormatters.currencyVnd((_balance?.pendingHoldVnd ?? 0).toDouble())}',
                                style: const TextStyle(color: GreenColors.accentGold, fontSize: 12)),
                            Text('Nợ tiền mặt: ${GreenFormatters.currencyVnd((_balance?.cashDebtVnd ?? 0).toDouble())}',
                                style: const TextStyle(color: GreenColors.errorRed, fontSize: 12)),
                          ],
                        ),
                        const SizedBox(height: 16),
                        SizedBox(
                          width: double.infinity,
                          child: ElevatedButton.icon(
                            onPressed: _showWithdrawModal,
                            icon: const Icon(Icons.account_balance_rounded),
                            label: const Text('Tạo lệnh Rút tiền', style: TextStyle(fontWeight: FontWeight.bold)),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: GreenColors.primaryEmerald,
                              foregroundColor: GreenColors.backgroundDark,
                              padding: const EdgeInsets.symmetric(vertical: 12),
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 18),
                  const Text('Lịch sử yêu cầu rút tiền', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15)),
                  const SizedBox(height: 10),
                  if (_withdrawals.isEmpty)
                    const Center(
                      child: Padding(
                        padding: EdgeInsets.symmetric(vertical: 30),
                        child: Text('Chưa có yêu cầu rút tiền nào', style: TextStyle(color: GreenColors.textSecondary)),
                      ),
                    )
                  else
                    ..._withdrawals.map((w) => Container(
                          margin: const EdgeInsets.only(bottom: 10),
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            color: GreenColors.surfaceDark,
                            borderRadius: BorderRadius.circular(14),
                            border: Border.all(color: GreenColors.cardBorder),
                          ),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text('${w.bankCode} •••• ${w.bankAccountLast4}',
                                      style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                                  Text(w.status, style: const TextStyle(color: GreenColors.electricCyan, fontSize: 12)),
                                ],
                              ),
                              Text(
                                GreenFormatters.currencyVnd(w.amountVnd.toDouble()),
                                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                              ),
                            ],
                          ),
                        )),
                ],
              ),
      ),
    );
  }
}
