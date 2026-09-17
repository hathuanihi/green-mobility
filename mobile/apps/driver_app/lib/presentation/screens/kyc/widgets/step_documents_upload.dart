import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

class StepDocumentsUpload extends StatelessWidget {
  final String? citizenFrontPath;
  final String? citizenBackPath;
  final String? licensePath;
  final String? registrationPath;
  final String? facePortraitPath;
  final void Function(String docKey) onPickDocument;

  const StepDocumentsUpload({
    super.key,
    required this.citizenFrontPath,
    required this.citizenBackPath,
    required this.licensePath,
    required this.registrationPath,
    required this.facePortraitPath,
    required this.onPickDocument,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Bước 3: Chụp ảnh Giấy tờ & Sinh trắc học',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            'Chụp rõ nét 5 loại ảnh minh chứng dưới đây để quản trị viên phê duyệt hồ sơ',
            style: TextStyle(fontSize: 13, color: GreenColors.textSecondary),
          ),
          const SizedBox(height: 16),

          // 1. Mặt trước CCCD
          DocumentPickerTile(
            title: '1. Mặt trước Căn cước công dân',
            subtitle: 'Chụp rõ 4 góc, đủ thông tin số CCCD và họ tên',
            imagePath: citizenFrontPath,
            onTap: () => onPickDocument('citizenFront'),
          ),
          const SizedBox(height: 12),

          // 2. Mặt sau CCCD
          DocumentPickerTile(
            title: '2. Mặt sau Căn cước công dân',
            subtitle: 'Chụp rõ mã vạch chip / dấu vân tay',
            imagePath: citizenBackPath,
            onTap: () => onPickDocument('citizenBack'),
          ),
          const SizedBox(height: 12),

          // 3. Giấy phép lái xe
          DocumentPickerTile(
            title: '3. Giấy phép lái xe (GPLX)',
            subtitle: 'Mặt trước GPLX còn nguyên vẹn và còn thời hạn',
            imagePath: licensePath,
            onTap: () => onPickDocument('license'),
          ),
          const SizedBox(height: 12),

          // 4. Đăng ký xe
          DocumentPickerTile(
            title: '4. Giấy đăng ký xe (Cà vẹt xe điện)',
            subtitle: 'Chụp rõ biển số, số khung và nhãn hiệu xe',
            imagePath: registrationPath,
            onTap: () => onPickDocument('registration'),
          ),
          const SizedBox(height: 12),

          // 5. Ảnh chân dung sinh trắc học
          DocumentPickerTile(
            title: '5. Ảnh chân dung chuẩn sinh trắc học',
            subtitle: 'Chính diện khuôn mặt, không đeo kính râm hay khẩu trang (dùng so khớp bật ca)',
            imagePath: facePortraitPath,
            onTap: () => onPickDocument('facePortrait'),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }
}
