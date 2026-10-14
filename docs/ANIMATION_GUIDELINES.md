# ProjectFlow Intelligence — Animation & Micro-Interaction Guidelines

## 1. Principles of Restraint & Performance
Animations in ProjectFlow Intelligence are designed to communicate state and provide tactile delight without impeding productivity or draining battery:
- **Duration Standard:** 200ms to 300ms for transitions and feedback; maximum 600ms for initial data count-ups.
- **Interpolators:** Decelerate / FastOutSlowIn for organic deceleration.
- **No Continuous Loops:** Progress spinners only run while a background task is executing.
- **Reduced Motion Support:** Respect user device accessibility preferences.

---

## 2. Standard Motion Patterns

### 2.1 Screen Transitions
- **Forward Navigation:** Slide in from right (`slide_in_right.xml`) with subtle fade.
- **Backward Navigation:** Slide out to right (`slide_out_right.xml`) with fade.
- **Bottom Sheets & Dialogs:** Slide up from bottom with 250ms duration.

### 2.2 Micro-Interactions
- **Touch Ripples:** Bounded ripple effect on cards and buttons (`?attr/selectableItemBackground`).
- **Checkboxes & Status Toggles:** Immediate visual toggle with animated strike-through or progress fill.
- **Circular Progress Ring:** Smooth single-pass animation from 0% to calculated target on screen entry using `ValueAnimator`.
