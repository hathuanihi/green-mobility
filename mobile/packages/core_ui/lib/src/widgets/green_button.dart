import 'package:flutter/material.dart';
import '../../core_ui.dart';

enum GreenButtonVariant { primary, secondary, danger, outline }

class GreenButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final bool isLoading;
  final IconData? icon;
  final GreenButtonVariant variant;
  final double? width;
  final double height;
  final EdgeInsetsGeometry padding;

  const GreenButton({
    super.key,
    required this.label,
    this.onPressed,
    this.isLoading = false,
    this.icon,
    this.variant = GreenButtonVariant.primary,
    this.width = double.infinity,
    this.height = 52,
    this.padding = const EdgeInsets.symmetric(horizontal: 20),
  });

  @override
  Widget build(BuildContext context) {
    final isDisabled = onPressed == null || isLoading;

    Color bg;
    Color fg;
    BorderSide? border;

    switch (variant) {
      case GreenButtonVariant.primary:
        bg = isDisabled ? GreenColors.primaryEmerald.withValues(alpha: 0.4) : GreenColors.primaryEmerald;
        fg = Colors.white;
        break;
      case GreenButtonVariant.secondary:
        bg = isDisabled ? GreenColors.surfaceDark.withValues(alpha: 0.4) : GreenColors.surfaceDark;
        fg = GreenColors.textPrimary;
        border = const BorderSide(color: Colors.white24);
        break;
      case GreenButtonVariant.danger:
        bg = isDisabled ? GreenColors.errorRed.withValues(alpha: 0.4) : GreenColors.errorRed;
        fg = Colors.white;
        break;
      case GreenButtonVariant.outline:
        bg = Colors.transparent;
        fg = isDisabled ? Colors.white30 : GreenColors.primaryEmerald;
        border = BorderSide(
          color: isDisabled ? Colors.white24 : GreenColors.primaryEmerald,
          width: 1.5,
        );
        break;
    }

    Widget content = Row(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        if (isLoading)
          SizedBox(
            width: 20,
            height: 20,
            child: CircularProgressIndicator(
              strokeWidth: 2.2,
              valueColor: AlwaysStoppedAnimation<Color>(fg),
            ),
          )
        else ...[
          if (icon != null) ...[
            Icon(icon, size: 20, color: fg),
            const SizedBox(width: 8),
          ],
          Text(
            label,
            style: TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w600,
              color: fg,
              letterSpacing: 0.3,
            ),
          ),
        ],
      ],
    );

    return SizedBox(
      width: width,
      height: height,
      child: ElevatedButton(
        style: ElevatedButton.styleFrom(
          backgroundColor: bg,
          foregroundColor: fg,
          elevation: variant == GreenButtonVariant.primary && !isDisabled ? 3 : 0,
          shadowColor: GreenColors.primaryEmerald.withValues(alpha: 0.35),
          padding: padding,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
            side: border ?? BorderSide.none,
          ),
        ),
        onPressed: isDisabled ? null : onPressed,
        child: content,
      ),
    );
  }
}
