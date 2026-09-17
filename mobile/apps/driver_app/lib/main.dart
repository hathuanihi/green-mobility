import 'package:core_network/core_network.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'data/repositories/auth_repository.dart';
import 'data/repositories/driver_repository.dart';
import 'presentation/bloc/auth/auth_cubit.dart';
import 'presentation/bloc/auth/auth_state.dart';
import 'presentation/bloc/driver/driver_cubit.dart';
import 'presentation/screens/auth/login_screen.dart';
import 'presentation/screens/home/driver_home_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Network & Repositories
  final tokenStorage = TokenStorage();
  final apiClient = ApiClient(tokenStorage: tokenStorage);
  final authApi = AuthApi(apiClient: apiClient);
  final driverApi = DriverApi(apiClient: apiClient);

  final authRepository = AuthRepository(
    authApi: authApi,
    tokenStorage: tokenStorage,
  );
  final driverRepository = DriverRepository(
    driverApi: driverApi,
  );

  runApp(
    MultiRepositoryProvider(
      providers: [
        RepositoryProvider.value(value: authRepository),
        RepositoryProvider.value(value: driverRepository),
      ],
      child: MultiBlocProvider(
        providers: [
          BlocProvider(
            create: (context) => AuthCubit(authRepository: authRepository)..checkAuthStatus(),
          ),
          BlocProvider(
            create: (context) => DriverCubit(driverRepository: driverRepository),
          ),
        ],
        child: const GreenMobilityDriverApp(),
      ),
    ),
  );
}

class GreenMobilityDriverApp extends StatelessWidget {
  const GreenMobilityDriverApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Green Mobility - Đối tác Tài xế',
      debugShowCheckedModeBanner: false,
      theme: GreenTheme.darkTheme,
      home: BlocBuilder<AuthCubit, AuthState>(
        builder: (context, state) {
          if (state is Authenticated) {
            return const DriverHomeScreen();
          }
          if (state is AuthInitial || state is AuthLoading) {
            return const Scaffold(
              backgroundColor: GreenColors.backgroundDark,
              body: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.electric_scooter_rounded, size: 56, color: GreenColors.primaryEmerald),
                    SizedBox(height: 16),
                    CircularProgressIndicator(
                      valueColor: AlwaysStoppedAnimation<Color>(GreenColors.primaryEmerald),
                    ),
                  ],
                ),
              ),
            );
          }
          return const LoginScreen();
        },
      ),
    );
  }
}
