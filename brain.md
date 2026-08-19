# SOE School Visit — Production Architecture & Engineering Memory (`brain.md`)

*Permanent technical memory document for the SOE School Visit Management Application (Mission Gyan field inspections).*
*This document accurately reflects the current production codebase, data flows, security rules, and architectural invariants.*

---

## 1. Application Architecture Overview

- **App Name**: SOE School Visit (Launcher: `SOE School Visit`, Application ID: `com.aistudio.soeschoolvisit.app`)
- **Primary Purpose**: Field management platform for School of Excellence (SOE) / Mission Gyan field officers and administrators to track, assign, verify, and inspect school installations across Rajasthan.
- **Architectural Paradigm**: MVVM (Model-View-ViewModel / Repository) + Offline-First Local-First Persistence + Idempotent Cloud Synchronization.

```
┌─────────────────────────────────────────────────────────────┐
│                      Jetpack Compose UI                     │
│  (AdminMainScreen / EmployeeMainScreen / VisitFormScreen)   │
└──────────────┬───────────────────────────────▲──────────────┘
               │ Actions / Invocations         │ State / Flow Observation
┌──────────────▼───────────────────────────────┴──────────────┐
│                    Repository Layer                         │
│ (AuthRepository, SchoolRepository, TaskRepository, Visit...) │
└──────────────┬───────────────────────────────▲──────────────┘
               │                               │
       ┌───────▼────────┐              ┌───────┴────────┐
       │   Local Room   │              │   Firestore    │
       │ SQLite DB v11  │              │ Cloud Database │
       └────────────────┘              └────────────────┘
               ▲                               ▲
               └─────────── SyncManager ───────┘
                     (Mutex-Protected Auto-Sync)
```

---

## 2. Entity & Identity Integrity

Every primary entity in the system is identified by an immutable, deterministic primary key. Natural names (e.g. `schoolName`, `employeeEmail`, `principalName`) are never used as relational foreign keys.

| Entity | Primary ID Key | ID Format / Generation | Description |
|---|---|---|---|
| **School** | `schoolId` | `sch_<uuid/hash>` | Master school record |
| **Task** | `taskId` | `task_<uuid>` | Administrative assignment |
| **Visit** | `visitId` | `vst_<uuid>` | Field inspection submission report |
| **VisitPhoto** | `photoId` | `pho_<visitId>_<cat>_<uuid>` | Individual inspection photo asset |
| **User/Employee** | `userId` | Firebase Auth `uid` | Authenticated officer or administrator |
| **AuditEvent** | `eventId` | `evt_<visitId>_<timestamp>` | Append-only lifecycle event |
| **SyncQueueItem** | `operationId` | `op_<entityId>_<timestamp>` | Scheduled background synchronization job |
| **ImportBatch** | `importBatchId` | `batch_<uuid>` | Bulk Excel/CSV import log and audit record |

---

## 3. Separation of Concerns: School vs Task vs Visit

- **School (Master Data)**: Permanent registry of schools with UDISE codes, contact numbers, and block/district mappings.
- **Task (Assignment Workflow)**: Represents an admin dispatching an officer to inspect a school on a specific scheduled date.
- **Visit (Inspection Record)**: The actual on-ground report submitted by the officer with questionnaire responses, photo evidence, and geolocation stamps.
- **Invariant**: Deleting or archiving a task or school **NEVER** deletes completed historical visits. Soft-delete only flags `isDeleted = true` on the target entity.

---

## 4. Role-Based Access Control (RBAC) & Central Permissions

Managed centrally via `AppPermissionManager.kt`:
- **Supported Roles**:
  - `UserRole.ADMIN`: Complete administrative control (User management, school CRUD, task assignment, report export, audit review).
  - `UserRole.SUPER_ADMIN`: High-level multi-region admin privilege.
  - `UserRole.SUPERVISOR`: Region lead with task creation and report review capabilities.
  - `UserRole.REVIEWER`: Dedicated auditor with read and visit review/approval capabilities.
  - `UserRole.EMPLOYEE`: Field officer with task execution, offline form entry, and visit submission capabilities.

### Centralized Permission Functions:
- `canCreateTask(role)` / `canAssignTask(role)`: Admin & Supervisor
- `canManageSchools(role)`: Admin & Supervisor
- `canManageEmployees(role)`: Admin
- `canReviewVisit(role)`: Admin, Supervisor, Reviewer
- `canExportReport(role)`: Admin, Supervisor, Reviewer
- `canStartVisit(role, taskEmployeeId, currentUid)`: Assigned employee or Admin
- `canEditVisit(role, visitEmployeeId, currentUid, status, submittedAt, editCount)`: Owner only before `REVIEWED` status; within 12h for `SUBMITTED` visits (max 1 edit).

---

## 5. Visit Lifecycle, Immutability & Revisions

