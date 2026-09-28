import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'goong_config.dart';
import 'polyline_decoder.dart';

/// Widget hiển thị bản đồ lộ trình Goong Map cho Mobile Apps (Customer & Driver)
class GoongRouteMapView extends StatefulWidget {
  final double pickupLat;
  final double pickupLng;
  final String? pickupTitle;
  final double dropoffLat;
  final double dropoffLng;
  final String? dropoffTitle;
  final String? encodedPolyline;
  final double? driverLat;
  final double? driverLng;
  final double? driverBearing;
  final String? distanceText;
  final String? durationText;
  final bool showRadarPulse;
  final double height;

  const GoongRouteMapView({
    super.key,
    required this.pickupLat,
    required this.pickupLng,
    this.pickupTitle,
    required this.dropoffLat,
    required this.dropoffLng,
    this.dropoffTitle,
    this.encodedPolyline,
    this.driverLat,
    this.driverLng,
    this.driverBearing,
    this.distanceText,
    this.durationText,
    this.showRadarPulse = true,
    this.height = 240,
  });

  @override
  State<GoongRouteMapView> createState() => _GoongRouteMapViewState();
}

class _GoongRouteMapViewState extends State<GoongRouteMapView>
    with SingleTickerProviderStateMixin {
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;
  List<MapCoordinate> _routePoints = [];

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1800),
    )..repeat();

    _pulseAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeOut),
    );

    _decodeRoute();
  }

  @override
  void didUpdateWidget(covariant GoongRouteMapView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.encodedPolyline != widget.encodedPolyline ||
        oldWidget.pickupLat != widget.pickupLat ||
        oldWidget.dropoffLat != widget.dropoffLat) {
      _decodeRoute();
    }
  }

  void _decodeRoute() {
    if (widget.encodedPolyline != null && widget.encodedPolyline!.isNotEmpty) {
      _routePoints = GoongPolylineDecoder.decode(widget.encodedPolyline!);
    } else {
      _routePoints = [
        MapCoordinate(widget.pickupLat, widget.pickupLng),
        MapCoordinate(
          widget.pickupLat + (widget.dropoffLat - widget.pickupLat) * 0.5 + 0.005,
          widget.pickupLng + (widget.dropoffLng - widget.pickupLng) * 0.5 - 0.005,
        ),
        MapCoordinate(widget.dropoffLat, widget.dropoffLng),
      ];
    }
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: widget.height,
      width: double.infinity,
      decoration: BoxDecoration(
        color: const Color(0xFF0B132B),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFF1E293B)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.5),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          // Tactical Map Canvas Renderer
          AnimatedBuilder(
            animation: _pulseAnimation,
            builder: (context, child) {
              return CustomPaint(
                size: Size(double.infinity, widget.height),
                painter: _GoongMapPainter(
                  pickup: MapCoordinate(widget.pickupLat, widget.pickupLng),
                  dropoff: MapCoordinate(widget.dropoffLat, widget.dropoffLng),
                  routePoints: _routePoints,
                  driver: (widget.driverLat != null && widget.driverLng != null)
                      ? MapCoordinate(widget.driverLat!, widget.driverLng!)
                      : null,
                  driverBearing: widget.driverBearing ?? 0.0,
                  pulseValue: widget.showRadarPulse ? _pulseAnimation.value : 0.0,
                ),
              );
            },
          ),

          // Top Info Chip: Goong Map Branding & Route Info
          Positioned(
            top: 12,
            left: 14,
            right: 14,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                // Goong Tag
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(
                    color: const Color(0xFF0F172A).withValues(alpha: 0.85),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: const Color(0xFF334155)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        width: 8,
                        height: 8,
                        decoration: const BoxDecoration(
                          color: Color(0xFF10B981),
                          shape: BoxShape.circle,
                        ),
                      ),
                      const SizedBox(width: 6),
                      const Text(
                        'Goong Map Vector',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 0.3,
                        ),
                      ),
                    ],
                  ),
                ),

                // Distance & Duration Badge
                if (widget.distanceText != null || widget.durationText != null)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(
                      color: const Color(0xFF064E3B).withValues(alpha: 0.9),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: const Color(0xFF10B981).withValues(alpha: 0.5)),
                    ),
                    child: Text(
                      '${widget.distanceText ?? ''} • ${widget.durationText ?? ''}',
                      style: const TextStyle(
                        color: Color(0xFFA7F3D0),
                        fontSize: 11,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
              ],
            ),
          ),

          // Bottom Quick Legend
          Positioned(
            bottom: 10,
            left: 14,
            right: 14,
            child: Row(
              children: [
                // Pickup indicator
                _buildLegendItem(const Color(0xFF10B981), widget.pickupTitle ?? 'Điểm đón'),
                const SizedBox(width: 14),
                // Dropoff indicator
                _buildLegendItem(const Color(0xFF06B6D4), widget.dropoffTitle ?? 'Điểm đến'),
                const Spacer(),
                const Text(
                  'TP.HCM',
                  style: TextStyle(color: Colors.white38, fontSize: 10, fontFamily: 'monospace'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLegendItem(Color color, String text) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 8,
          height: 8,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 5),
        Text(
          text.length > 18 ? '${text.substring(0, 16)}...' : text,
          style: const TextStyle(color: Colors.white70, fontSize: 10, fontWeight: FontWeight.w600),
        ),
      ],
    );
  }
}

class _GoongMapPainter extends CustomPainter {
  final MapCoordinate pickup;
  final MapCoordinate dropoff;
  final List<MapCoordinate> routePoints;
  final MapCoordinate? driver;
  final double driverBearing;
  final double pulseValue;

  _GoongMapPainter({
    required this.pickup,
    required this.dropoff,
    required this.routePoints,
    this.driver,
    required this.driverBearing,
    required this.pulseValue,
  });

