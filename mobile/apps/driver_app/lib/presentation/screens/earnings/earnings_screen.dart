import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import '../wallet/driver_wallet_screen.dart';

class EarningsScreen extends StatefulWidget {
  final EarningsApi earningsApi;
  final WalletApi? walletApi;

  const EarningsScreen({
    super.key,
    required this.earningsApi,
    this.walletApi,
  });

  @override
  State<EarningsScreen> createState() => _EarningsScreenState();
}

class _EarningsScreenState extends State<EarningsScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  DriverEarningsSummaryModel? _summary;
  List<DriverTripEarningModel> _trips = [];
  bool _isLoading = true;
  String _currentPeriod = 'TODAY';

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    _tabController.addListener(() {
      if (!_tabController.indexIsChanging) {
        setState(() {
          _currentPeriod = _tabController.index == 0
              ? 'TODAY'
              : (_tabController.index == 1 ? 'WEEK' : 'MONTH');
        });
        _loadData();
      }
    });
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final results = await Future.wait([
        widget.earningsApi.getSummary(period: _currentPeriod),
        widget.earningsApi.getTrips(),
      ]);
      if (mounted) {
        setState(() {
          _summary = results[0] as DriverEarningsSummaryModel;
          _trips = results[1] as List<DriverTripEarningModel>;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final walletApi = widget.walletApi ?? WalletApi(apiClient: widget.earningsApi.apiClient);
    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      appBar: AppBar(
        backgroundColor: GreenColors.backgroundDark,
        title: const Text('Thu nhập tài xế', style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          TextButton.icon(
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => DriverWalletScreen(walletApi: walletApi, earningsApi: widget.earningsApi),
                ),
              );
            },
            icon: const Icon(Icons.account_balance_wallet_rounded, color: GreenColors.primaryEmerald, size: 18),
            label: const Text('Ví & Rút tiền', style: TextStyle(color: GreenColors.primaryEmerald, fontWeight: FontWeight.bold)),
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: GreenColors.primaryEmerald,
          labelColor: GreenColors.primaryEmerald,
          unselectedLabelColor: GreenColors.textSecondary,
          tabs: const [
            Tab(text: 'Hôm nay'),
            Tab(text: 'Tuần này'),
            Tab(text: 'Tháng này'),
          ],
        ),
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
                      gradient: const LinearGradient(colors: [Color(0xFF065F46), Color(0xFF047857)]),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Thu nhập ròng thực nhận', style: TextStyle(color: Color(0xFFD1FAE5), fontSize: 13)),
                        const SizedBox(height: 6),
                        Text(
                          GreenFormatters.currencyVnd((_summary?.netEarningsVnd ?? 0).toDouble()),
                          style: const TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.w900),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text('Tổng cước: ${GreenFormatters.currencyVnd((_summary?.totalGrossVnd ?? 0).toDouble())}',
                                style: const TextStyle(color: Color(0xFFA7F3D0), fontSize: 12)),
                            Text('${_summary?.tripCount ?? 0} cuốc',
                                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12)),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 14),
                  Row(
                    children: [
                      _statBox('Hoa hồng sàn', GreenFormatters.currencyVnd((_summary?.totalCommissionVnd ?? 0).toDouble()), GreenColors.accentGold),
                      const SizedBox(width: 10),
                      _statBox('Nợ tiền mặt', GreenFormatters.currencyVnd((_summary?.cashDebtRemainingVnd ?? 0).toDouble()), GreenColors.errorRed),
                    ],
                  ),
                  const SizedBox(height: 18),
                  const Text('Lịch sử chuyến gần đây', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15)),
                  const SizedBox(height: 10),
                  if (_trips.isEmpty)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 40),
                      child: Center(
                        child: Text('Chưa có cuốc xe nào trong khoảng thời gian này', style: TextStyle(color: GreenColors.textSecondary)),
                      ),
                    )
                  else
                    ..._trips.map((trip) => Container(
                          margin: const EdgeInsets.only(bottom: 10),
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            color: GreenColors.surfaceDark,
                            borderRadius: BorderRadius.circular(14),
                            border: Border.all(color: GreenColors.cardBorder),
                          ),
                          child: Row(
                            children: [
                              Container(
                                width: 40,
                                height: 40,
                                decoration: BoxDecoration(
                                  color: trip.isCash
                                      ? GreenColors.accentGold.withValues(alpha: 0.15)
                                      : GreenColors.primaryEmerald.withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(10),
                                ),
                                child: Icon(
                                  trip.isCash ? Icons.payments_rounded : Icons.electric_car_rounded,
                                  color: trip.isCash ? GreenColors.accentGold : GreenColors.primaryEmerald,
                                  size: 20,
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text('Chuyến #${trip.tripCode}',
                                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                    Text(trip.paymentMethod,
                                        style: const TextStyle(fontSize: 12, color: GreenColors.textSecondary)),
                                  ],
                                ),
                              ),
                              Text(
                                '+${GreenFormatters.currencyVnd(trip.netEarnedVnd.toDouble())}',
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: GreenColors.primaryEmerald),
                              ),
                            ],
                          ),
                        )),
                ],
              ),
      ),
    );
  }

  Widget _statBox(String label, String val, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: GreenColors.surfaceDark,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: GreenColors.cardBorder),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: const TextStyle(color: GreenColors.textSecondary, fontSize: 11)),
            const SizedBox(height: 4),
            Text(val, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 14)),
          ],
        ),
      ),
    );
  }
}
