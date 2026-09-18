import 'package:core_model/core_model.dart';
import 'package:core_network/core_network.dart';

class TripRepository {
  final TripApi tripApi;

  TripRepository({required this.tripApi});

  Future<TripEstimateModel> estimateTrip(TripEstimateRequestModel request) async {
    return await tripApi.estimateTrip(request);
  }

  Future<TripModel> requestTrip(TripRequestModel request) async {
    return await tripApi.requestTrip(request);
  }

  Future<TripModel> getTripDetails(String tripId) async {
    return await tripApi.getTripDetails(tripId);
  }

  Future<TripModel> cancelTrip(String tripId, {String? reason}) async {
    return await tripApi.cancelTrip(tripId, reason: reason);
  }
}
