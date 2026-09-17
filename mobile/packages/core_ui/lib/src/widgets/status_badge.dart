import 'package:flutter/material.dart';
import '../../core_ui.dart';

class StatusBadge extends StatelessWidget {
  final String status;
  final String? customLabel;
  final double fontSize;
  final EdgeInsetsGeometry padding;

  const StatusBadge({
    super.key,
    required this.status,
    this.customLabel,
    this.fontSize = 11,
    this.padding = const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
  });

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color fg;
    Border border;
    String label = customLabel ?? status;

    final normalized = status.toUpperCase();

    if (normalized == 'APPROVED' || normalized == 'ACTIVE') {
      bg = GreenColors.primaryEmerald.withValues(alpha: 0.16);
      fg = GreenColors.primaryEmerald;
      border = Border.all(color: GreenColors.primaryEmerald, width: 1);
      label = customLabel ?? 'ĐÃ PHÊ DUYỆT';
    } else if (normalized == 'PENDING') {
      bg = GreenColors.warningAmber.withValues(alpha: 0.16);
      fg = GreenColors.warningAmber;
      border = Border.all(color: GreenColors.warningAmber, width: 1);
      label = customLabel ?? 'CHỜ XÉT DUYỆT';
    } else if (normalized == 'REJECTED') {
      bg = GreenColors.errorRed.withValues(alpha: 0.16);
      fg = GreenColors.errorRed;
      border = Border.all(color: GreenColors.errorRed, width: 1);
      label = customLabel ?? 'BỊ TỪ CHỐI';
    } else {
      bg = Colors.white10;
      fg = Colors.white60;
      border = Border.all(color: Colors.white24, width: 1);
      label = customLabel ?? 'CHƯA NỘP HỒ SƠ';
    }

    return Container(
      padding: padding,
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(10),
        border: border,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 6,
            height: 6,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: fg,
            ),
          ),
          const SizedBox(width: 6),
          Text(
            label,
            style: TextStyle(
              fontSize: fontSize,
              fontWeight: FontWeight.bold,
              color: fg,
              letterSpacing: 0.4,
            ),
          ),
        ],
      ),
    );
  }
}
