import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import 'data/repositories/auth_repository.dart';
import 'data/repositories/trip_repository.dart';
import 'presentation/screens/auth/customer_login_screen.dart';
import 'presentation/screens/booking/ride_booking_screen.dart';
import 'presentation/screens/wallet/eco_store_screen.dart';
import 'presentation/screens/wallet/wallet_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  final tokenStorage = TokenStorage();
  final apiClient = ApiClient(tokenStorage: tokenStorage);
  final authRepository = AuthRepository(authApi: AuthApi(apiClient: apiClient), tokenStorage: tokenStorage);
  final tripRepository = TripRepository(tripApi: TripApi(apiClient: apiClient));

  runApp(GreenMobilityCustomerApp(
    tripRepository: tripRepository,
    authRepository: authRepository,
  ));
}

class GreenMobilityCustomerApp extends StatelessWidget {
  final TripRepository? tripRepository;
  final AuthRepository? authRepository;

  const GreenMobilityCustomerApp({super.key, this.tripRepository, this.authRepository});

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
              body: Center(child: CircularProgressIndicator(color: GreenColors.primaryEmerald)),
            );
          }
          final token = snapshot.data;
          if (token != null && token.isNotEmpty) {
            return CustomerHomeScreen(tripRepository: tripRepo, authRepository: authRepo);
          }
          return CustomerLoginScreen(authRepository: authRepo, tripRepository: tripRepo);
        },
      ),
    );
  }
}

class CustomerHomeScreen extends StatefulWidget {
  final TripRepository tripRepository;
  final AuthRepository authRepository;

  const CustomerHomeScreen({
    super.key,
    required this.tripRepository,
    required this.authRepository,
  });

  @override
  State<CustomerHomeScreen> createState() => _CustomerHomeScreenState();
}

class _CustomerHomeScreenState extends State<CustomerHomeScreen> {
  int _tabIndex = 0;
  late final WalletApi _walletApi;
  late final IncentiveApi _incentiveApi;
  late final CarbonApi _carbonApi;

  UserCarbonSummaryModel? _carbonSummary;
  IncentiveSummaryModel? _incentiveSummary;
  UserProfile? _profile;

  @override
  void initState() {
    super.initState();
    final apiClient = widget.tripRepository.tripApi.apiClient;
    _walletApi = WalletApi(apiClient: apiClient);
    _incentiveApi = IncentiveApi(apiClient: apiClient);
    _carbonApi = CarbonApi(apiClient: apiClient);
    _loadDashboard();
  }

  Future<void> _loadDashboard() async {
    try {
      final res = await Future.wait([
        widget.authRepository.authApi.getCurrentUser(),
        _carbonApi.getUserSummary(),
        _incentiveApi.getSummary(),
      ]);
      if (mounted) {
        setState(() {
          _profile = res[0] as UserProfile;
          _carbonSummary = res[1] as UserCarbonSummaryModel;
          _incentiveSummary = res[2] as IncentiveSummaryModel;
        });
      }
    } catch (_) {}
  }

