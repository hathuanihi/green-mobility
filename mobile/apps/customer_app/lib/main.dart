import 'package:flutter/material.dart';
import 'package:core_map/core_map.dart';
import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import 'data/repositories/auth_repository.dart';
import 'data/repositories/trip_repository.dart';
import 'presentation/screens/auth/customer_login_screen.dart';
import 'presentation/screens/booking/ride_booking_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  final tokenStorage = TokenStorage();
  final apiClient = ApiClient(tokenStorage: tokenStorage);
  final authApi = AuthApi(apiClient: apiClient);
  final tripApi = TripApi(apiClient: apiClient);

  final authRepository = AuthRepository(authApi: authApi, tokenStorage: tokenStorage);
  final tripRepository = TripRepository(tripApi: tripApi);

  runApp(GreenMobilityCustomerApp(
    tripRepository: tripRepository,
    authRepository: authRepository,
  ));
}

class GreenMobilityCustomerApp extends StatelessWidget {
  final TripRepository? tripRepository;
  final AuthRepository? authRepository;

  const GreenMobilityCustomerApp({
    super.key,
    this.tripRepository,
    this.authRepository,
  });

  @override
  Widget build(BuildContext context) {
    final tokenStorage = TokenStorage();
    final apiClient = ApiClient(tokenStorage: tokenStorage);
    final authRepo = authRepository ?? AuthRepository(authApi: AuthApi(apiClient: apiClient), tokenStorage: tokenStorage);
    final tripRepo = tripRepository ?? TripRepository(tripApi: TripApi(apiClient: apiClient));

    return MaterialApp(
      title: 'Green Mobility - Khách hàng',
      debugShowCheckedModeBanner: false,
      theme: GreenTheme.darkTheme,
      home: FutureBuilder<String?>(
        future: authRepo.checkToken(),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Scaffold(
              backgroundColor: GreenColors.backgroundDark,
              body: Center(
                child: CircularProgressIndicator(color: GreenColors.primaryEmerald),
              ),
            );
          }

          final token = snapshot.data;
          if (token != null && token.isNotEmpty) {
            return CustomerHomeScreen(
              tripRepository: tripRepo,
              authRepository: authRepo,
            );
          }

          return CustomerLoginScreen(
            authRepository: authRepo,
            tripRepository: tripRepo,
          );
        },
      ),
    );
  }
}

class CustomerHomeScreen extends StatelessWidget {
  final TripRepository tripRepository;
  final AuthRepository authRepository;

  const CustomerHomeScreen({
    super.key,
    required this.tripRepository,
    required this.authRepository,
  });

  void _openBooking(BuildContext context) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => RideBookingScreen(tripRepository: tripRepository),
      ),
    );
  }

  void _handleLogout(BuildContext context) async {
    await authRepository.logout();
    if (context.mounted) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => CustomerLoginScreen(
            authRepository: authRepository,
            tripRepository: tripRepository,
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Green Mobility', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: GreenColors.backgroundDark,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.logout_rounded, color: Colors.white60, size: 20),
            tooltip: 'Đăng xuất',
            onPressed: () => _handleLogout(context),
          ),
          IconButton(
            icon: const Icon(Icons.eco, color: GreenColors.primaryEmerald),
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Hệ sinh thái giao thông điện Green Mobility - Giảm phát thải ròng Net Zero'),
                  backgroundColor: GreenColors.primaryDark,
                ),
              );
            },
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
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
                  boxShadow: [
                    BoxShadow(
                      color: GreenColors.primaryEmerald.withValues(alpha: 0.2),
                      blurRadius: 16,
                      offset: const Offset(0, 4),
                    ),
                  ],
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
                      onPressed: () => _openBooking(context),
                      icon: const Icon(Icons.electric_scooter, color: GreenColors.backgroundDark),
                      label: const Text(
                        'Đặt xe điện ngay',
                        style: TextStyle(fontWeight: FontWeight.bold, color: GreenColors.backgroundDark),
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: GreenColors.primaryEmerald,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),

              // Carbon Impact Summary
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
              const SizedBox(height: 24),

              // Quick Book Destinations
              const Text(
                'Điểm đến đề xuất',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white),
              ),
              const SizedBox(height: 12),
              ...PresetLocation.all.map((loc) {
                return Padding(
                  padding: const EdgeInsets.only(bottom: 10.0),
                  child: _buildQuickDestinationTile(context, location: loc),
                );
              }),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildQuickDestinationTile(
    BuildContext context, {
    required PresetLocation location,
  }) {
    IconData icon = Icons.location_on;
    if (location.id == 'landmark_81') icon = Icons.apartment;
    if (location.id == 'tan_son_nhat') icon = Icons.flight_takeoff;
    if (location.id == 'ben_thanh') icon = Icons.store;
    if (location.id == 'bitexco') icon = Icons.domain;
    return GestureDetector(
      onTap: () => _openBooking(context),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: GreenColors.surfaceDark,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: GreenColors.cardBorder),
        ),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: GreenColors.primaryEmerald.withValues(alpha: 0.15),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(icon, color: GreenColors.primaryEmerald, size: 20),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(location.title, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: Colors.white)),
                  const SizedBox(height: 2),
                  Text(location.address, style: const TextStyle(fontSize: 11, color: GreenColors.textSecondary), maxLines: 1, overflow: TextOverflow.ellipsis),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Text('🌱 -${GreenFormatters.co2(location.co2EstimateGrams)}', style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: GreenColors.primaryEmerald)),
          ],
        ),
      ),
    );
  }
}
