// Centralized Constants & Enums for Green Mobility Mobile

class KycStatus {
  static const String notSubmitted = 'NOT_SUBMITTED';
  static const String pending = 'PENDING';
  static const String approved = 'APPROVED';
  static const String rejected = 'REJECTED';

  static String getLabel(String? status) {
    switch (status) {
      case approved:
        return 'Đã phê duyệt';
      case pending:
        return 'Chờ xét duyệt';
      case rejected:
        return 'Bị từ chối';
      case notSubmitted:
      default:
        return 'Chưa nộp hồ sơ';
    }
  }
}

class VehicleTypeOption {
  final String code;
  final String label;
  final String defaultBatteryKwh;
  final String defaultRangeKm;
  final String icon;

  const VehicleTypeOption({
    required this.code,
    required this.label,
    required this.defaultBatteryKwh,
    required this.defaultRangeKm,
    required this.icon,
  });
}

class VehicleTypes {
  static const String electricMotorbike = 'ELECTRIC_MOTORBIKE';
  static const String electricCar4Seat = 'ELECTRIC_CAR_4SEAT';
  static const String electricCar7Seat = 'ELECTRIC_CAR_7SEAT';

  static const List<VehicleTypeOption> options = [
    VehicleTypeOption(
      code: electricMotorbike,
      label: 'Xe máy điện (VinFast/Dat Bike)',
      defaultBatteryKwh: '3.5',
      defaultRangeKm: '150',
      icon: 'two_wheeler',
    ),
    VehicleTypeOption(
      code: electricCar4Seat,
      label: 'Ô tô điện 4 chỗ (VF e34 / VF 5 / VF 6)',
      defaultBatteryKwh: '42.0',
      defaultRangeKm: '300',
      icon: 'directions_car',
    ),
    VehicleTypeOption(
      code: electricCar7Seat,
      label: 'Ô tô điện 7 chỗ (VF 8 / VF 9)',
      defaultBatteryKwh: '87.7',
      defaultRangeKm: '420',
      icon: 'airport_shuttle',
    ),
  ];

  static String getLabel(String? code) {
    for (final opt in options) {
      if (opt.code == code) return opt.label;
    }
    return code ?? 'Chưa xác định';
  }
}

class LicenseClasses {
  static const List<String> motorbikeClasses = ['A1', 'A2'];
  static const List<String> carClasses = ['B1', 'B2', 'C', 'D', 'E'];
  static const List<String> all = ['A1', 'A2', 'B1', 'B2', 'C', 'D', 'E'];
}

class UserRole {
  static const String driver = 'ROLE_DRIVER';
  static const String customer = 'ROLE_CUSTOMER';
  static const String admin = 'ROLE_ADMIN';
}
