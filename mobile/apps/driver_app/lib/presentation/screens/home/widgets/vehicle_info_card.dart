import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';

class VehicleInfoCard extends StatelessWidget {
  final VehicleModel? vehicle;
  final VoidCallback? onEditVehicle;

  const VehicleInfoCard({
    super.key,
    required this.vehicle,
    this.onEditVehicle,
  });

  @override
  Widget build(BuildContext context) {
    if (vehicle == null) {
      return Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: GreenColors.surfaceDark,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: Colors.white10),
        ),
        child: Row(
          children: [
            const Icon(Icons.electric_car_outlined, color: Colors.white38, size: 28),
            const SizedBox(width: 12),
            const Expanded(
              child: Text(
                'Chưa có thông tin xe điện liên kết',
                style: TextStyle(color: Colors.white54, fontSize: 13),
              ),
            ),
            if (onEditVehicle != null)
              TextButton(
                onPressed: onEditVehicle,
                child: const Text('Thêm xe', style: TextStyle(color: GreenColors.electricCyan)),
              ),
          ],
        ),
      );
    }

    final v = vehicle!;

    return GreenCard(
      title: '${v.make} ${v.model}',
      subtitle: VehicleTypes.getLabel(v.vehicleType),
      icon: v.vehicleType == VehicleTypes.electricMotorbike
          ? Icons.electric_moped_rounded
          : Icons.electric_car_rounded,
      trailing: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
          color: Colors.white10,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.white24),
        ),
        child: Text(
          v.licensePlate,
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
            color: Colors.white,
            letterSpacing: 0.8,
          ),
        ),
      ),
      child: Column(
        children: [
          const Divider(color: Colors.white10, height: 20),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _buildMetric('DUNG LƯỢNG PIN', '${v.batteryCapacityKwh} kWh', Icons.battery_charging_full_rounded),
              Container(width: 1, height: 28, color: Colors.white10),
              _buildMetric('TẦM HOẠT ĐỘNG', '${v.rangePerChargeKm} km', Icons.route_rounded),
              Container(width: 1, height: 28, color: Colors.white10),
              _buildMetric('MÀU SẮC', v.color, Icons.palette_outlined),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMetric(String label, String value, IconData icon) {
    return Column(
      children: [
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 12, color: Colors.white38),
            const SizedBox(width: 4),
            Text(label, style: const TextStyle(fontSize: 10, color: Colors.white38)),
          ],
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: const TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.bold,
            color: GreenColors.textPrimary,
          ),
        ),
      ],
    );
  }
}
