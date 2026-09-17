import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

class DriverStatsCard extends StatelessWidget {
  final double totalCo2SavedKg;
  final int totalTrips;
  final double ratingAvg;

  const DriverStatsCard({
    super.key,
    required this.totalCo2SavedKg,
    required this.totalTrips,
    required this.ratingAvg,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        // CO2 Saved Card
        Expanded(
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: GreenColors.surfaceDark,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: Colors.white10),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.eco_rounded, size: 16, color: GreenColors.primaryEmerald),
                    SizedBox(width: 4),
                    Text('CO2 GIẢM PHÁT THẢI', style: TextStyle(fontSize: 10, color: Colors.white54, fontWeight: FontWeight.bold)),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  '${totalCo2SavedKg.toStringAsFixed(1)} kg',
                  style: const TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: GreenColors.primaryEmerald,
                  ),
                ),
                const SizedBox(height: 4),
                const Text('Tích lũy từ xe điện', style: TextStyle(fontSize: 11, color: Colors.white38)),
              ],
            ),
          ),
        ),
        const SizedBox(width: 12),
        // Trips & Rating Card
        Expanded(
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: GreenColors.surfaceDark,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: Colors.white10),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.star_rounded, size: 16, color: GreenColors.accentGold),
                    SizedBox(width: 4),
                    Text('ĐÁNH GIÁ & CUỐC', style: TextStyle(fontSize: 10, color: Colors.white54, fontWeight: FontWeight.bold)),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Text(
                      ratingAvg.toStringAsFixed(1),
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                        color: GreenColors.accentGold,
                      ),
                    ),
                    const Text(' ★', style: TextStyle(color: GreenColors.accentGold, fontSize: 16)),
                    const Spacer(),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: Colors.white10,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        '$totalTrips chuyến',
                        style: const TextStyle(fontSize: 11, color: Colors.white70),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                const Text('Tổng chuyến hoàn thành', style: TextStyle(fontSize: 11, color: Colors.white38)),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
