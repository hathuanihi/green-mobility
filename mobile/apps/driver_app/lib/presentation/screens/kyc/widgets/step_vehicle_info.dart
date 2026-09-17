import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

class StepVehicleInfo extends StatelessWidget {
  final String selectedVehicleType;
  final ValueChanged<String> onVehicleTypeChanged;
  final TextEditingController makeController;
  final TextEditingController modelController;
  final TextEditingController licensePlateController;
  final TextEditingController colorController;
  final TextEditingController batteryController;
  final TextEditingController rangeController;
  final TextEditingController expiryDateController;
  final VoidCallback onPickExpiryDate;

  const StepVehicleInfo({
    super.key,
    required this.selectedVehicleType,
    required this.onVehicleTypeChanged,
    required this.makeController,
    required this.modelController,
    required this.licensePlateController,
    required this.colorController,
    required this.batteryController,
    required this.rangeController,
    required this.expiryDateController,
    required this.onPickExpiryDate,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Bước 2: Thông số Phương tiện Xe điện',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            'Thông tin chính xác của xe điện sẽ được dùng để đối chiếu cà vẹt và tính toán giảm phát thải CO2',
            style: TextStyle(fontSize: 13, color: GreenColors.textSecondary),
          ),
          const SizedBox(height: 20),
          GreenCard(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Loại phương tiện xe điện',
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
                      value: selectedVehicleType,
                      isExpanded: true,
                      dropdownColor: GreenColors.surfaceDark,
                      items: VehicleTypes.options.map((opt) {
                        return DropdownMenuItem<String>(
                          value: opt.code,
                          child: Text(
                            opt.label,
                            style: const TextStyle(color: GreenColors.textPrimary, fontSize: 13),
                          ),
                        );
                      }).toList(),
                      onChanged: (val) {
                        if (val != null) onVehicleTypeChanged(val);
                      },
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: GreenTextField(
                        label: 'Hãng xe',
                        hint: 'VinFast / Dat Bike',
                        controller: makeController,
                        prefixIcon: Icons.business_outlined,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: GreenTextField(
                        label: 'Dòng xe / Model',
                        hint: 'Feliz S / VF e34',
                        controller: modelController,
                        prefixIcon: Icons.model_training,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: GreenTextField(
                        label: 'Biển số xe',
                        hint: '29A-123.45',
                        controller: licensePlateController,
                        prefixIcon: Icons.confirmation_number_outlined,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: GreenTextField(
                        label: 'Màu sơn',
                        hint: 'Xanh lục / Đen',
                        controller: colorController,
                        prefixIcon: Icons.color_lens_outlined,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: GreenTextField(
                        label: 'Pin (kWh)',
                        hint: 'Ví dụ: 3.5',
                        controller: batteryController,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        prefixIcon: Icons.battery_charging_full_outlined,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: GreenTextField(
                        label: 'Tầm hoạt động (km)',
                        hint: 'Ví dụ: 150',
                        controller: rangeController,
                        keyboardType: TextInputType.number,
                        prefixIcon: Icons.speed_outlined,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                GreenTextField(
                  label: 'Hạn kiểm định / Đăng kiểm',
                  hint: 'YYYY-MM-DD (VD: 2027-12-31)',
                  controller: expiryDateController,
                  readOnly: true,
                  prefixIcon: Icons.calendar_today_outlined,
                  suffixIcon: IconButton(
                    icon: const Icon(Icons.edit_calendar, color: GreenColors.primaryEmerald),
                    onPressed: onPickExpiryDate,
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
