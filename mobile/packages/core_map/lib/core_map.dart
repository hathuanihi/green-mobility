library core_map;

class MapDefaults {
  // Tọa độ mặc định: Trung tâm Quận 1, TP. Hồ Chí Minh
  static const double defaultLat = 10.776530;
  static const double defaultLng = 106.700981;
  static const double defaultZoom = 15.0;
}

class PresetLocation {
  final String id;
  final String title;
  final String address;
  final double lat;
  final double lng;
  final double co2EstimateGrams;

  const PresetLocation({
    required this.id,
    required this.title,
    required this.address,
    required this.lat,
    required this.lng,
    this.co2EstimateGrams = 800.0,
  });

  static const PresetLocation benThanh = PresetLocation(
    id: 'ben_thanh',
    title: 'Chợ Bến Thành',
    address: 'Chợ Bến Thành, Lê Lợi, Phường Bến Thành, Quận 1',
    lat: 10.7725,
    lng: 106.6980,
    co2EstimateGrams: 550,
  );

  static const PresetLocation landmark81 = PresetLocation(
    id: 'landmark_81',
    title: 'Landmark 81',
    address: 'Landmark 81, 720A Điện Biên Phủ, Phường 22, Bình Thạnh',
    lat: 10.7951,
    lng: 106.7218,
    co2EstimateGrams: 817,
  );

  static const PresetLocation bitexco = PresetLocation(
    id: 'bitexco',
    title: 'Bitexco Tower',
    address: 'Tòa nhà Bitexco, 2 Hải Triều, Bến Nghé, Quận 1',
    lat: 10.7716,
    lng: 106.7044,
    co2EstimateGrams: 620,
  );

  static const PresetLocation tanSonNhat = PresetLocation(
    id: 'tan_son_nhat',
    title: 'Sân bay Tân Sơn Nhất',
    address: 'Sân bay Quốc tế Tân Sơn Nhất (Ga T1), Trường Sơn, Tân Bình',
    lat: 10.8184,
    lng: 106.6588,
    co2EstimateGrams: 1250,
  );

  static const List<PresetLocation> all = [
    benThanh,
    landmark81,
    bitexco,
    tanSonNhat,
  ];
}
