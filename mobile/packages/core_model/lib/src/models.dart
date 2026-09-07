// Core Domain Models for Green Mobility Mobile Apps

class UserProfile {
  final String id;
  final String phoneNumber;
  final String fullName;
  final String role;
  final String? avatarUrl;

  const UserProfile({
    required this.id,
    required this.phoneNumber,
    required this.fullName,
    required this.role,
    this.avatarUrl,
  });
}

class CarbonImpact {
  final double co2SavedGrams;
  final double treeAbsorptionDays;
  final double ledBulbHours;
  final double carbonCredits;

  const CarbonImpact({
    required this.co2SavedGrams,
    required this.treeAbsorptionDays,
    required this.ledBulbHours,
    required this.carbonCredits,
  });
}
