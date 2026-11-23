import 'package:core_model/core_model.dart';
import 'package:dio/dio.dart';
import 'api_client.dart';

class IncentiveApi {
  final ApiClient apiClient;

  IncentiveApi({required this.apiClient});

  Future<IncentiveSummaryModel> getSummary() async {
    final response = await apiClient.dio.get('/incentive/summary');
    final data = response.data['data'] as Map<String, dynamic>;
    return IncentiveSummaryModel.fromJson(data);
  }

  Future<IncentiveLedgerPageModel> getLedgerHistory({
    String? currency,
    String? type,
    String? cursor,
    int limit = 20,
  }) async {
    final query = <String, dynamic>{'limit': limit};
    if (currency != null && currency.isNotEmpty) query['currency'] = currency;
    if (type != null && type.isNotEmpty) query['type'] = type;
    if (cursor != null && cursor.isNotEmpty) query['cursor'] = cursor;

    final response = await apiClient.dio.get(
      '/incentive/ledger',
      queryParameters: query,
    );
    final data = response.data['data'] as Map<String, dynamic>;
    return IncentiveLedgerPageModel.fromJson(data);
  }

  Future<List<RewardCatalogItemModel>> getCatalog({
    String? category,
    int page = 0,
    int size = 30,
  }) async {
    final query = <String, dynamic>{'page': page, 'size': size};
    if (category != null && category.isNotEmpty && category != 'ALL') {
      query['category'] = category;
    }
    final response = await apiClient.dio.get(
      '/incentive/catalog',
      queryParameters: query,
    );
    final data = response.data['data'];
    final List<dynamic> list = data is Map<String, dynamic> && data['content'] is List
        ? data['content'] as List<dynamic>
        : (data is List ? data : []);
    return list
        .map((e) => RewardCatalogItemModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<RedemptionItemModel>> getMyRedemptions({
    String? status,
    int page = 0,
    int size = 30,
  }) async {
    final query = <String, dynamic>{'page': page, 'size': size};
    if (status != null && status.isNotEmpty) query['status'] = status;
    final response = await apiClient.dio.get(
      '/incentive/redemptions',
      queryParameters: query,
    );
    final data = response.data['data'];
    final List<dynamic> list = data is Map<String, dynamic> && data['content'] is List
        ? data['content'] as List<dynamic>
        : (data is List ? data : []);
    return list
        .map((e) => RedemptionItemModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<VoucherPreviewModel> previewVoucher({
    String? voucherRedemptionId,
    String? voucherCode,
    required double grossFareVnd,
  }) async {
    final query = <String, dynamic>{'grossFareVnd': grossFareVnd};
    if (voucherRedemptionId != null) {
      query['voucherRedemptionId'] = voucherRedemptionId;
    }
    if (voucherCode != null && voucherCode.isNotEmpty) {
      query['voucherCode'] = voucherCode;
    }
    final response = await apiClient.dio.get(
      '/incentive/vouchers/preview',
      queryParameters: query,
    );
    return VoucherPreviewModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  Future<RedemptionItemModel> redeemReward(
    String rewardId, {
    String? idempotencyKey,
  }) async {
    final key = idempotencyKey ?? ApiClient.generateIdempotencyKey();
    final response = await apiClient.dio.post(
      '/incentive/redeem',
      data: {'rewardId': rewardId},
      options: Options(headers: {'Idempotency-Key': key}),
    );
    return RedemptionItemModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  Future<RedemptionItemModel> cancelRedemption(
    String redemptionId, {
    String? reason,
  }) async {
    final response = await apiClient.dio.post(
      '/incentive/redemptions/$redemptionId/cancel',
      data: reason != null ? {'reason': reason} : {},
    );
    return RedemptionItemModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  Future<ConvertPccResultModel> convertPccToVnd(
    double pccAmount, {
    String? idempotencyKey,
  }) async {
    final key = idempotencyKey ?? ApiClient.generateIdempotencyKey();
    final response = await apiClient.dio.post(
      '/incentive/carbon/convert',
      data: {'pccAmount': pccAmount},
      options: Options(headers: {'Idempotency-Key': key}),
    );
    return ConvertPccResultModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
