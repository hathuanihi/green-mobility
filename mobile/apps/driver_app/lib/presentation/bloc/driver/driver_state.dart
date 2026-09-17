import 'package:core_model/core_model.dart';

abstract class DriverState {
  const DriverState();
}

class DriverInitial extends DriverState {
  const DriverInitial();
}

class DriverLoading extends DriverState {
  const DriverLoading();
}

class DriverLoaded extends DriverState {
  final DriverProfile profile;
  const DriverLoaded(this.profile);
}

class DriverKycSubmitting extends DriverState {
  const DriverKycSubmitting();
}

class DriverKycSuccess extends DriverState {
  final DriverProfile profile;
  final String message;
  const DriverKycSuccess({required this.profile, required this.message});
}

class DriverError extends DriverState {
  final String message;
  const DriverError(this.message);
}
