// Centralized UI Formatters for Green Mobility

class GreenFormatters {
  /// Định dạng tiền tệ VND chuẩn (ví dụ: 60.800 đ)
  static String currencyVnd(double amount, {bool includeSymbol = true}) {
    final int val = amount.round();
    final s = val.toString();
    final reg = RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))');
    final formatted = s.replaceAllMapped(reg, (Match m) => '${m[1]}.');
    return includeSymbol ? '$formatted đ' : formatted;
  }

  /// Định dạng lượng phát thải CO2 (ví dụ: "817g CO2" hoặc "1.2 kg CO2")
  static String co2(double grams) {
    if (grams >= 1000) {
      return '${(grams / 1000).toStringAsFixed(1)} kg CO2';
    }
    return '${grams.round()}g CO2';
  }

  /// Tính và định dạng Tín chỉ Carbon cá nhân (PCC - 1 PCC = 1 kg CO2)
  static String carbonCredits(double co2Grams) {
    final credits = (co2Grams / 1000.0).toStringAsFixed(3);
    return '$credits PCC';
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
