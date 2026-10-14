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
  ]
};

module.exports = { supabase, store };
