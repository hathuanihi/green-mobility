import 'package:flutter/material.dart';
import 'package:core_ui/core_ui.dart';

void main() {
  runApp(const GreenMobilityCustomerApp());
}

class GreenMobilityCustomerApp extends StatelessWidget {
  const GreenMobilityCustomerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Green Mobility - Khách hàng',
      debugShowCheckedModeBanner: false,
      theme: GreenTheme.darkTheme,
      home: const CustomerHomeScreen(),
    );
  }
}

class CustomerHomeScreen extends StatelessWidget {
  const CustomerHomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Green Mobility', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: GreenColors.backgroundDark,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.eco, color: GreenColors.primaryEmerald),
            onPressed: () {},
          ),
        ],
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Welcome Card
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFF065F46), Color(0xFF047857)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Chào bạn! 🌿',
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Cùng di chuyển xanh, giảm thiểu CO2 và tích lũy tín chỉ carbon hôm nay.',
                      style: TextStyle(fontSize: 14, color: Color(0xFFD1FAE5)),
                    ),
                    const SizedBox(height: 16),
                    ElevatedButton.icon(
                      onPressed: () {},
                      icon: const Icon(Icons.electric_scooter, color: GreenColors.backgroundDark),
                      label: const Text(
                        'Đặt xe điện ngay',
                        style: TextStyle(fontWeight: FontWeight.bold, color: GreenColors.backgroundDark),
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: GreenColors.primaryEmerald,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              const Text(
                'Tác động Môi trường của bạn',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: GreenColors.surfaceDark,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: Colors.white10),
                      ),
                      child: const Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('CO2 ĐÃ GIẢM', style: TextStyle(fontSize: 11, color: Colors.white54)),
                          SizedBox(height: 6),
                          Text('45.8 kg', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald)),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: GreenColors.surfaceDark,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: Colors.white10),
                      ),
                      child: const Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('TÍN CHỈ CARBON', style: TextStyle(fontSize: 11, color: Colors.white54)),
                          SizedBox(height: 6),
                          Text('45.8 PCC', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: GreenColors.electricCyan)),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
