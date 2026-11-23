import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

class DriverCarbonScreen extends StatefulWidget {
  final IncentiveApi incentiveApi;

  const DriverCarbonScreen({super.key, required this.incentiveApi});

  @override
  State<DriverCarbonScreen> createState() => _DriverCarbonScreenState();
}

class _DriverCarbonScreenState extends State<DriverCarbonScreen> {
  IncentiveSummaryModel? _summary;
  List<IncentiveLedgerEntryModel> _entries = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final results = await Future.wait([
        widget.incentiveApi.getSummary(),
        widget.incentiveApi.getLedgerHistory(limit: 25),
      ]);
      if (mounted) {
        setState(() {
          _summary = results[0] as IncentiveSummaryModel;
          _entries = (results[1] as IncentiveLedgerPageModel).items;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showConvertDialog() {
    if (_summary == null) return;
    final ctrl = TextEditingController(text: _summary!.pccConvertMin.toStringAsFixed(2));
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
              const Text('Quy đổi PCC → Thu nhập Tài xế', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
              const SizedBox(height: 6),
              Text(
                'Tỷ giá trợ giá: ${GreenFormatters.currencyVnd(_summary!.pccToVndRateVnd.toDouble())}/PCC • Còn lại tháng này: ${GreenFormatters.pcc(_summary!.remainingMonthlyCap)}',
                style: const TextStyle(color: GreenColors.textSecondary, fontSize: 12),
              ),
              const SizedBox(height: 14),
              TextField(
                controller: ctrl,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(
                  labelText: 'Số lượng PCC muốn quy đổi',
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
                  onPressed: converting
                      ? null
                      : () async {
                          final amt = double.tryParse(ctrl.text.trim()) ?? 0.0;
                          if (amt <= 0) return;
                          setModalState(() => converting = true);
                          try {
                            final res = await widget.incentiveApi.convertPccToVnd(amt);
                            if (mounted) {
                              Navigator.pop(ctx);
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: Text('Đã cộng +${GreenFormatters.currencyVnd(res.vndCredited.toDouble())} vào ví Thu nhập tài xế!'),
                                  backgroundColor: GreenColors.primaryDark,
                                ),
                              );
                              await _loadData();
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
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  child: Text(converting ? 'Đang quy đổi...' : 'Quy đổi ngay', style: const TextStyle(fontWeight: FontWeight.bold)),
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
    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      appBar: AppBar(
        backgroundColor: GreenColors.backgroundDark,
        title: const Text('Carbon Xanh Tài xế', style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          IconButton(onPressed: _loadData, icon: const Icon(Icons.refresh_rounded)),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadData,
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
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text('TÍN CHỈ CARBON (PCC)', style: TextStyle(color: GreenColors.textSecondary, fontSize: 11)),
                                const SizedBox(height: 4),
                                Text(
                                  GreenFormatters.pcc(_summary?.pccBalance ?? 0.0),
                                  style: const TextStyle(color: GreenColors.primaryEmerald, fontSize: 22, fontWeight: FontWeight.w900),
                                ),
                              ],
                            ),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                const Text('ĐIỂM XANH', style: TextStyle(color: GreenColors.textSecondary, fontSize: 11)),
                                const SizedBox(height: 4),
                                Text(
                                  GreenFormatters.ecoPoints(_summary?.ecoPointsBalance ?? 0),
                                  style: const TextStyle(color: GreenColors.accentGold, fontSize: 20, fontWeight: FontWeight.bold),
                                ),
                              ],
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),
                        SizedBox(
                          width: double.infinity,
                          child: ElevatedButton.icon(
                            onPressed: (_summary?.pccFrozen == true || _summary?.redemptionsFrozen == true) ? null : _showConvertDialog,
                            icon: const Icon(Icons.currency_exchange_rounded),
                            label: const Text('Quy đổi PCC → Thu nhập (VNĐ)', style: TextStyle(fontWeight: FontWeight.bold)),
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
                  const Text('Lịch sử Sổ cái PCC & Điểm Xanh', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15)),
                  const SizedBox(height: 10),
                  ..._entries.map((e) => Container(
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
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(e.description.isNotEmpty ? e.description : e.transactionType,
                                      style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600)),
                                  Text(e.currency, style: const TextStyle(color: GreenColors.textSecondary, fontSize: 11)),
                                ],
                              ),
                            ),
                            Text(
                              '${e.isCredit ? '+' : '-'}${e.currency == 'PCC' ? GreenFormatters.pcc(e.amount) : GreenFormatters.ecoPoints(e.amount.round())}',
                              style: TextStyle(
                                color: e.isCredit ? GreenColors.primaryEmerald : GreenColors.accentGold,
                                fontWeight: FontWeight.bold,
                              ),
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
