import 'package:core_ui/core_ui.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('GreenFormatters', () {
    test('formats VND currency with thousand dots', () {
      expect(GreenFormatters.currencyVnd(76000), '76.000 đ');
      expect(GreenFormatters.currencyVnd(1250000, includeSymbol: false), '1.250.000');
    });

    test('formats PCC with 4 decimal places', () {
      expect(GreenFormatters.pcc(0.7532), '0.7532 PCC');
      expect(GreenFormatters.carbonCredits(753.2), '0.7532 PCC');
    });

    test('formats Eco-Points and CO2 cleanly', () {
      expect(GreenFormatters.ecoPoints(1250), '1.250 PTS');
      expect(GreenFormatters.co2(842), '842g CO2');
      expect(GreenFormatters.co2(1500), '1.50 kg CO2');
    });
  });
}
