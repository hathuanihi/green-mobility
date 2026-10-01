import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../core_ui.dart';

/// Widget nút trượt (Slide-to-Action) chuẩn Green Mobility Design System
class GreenSwipeButton extends StatefulWidget {
  final String text;
  final IconData icon;
  final VoidCallback onSwipeComplete;
  final bool isLoading;
  final Color thumbColor;
  final Color trackColor;
  final double height;

  const GreenSwipeButton({
    super.key,
    required this.text,
    required this.onSwipeComplete,
    this.icon = Icons.chevron_right_rounded,
    this.isLoading = false,
    this.thumbColor = GreenColors.primaryEmerald,
    this.trackColor = GreenColors.surfaceDark,
    this.height = 58,
  });

  @override
  State<GreenSwipeButton> createState() => _GreenSwipeButtonState();
}

class _GreenSwipeButtonState extends State<GreenSwipeButton>
    with SingleTickerProviderStateMixin {
  double _dragPosition = 0.0;
  late AnimationController _resetController;
  late Animation<double> _resetAnimation;

  @override
  void initState() {
    super.initState();
    _resetController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 250),
    )..addListener(() {
        setState(() {
          _dragPosition = _resetAnimation.value;
        });
      });
  }

  @override
  void dispose() {
    _resetController.dispose();
    super.dispose();
  }

  void _onHorizontalDragUpdate(DragUpdateDetails details, double maxDrag) {
    if (widget.isLoading) return;
    setState(() {
      _dragPosition = (_dragPosition + details.delta.dx).clamp(0.0, maxDrag);
    });
  }

  void _onHorizontalDragEnd(DragEndDetails details, double maxDrag) {
    if (widget.isLoading) return;
    // Trigger when dragged past 75%
    if (_dragPosition >= maxDrag * 0.75) {
      HapticFeedback.mediumImpact();
      setState(() {
        _dragPosition = maxDrag;
      });
      widget.onSwipeComplete();
      // Snap back after a short delay if not loading
      Future.delayed(const Duration(milliseconds: 600), () {
        if (mounted && !widget.isLoading) {
          _animateReset();
        }
      });
    } else {
      _animateReset();
    }
  }

  void _animateReset() {
    _resetAnimation = Tween<double>(
      begin: _dragPosition,
      end: 0.0,
    ).animate(CurvedAnimation(parent: _resetController, curve: Curves.easeOut));
    _resetController.forward(from: 0.0);
  }

  @override
  Widget build(BuildContext context) {
    final thumbSize = widget.height - 8;

    return LayoutBuilder(
      builder: (context, constraints) {
        final maxDrag = constraints.maxWidth - thumbSize - 8;

        return Container(
          height: widget.height,
          decoration: BoxDecoration(
            color: widget.trackColor,
            borderRadius: BorderRadius.circular(widget.height / 2),
            border: Border.all(color: GreenColors.cardBorder, width: 1.5),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.25),
                blurRadius: 8,
                offset: const Offset(0, 3),
              ),
            ],
          ),
          child: Stack(
            alignment: Alignment.centerLeft,
            children: [
              // Shimmer / Text Label in the center
              Center(
                child: Padding(
                  padding: EdgeInsets.only(left: thumbSize * 0.5),
                  child: widget.isLoading
                      ? const SizedBox(
                          width: 22,
                          height: 22,
                          child: CircularProgressIndicator(
                            strokeWidth: 2.5,
                            valueColor: AlwaysStoppedAnimation<Color>(GreenColors.primaryEmerald),
                          ),
                        )
                      : Text(
                          widget.text,
                          style: const TextStyle(
                            color: GreenColors.textPrimary,
                            fontSize: 13,
                            fontWeight: FontWeight.w800,
                            letterSpacing: 0.8,
                          ),
                        ),
                ),
              ),

              // Progress Track Fill
              Positioned(
                left: 4,
                child: Container(
                  width: _dragPosition + thumbSize,
                  height: thumbSize,
                  decoration: BoxDecoration(
                    color: widget.thumbColor.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(thumbSize / 2),
                  ),
                ),
              ),

              // Draggable Thumb Button
              Positioned(
                left: 4 + _dragPosition,
                child: GestureDetector(
                  onHorizontalDragUpdate: (details) =>
                      _onHorizontalDragUpdate(details, maxDrag),
                  onHorizontalDragEnd: (details) =>
                      _onHorizontalDragEnd(details, maxDrag),
                  child: Container(
                    width: thumbSize,
                    height: thumbSize,
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [widget.thumbColor, GreenColors.primaryDark],
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                      ),
                      shape: BoxShape.circle,
                      boxShadow: [
                        BoxShadow(
                          color: widget.thumbColor.withValues(alpha: 0.4),
                          blurRadius: 10,
                          offset: const Offset(0, 2),
                        ),
                      ],
                    ),
                    child: Center(
                      child: Icon(
                        widget.icon,
                        color: Colors.white,
                        size: 26,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