  @override
  void paint(Canvas canvas, Size size) {
    // Calculate bounding box for coordinates
    double minLat = math.min(pickup.latitude, dropoff.latitude);
    double maxLat = math.max(pickup.latitude, dropoff.latitude);
    double minLng = math.min(pickup.longitude, dropoff.longitude);
    double maxLng = math.max(pickup.longitude, dropoff.longitude);

    for (final pt in routePoints) {
      minLat = math.min(minLat, pt.latitude);
      maxLat = math.max(maxLat, pt.latitude);
      minLng = math.min(minLng, pt.longitude);
      maxLng = math.max(maxLng, pt.longitude);
    }

    if (driver != null) {
      minLat = math.min(minLat, driver!.latitude);
      maxLat = math.max(maxLat, driver!.latitude);
      minLng = math.min(minLng, driver!.longitude);
      maxLng = math.max(maxLng, driver!.longitude);
    }

    // Add padding to bounding box
    final dLat = math.max(maxLat - minLat, 0.01) * 0.35;
    final dLng = math.max(maxLng - minLng, 0.01) * 0.35;
    minLat -= dLat;
    maxLat += dLat;
    minLng -= dLng;
    maxLng += dLng;

    Offset project(MapCoordinate coord) {
      final x = ((coord.longitude - minLng) / (maxLng - minLng)) * (size.width - 60) + 30;
      final y = (size.height - 50) - ((coord.latitude - minLat) / (maxLat - minLat)) * (size.height - 80);
      return Offset(x, y);
    }

    // 1. Draw subtle background grid
    final gridPaint = Paint()
      ..color = const Color(0xFF1E293B).withValues(alpha: 0.4)
      ..strokeWidth = 1.0;

    const gridSize = 32.0;
    for (double x = 0; x < size.width; x += gridSize) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), gridPaint);
    }
    for (double y = 0; y < size.height; y += gridSize) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), gridPaint);
    }

    // 2. Draw Polyline Route
    if (routePoints.isNotEmpty) {
      final path = Path();
      final startOffset = project(routePoints.first);
      path.moveTo(startOffset.dx, startOffset.dy);

      for (int i = 1; i < routePoints.length; i++) {
        final pt = project(routePoints[i]);
        path.lineTo(pt.dx, pt.dy);
      }

      // Outer glow
      final glowPaint = Paint()
        ..color = const Color(0xFF10B981).withValues(alpha: 0.3)
        ..strokeWidth = 8.0
        ..style = PaintingStyle.stroke
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round;
      canvas.drawPath(path, glowPaint);

      // Core line
      final linePaint = Paint()
        ..color = const Color(0xFF10B981)
        ..strokeWidth = 4.0
        ..style = PaintingStyle.stroke
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round;
      canvas.drawPath(path, linePaint);
    }

    // 3. Draw Pickup Marker (Emerald with Radar Pulse)
    final pickupOffset = project(pickup);

    if (pulseValue > 0) {
      final pulseRadius = 14.0 + (pulseValue * 26.0);
      final pulseOpacity = (1.0 - pulseValue).clamp(0.0, 1.0) * 0.6;
      final pulsePaint = Paint()
        ..color = const Color(0xFF10B981).withValues(alpha: pulseOpacity)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.0;
      canvas.drawCircle(pickupOffset, pulseRadius, pulsePaint);

      final innerFill = Paint()
        ..color = const Color(0xFF10B981).withValues(alpha: pulseOpacity * 0.3)
        ..style = PaintingStyle.fill;
      canvas.drawCircle(pickupOffset, pulseRadius, innerFill);
    }

    final pickupGlow = Paint()
      ..color = const Color(0xFF10B981).withValues(alpha: 0.4)
      ..style = PaintingStyle.fill;
    canvas.drawCircle(pickupOffset, 12, pickupGlow);

    final pickupCore = Paint()
      ..color = const Color(0xFF10B981)
      ..style = PaintingStyle.fill;
    canvas.drawCircle(pickupOffset, 7, pickupCore);

    final pickupCenter = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.fill;
    canvas.drawCircle(pickupOffset, 3, pickupCenter);

    // 4. Draw Dropoff Marker (Cyan Pin)
    final dropoffOffset = project(dropoff);
    final dropoffGlow = Paint()
      ..color = const Color(0xFF06B6D4).withValues(alpha: 0.4)
      ..style = PaintingStyle.fill;
    canvas.drawCircle(dropoffOffset, 10, dropoffGlow);

    final dropoffCore = Paint()
      ..color = const Color(0xFF06B6D4)
      ..style = PaintingStyle.fill;
    canvas.drawCircle(dropoffOffset, 6, dropoffCore);

    final dropoffCenter = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.fill;
    canvas.drawCircle(dropoffOffset, 2.5, dropoffCenter);

    // 5. Draw Driver Marker (if present)
    if (driver != null) {
      final driverOffset = project(driver!);
      final driverGlow = Paint()
        ..color = const Color(0xFFF59E0B).withValues(alpha: 0.4)
        ..style = PaintingStyle.fill;
      canvas.drawCircle(driverOffset, 14, driverGlow);

      final driverCore = Paint()
        ..color = const Color(0xFFF59E0B)
        ..style = PaintingStyle.fill;
      canvas.drawCircle(driverOffset, 8, driverCore);
    }
  }

  @override
  bool shouldRepaint(covariant _GoongMapPainter oldDelegate) {
    return oldDelegate.pulseValue != pulseValue ||
        oldDelegate.routePoints != routePoints ||
        oldDelegate.pickup != pickup ||
        oldDelegate.dropoff != dropoff ||
        oldDelegate.driver != driver;
  }
}