  Future<void> _handleLogout() async {
    await widget.authRepository.logout();
    if (mounted) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => CustomerLoginScreen(
            authRepository: widget.authRepository,
            tripRepository: widget.tripRepository,
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final tabs = [
      _buildHomeTab(),
      WalletScreen(walletApi: _walletApi, incentiveApi: _incentiveApi),
      EcoStoreScreen(incentiveApi: _incentiveApi),
      _buildAccountTab(),
    ];

    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      body: IndexedStack(index: _tabIndex, children: tabs),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _tabIndex,
        onTap: (i) => setState(() => _tabIndex = i),
        type: BottomNavigationBarType.fixed,
        backgroundColor: GreenColors.surfaceDark,
        selectedItemColor: GreenColors.primaryEmerald,
        unselectedItemColor: GreenColors.textSecondary,
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.electric_scooter_rounded), label: 'Đặt xe'),
          BottomNavigationBarItem(icon: Icon(Icons.account_balance_wallet_rounded), label: 'Ví & Carbon'),
          BottomNavigationBarItem(icon: Icon(Icons.card_giftcard_rounded), label: 'Eco-Store'),
          BottomNavigationBarItem(icon: Icon(Icons.person_rounded), label: 'Tài khoản'),
        ],
      ),
    );
  }

  Widget _buildHomeTab() {
    final co2Grams = _carbonSummary?.totalCo2SavedGrams ?? 0.0;
    final pcc = _incentiveSummary?.pccBalance ?? 0.0;
    final pts = _incentiveSummary?.ecoPointsBalance ?? 0;

    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      appBar: AppBar(
        backgroundColor: GreenColors.backgroundDark,
        title: const Text('Green Mobility', style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          IconButton(icon: const Icon(Icons.refresh_rounded), onPressed: _loadDashboard),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadDashboard,
        child: ListView(
          padding: const EdgeInsets.all(20),
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
                  Text(
                    'Chào ${_profile?.fullName ?? 'bạn'}! 🌿',
                    style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Di chuyển thuần điện, giảm phát thải CO2 và nhận Tín chỉ Carbon.',
                    style: TextStyle(fontSize: 13, color: Color(0xFFD1FAE5)),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton.icon(
                    onPressed: () {
                      Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => RideBookingScreen(
                            tripRepository: widget.tripRepository,
                            authRepository: widget.authRepository,
                            incentiveApi: _incentiveApi,
                          ),
                        ),
                      );
                    },
                    icon: const Icon(Icons.electric_scooter, color: GreenColors.backgroundDark),
                    label: const Text('Đặt xe điện ngay', style: TextStyle(fontWeight: FontWeight.bold, color: GreenColors.backgroundDark)),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: GreenColors.primaryEmerald,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                _statCard('CO2 CẮT GIẢM', GreenFormatters.co2(co2Grams), GreenColors.primaryEmerald),
                const SizedBox(width: 12),
                _statCard('TÍN CHỈ CARBON', GreenFormatters.pcc(pcc), GreenColors.electricCyan),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                _statCard('ĐIỂM XANH', GreenFormatters.ecoPoints(pts), GreenColors.accentGold),
                const SizedBox(width: 12),
                _statCard('VOUCHER KHẢ DỤNG', '${_incentiveSummary?.availableVouchersCount ?? 0} mã', Colors.white),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _statCard(String label, String val, Color valueColor) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: GreenColors.surfaceDark,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: GreenColors.cardBorder),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: const TextStyle(fontSize: 10, color: GreenColors.textSecondary, fontWeight: FontWeight.w600)),
            const SizedBox(height: 6),
            Text(val, style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: valueColor)),
          ],
        ),
      ),
    );
  }

  Widget _buildAccountTab() {
    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      appBar: AppBar(backgroundColor: GreenColors.backgroundDark, title: const Text('Tài khoản')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: GreenColors.surfaceDark,
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: GreenColors.cardBorder),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(_profile?.fullName ?? 'Khách hàng Green Mobility',
                    style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
                const SizedBox(height: 4),
                Text(_profile?.phoneNumber ?? '', style: const TextStyle(color: GreenColors.textSecondary)),
                const SizedBox(height: 12),
                Text('Tổng số chuyến xanh: ${_carbonSummary?.totalTrips ?? 0}',
                    style: const TextStyle(color: GreenColors.primaryEmerald, fontWeight: FontWeight.w600)),
              ],
            ),
          ),
          const SizedBox(height: 20),
          ElevatedButton.icon(
            onPressed: _handleLogout,
            icon: const Icon(Icons.logout_rounded),
            label: const Text('Đăng xuất'),
            style: ElevatedButton.styleFrom(
              backgroundColor: GreenColors.errorRed,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 14),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
            ),
          ),
        ],
      ),
    );
  }
}
