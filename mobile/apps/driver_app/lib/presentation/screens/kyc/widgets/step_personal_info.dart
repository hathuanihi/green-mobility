import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

class StepPersonalInfo extends StatelessWidget {
  final TextEditingController fullNameController;
  final TextEditingController citizenIdController;
  final TextEditingController licenseNumberController;
  final String selectedLicenseClass;
  final ValueChanged<String> onLicenseClassChanged;

  const StepPersonalInfo({
    super.key,
    required this.fullNameController,
    required this.citizenIdController,
    required this.licenseNumberController,
    required this.selectedLicenseClass,
    required this.onLicenseClassChanged,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Bước 1: Thông tin cá nhân & Giấy tờ',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            'Vui lòng nhập chính xác thông tin theo Căn cước công dân và Giấy phép lái xe',
            style: TextStyle(fontSize: 13, color: GreenColors.textSecondary),
          ),
          const SizedBox(height: 20),
          GreenCard(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                GreenTextField(
                  label: 'Họ và tên tài xế',
                  hint: 'Nguyễn Văn A',
                  controller: fullNameController,
                  prefixIcon: Icons.badge_outlined,
                ),
                const SizedBox(height: 16),
                GreenTextField(
                  label: 'Số Căn cước công dân (12 số)',
                  hint: '001xxxxxxxx',
                  controller: citizenIdController,
                  keyboardType: TextInputType.number,
                  prefixIcon: Icons.credit_card_outlined,
                ),
                const SizedBox(height: 16),
                GreenTextField(
                  label: 'Số Giấy phép lái xe (GPLX)',
                  hint: 'Số GPLX còn hiệu lực',
                  controller: licenseNumberController,
                  prefixIcon: Icons.drive_eta_outlined,
                ),
                const SizedBox(height: 16),
                const Text(
                  'Hạng Giấy phép lái xe',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                    color: GreenColors.textSecondary,
                  ),
                ),
                const SizedBox(height: 6),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  decoration: BoxDecoration(
                    color: GreenColors.surfaceDark,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.white12),
                  ),
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton<String>(
                      value: selectedLicenseClass,
                      isExpanded: true,
                      dropdownColor: GreenColors.surfaceDark,
                      items: LicenseClasses.all.map((code) {
                        return DropdownMenuItem<String>(
                          value: code,
                          child: Text(
                            'Hạng $code',
                            style: const TextStyle(color: GreenColors.textPrimary, fontSize: 14),
                          ),
                        );
                      }).toList(),
                      onChanged: (val) {
                        if (val != null) onLicenseClassChanged(val);
                      },
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
