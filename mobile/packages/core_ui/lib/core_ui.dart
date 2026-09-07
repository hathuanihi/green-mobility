library core_ui;

import 'package:flutter/material.dart';

class GreenColors {
  static const Color primaryEmerald = Color(0xFF10B981);
  static const Color primaryDark = Color(0xFF047857);
  static const Color electricCyan = Color(0xFF06B6D4);
  static const Color backgroundDark = Color(0xFF0F172A);
  static const Color surfaceDark = Color(0xFF1E293B);
  static const Color textPrimary = Color(0xFFF8FAFC);
  static const Color textSecondary = Color(0xFF94A3B8);
  static const Color accentGold = Color(0xFFF59E0B);
}

class GreenTheme {
  static ThemeData get darkTheme {
    return ThemeData(
      brightness: Brightness.dark,
      primaryColor: GreenColors.primaryEmerald,
      scaffoldBackgroundColor: GreenColors.backgroundDark,
      cardColor: GreenColors.surfaceDark,
      colorScheme: const ColorScheme.dark(
        primary: GreenColors.primaryEmerald,
        secondary: GreenColors.electricCyan,
        surface: GreenColors.surfaceDark,
      ),
      useMaterial3: true,
    );
  }
}
