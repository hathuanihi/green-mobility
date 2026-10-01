import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../../core_ui.dart';

/// Hiệu ứng Confetti chúc mừng hoàn thành chuyến đi xanh
class ConfettiCelebration extends StatefulWidget {
  final Widget child;
  final bool isPlaying;

  const ConfettiCelebration({
    super.key,
    required this.child,
    this.isPlaying = true,
  });

  @override
  State<ConfettiCelebration> createState() => _ConfettiCelebrationState();
}

class _ConfettiCelebrationState extends State<ConfettiCelebration>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  final List<_ConfettiParticle> _particles = [];
  final math.Random _random = math.Random();

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 3500),
    );

    _initParticles();

    if (widget.isPlaying) {
      _controller.forward();
    }
  }

  void _initParticles() {
    _particles.clear();
    final colors = [
      GreenColors.primaryEmerald,
      GreenColors.electricCyan,
      GreenColors.accentGold,
      Colors.white,
      const Color(0xFF34D399),
      const Color(0xFF38BDF8),
    ];

    for (int i = 0; i < 65; i++) {
      _particles.add(
        _ConfettiParticle(
          x: _random.nextDouble(),
          y: -_random.nextDouble() * 0.4,
          vx: (_random.nextDouble() - 0.5) * 0.25,
          vy: 0.3 + _random.nextDouble() * 0.6,
          size: 6.0 + _random.nextDouble() * 8.0,
          color: colors[_random.nextInt(colors.length)],
          rotation: _random.nextDouble() * 2 * math.pi,
          vRotation: (_random.nextDouble() - 0.5) * 8.0,
        ),
      );
    }
  }

  @override
  void didUpdateWidget(covariant ConfettiCelebration oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.isPlaying && !oldWidget.isPlaying) {
      _initParticles();
      _controller.forward(from: 0.0);
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        widget.child,
        if (widget.isPlaying)
          Positioned.fill(
            child: IgnorePointer(
              child: AnimatedBuilder(
                animation: _controller,
                builder: (context, _) {
                  return CustomPaint(
                    painter: _ConfettiPainter(
                      particles: _particles,
                      progress: _controller.value,
                    ),
                  );
                },
              ),
            ),
          ),
      ],
    );
  }
}

class _ConfettiParticle {
  double x;
  double y;
  double vx;
  double vy;
  double size;
  Color color;
  double rotation;
  double vRotation;

  _ConfettiParticle({
    required this.x,
    required this.y,
    required this.vx,
    required this.vy,
    required this.size,
    required this.color,
    required this.rotation,
    required this.vRotation,
  });
}

class _ConfettiPainter extends CustomPainter {
  final List<_ConfettiParticle> particles;
  final double progress;

  _ConfettiPainter({required this.particles, required this.progress});

  @override
  void paint(Canvas canvas, Size size) {
    if (progress <= 0.0 || progress >= 1.0) return;

    for (final p in particles) {
      final currentX = (p.x + p.vx * progress) * size.width;
      final currentY = (p.y + p.vy * progress) * size.height;
      final currentRotation = p.rotation + p.vRotation * progress;
      final opacity = (1.0 - (progress - 0.6) / 0.4).clamp(0.0, 1.0);

      final paint = Paint()
        ..color = p.color.withValues(alpha: opacity)
        ..style = PaintingStyle.fill;

      canvas.save();
      canvas.translate(currentX, currentY);
      canvas.rotate(currentRotation);
      canvas.drawRect(
        Rect.fromCenter(
          center: Offset.zero,
          width: p.size,
          height: p.size * 0.6,
        ),
        paint,
      );
      canvas.restore();
    }
  }

  @override
  bool shouldRepaint(covariant _ConfettiPainter oldDelegate) {
    return oldDelegate.progress != progress;
  }
}
