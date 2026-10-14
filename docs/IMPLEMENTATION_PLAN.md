# ProjectFlow Intelligence — Frontend Master Implementation Plan

## 1. Overview & Objectives
This document establishes the step-by-step technical roadmap for completely revamping and completing the ProjectFlow Intelligence Android frontend, aligning with the customer's **मिट्टी (earth-tone)** design system, replacing all static placeholders with real calculations and live data, integrating all 17 screens, and verifying on physical hardware.

---

## 2. Phase-by-Phase Roadmap

```
+---------------------------------------------------------------------------------+
| PHASE 1: Audit & Design System Tokens (मिट्टी Earth-Tone Light & Dark)           |
| - Update colors.xml with exact hex codes                                         |
| - Update values-night/themes.xml and colors.xml for Dark Mode                   |
| - Create reusable card, chip, button, and progress drawables                     |
+---------------------------------------------------------------------------------+
                                      |
                                      v
+---------------------------------------------------------------------------------+
| PHASE 2: Backend & Supabase Extensions                                          |
| - Add notifications, chat messages, team endpoints, and reports summary         |
| - Update server.js and src/supabase.js                                           |
| - Run backend verification tests (npm test)                                      |
+---------------------------------------------------------------------------------+
                                      |
                                      v
+---------------------------------------------------------------------------------+
| PHASE 3: Android Data & Networking Architecture                                 |
| - Add DTOs (NotificationDto, ChatMessageDto, TeamMemberDto, ReportSummaryDto)   |
| - Declare ApiService endpoints                                                   |
| - Add repository methods in ProjectFlowRepository                               |
+---------------------------------------------------------------------------------+
                                      |
                                      v
+---------------------------------------------------------------------------------+
| PHASE 4: Core Screen Redesigns & Implementations                                |
| 1. Splash & Onboarding (Auth state restore, page slider, indicators)             |
| 2. Authentication (Validation, session tokens, remember-me)                      |
| 3. Dashboard (Real metrics, progress ring, quick actions, assigned tasks)        |
| 4. Projects Explorer & Project Details (Filters, search, creation, tabs)         |
| 5. Tasks Explorer & Details (Search, tabs, status updates, creation form)        |
| 6. Team Members & Member Details (Search, roles, contact info)                   |
| 7. Dynamic Calendar (Real month math, date picker, event markers, task list)     |
| 8. Reports & Analytics (Real calculated stats, animated progress visuals)        |
| 9. Notifications (Real backend records, unread badges, mark as read)            |
| 10. Chat & Discussion (Message history, composer, real-time message flow)        |
| 11. Profile & Settings (Edit profile, theme switcher, verified APK sharing)      |
| 12. Navigation Drawer & Bottom Nav (Unified routing, brand header)               |
+---------------------------------------------------------------------------------+
                                      |
                                      v
+---------------------------------------------------------------------------------+
| PHASE 5: Animated Views & Micro-Interactions                                    |
| - Circular progress ring view for work completion                                |
| - Smooth fragment transitions and tactile ripple feedback                        |
+---------------------------------------------------------------------------------+
                                      |
                                      v
+---------------------------------------------------------------------------------+
| PHASE 6: Testing, Hardware ADB Verification & Documentation                     |
| - Gradle assembleDebug verification                                              |
| - ADB streamed install and live run on connected physical device                 |
| - Complete 17 documentation deliverables                                          |
| - Git commit and push to GitHub origin main                                      |
+---------------------------------------------------------------------------------+
```

---

## 3. Milestones & Checkpoints

- **Milestone A:** Palette and theme resources updated with zero compilation errors.
- **Milestone B:** Backend endpoints live and tested (`npm test` passing).
- **Milestone C:** Dashboard, Projects, Tasks, and Calendar running on real calculated data.
- **Milestone D:** Chat, Notifications, Reports, and Profile screens fully interactive.
- **Milestone E:** Live ADB installation on `UT03032274562709251` verified with zero crashes.
- **Milestone F:** All documentation updated and synchronized with code.
