require('dotenv').config();
const { createClient } = require('@supabase/supabase-js');

const SUPABASE_URL = process.env.SUPABASE_URL || 'https://loqiqdgnjgozeixepfkk.supabase.co';
const SUPABASE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY || 
                     process.env.SUPABASE_KEY || 
                     process.env.SUPABASE_ANON_KEY || 
                     '';

let supabase = null;
try {
  if (SUPABASE_URL && SUPABASE_KEY) {
    supabase = createClient(SUPABASE_URL, SUPABASE_KEY);
    console.log('[SUPABASE] Connected to', SUPABASE_URL);
  }
} catch (e) {
  console.warn('[SUPABASE] Client init fallback:', e.message);
}

// In-Memory Database Store for fast fallback and instant responses
const store = {
  users: [
    {
      id: '00000000-0000-0000-0000-000000000001',
      email: 'admin@projectflow.io',
      fullName: 'System Administrator',
      role: 'ADMINISTRATOR',
      active: true,
      createdAt: new Date().toISOString()
    },
    {
      id: '00000000-0000-0000-0000-000000000002',
      email: 'manager@projectflow.io',
      fullName: 'Project Manager',
      role: 'MANAGER',
      active: true,
      createdAt: new Date().toISOString()
    },
    {
      id: '00000000-0000-0000-0000-000000000003',
      email: 'member@projectflow.io',
      fullName: 'Team Member',
      role: 'MEMBER',
      active: true,
      createdAt: new Date().toISOString()
    }
  ],
  workspaces: [
    {
      id: '00000000-0000-0000-0000-000000000001',
      name: 'Engineering & Innovation',
      slug: 'engineering-innovation',
      description: 'Core product development workspace domain.',
      ownerId: '00000000-0000-0000-0000-000000000001',
      archived: false,
      createdAt: new Date().toISOString()
    }
  ],
  projects: [
    {
      id: '10000000-0000-0000-0000-000000000001',
      workspaceId: '00000000-0000-0000-0000-000000000001',
      name: 'ProjectFlow Core Platform',
      code: 'PF-CORE',
      description: 'Full-stack cross-platform intelligence application.',
      status: 'IN_PROGRESS',
      health: 'ON_TRACK',
      startDate: new Date().toISOString().split('T')[0],
      targetDate: new Date(Date.now() + 30 * 86400000).toISOString().split('T')[0],
      createdAt: new Date().toISOString()
    }
  ],
  tasks: [
    {
      id: '20000000-0000-0000-0000-000000000001',
      projectId: '10000000-0000-0000-0000-000000000001',
      title: 'Database & API Architecture',
      description: 'Setup Supabase tables, REST endpoints, and security policies.',
      priority: 'CRITICAL',
      statusCategory: 'DONE',
      progressPercent: 100,
      completedAt: new Date().toISOString(),
      dueDate: new Date(Date.now() - 2 * 86400000).toISOString().split('T')[0],
      createdAt: new Date().toISOString()
    },
    {
      id: '20000000-0000-0000-0000-000000000002',
      projectId: '10000000-0000-0000-0000-000000000001',
      title: 'Frontend Admin Panel & Telemetry',
      description: 'React 18 Natural Mineral dashboard with live metrics and logo branding.',
      priority: 'HIGH',
      statusCategory: 'IN_PROGRESS',
      progressPercent: 85,
      dueDate: new Date(Date.now() + 2 * 86400000).toISOString().split('T')[0],
      createdAt: new Date().toISOString()
    },
    {
      id: '20000000-0000-0000-0000-000000000003',
      projectId: '10000000-0000-0000-0000-000000000001',
      title: 'Deterministic Risk Radar Engine',
      description: 'Implement 6 mathematical risk rules in JavaScript.',
      priority: 'HIGH',
      statusCategory: 'IN_PROGRESS',
      progressPercent: 90,
      dueDate: new Date(Date.now() + 5 * 86400000).toISOString().split('T')[0],
      createdAt: new Date().toISOString()
    }
  ],
  dependencies: [
    {
      id: '30000000-0000-0000-0000-000000000001',
      taskId: '20000000-0000-0000-0000-000000000002',
      predecessorTaskId: '20000000-0000-0000-0000-000000000001',
      dependencyType: 'FINISH_TO_START'
    }
  ],
  auditLogs: [
    {
      id: '40000000-0000-0000-0000-000000000001',
      workspaceId: '00000000-0000-0000-0000-000000000001',
      actorId: '00000000-0000-0000-0000-000000000001',
      action: 'SYSTEM_INITIALIZED',
      entityType: 'PLATFORM',
      metadata: '{"engine":"JavaScript Node.js Express","supabase":"connected"}',
      timestamp: new Date().toISOString()
    }
  ],
  releases: [
    {
      id: '50000000-0000-0000-0000-000000000001',
      applicationId: 'com.projectflow.android',
      versionName: '2.0.0',
      versionCode: 200,
      artifactFilename: 'ProjectFlow-Intelligence-v2.0.0.apk',
      storagePath: 'releases/android/ProjectFlow-Intelligence-v2.0.0.apk',
      mimeType: 'application/vnd.android.package-archive',
      fileSizeBytes: 8114674,
      sha256Checksum: '8c20000160064c7bb80af8765f912ed1d9bb20ef6bffbb4d35ed2b3729030409',
      releaseChannel: 'STABLE',
      releaseNotes: 'Official universal signed release of ProjectFlow Intelligence with 16-screen UI flow, Natural Mineral design system, and deterministic risk engine.',
      minimumSupportedVersionCode: 1,
      status: 'PUBLISHED',
      createdAt: new Date().toISOString(),
      publishedAt: new Date().toISOString()
    }
  ],
  notifications: [
    {
      id: '60000000-0000-0000-0000-000000000001',
      title: 'Task Assigned',
      message: 'You have been assigned to "Deterministic Risk Radar Engine"',
      type: 'TASK_ASSIGNED',
      read: false,
      createdAt: new Date(Date.now() - 35 * 60000).toISOString()
    },
    {
      id: '60000000-0000-0000-0000-000000000002',
      title: 'Risk Detected',
      message: 'Critical deadline approaching for Frontend Admin Panel',
      type: 'RISK_ALERT',
      read: false,
      createdAt: new Date(Date.now() - 120 * 60000).toISOString()
    },
    {
      id: '60000000-0000-0000-0000-000000000003',
      title: 'Release Published',
      message: 'ProjectFlow Intelligence v2.0.0 is now live for download',
      type: 'SYSTEM',
      read: true,
      createdAt: new Date(Date.now() - 360 * 60000).toISOString()
    }
  ],
  messages: [
    {
      id: '70000000-0000-0000-0000-000000000001',
      senderId: '00000000-0000-0000-0000-000000000001',
      senderName: 'System Administrator',
      content: 'Welcome to the ProjectFlow engineering channel! Architecture discussions and task updates take place here.',
      channel: 'general',
      reactions: { '👍': 4, '🚀': 3, '❤️': 2 },
      createdAt: new Date(Date.now() - 240 * 60000).toISOString()
    },
    {
      id: '70000000-0000-0000-0000-000000000002',
      senderId: '00000000-0000-0000-0000-000000000002',
      senderName: 'Project Manager',
      content: 'The v2.0.0 APK release is verified with SHA-256 integrity check and ready for team distribution.',
      channel: 'general',
      reactions: { '🔥': 5, '🎯': 3 },
      createdAt: new Date(Date.now() - 60 * 60000).toISOString()
    },
    {
      id: '70000000-0000-0000-0000-000000000003',
      senderId: '00000000-0000-0000-0000-000000000003',
      senderName: 'Team Member',
      content: 'Neumorphic 3D UI components and multi-color charts are rendering smoothly on the mobile device!',
      channel: 'engineering',
      reactions: { '🚀': 6, '👍': 2 },
      createdAt: new Date(Date.now() - 15 * 60000).toISOString()
    }
  ],
  teams: [
    {
      id: 'team-0000-0001',
      name: 'Core Engineering',
      slug: 'engineering',
      description: 'Full-stack architecture, backend APIs, and Android platform development.',
      memberCount: 5,
      channel: 'engineering',
      createdAt: new Date().toISOString()
    },
    {
      id: 'team-0000-0002',
      name: 'UI/UX Design Squad',
      slug: 'design-squad',
      description: 'Neumorphism 3D visual language, design tokens, and fluid animations.',
      memberCount: 3,
      channel: 'design-squad',
      createdAt: new Date().toISOString()
    },
    {
      id: 'team-0000-0003',
      name: 'Risk & Quality Assurance',
      slug: 'risk-qa',
      description: 'Deterministic risk engine auditing, stress testing, and hardware verification.',
      memberCount: 2,
      channel: 'risk-qa',
      createdAt: new Date().toISOString()
    }
  ],
  errors: [
    {
      id: '80000000-0000-0000-0000-000000000001',
      severity: 'WARNING',
      message: 'Network socket timeout during background sync (auto-retried successfully)',
      stack: 'SocketTimeoutException: timeout after 10000ms\n  at OkHttpClient.newCall\n  at ProjectFlowSyncManager.syncNow',
      source: 'ANDROID_APP',
      userId: '00000000-0000-0000-0000-000000000001',
      resolved: false,
      timestamp: new Date(Date.now() - 45 * 60000).toISOString()
    },
    {
      id: '80000000-0000-0000-0000-000000000002',
      severity: 'INFO',
      message: 'Room SQLite database migration V1 verified cleanly',
      stack: null,
      source: 'ANDROID_APP',
      userId: '00000000-0000-0000-0000-000000000001',
      resolved: true,
      timestamp: new Date(Date.now() - 180 * 60000).toISOString()
    }
  ]
};

module.exports = { supabase, store };
