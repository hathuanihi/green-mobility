import 'package:flutter/material.dart';
import 'package:core_ui/core_ui.dart';

void main() {
  runApp(const GreenMobilityDriverApp());
}

class GreenMobilityDriverApp extends StatelessWidget {
  const GreenMobilityDriverApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Green Mobility - Tài xế',
      debugShowCheckedModeBanner: false,
      theme: GreenTheme.darkTheme,
      home: const DriverHomeScreen(),
    );
  }
}

class DriverHomeScreen extends StatefulWidget {
  const DriverHomeScreen({super.key});

  @override
  State<DriverHomeScreen> createState() => _DriverHomeScreenState();
}

class _DriverHomeScreenState extends State<DriverHomeScreen> {
  bool isOnline = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Green Driver', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: GreenColors.backgroundDark,
        elevation: 0,
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16.0),
            child: Center(
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: isOnline ? GreenColors.primaryEmerald.withOpacity(0.2) : Colors.white10,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: isOnline ? GreenColors.primaryEmerald : Colors.white24,
                  ),
                ),
                child: Text(
                  isOnline ? 'TRỰC TUYẾN' : 'NGOẠI TUYẾN',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    color: isOnline ? GreenColors.primaryEmerald : Colors.white60,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            children: [
              // Shift switch card
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: GreenColors.surfaceDark,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: Colors.white10),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          isOnline ? 'Sẵn sàng nhận cuốc xe' : 'Chưa bật ca làm việc',
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.white),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          isOnline ? 'Đang kết nối WebSocket GPS' : 'Gạt để xác thực khuôn mặt và bắt đầu',
                          style: const TextStyle(fontSize: 12, color: Colors.white54),
                        ),
                      ],
                    ),
                    Switch(
                      value: isOnline,
                      activeColor: GreenColors.primaryEmerald,
                      onChanged: (val) {
                        setState(() {
                          isOnline = val;
                        });
                      },
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              // Income & Green Contribution Cards
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
                          Text('THU NHẬP HÔM NAY', style: TextStyle(fontSize: 11, color: Colors.white54)),
                          SizedBox(height: 6),
                          Text('385.000 đ', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: GreenColors.accentGold)),
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
                          Text('CO2 GIÚP GIẢM', style: TextStyle(fontSize: 11, color: Colors.white54)),
                          SizedBox(height: 6),
                          Text('14.2 kg', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald)),
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
