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
  final String subtitle;
  final String defaultBatteryKwh;
  final String defaultRangeKm;
  final String icon;
  final double baseFareVnd;
  final double perKmFareVnd;
  final double co2SavedPerKmGrams;

  const VehicleTypeOption({
    required this.code,
    required this.label,
    required this.subtitle,
    required this.defaultBatteryKwh,
    required this.defaultRangeKm,
    required this.icon,
    this.baseFareVnd = 12000,
    this.perKmFareVnd = 4500,
    this.co2SavedPerKmGrams = 151,
  });

  double calculateFare(double distanceKm) => baseFareVnd + (distanceKm * perKmFareVnd);
  double calculateCo2Saved(double distanceKm) => distanceKm * co2SavedPerKmGrams;
}

class VehicleTypes {
  static const String electricMotorbike = 'ELECTRIC_MOTORBIKE';
  static const String electricCar4Seat = 'ELECTRIC_CAR_4SEAT';
  static const String electricCar7Seat = 'ELECTRIC_CAR_7SEAT';

  static const List<VehicleTypeOption> options = [
    VehicleTypeOption(
      code: electricMotorbike,
      label: 'E-Bike (Xe máy điện)',
      subtitle: 'VinFast Feliz S / Klara S • Di chuyển linh hoạt',
      defaultBatteryKwh: '3.5',
      defaultRangeKm: '150',
      icon: 'two_wheeler',
      baseFareVnd: 12000,
      perKmFareVnd: 4500,
      co2SavedPerKmGrams: 151,
    ),
    VehicleTypeOption(
      code: electricCar4Seat,
      label: 'E-Car 4S (Ô tô điện 4 chỗ)',
      subtitle: 'VinFast VF e34 / VF 5 • Êm ái, hiện đại',
      defaultBatteryKwh: '42.0',
      defaultRangeKm: '300',
      icon: 'directions_car',
      baseFareVnd: 20000,
      perKmFareVnd: 8500,
      co2SavedPerKmGrams: 181,
    ),
    VehicleTypeOption(
      code: electricCar7Seat,
      label: 'E-Car 7S (Ô tô điện 7 chỗ)',
      subtitle: 'VinFast VF 8 / VF 9 • Rộng rãi, cao cấp',
      defaultBatteryKwh: '87.7',
      defaultRangeKm: '420',
      icon: 'airport_shuttle',
      baseFareVnd: 32000,
      perKmFareVnd: 14000,
      co2SavedPerKmGrams: 231,
    ),
  ];

  static VehicleTypeOption getOption(String? code) {
    for (final opt in options) {
      if (opt.code == code) return opt;
    }
    return options.first;
  }

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

class TripStatus {
  static const String requested = 'REQUESTED';
  static const String searching = 'SEARCHING';
  static const String matched = 'MATCHED';
  static const String driverArriving = 'DRIVER_ARRIVING';
  static const String arrived = 'ARRIVED';
  static const String inTrip = 'IN_TRIP';
  static const String completed = 'COMPLETED';
  static const String cancelled = 'CANCELLED';

  static String getLabel(String? status) {
    switch (status) {
      case requested:
        return 'Đã gửi yêu cầu';
      case searching:
        return 'Đang tìm tài xế';
      case matched:
        return 'Đã ghép tài xế';
      case driverArriving:
        return 'Tài xế đang đến';
      case arrived:
        return 'Tài xế đã đến điểm đón';
      case inTrip:
        return 'Đang di chuyển';
      case completed:
        return 'Hoàn thành';
      case cancelled:
        return 'Đã hủy';
      default:
        return status ?? 'Chưa xác định';
    }
  }

  static bool isActive(String? status) {
    return status == requested ||
        status == searching ||
        status == matched ||
        status == driverArriving ||
        status == arrived ||
        status == inTrip;
  }
}

class PaymentMethod {
  static const String cash = 'CASH';
  static const String wallet = 'WALLET';
  static const String card = 'CARD';

  static String getLabel(String? method) {
    switch (method) {
      case cash:
        return 'Tiền mặt';
      case wallet:
        return 'Ví điện tử';
      case card:
        return 'Thẻ ngân hàng';
      default:
        return method ?? 'Tiền mặt';
    }
  }
}

class PaymentStatus {
  static const String pending = 'PENDING';
  static const String paid = 'PAID';
  static const String refunded = 'REFUNDED';
}
