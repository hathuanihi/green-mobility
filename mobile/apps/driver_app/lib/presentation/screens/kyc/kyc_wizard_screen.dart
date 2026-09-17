import 'package:core_model/core_model.dart';
import 'package:core_ui/core_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:image_picker/image_picker.dart';
import '../../bloc/driver/driver_cubit.dart';
import '../../bloc/driver/driver_state.dart';
import 'widgets/step_personal_info.dart';
import 'widgets/step_vehicle_info.dart';
import 'widgets/step_documents_upload.dart';

class KycWizardScreen extends StatefulWidget {
  final DriverProfile? initialProfile;

  const KycWizardScreen({super.key, this.initialProfile});

  @override
  State<KycWizardScreen> createState() => _KycWizardScreenState();
}

class _KycWizardScreenState extends State<KycWizardScreen> {
  int _currentStep = 0;
  final _picker = ImagePicker();

  // Step 1 Controllers
  final _fullNameController = TextEditingController();
  final _citizenIdController = TextEditingController();
  final _licenseNumberController = TextEditingController();
  String _selectedLicenseClass = 'A1';

  // Step 2 Controllers
  String _selectedVehicleType = VehicleTypes.electricMotorbike;
  final _makeController = TextEditingController(text: 'VinFast');
  final _modelController = TextEditingController(text: 'Feliz S');
  final _licensePlateController = TextEditingController();
  final _colorController = TextEditingController(text: 'Xanh lục');
  final _batteryController = TextEditingController(text: '3.5');
  final _rangeController = TextEditingController(text: '150');
  final _expiryDateController = TextEditingController(text: '2027-12-31');

  // Step 3 Document Image Paths
  String? _citizenFrontPath;
  String? _citizenBackPath;
  String? _licensePath;
  String? _registrationPath;
  String? _facePortraitPath;

  @override
  void initState() {
    super.initState();
    final p = widget.initialProfile;
    if (p != null) {
      _fullNameController.text = p.fullName;
      _citizenIdController.text = p.citizenId ?? '';
      _licenseNumberController.text = p.driverLicenseNumber ?? '';
      if (p.licenseClass != null && LicenseClasses.all.contains(p.licenseClass)) {
        _selectedLicenseClass = p.licenseClass!;
      }
      if (p.vehicle != null) {
        _selectedVehicleType = p.vehicle!.vehicleType;
        _makeController.text = p.vehicle!.make;
        _modelController.text = p.vehicle!.model;
        _licensePlateController.text = p.vehicle!.licensePlate;
        _colorController.text = p.vehicle!.color;
        _batteryController.text = p.vehicle!.batteryCapacityKwh.toString();
        _rangeController.text = p.vehicle!.rangePerChargeKm.toString();
        _expiryDateController.text = p.vehicle!.inspectionExpiryDate ?? '2027-12-31';
      }
    }
  }

  @override
  void dispose() {
    _fullNameController.dispose();
    _citizenIdController.dispose();
    _licenseNumberController.dispose();
    _makeController.dispose();
    _modelController.dispose();
    _licensePlateController.dispose();
    _colorController.dispose();
    _batteryController.dispose();
    _rangeController.dispose();
    _expiryDateController.dispose();
    super.dispose();
  }

  void _onVehicleTypeChanged(String type) {
    setState(() {
      _selectedVehicleType = type;
      // Auto suggest battery and range
      for (final opt in VehicleTypes.options) {
        if (opt.code == type) {
          _batteryController.text = opt.defaultBatteryKwh;
          _rangeController.text = opt.defaultRangeKm;
          if (type == VehicleTypes.electricMotorbike) {
            _makeController.text = 'VinFast';
            _modelController.text = 'Feliz S';
            _selectedLicenseClass = 'A1';
          } else {
            _makeController.text = 'VinFast';
            _modelController.text = 'VF e34';
            _selectedLicenseClass = 'B2';
          }
          break;
        }
      }
    });
  }

