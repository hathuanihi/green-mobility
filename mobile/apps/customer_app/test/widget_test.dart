import 'package:core_model/core_model.dart';
import 'package:customer_app/main.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('Customer app smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const GreenMobilityCustomerApp());
    await tester.pump(const Duration(milliseconds: 500));
    expect(find.byType(GreenMobilityCustomerApp), findsOneWidget);
  });

  group('Sprint 6 core_model serialization', () {
    test('parses IncentiveSummaryModel cleanly', () {
      final model = IncentiveSummaryModel.fromJson({
        'pccBalance': 12.3456,
        'pccFrozen': false,
        'ecoPointsBalance': 240,
        'monthlyPccConverted': 10.5,
        'monthlyPccConvertCap': 100.0,
        'pccConvertMin': 0.5,
        'pccToVndRateVnd': 500,
        'redemptionsFrozen': false,
        'availableVouchersCount': 3,
        'lifetimeCo2SavedGrams': 18500.0,
      });

      expect(model.pccBalance, 12.3456);
      expect(model.ecoPointsBalance, 240);
      expect(model.remainingMonthlyCap, 89.5);
      expect(model.pccToVndRateVnd, 500);
    });

    test('parses WalletBalanceModel holdVnd and DriverEarningsSummaryModel grossFareVnd', () {
      final wallet = WalletBalanceModel.fromJson({
        'balanceVnd': 500000,
        'holdVnd': 100000,
        'cashDebtVnd': 20000,
        'cashTripsAllowed': true,
      });
      expect(wallet.pendingHoldVnd, 100000);
      expect(wallet.availableBalanceVnd, 500000);

      final earn = DriverEarningsSummaryModel.fromJson({
        'grossFareVnd': 100000,
        'commissionVnd': 20000,
        'netEarningsVnd': 80000,
        'onlineCollectedVnd': 80000,
        'cashCollectedVnd': 0,
        'cashDebtRemainingVnd': 0,
        'tripCount': 2,
      });
      expect(earn.totalGrossVnd, 100000);
      expect(earn.totalCommissionVnd, 20000);
      expect(earn.netEarningsVnd, 80000);
    });
  });
}
