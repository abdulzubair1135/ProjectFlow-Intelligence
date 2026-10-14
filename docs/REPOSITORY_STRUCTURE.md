# Repository Structure Reference

```
projectflow-intelligence/
├── shared-core/                        # Shared Java 21 Domain & Math Core
│   ├── src/main/java/com/projectflow/core/
│   │   ├── model/                      # POJO Domain Entities & Enums
│   │   ├── risk/                       # DeterministicRiskEngine (6 Rules)
│   │   ├── scheduling/                 # WhatIfSchedulingEngine & Comparison
│   │   └── validation/                 # DependencyValidator (DAG cycles)
│   └── src/test/java/                  # JUnit 5 Unit Tests
├── backend/                            # Spring Boot 3.3.4 Application
│   ├── src/main/java/com/projectflow/server/
│   │   ├── controller/                 # REST Controllers (/api/v1/*)
│   │   ├── dto/                        # Request/Response Data Transfer Objects
│   │   ├── entity/                     # JPA Hibernate Entities
│   │   ├── repository/                 # Spring Data Repositories
│   │   ├── security/                   # JWT Filter & SecurityConfig
│   │   └── service/                    # Business Logic & Risk Service
│   └── src/main/resources/
│       ├── application.yml             # H2-PG Mode / Supabase Configuration
│       └── db/migration/               # Flyway V1__init_schema.sql (19 Tables)
├── admin-panel/                        # React 18 + Vite Web Admin Console
│   ├── src/
│   │   ├── api/client.ts               # Authenticated REST Client
│   │   ├── components/                 # Sidebar, Header, Layout
│   │   ├── context/AuthContext.tsx     # Authentication State Provider
│   │   └── pages/                      # Dashboard, Workspaces, Projects, Tasks, Risks, Users, Logs, Health, Settings
│   └── package.json                    # Dependencies (React, Lucide, Tailwind)
├── desktop-app/                        # JavaFX 21 Windows Manager Workspace
│   ├── src/main/java/com/projectflow/desktop/
│   │   ├── controller/MainViewController.java # 15 Manager Navigation Panes
│   │   └── service/                    # Local SQLite & HTTP API Client
│   └── src/main/resources/css/styles.css # Natural Mineral JavaFX CSS
├── android-app/                        # Native Android Application (API 26-35)
│   ├── src/main/java/com/projectflow/android/
│   │   ├── data/local/                 # Room Entities, DAOs, AppDatabase
│   │   ├── data/remote/                # Retrofit ApiService & OkHttpClient
│   │   ├── ui/                         # Activities, Fragments, Adapters
│   │   └── worker/SyncWorker.java      # WorkManager Background Synchronization
│   └── src/main/res/                   # XML Layouts, Colors, Themes, Menus
├── database/migrations/                # Standalone SQL Migration Scripts
├── docs/                               # 33 Complete Technical Documents
└── *.bat                               # START, STOP, BUILD, TEST, INSTALL Scripts
```