  Future<void> _pickExpiryDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: DateTime(now.year + 2, 12, 31),
      firstDate: now,
      lastDate: DateTime(now.year + 10),
      builder: (context, child) {
        return Theme(
          data: ThemeData.dark().copyWith(
            colorScheme: const ColorScheme.dark(
              primary: GreenColors.primaryEmerald,
              surface: GreenColors.surfaceDark,
            ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      final formatted = "${picked.year.toString().padLeft(4, '0')}-"
          "${picked.month.toString().padLeft(2, '0')}-"
          "${picked.day.toString().padLeft(2, '0')}";
      setState(() {
        _expiryDateController.text = formatted;
      });
    }
  }

  Future<void> _onPickDocument(String docKey) async {
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      backgroundColor: GreenColors.surfaceDark,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                'Chọn nguồn ảnh',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white),
              ),
              const SizedBox(height: 16),
              ListTile(
                leading: const Icon(Icons.camera_alt_rounded, color: GreenColors.primaryEmerald),
                title: const Text('Chụp ảnh từ Camera'),
                onTap: () => Navigator.of(ctx).pop(ImageSource.camera),
              ),
              ListTile(
                leading: const Icon(Icons.photo_library_rounded, color: GreenColors.electricCyan),
                title: const Text('Chọn từ Thư viện ảnh'),
                onTap: () => Navigator.of(ctx).pop(ImageSource.gallery),
              ),
            ],
          ),
        ),
      ),
    );

    if (source != null) {
      final file = await _picker.pickImage(source: source, imageQuality: 85);
      if (file != null) {
        setState(() {
          switch (docKey) {
            case 'citizenFront':
              _citizenFrontPath = file.path;
              break;
            case 'citizenBack':
              _citizenBackPath = file.path;
              break;
            case 'license':
              _licensePath = file.path;
              break;
            case 'registration':
              _registrationPath = file.path;
              break;
            case 'facePortrait':
              _facePortraitPath = file.path;
              break;
          }
        });
      }
    }
  }

  bool _validateStep1() {
    if (_fullNameController.text.trim().isEmpty) {
      _showToast('Vui lòng nhập họ và tên tài xế');
      return false;
    }
    if (_citizenIdController.text.trim().length < 9) {
      _showToast('Số CCCD phải có từ 9 đến 12 số');
      return false;
    }
    if (_licenseNumberController.text.trim().isEmpty) {
      _showToast('Vui lòng nhập số Giấy phép lái xe');
      return false;
    }
    return true;
  }

  bool _validateStep2() {
    if (_makeController.text.trim().isEmpty || _modelController.text.trim().isEmpty) {
      _showToast('Vui lòng nhập hãng xe và dòng xe');
      return false;
    }
    if (_licensePlateController.text.trim().isEmpty) {
      _showToast('Vui lòng nhập biển số xe điện');
      return false;
    }
    final battery = double.tryParse(_batteryController.text);
    if (battery == null || battery <= 0) {
      _showToast('Dung lượng pin không hợp lệ');
      return false;
    }
    final range = int.tryParse(_rangeController.text);
    if (range == null || range <= 0) {
      _showToast('Tầm hoạt động không hợp lệ');
      return false;
    }
    return true;
  }

  bool _validateStep3() {
    if (_citizenFrontPath == null ||
        _citizenBackPath == null ||
        _licensePath == null ||
        _registrationPath == null ||
        _facePortraitPath == null) {
      _showToast('Vui lòng chụp đủ 5 ảnh minh chứng');
      return false;
    }
    return true;
  }

  void _showToast(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), backgroundColor: GreenColors.errorRed),
    );
  }

  void _onNext() {
    if (_currentStep == 0) {
      if (_validateStep1()) setState(() => _currentStep = 1);
    } else if (_currentStep == 1) {
      if (_validateStep2()) setState(() => _currentStep = 2);
    } else if (_currentStep == 2) {
      if (_validateStep3()) _onSubmit();
    }
  }

  void _onBack() {
    if (_currentStep > 0) {
      setState(() => _currentStep -= 1);
    } else {
      Navigator.of(context).pop();
    }
  }

  Future<void> _onSubmit() async {
    final submission = KycSubmissionModel(
      citizenId: _citizenIdController.text.trim(),
      licenseNumber: _licenseNumberController.text.trim(),
      licenseClass: _selectedLicenseClass,
      vehicleType: _selectedVehicleType,
      make: _makeController.text.trim(),
      model: _modelController.text.trim(),
      licensePlate: _licensePlateController.text.trim(),
      color: _colorController.text.trim(),
      batteryCapacityKwh: double.tryParse(_batteryController.text) ?? 3.5,
      rangePerChargeKm: int.tryParse(_rangeController.text) ?? 150,
      inspectionExpiryDate: _expiryDateController.text,
      citizenFrontPath: _citizenFrontPath!,
      citizenBackPath: _citizenBackPath!,
      licenseImagePath: _licensePath!,
      vehicleRegistrationPath: _registrationPath!,
      facePortraitPath: _facePortraitPath!,
    );

    final success = await context.read<DriverCubit>().submitKyc(submission);
    if (success && mounted) {
      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (ctx) => AlertDialog(
          title: const Row(
            children: [
              Icon(Icons.check_circle, color: GreenColors.primaryEmerald),
              SizedBox(width: 8),
              Text('Gửi hồ sơ thành công!'),
            ],
          ),
          content: const Text(
            'Hồ sơ của bạn đã được tiếp nhận và chuyển sang trạng thái "Chờ xét duyệt". '
            'Quản trị viên sẽ đối chiếu hồ sơ trong vòng 24h làm việc.',
            style: TextStyle(fontSize: 14, color: Colors.white70),
          ),
          actions: [
            GreenButton(
              label: 'Về Trang chủ',
              onPressed: () {
                Navigator.of(ctx).pop();
                Navigator.of(context).pop();
              },
            ),
          ],
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return BlocListener<DriverCubit, DriverState>(
      listener: (context, state) {
        if (state is DriverError) {
          _showToast(state.message);
        }
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Đăng ký Hồ sơ KYC Xe điện'),
          leading: IconButton(
            icon: const Icon(Icons.arrow_back_ios_new_rounded),
            onPressed: _onBack,
          ),
        ),
        body: Column(
          children: [
            // Progress Indicator bar (3 steps)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              color: GreenColors.surfaceDark.withValues(alpha: 0.5),
              child: Row(
                children: [
                  _buildStepIndicator(0, 'Cá nhân'),
                  _buildStepDivider(0),
                  _buildStepIndicator(1, 'Xe điện'),
                  _buildStepDivider(1),
                  _buildStepIndicator(2, 'Giấy tờ & Mặt'),
                ],
              ),
            ),

            // Step Content
            Expanded(
              child: IndexedStack(
                index: _currentStep,
                children: [
                  StepPersonalInfo(
                    fullNameController: _fullNameController,
                    citizenIdController: _citizenIdController,
                    licenseNumberController: _licenseNumberController,
                    selectedLicenseClass: _selectedLicenseClass,
                    onLicenseClassChanged: (val) => setState(() => _selectedLicenseClass = val),
                  ),
                  StepVehicleInfo(
                    selectedVehicleType: _selectedVehicleType,
                    onVehicleTypeChanged: _onVehicleTypeChanged,
                    makeController: _makeController,
                    modelController: _modelController,
                    licensePlateController: _licensePlateController,
                    colorController: _colorController,
                    batteryController: _batteryController,
                    rangeController: _rangeController,
                    expiryDateController: _expiryDateController,
                    onPickExpiryDate: _pickExpiryDate,
                  ),
                  StepDocumentsUpload(
                    citizenFrontPath: _citizenFrontPath,
                    citizenBackPath: _citizenBackPath,
                    licensePath: _licensePath,
                    registrationPath: _registrationPath,
                    facePortraitPath: _facePortraitPath,
                    onPickDocument: _onPickDocument,
                  ),
                ],
              ),
            ),

            // Bottom Navigation Actions
            Container(
              padding: const EdgeInsets.all(16),
              decoration: const BoxDecoration(
                color: GreenColors.surfaceDark,
                border: Border(top: BorderSide(color: Colors.white10)),
              ),
              child: SafeArea(
                child: Row(
                  children: [
                    if (_currentStep > 0) ...[
                      Expanded(
                        flex: 1,
                        child: GreenButton(
                          label: 'Quay lại',
                          variant: GreenButtonVariant.secondary,
                          onPressed: _onBack,
                        ),
                      ),
                      const SizedBox(width: 12),
                    ],
                    Expanded(
                      flex: 2,
                      child: BlocBuilder<DriverCubit, DriverState>(
                        builder: (context, state) {
                          final isSubmitting = state is DriverKycSubmitting;
                          return GreenButton(
                            label: _currentStep == 2 ? 'Nộp hồ sơ KYC' : 'Tiếp tục',
                            icon: _currentStep == 2 ? Icons.cloud_upload_rounded : Icons.arrow_forward_rounded,
                            isLoading: isSubmitting,
                            onPressed: isSubmitting ? null : _onNext,
                          );
                        },
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStepIndicator(int index, String label) {
    final isActive = _currentStep == index;
    final isDone = _currentStep > index;

    Color color;
    if (isDone) {
      color = GreenColors.primaryEmerald;
    } else if (isActive) {
      color = GreenColors.electricCyan;
    } else {
      color = Colors.white24;
    }

    return Expanded(
      child: Column(
        children: [
          Container(
            width: 28,
            height: 28,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: isDone
                  ? GreenColors.primaryEmerald
                  : (isActive ? GreenColors.electricCyan.withValues(alpha: 0.2) : Colors.transparent),
              border: Border.all(color: color, width: 2),
            ),
            child: Center(
              child: isDone
                  ? const Icon(Icons.check, size: 16, color: Colors.white)
                  : Text(
                      '${index + 1}',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: isActive ? Colors.white : Colors.white54,
                      ),
                    ),
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              fontSize: 11,
              fontWeight: isActive ? FontWeight.bold : FontWeight.normal,
              color: isActive ? Colors.white : Colors.white54,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildStepDivider(int index) {
    final isDone = _currentStep > index;
    return Container(
      width: 24,
      height: 2,
      margin: const EdgeInsets.only(bottom: 16),
      color: isDone ? GreenColors.primaryEmerald : Colors.white12,
    );
  }
}
