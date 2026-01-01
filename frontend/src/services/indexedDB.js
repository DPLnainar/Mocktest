import { openDB } from 'idb'

/**
 * IndexedDB Service
 * Offline-first storage for code, submissions, and violations
 * 
 * Survives 5-minute Wi-Fi outages
 */

const DB_NAME = 'exam-portal-db'
const DB_VERSION = 1

// Initialize database
export async function initDB() {
  return openDB(DB_NAME, DB_VERSION, {
    upgrade(db) {
      // Code snapshots store
      if (!db.objectStoreNames.contains('code-snapshots')) {
        const codeStore = db.createObjectStore('code-snapshots', {
          keyPath: 'id',
          autoIncrement: true,
        })
        codeStore.createIndex('sessionId', 'sessionId')
        codeStore.createIndex('timestamp', 'timestamp')
      }

      // Submissions queue (pending sync)
      if (!db.objectStoreNames.contains('submissions-queue')) {
        const submissionStore = db.createObjectStore('submissions-queue', {
          keyPath: 'id',
          autoIncrement: true,
        })
        submissionStore.createIndex('sessionId', 'sessionId')
        submissionStore.createIndex('synced', 'synced')
      }

      // Violations queue
      if (!db.objectStoreNames.contains('violations-queue')) {
        const violationStore = db.createObjectStore('violations-queue', {
          keyPath: 'id',
          autoIncrement: true,
        })
        violationStore.createIndex('sessionId', 'sessionId')
        violationStore.createIndex('synced', 'synced')
      }
    },
  })
}

// ===== Code Snapshots =====

export async function saveCodeSnapshot(sessionId, code, language) {
  const db = await initDB()
  return db.add('code-snapshots', {
    sessionId,
    code,
    language,
    timestamp: Date.now(),
  })
}

export async function getLatestCodeSnapshot(sessionId) {
  const db = await initDB()
  const tx = db.transaction('code-snapshots', 'readonly')
  const index = tx.store.index('sessionId')
  const snapshots = await index.getAll(sessionId)
  
  if (snapshots.length === 0) return null
  
  // Return most recent
  return snapshots.sort((a, b) => b.timestamp - a.timestamp)[0]
}

export async function getAllCodeSnapshots(sessionId) {
  const db = await initDB()
  const tx = db.transaction('code-snapshots', 'readonly')
  const index = tx.store.index('sessionId')
  return index.getAll(sessionId)
}

export async function clearOldSnapshots(sessionId, keepLast = 50) {
  const db = await initDB()
  const snapshots = await getAllCodeSnapshots(sessionId)
  
  if (snapshots.length <= keepLast) return
  
  // Sort by timestamp, delete oldest
  const toDelete = snapshots
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(keepLast)
  
  const tx = db.transaction('code-snapshots', 'readwrite')
  await Promise.all(toDelete.map((s) => tx.store.delete(s.id)))
}

// ===== Submissions Queue =====

export async function queueSubmission(sessionId, code, language, testCases) {
  const db = await initDB()
  return db.add('submissions-queue', {
    sessionId,
    code,
    language,
    testCases,
    timestamp: Date.now(),
    synced: false,
  })
}

export async function getPendingSubmissions() {
  const db = await initDB()
  const tx = db.transaction('submissions-queue', 'readonly')
  const index = tx.store.index('synced')
  return index.getAll(false)
}

export async function markSubmissionSynced(id, submissionId) {
  const db = await initDB()
  const submission = await db.get('submissions-queue', id)
  if (submission) {
    submission.synced = true
    submission.submissionId = submissionId
    submission.syncedAt = Date.now()
    await db.put('submissions-queue', submission)
  }
}

// ===== Violations Queue =====

export async function queueViolation(sessionId, violation) {
  const db = await initDB()
  return db.add('violations-queue', {
    sessionId,
    ...violation,
    timestamp: Date.now(),
    synced: false,
  })
}

export async function getPendingViolations() {
  const db = await initDB()
  const tx = db.transaction('violations-queue', 'readonly')
  const index = tx.store.index('synced')
  return index.getAll(false)
}

export async function markViolationSynced(id) {
  const db = await initDB()
  const violation = await db.get('violations-queue', id)
  if (violation) {
    violation.synced = true
    violation.syncedAt = Date.now()
    await db.put('violations-queue', violation)
  }
}

// ===== Cleanup =====

export async function clearSessionData(sessionId) {
  const db = await initDB()
  
  // Clear code snapshots
  const codeSnapshots = await getAllCodeSnapshots(sessionId)
  await Promise.all(codeSnapshots.map((s) => db.delete('code-snapshots', s.id)))
  
  // Clear submissions
  const submissions = await db.getAllFromIndex('submissions-queue', 'sessionId', sessionId)
  await Promise.all(submissions.map((s) => db.delete('submissions-queue', s.id)))
  
  // Clear violations
  const violations = await db.getAllFromIndex('violations-queue', 'sessionId', sessionId)
  await Promise.all(violations.map((v) => db.delete('violations-queue', v.id)))
}

export default {
  initDB,
  saveCodeSnapshot,
  getLatestCodeSnapshot,
  getAllCodeSnapshots,
  clearOldSnapshots,
  queueSubmission,
  getPendingSubmissions,
  markSubmissionSynced,
  queueViolation,
  getPendingViolations,
  markViolationSynced,
  clearSessionData,
}
