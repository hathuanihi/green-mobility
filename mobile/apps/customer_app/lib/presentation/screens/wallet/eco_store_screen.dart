import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

class EcoStoreScreen extends StatefulWidget {
  final IncentiveApi incentiveApi;

  const EcoStoreScreen({super.key, required this.incentiveApi});

  @override
  State<EcoStoreScreen> createState() => _EcoStoreScreenState();
}

class _EcoStoreScreenState extends State<EcoStoreScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  bool _isLoading = true;
  bool _isMutating = false;
  String _selectedCategory = 'ALL';

  IncentiveSummaryModel? _summary;
  List<RewardCatalogItemModel> _catalog = [];
  List<RedemptionItemModel> _myRedemptions = [];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
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
        widget.incentiveApi.getSummary(),
        widget.incentiveApi.getCatalog(
          category: _selectedCategory == 'ALL' ? null : _selectedCategory,
        ),
        widget.incentiveApi.getMyRedemptions(),
      ]);
      if (mounted) {
        setState(() {
          _summary = results[0] as IncentiveSummaryModel;
          _catalog = results[1] as List<RewardCatalogItemModel>;
          _myRedemptions = results[2] as List<RedemptionItemModel>;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        _showSnack(e.toString().replaceFirst('Exception: ', ''), isError: true);
      }
    }
  }

  Future<void> _redeem(RewardCatalogItemModel item) async {
    if (_isMutating) return;
    setState(() => _isMutating = true);
    try {
      final res = await widget.incentiveApi.redeemReward(item.id);
      if (mounted) {
        _showSnack('Đổi thành công "${item.title}"! Mã: ${res.redemptionCode}');
        await _loadData();
        _tabController.animateTo(1);
      }
    } catch (e) {
      if (mounted) {
        _showSnack(e.toString().replaceFirst('Exception: ', ''), isError: true);
      }
    } finally {
      if (mounted) setState(() => _isMutating = false);
    }
  }

  Future<void> _cancelRedemption(RedemptionItemModel item) async {
    if (_isMutating) return;
    setState(() => _isMutating = true);
    try {
      await widget.incentiveApi.cancelRedemption(item.id, reason: 'Khách hàng hủy đổi ưu đãi');
      if (mounted) {
        _showSnack('Đã hủy ưu đãi ${item.redemptionCode} và hoàn trả PCC / Điểm Xanh');
        await _loadData();
      }
    } catch (e) {
      if (mounted) {
        _showSnack(e.toString().replaceFirst('Exception: ', ''), isError: true);
      }
    } finally {
      if (mounted) setState(() => _isMutating = false);
    }
  }

  void _showSnack(String msg, {bool isError = false}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(msg),
        backgroundColor: isError ? GreenColors.errorRed : GreenColors.primaryDark,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      appBar: AppBar(
        backgroundColor: GreenColors.backgroundDark,
        title: const Text('Eco-Store Xanh', style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          IconButton(
            onPressed: _loadData,
            icon: const Icon(Icons.refresh_rounded, color: Colors.white70),
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: GreenColors.primaryEmerald,
          labelColor: GreenColors.primaryEmerald,
          unselectedLabelColor: GreenColors.textSecondary,
          tabs: [
            const Tab(text: 'Cửa hàng Ưu đãi'),
            Tab(text: 'Ưu đãi của tôi (${_myRedemptions.length})'),
          ],
        ),
      ),
      body: Column(
        children: [
          if (_summary != null)
            Container(
              margin: const EdgeInsets.fromLTRB(16, 12, 16, 4),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: GreenColors.surfaceDark,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: GreenColors.cardBorder),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.eco_rounded, color: GreenColors.primaryEmerald, size: 18),
                      const SizedBox(width: 6),
                      Text(
                        GreenFormatters.pcc(_summary!.pccBalance),
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
                      ),
                    ],
                  ),
                  Row(
                    children: [
                      const Icon(Icons.stars_rounded, color: GreenColors.accentGold, size: 18),
                      const SizedBox(width: 6),
                      Text(
                        GreenFormatters.ecoPoints(_summary!.ecoPointsBalance),
                        style: const TextStyle(color: GreenColors.accentGold, fontWeight: FontWeight.bold, fontSize: 13),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                _buildCatalogTab(),
                _buildMyRedemptionsTab(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCatalogTab() {
    final cats = [
      ('ALL', 'Tất cả'),
      ('VOUCHER_RIDE', 'Voucher Xe'),
      ('TREE_PLANTING', 'Trồng cây'),
      ('ECO_PRODUCT', 'Quà Đối tác'),
    ];
    return Column(
      children: [
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          child: Row(
            children: cats.map((c) {
              final active = _selectedCategory == c.$1;
              return Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: Text(c.$2),
                  selected: active,
                  selectedColor: GreenColors.primaryEmerald.withValues(alpha: 0.25),
                  backgroundColor: GreenColors.surfaceDark,
                  labelStyle: TextStyle(
                    color: active ? GreenColors.primaryEmerald : GreenColors.textSecondary,
                    fontWeight: FontWeight.w600,
                    fontSize: 12,
                  ),
                  side: BorderSide(color: active ? GreenColors.primaryEmerald : GreenColors.cardBorder),
                  onSelected: (_) {
                    setState(() => _selectedCategory = c.$1);
                    _loadData();
                  },
                ),
              );
            }).toList(),
          ),
        ),
        Expanded(
          child: _isLoading
              ? const Center(child: CircularProgressIndicator(color: GreenColors.primaryEmerald))
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: _catalog.length,
                    itemBuilder: (ctx, i) => _buildRewardCard(_catalog[i]),
                  ),
                ),
        ),
      ],
    );
  }

  Widget _buildRewardCard(RewardCatalogItemModel item) {
    final IconData catIcon = item.category == 'TREE_PLANTING'
        ? Icons.forest_rounded
        : item.category == 'ECO_PRODUCT'
            ? Icons.local_cafe_rounded
            : Icons.confirmation_number_rounded;

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: GreenColors.surfaceDark,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: GreenColors.cardBorder),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: GreenColors.primaryEmerald.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(catIcon, color: GreenColors.primaryEmerald, size: 22),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.title,
                      style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      '${item.partnerName} • Còn ${item.quantityAvailable} suất',
                      style: const TextStyle(color: GreenColors.textSecondary, fontSize: 12),
                    ),
                  ],
                ),
              ),
            ],
          ),
          if (item.description.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text(item.description, style: const TextStyle(color: Colors.white70, fontSize: 12)),
          ],
          const SizedBox(height: 14),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Wrap(
                spacing: 8,
                children: [
                  if (item.carbonCreditCost > 0)
                    _costChip(GreenFormatters.pcc(item.carbonCreditCost), GreenColors.primaryEmerald),
                  if (item.loyaltyPointCost > 0)
                    _costChip(GreenFormatters.ecoPoints(item.loyaltyPointCost), GreenColors.accentGold),
                ],
              ),
              ElevatedButton(
                onPressed: _isMutating || item.quantityAvailable <= 0 ? null : () => _redeem(item),
                style: ElevatedButton.styleFrom(
                  backgroundColor: GreenColors.primaryEmerald,
                  foregroundColor: GreenColors.backgroundDark,
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: Text(
                  item.quantityAvailable <= 0 ? 'Hết lượt' : 'Đổi ngay',
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _costChip(String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.14),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: color.withValues(alpha: 0.35)),
      ),
      child: Text(label, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 12)),
    );
  }

  Widget _buildMyRedemptionsTab() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator(color: GreenColors.primaryEmerald));
    }
    if (_myRedemptions.isEmpty) {
      return const Center(
        child: Text('Bạn chưa đổi quà hay voucher nào', style: TextStyle(color: GreenColors.textSecondary)),
      );
    }
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _myRedemptions.length,
        itemBuilder: (ctx, i) {
          final rd = _myRedemptions[i];
          final canCancel = (rd.status == 'AVAILABLE' || rd.status == 'ISSUED' || rd.status == 'COMPLETED') &&
              rd.category != 'TREE_PLANTING';
          return Container(
            margin: const EdgeInsets.only(bottom: 12),
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: GreenColors.surfaceDark,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: GreenColors.cardBorder),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Text(
                        rd.rewardTitle,
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14),
                      ),
                    ),
                    _costChip(
                      rd.status,
                      rd.status == 'AVAILABLE'
                          ? GreenColors.primaryEmerald
                          : rd.status == 'USED'
                              ? GreenColors.electricCyan
                              : GreenColors.textSecondary,
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  'Mã: ${rd.redemptionCode} • ${rd.partnerName}',
                  style: const TextStyle(color: GreenColors.electricCyan, fontFamily: 'monospace', fontSize: 12),
                ),
                if (canCancel) ...[
                  const SizedBox(height: 10),
                  Align(
                    alignment: Alignment.centerRight,
                    child: OutlinedButton(
                      onPressed: _isMutating ? null : () => _cancelRedemption(rd),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: GreenColors.errorRed,
                        side: BorderSide(color: GreenColors.errorRed.withValues(alpha: 0.5)),
                      ),
                      child: const Text('Hủy & Hoàn PCC/PTS', style: TextStyle(fontSize: 12)),
                    ),
                  ),
                ],
              ],
            ),
          );
        },
      ),
    );
  }
}
