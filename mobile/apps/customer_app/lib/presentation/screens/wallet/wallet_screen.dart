import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

class WalletScreen extends StatefulWidget {
  final WalletApi walletApi;
  final IncentiveApi? incentiveApi;

  const WalletScreen({
    super.key,
    required this.walletApi,
    this.incentiveApi,
  });

  @override
  State<WalletScreen> createState() => _WalletScreenState();
}

class _WalletScreenState extends State<WalletScreen> {
  WalletBalanceModel? _balance;
  IncentiveSummaryModel? _incentiveSummary;
  List<WalletTransactionModel> _transactions = [];
  List<IncentiveLedgerEntryModel> _incentiveEntries = [];

  bool _isLoadingBalance = true;
  int _historyTabIndex = 0; // 0: VND, 1: PCC & Eco-Points

  @override
  void initState() {
    super.initState();
    _loadAll();
  }

  Future<void> _loadAll() async {
    setState(() => _isLoadingBalance = true);
    try {
      final futures = <Future<dynamic>>[
        widget.walletApi.getBalance(),
        widget.walletApi.getTransactions(limit: 20),
        if (widget.incentiveApi != null) widget.incentiveApi!.getSummary(),
        if (widget.incentiveApi != null) widget.incentiveApi!.getLedgerHistory(limit: 20),
      ];
      final res = await Future.wait(futures);
      if (mounted) {
        setState(() {
          _balance = res[0] as WalletBalanceModel;
          _transactions = (res[1] as WalletTransactionsCursorResponse).items;
          if (widget.incentiveApi != null) {
            _incentiveSummary = res[2] as IncentiveSummaryModel;
            _incentiveEntries = (res[3] as IncentiveLedgerPageModel).items;
          }
          _isLoadingBalance = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoadingBalance = false);
    }
  }

  void _showTopupSheet() {
    int selectedAmount = 100000;
    String selectedGateway = 'VNPAY';
    final List<int> presets = [50000, 100000, 200000, 500000, 1000000];

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: GreenColors.surfaceDark,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => Padding(
          padding: const EdgeInsets.fromLTRB(20, 20, 20, 32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Nạp tiền vào Ví Xanh', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
              const SizedBox(height: 16),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: presets.map((amt) {
                  final active = selectedAmount == amt;
                  return ChoiceChip(
                    label: Text(GreenFormatters.currencyVnd(amt.toDouble())),
                    selected: active,
                    selectedColor: GreenColors.primaryEmerald,
                    backgroundColor: GreenColors.cardDark,
                    labelStyle: TextStyle(
                      color: active ? GreenColors.backgroundDark : Colors.white,
                      fontWeight: FontWeight.bold,
                    ),
                    onSelected: (_) => setModalState(() => selectedAmount = amt),
                  );
                }).toList(),
              ),
              const SizedBox(height: 18),
              Row(
                children: [
                  Expanded(
                    child: _gatewayOption(
                      'VNPay QR',
                      selectedGateway == 'VNPAY',
                      () => setModalState(() => selectedGateway = 'VNPAY'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _gatewayOption(
                      'Ví MoMo',
                      selectedGateway == 'MOMO',
                      () => setModalState(() => selectedGateway = 'MOMO'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 22),
              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton(
                  onPressed: () async {
                    Navigator.pop(ctx);
                    try {
                      final res = await widget.walletApi.initiateTopup(
                        selectedAmount,
                        selectedGateway,
                        returnUrl: 'greenmobility://payment/result?topup=true',
                      );
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('Đã tạo yêu cầu nạp #${res.topupId}')),
                        );
                      }
                    } catch (e) {
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text(e.toString()), backgroundColor: GreenColors.errorRed),
                        );
                      }
                    }
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: GreenColors.primaryEmerald,
                    foregroundColor: GreenColors.backgroundDark,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  child: const Text('Tiếp tục thanh toán', style: TextStyle(fontWeight: FontWeight.bold)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showConvertPccDialog() {
    if (_incentiveSummary == null || widget.incentiveApi == null) return;
    final ctrl = TextEditingController(
      text: _incentiveSummary!.pccConvertMin.toStringAsFixed(2),
    );
    bool converting = false;

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
              const Text('Quy đổi Tín chỉ Carbon → VNĐ', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
              const SizedBox(height: 6),
              Text(
                'Tỷ giá trợ giá: ${GreenFormatters.currencyVnd(_incentiveSummary!.pccToVndRateVnd.toDouble())} / 1 PCC\nTối thiểu ${_incentiveSummary!.pccConvertMin} PCC • Trần tháng còn lại: ${GreenFormatters.pcc(_incentiveSummary!.remainingMonthlyCap)}',
                style: const TextStyle(color: GreenColors.textSecondary, fontSize: 12),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: ctrl,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                decoration: InputDecoration(
                  labelText: 'Số lượng PCC muốn quy đổi',
                  labelStyle: const TextStyle(color: GreenColors.textSecondary),
                  filled: true,
                  fillColor: GreenColors.cardDark,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(14)),
                ),
              ),
              const SizedBox(height: 18),
              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton(
                  onPressed: converting
                      ? null
                      : () async {
                          final amount = double.tryParse(ctrl.text.trim()) ?? 0.0;
                          if (amount <= 0) return;
                          setModalState(() => converting = true);
                          try {
                            final res = await widget.incentiveApi!.convertPccToVnd(amount);
                            if (mounted) {
                              Navigator.pop(ctx);
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: Text('Đã nhận +${GreenFormatters.currencyVnd(res.vndCredited.toDouble())} vào Ví Xanh!'),
                                  backgroundColor: GreenColors.primaryDark,
                                ),
                              );
                              await _loadAll();
                            }
                          } catch (e) {
                            setModalState(() => converting = false);
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
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                  ),
                  child: Text(converting ? 'Đang quy đổi...' : 'Xác nhận quy đổi', style: const TextStyle(fontWeight: FontWeight.bold)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _gatewayOption(String title, bool active, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: BoxDecoration(
          color: active ? GreenColors.primaryEmerald.withValues(alpha: 0.15) : GreenColors.cardDark,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: active ? GreenColors.primaryEmerald : GreenColors.cardBorder),
        ),
        child: Center(
          child: Text(
            title,
            style: TextStyle(color: active ? GreenColors.primaryEmerald : Colors.white, fontWeight: FontWeight.bold),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      appBar: AppBar(
        backgroundColor: GreenColors.backgroundDark,
        title: const Text('Ví Xanh & Tín chỉ Carbon', style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          IconButton(onPressed: _loadAll, icon: const Icon(Icons.refresh_rounded, color: Colors.white70)),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadAll,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: const LinearGradient(colors: [Color(0xFF065F46), Color(0xFF047857)]),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Số dư Ví Xanh (VNĐ)', style: TextStyle(color: Color(0xFFD1FAE5), fontSize: 13)),
                  const SizedBox(height: 6),
                  Text(
                    _isLoadingBalance
                        ? '...'
                        : GreenFormatters.currencyVnd((_balance?.availableBalanceVnd ?? 0).toDouble()),
                    style: const TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.w900),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton.icon(
                    onPressed: _showTopupSheet,
                    icon: const Icon(Icons.add_circle_outline, size: 18),
                    label: const Text('Nạp tiền Ví Xanh', style: TextStyle(fontWeight: FontWeight.bold)),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.white,
                      foregroundColor: const Color(0xFF047857),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                  ),
                ],
              ),
            ),
            if (_incentiveSummary != null) ...[
              const SizedBox(height: 14),
              Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: GreenColors.surfaceDark,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: GreenColors.cardBorder),
                ),
                child: Column(
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('TÍN CHỈ CARBON (PCC)', style: TextStyle(color: GreenColors.textSecondary, fontSize: 11)),
                              const SizedBox(height: 4),
                              Text(
                                GreenFormatters.pcc(_incentiveSummary!.pccBalance),
                                style: const TextStyle(color: GreenColors.primaryEmerald, fontSize: 18, fontWeight: FontWeight.bold),
                              ),
                            ],
                          ),
                        ),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              const Text('ĐIỂM XANH (ECO-PTS)', style: TextStyle(color: GreenColors.textSecondary, fontSize: 11)),
                              const SizedBox(height: 4),
                              Text(
                                GreenFormatters.ecoPoints(_incentiveSummary!.ecoPointsBalance),
                                style: const TextStyle(color: GreenColors.accentGold, fontSize: 18, fontWeight: FontWeight.bold),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    SizedBox(
                      width: double.infinity,
                      child: OutlinedButton.icon(
                        onPressed: _incentiveSummary!.pccFrozen || _incentiveSummary!.redemptionsFrozen
                            ? null
                            : _showConvertPccDialog,
                        icon: const Icon(Icons.currency_exchange_rounded, color: GreenColors.electricCyan, size: 18),
                        label: Text(
                          _incentiveSummary!.pccFrozen
                              ? 'Ví Carbon đang bị đóng băng'
                              : 'Quy đổi PCC → VNĐ (${GreenFormatters.currencyVnd(_incentiveSummary!.pccToVndRateVnd.toDouble())}/PCC)',
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 12),
                        ),
                        style: OutlinedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          side: const BorderSide(color: GreenColors.electricCyan),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 18),
            Row(
              children: [
                ChoiceChip(
                  label: const Text('Biến động VNĐ'),
                  selected: _historyTabIndex == 0,
                  selectedColor: GreenColors.primaryEmerald,
                  backgroundColor: GreenColors.surfaceDark,
                  labelStyle: TextStyle(color: _historyTabIndex == 0 ? GreenColors.backgroundDark : Colors.white),
                  onSelected: (_) => setState(() => _historyTabIndex = 0),
                ),
                const SizedBox(width: 8),
                ChoiceChip(
                  label: const Text('Sổ cái PCC & Điểm Xanh'),
                  selected: _historyTabIndex == 1,
                  selectedColor: GreenColors.primaryEmerald,
                  backgroundColor: GreenColors.surfaceDark,
                  labelStyle: TextStyle(color: _historyTabIndex == 1 ? GreenColors.backgroundDark : Colors.white),
                  onSelected: (_) => setState(() => _historyTabIndex = 1),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (_historyTabIndex == 0)
              ..._transactions.map(_buildVndTxRow)
            else
              ..._incentiveEntries.map(_buildIncentiveTxRow),
          ],
        ),
      ),
    );
  }

  Widget _buildVndTxRow(WalletTransactionModel tx) {
    final isIn = tx.isCredit;
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: GreenColors.surfaceDark,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: GreenColors.cardBorder),
      ),
      child: Row(
        children: [
          Icon(isIn ? Icons.arrow_downward_rounded : Icons.arrow_upward_rounded,
              color: isIn ? GreenColors.primaryEmerald : GreenColors.errorRed, size: 20),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(tx.description.isNotEmpty ? tx.description : tx.type,
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                Text(tx.type, style: const TextStyle(color: GreenColors.textSecondary, fontSize: 11)),
              ],
            ),
          ),
          Text(
            '${isIn ? '+' : '-'}${GreenFormatters.currencyVnd(tx.amountVnd.toDouble())}',
            style: TextStyle(
              color: isIn ? GreenColors.primaryEmerald : GreenColors.errorRed,
              fontWeight: FontWeight.bold,
              fontSize: 14,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildIncentiveTxRow(IncentiveLedgerEntryModel e) {
    final isIn = e.isCredit;
    final valStr = e.currency == 'PCC'
        ? GreenFormatters.pcc(e.amount)
        : GreenFormatters.ecoPoints(e.amount.round());
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: GreenColors.surfaceDark,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: GreenColors.cardBorder),
      ),
      child: Row(
        children: [
          Icon(Icons.eco_rounded, color: isIn ? GreenColors.primaryEmerald : GreenColors.accentGold, size: 20),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(e.description.isNotEmpty ? e.description : e.transactionType,
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                Text(e.currency, style: const TextStyle(color: GreenColors.textSecondary, fontSize: 11)),
              ],
            ),
          ),
          Text(
            '${isIn ? '+' : '-'}$valStr',
            style: TextStyle(
              color: isIn ? GreenColors.primaryEmerald : GreenColors.accentGold,
              fontWeight: FontWeight.bold,
              fontSize: 13,
            ),
          ),
        ],
      ),
    );
  }
}
