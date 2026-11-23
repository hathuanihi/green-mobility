import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class ImpactReceiptScreen extends StatefulWidget {
  final String tripId;
  final CarbonApi carbonApi;
  final VoidCallback onDone;

  const ImpactReceiptScreen({
    super.key,
    required this.tripId,
    required this.carbonApi,
    required this.onDone,
  });

  @override
  State<ImpactReceiptScreen> createState() => _ImpactReceiptScreenState();
}

class _ImpactReceiptScreenState extends State<ImpactReceiptScreen> {
  TripImpactReceiptModel? _receipt;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadReceipt();
  }

  Future<void> _loadReceipt() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final receipt = await widget.carbonApi.getReceiptByTripId(widget.tripId);
      if (mounted) {
        setState(() {
          _receipt = receipt;
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _loading = false;
          _error = 'Hóa đơn Carbon đang được xử lý đồng bộ, vui lòng thử lại sau giây lát.';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      appBar: AppBar(
        backgroundColor: GreenColors.backgroundDark,
        title: const Text('Chứng nhận Tác động Xanh'),
        automaticallyImplyLeading: false,
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: _loading
              ? const Center(child: CircularProgressIndicator(color: GreenColors.primaryEmerald))
              : _receipt == null
                  ? Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.hourglass_top_rounded, color: GreenColors.accentGold, size: 56),
                        const SizedBox(height: 16),
                        Text(
                          _error ?? '',
                          textAlign: TextAlign.center,
                          style: const TextStyle(color: Colors.white70),
                        ),
                        const SizedBox(height: 20),
                        ElevatedButton(
                          onPressed: _loadReceipt,
                          style: ElevatedButton.styleFrom(backgroundColor: GreenColors.surfaceDark),
                          child: const Text('Tải lại hóa đơn'),
                        ),
                        const SizedBox(height: 10),
                        TextButton(
                          onPressed: widget.onDone,
                          child: const Text('Về Trang chủ', style: TextStyle(color: GreenColors.primaryEmerald)),
                        ),
                      ],
                    )
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Container(
                          padding: const EdgeInsets.all(22),
                          decoration: BoxDecoration(
                            gradient: const LinearGradient(
                              colors: [Color(0xFF064E3B), Color(0xFF0F172A)],
                              begin: Alignment.topLeft,
                              end: Alignment.bottomRight,
                            ),
                            borderRadius: BorderRadius.circular(24),
                            border: Border.all(color: GreenColors.primaryEmerald.withValues(alpha: 0.5)),
                          ),
                          child: Column(
                            children: [
                              const Icon(Icons.verified_rounded, color: GreenColors.primaryEmerald, size: 42),
                              const SizedBox(height: 8),
                              Text(
                                'CHUYẾN #${_receipt!.tripCode}',
                                style: const TextStyle(color: GreenColors.textSecondary, fontSize: 12),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                '-${GreenFormatters.co2(_receipt!.co2SavedGrams)}',
                                style: const TextStyle(
                                  fontSize: 30,
                                  fontWeight: FontWeight.w900,
                                  color: GreenColors.primaryEmerald,
                                ),
                              ),
                              const SizedBox(height: 10),
                              Wrap(
                                spacing: 10,
                                children: [
                                  _badge(GreenFormatters.pcc(_receipt!.carbonCreditsEarned), GreenColors.primaryEmerald),
                                  _badge('+${GreenFormatters.ecoPoints(_receipt!.loyaltyPointsEarned)}', GreenColors.accentGold),
                                ],
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 18),
                        Row(
                          children: [
                            _equivCard(Icons.forest_rounded, '${_receipt!.treeAbsorptionDaysEquiv.toStringAsFixed(1)} ngày', 'Cây xanh hấp thụ'),
                            const SizedBox(width: 10),
                            _equivCard(Icons.lightbulb_rounded, '${_receipt!.ledBulbHoursEquiv.toStringAsFixed(1)} giờ', 'Đèn LED 10W'),
                            const SizedBox(width: 10),
                            _equivCard(Icons.battery_charging_full, '${_receipt!.smartphoneChargesEquiv.toStringAsFixed(1)} lần', 'Sạc Smartphone'),
                          ],
                        ),
                        const SizedBox(height: 18),
                        OutlinedButton.icon(
                          onPressed: () {
                            Clipboard.setData(ClipboardData(text: _receipt!.shareableSlug));
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('Đã sao chép mã chứng nhận chia sẻ')),
                            );
                          },
                          icon: const Icon(Icons.share_rounded, color: GreenColors.electricCyan),
                          label: Text(
                            'Mã xác thực: ${_receipt!.shareableSlug}',
                            style: const TextStyle(color: Colors.white),
                          ),
                          style: OutlinedButton.styleFrom(
                            padding: const EdgeInsets.symmetric(vertical: 14),
                            side: const BorderSide(color: GreenColors.cardBorder),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                          ),
                        ),
                        const Spacer(),
                        ElevatedButton(
                          onPressed: widget.onDone,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: GreenColors.primaryEmerald,
                            foregroundColor: GreenColors.backgroundDark,
                            padding: const EdgeInsets.symmetric(vertical: 16),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                          ),
                          child: const Text('HOÀN TẤT', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                        ),
                      ],
                    ),
        ),
      ),
    );
  }

  Widget _badge(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.16),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Text(text, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 13)),
    );
  }

  Widget _equivCard(IconData icon, String value, String label) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 8),
        decoration: BoxDecoration(
          color: GreenColors.surfaceDark,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: GreenColors.cardBorder),
        ),
        child: Column(
          children: [
            Icon(icon, color: GreenColors.electricCyan, size: 22),
            const SizedBox(height: 6),
            Text(value, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
            const SizedBox(height: 2),
            Text(label, textAlign: TextAlign.center, style: const TextStyle(color: GreenColors.textSecondary, fontSize: 10)),
          ],
        ),
      ),
    );
  }
}
