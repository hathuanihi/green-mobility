import 'package:flutter/material.dart';
import '../../core_ui.dart';

class GreenTextField extends StatelessWidget {
  final String label;
  final String? hint;
  final TextEditingController? controller;
  final String? initialValue;
  final bool obscureText;
  final TextInputType? keyboardType;
  final IconData? prefixIcon;
  final Widget? suffixIcon;
  final String? Function(String?)? validator;
  final void Function(String)? onChanged;
  final bool readOnly;
  final bool enabled;
  final int maxLines;
  final String? helperText;

  const GreenTextField({
    super.key,
    required this.label,
    this.hint,
    this.controller,
    this.initialValue,
    this.obscureText = false,
    this.keyboardType,
    this.prefixIcon,
    this.suffixIcon,
    this.validator,
    this.onChanged,
    this.readOnly = false,
    this.enabled = true,
    this.maxLines = 1,
    this.helperText,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w500,
            color: GreenColors.textSecondary,
          ),
        ),
        const SizedBox(height: 6),
        TextFormField(
          controller: controller,
          initialValue: initialValue,
          obscureText: obscureText,
          keyboardType: keyboardType,
          validator: validator,
          onChanged: onChanged,
          readOnly: readOnly,
          enabled: enabled,
          maxLines: maxLines,
          style: const TextStyle(
            fontSize: 15,
            color: GreenColors.textPrimary,
            fontWeight: FontWeight.w400,
          ),
          decoration: InputDecoration(
            hintText: hint,
            hintStyle: const TextStyle(color: Colors.white30, fontSize: 14),
            helperText: helperText,
            helperStyle: const TextStyle(color: Colors.white38, fontSize: 11),
            prefixIcon: prefixIcon != null
                ? Icon(prefixIcon, color: GreenColors.textSecondary, size: 20)
                : null,
            suffixIcon: suffixIcon,
            filled: true,
            fillColor: GreenColors.surfaceDark,
            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: Colors.white12),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: Colors.white12),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: GreenColors.primaryEmerald, width: 1.5),
            ),
            errorBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: GreenColors.errorRed, width: 1.2),
            ),
            focusedErrorBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: GreenColors.errorRed, width: 1.5),
            ),
          ),
        ),
      ],
    );
  }
}
