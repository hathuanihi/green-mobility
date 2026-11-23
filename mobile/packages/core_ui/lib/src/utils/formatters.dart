// Centralized UI Formatters for Green Mobility

class GreenFormatters {
  /// Định dạng tiền tệ VND chuẩn (ví dụ: 60.800 đ)
  static String currencyVnd(double amount, {bool includeSymbol = true}) {
    final int val = amount.round();
    final bool neg = val < 0;
    final s = val.abs().toString();
    final reg = RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))');
    final formatted = (neg ? '-' : '') + s.replaceAllMapped(reg, (Match m) => '${m[1]}.');
    return includeSymbol ? '$formatted đ' : formatted;
  }

  /// Định dạng lượng phát thải CO2 (ví dụ: "842g CO2" hoặc "1.50 kg CO2")
  static String co2(double grams) {
    if (grams >= 1000) {
      return '${(grams / 1000).toStringAsFixed(2)} kg CO2';
    }
    return '${grams.round()}g CO2';
  }

  /// Định dạng trực tiếp số dư Tín chỉ Carbon (4 chữ số thập phân)
  static String pcc(double credits) {
    return '${credits.toStringAsFixed(4)} PCC';
  }

  /// Quy đổi gram CO2 -> Tín chỉ Carbon cá nhân (1 PCC = 1,000g CO2)
  static String carbonCredits(double co2Grams) {
    return pcc(co2Grams / 1000.0);
  }

  /// Định dạng Điểm Xanh (Eco-Points)
  static String ecoPoints(int pts) {
    final s = pts.toString();
    final reg = RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))');
    final formatted = s.replaceAllMapped(reg, (Match m) => '${m[1]}.');
    return '$formatted PTS';
  }

  /// Định dạng khoảng cách (ví dụ: "5.4 km" hoặc "800 m")
  static String distance(double distanceKm) {
    if (distanceKm < 1.0) {
      return '${(distanceKm * 1000).round()} m';
    }
    return '${distanceKm.toStringAsFixed(1)} km';
  }

  /// Định dạng thời gian di chuyển (ví dụ: "14 phút")
  static String duration(int minutes) {
    if (minutes >= 60) {
      final hours = minutes ~/ 60;
      final remaining = minutes % 60;
      return remaining > 0 ? '$hours giờ $remaining phút' : '$hours giờ';
    }
    return '$minutes phút';
  }
}