### State Transitions:
`ASSIGNED` ──► `STARTED` ──► `IN_PROGRESS` ──► `SUBMITTED` ──► `REVIEWED` (Locked)
                                                    │
                                                    └──► `REJECTED` (with reason)
                                                              │
                                                              ▼
                                                        `IN_PROGRESS` (Revision increment)

### Immutability & Revision Tracking:
- `status = VisitStatus.REVIEWED` locks all further edits on both client and server (enforced via Firestore rules).
- Resubmission increments `editCount` and records `revisionNumber`, `previousRevisionId`, and `correctionReason`.
- Audit logs in `visit_events` (and Firestore `/visits/{visitId}/visitEvents/{eventId}`) record every state transition with timestamp, actor ID, and GPS coordinates.

---

## 6. Central Business Rules & Validators

- **`TaskValidator`**:
  - Validates school selection, employee assignment, and target dates.
  - Rejects attempts by an officer to start another officer's task.
  - Rejects starting cancelled or reviewed tasks.
- **`VisitValidator`**:
  - Enforces mandatory hardware questions, smart classroom answers, student counts, and basic school details.
  - Ensures officer coordinates and timestamps are captured.
- **`PhotoValidator`**:
  - Enforces mandatory photo categories (`school_photo`, `explaining_app`, `students_smart_board`, `principal_photo`, `letter_photo`).
  - Validates image size limits (max 15MB) and payload format.
- **`ImportValidator`**:
  - Validates CSV and Excel header rows and required Column C (School Name).
  - Sanitizes scientific notation and float formatting from principal mobile numbers.

---

## 7. Photo Reliability & State Machine

Photos are tracked via the `visit_photos` table and `VisitPhoto` domain model:
```
CAPTURED ──► LOCAL_SAVED ──► PENDING ──► UPLOADING ──► UPLOADED
                                              │
                                              ▼ (on network failure)
                                            FAILED (auto-retried)
```
- Local files are preserved in app-internal cache until Cloudinary or Firebase Storage upload is verified.
- Pre-upload hash verification prevents duplicate uploads.

---

## 8. Offline-First Local Database (Room v11)

- **Database**: `AppDatabase` (Version 11) with non-destructive migrations (`MIGRATION_6_7` through `MIGRATION_10_11`).
- **Entities**:
  - `School` (`schools`)
  - `Visit` (`visits`)
  - `Task` (`tasks`)
  - `UserEntity` (`users`)
  - `AppNotification` (`app_notifications`)
  - `VisitEvent` (`visit_events`)
  - `ImportBatch` (`import_batches`)
  - `SyncQueueItem` (`sync_queue`)
  - `VisitPhoto` (`visit_photos`)
- **Data Protection Invariant**: `syncVisitsFromFirestore` and sync sweeps never purge local records marked `PENDING` or `FAILED`. Local drafts auto-save to Room after every form category.

---

## 9. Cloud Firestore Security Rules Summary (`firestore.rules`)

1. **Privilege Escalation Prevention (`/users/{userId}`)**: Non-admin users cannot alter their role, status, or deletion flags.
2. **Task Protection (`/tasks/{taskId}`)**: Employees can only read tasks assigned to their UID/email; employees cannot reassign task ownership.
3. **Visit Integrity (`/visits/{visitId}`)**:
   - Employees can only submit/update their own reports (`request.resource.data.employeeId == request.auth.uid`).
   - Updates are forbidden once a report reaches `REVIEWED` status.
   - Resubmission fields (`editCount`, `answersJson`, `photosJson`, `updatedAt`) are strictly validated against mutation of immutable fields (`schoolId`, `createdAt`, `employeeId`).
4. **Audit Trail Subcollection (`/visits/{visitId}/visitEvents/{eventId}`)**: Append-only. Updates and deletions are denied (`allow update, delete: if false`).
5. **Import Batches (`/importBatches/{batchId}`)**: Accessible strictly to authenticated Admins.
6. **Notifications (`/notifications/{notifId}`)**: Authenticated recipients can read and mark alerts as read; admins can broadcast.

---

## 10. Automated Test Suite

- **Test Suite**: `app/src/test/java/com/example/`
  - `ProductionHardeningTest.kt`: Unit tests verifying RBAC permissions, task validation, photo validation, import validation, and entity defaults.
  - `ExampleRobolectricTest.kt`: Context string and resource verification under Robolectric JVM runner.
  - `GreetingScreenshotTest.kt`: Theme and visual snapshot test under Roborazzi.
- **Verification Status**: `BUILD SUCCESSFUL` via `gradle :app:testDebugUnitTest`.

---

## 11. Known Limitations & Technical Debt

- **Cloudinary Secret Separation**: Cloudinary uploads utilize signed tokens from the backend (`https://cloudinary-server-six.vercel.app/api/sign-upload`); no private API secret is embedded in the APK.
- **Single Active Mutex**: While `SyncManager` serializes uploads per device, concurrent writes across multiple devices for the same school are handled via deterministic last-write-wins on school metadata while preserving separate visit IDs.
