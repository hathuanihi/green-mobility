library core_ui;

import 'package:flutter/material.dart';

export 'src/widgets/green_button.dart';
export 'src/widgets/green_text_field.dart';
export 'src/widgets/green_card.dart';
export 'src/widgets/status_badge.dart';
export 'src/widgets/document_picker_tile.dart';
export 'src/utils/formatters.dart';

class GreenColors {
  static const Color primaryEmerald = Color(0xFF10B981);
  static const Color primaryDark = Color(0xFF047857);
  static const Color electricCyan = Color(0xFF06B6D4);
  static const Color backgroundDark = Color(0xFF0F172A);
  static const Color surfaceDark = Color(0xFF1E293B);
  static const Color cardDark = Color(0xFF162032);
  static const Color cardBorder = Color(0xFF334155);
  static const Color textPrimary = Color(0xFFF8FAFC);
  static const Color textSecondary = Color(0xFF94A3B8);
  static const Color accentGold = Color(0xFFF59E0B);
  static const Color warningAmber = Color(0xFFF59E0B);
  static const Color errorRed = Color(0xFFEF4444);
}

class GreenTheme {
  static ThemeData get darkTheme {
    return ThemeData(
      brightness: Brightness.dark,
      primaryColor: GreenColors.primaryEmerald,
      scaffoldBackgroundColor: GreenColors.backgroundDark,
      cardColor: GreenColors.surfaceDark,
      canvasColor: GreenColors.backgroundDark,
      colorScheme: const ColorScheme.dark(
        primary: GreenColors.primaryEmerald,
        secondary: GreenColors.electricCyan,
        surface: GreenColors.surfaceDark,
        error: GreenColors.errorRed,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: GreenColors.backgroundDark,
        elevation: 0,
        centerTitle: false,
        iconTheme: IconThemeData(color: Colors.white),
        titleTextStyle: TextStyle(
          color: Colors.white,
          fontSize: 18,
          fontWeight: FontWeight.bold,
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: GreenColors.surfaceDark,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      ),
      useMaterial3: true,
    );
  }
}
