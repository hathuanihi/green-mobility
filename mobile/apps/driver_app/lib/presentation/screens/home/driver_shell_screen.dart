import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../data/repositories/driver_repository.dart';
import '../../bloc/auth/auth_cubit.dart';
import '../../bloc/auth/auth_state.dart';
import '../earnings/earnings_screen.dart';
import '../wallet/driver_carbon_screen.dart';
import 'driver_home_screen.dart';

class DriverShellScreen extends StatefulWidget {
  const DriverShellScreen({super.key});

  @override
  State<DriverShellScreen> createState() => _DriverShellScreenState();
}

class _DriverShellScreenState extends State<DriverShellScreen> {
  int _tabIndex = 0;

  @override
  Widget build(BuildContext context) {
    final apiClient = context.read<DriverRepository>().driverApi.apiClient;
    final earningsApi = EarningsApi(apiClient: apiClient);
    final walletApi = WalletApi(apiClient: apiClient);
    final incentiveApi = IncentiveApi(apiClient: apiClient);

    final pages = [
      const DriverHomeScreen(),
      EarningsScreen(earningsApi: earningsApi, walletApi: walletApi),
      DriverCarbonScreen(incentiveApi: incentiveApi),
      _buildAccountTab(context),
    ];

    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      body: IndexedStack(index: _tabIndex, children: pages),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _tabIndex,
        onTap: (i) => setState(() => _tabIndex = i),
        type: BottomNavigationBarType.fixed,
        backgroundColor: GreenColors.surfaceDark,
        selectedItemColor: GreenColors.primaryEmerald,
        unselectedItemColor: GreenColors.textSecondary,
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.electric_moped_rounded), label: 'Nhận chuyến'),
          BottomNavigationBarItem(icon: Icon(Icons.account_balance_wallet_rounded), label: 'Thu nhập'),
          BottomNavigationBarItem(icon: Icon(Icons.eco_rounded), label: 'Carbon'),
          BottomNavigationBarItem(icon: Icon(Icons.person_rounded), label: 'Tài khoản'),
        ],
      ),
    );
  }

  Widget _buildAccountTab(BuildContext context) {
    return Scaffold(
      backgroundColor: GreenColors.backgroundDark,
      appBar: AppBar(backgroundColor: GreenColors.backgroundDark, title: const Text('Tài khoản Tài xế')),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: BlocBuilder<AuthCubit, AuthState>(
          builder: (ctx, authState) {
            final name = authState is Authenticated ? authState.user.fullName : 'Tài xế Green Mobility';
            final phone = authState is Authenticated ? authState.user.phoneNumber : '';
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
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
                      Text(name, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 18)),
                      const SizedBox(height: 4),
                      Text(phone, style: const TextStyle(color: GreenColors.textSecondary)),
                    ],
                  ),
                ),
                const SizedBox(height: 20),
                ElevatedButton.icon(
                  onPressed: () => context.read<AuthCubit>().logout(),
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
            );
          },
        ),
      ),
    );
  }
}
