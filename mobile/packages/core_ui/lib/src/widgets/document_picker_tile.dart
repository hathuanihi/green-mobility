import 'dart:io';
import 'package:flutter/material.dart';
import '../../core_ui.dart';

class DocumentPickerTile extends StatelessWidget {
  final String title;
  final String subtitle;
  final String? imagePath;
  final VoidCallback onTap;
  final bool isRequired;

  const DocumentPickerTile({
    super.key,
    required this.title,
    required this.subtitle,
    required this.imagePath,
    required this.onTap,
    this.isRequired = true,
  });

  @override
  Widget build(BuildContext context) {
    final hasImage = imagePath != null && imagePath!.isNotEmpty;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        height: 120,
        decoration: BoxDecoration(
          color: GreenColors.surfaceDark,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: hasImage ? GreenColors.primaryEmerald : Colors.white12,
            width: hasImage ? 1.5 : 1,
          ),
        ),
        clipBehavior: Clip.antiAlias,
        child: Row(
          children: [
            // Left: Thumbnail preview or placeholder
            Container(
              width: 120,
              height: double.infinity,
              color: Colors.black26,
              child: hasImage
                  ? Image.file(
                      File(imagePath!),
                      fit: BoxFit.cover,
                      errorBuilder: (ctx, err, stack) => const Center(
                        child: Icon(Icons.broken_image, color: Colors.white38),
                      ),
                    )
                  : Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            Icons.add_a_photo_outlined,
                            size: 28,
                            color: GreenColors.primaryEmerald.withValues(alpha: 0.8),
                          ),
                          const SizedBox(height: 4),
                          const Text(
                            'Chụp / Chọn',
                            style: TextStyle(fontSize: 10, color: Colors.white54),
                          ),
                        ],
                      ),
                    ),
            ),
            // Right: Document title, subtitle, status
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            title,
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                              color: GreenColors.textPrimary,
                            ),
                          ),
                        ),
                        if (isRequired)
                          const Text(
                            '*',
                            style: TextStyle(color: GreenColors.errorRed, fontSize: 16),
                          ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      subtitle,
                      style: const TextStyle(fontSize: 11, color: GreenColors.textSecondary),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Icon(
                          hasImage ? Icons.check_circle : Icons.radio_button_unchecked,
                          size: 14,
                          color: hasImage ? GreenColors.primaryEmerald : Colors.white38,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          hasImage ? 'Đã tải ảnh' : 'Chưa tải lên',
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w500,
                            color: hasImage ? GreenColors.primaryEmerald : Colors.white38,
                          ),
                        ),
                        const Spacer(),
                        Text(
                          hasImage ? 'Đổi ảnh' : 'Chọn ảnh',
                          style: const TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                            color: GreenColors.electricCyan,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
