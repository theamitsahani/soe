# SOE School Visit — Technical Brain & Architectural Memory (`brain.md`)

*Permanent technical memory document for the SOE School Visit Management Application (Mission Gyan field visits).*
*This document accurately reflects the current production codebase, data flows, security rules, and architectural invariants.*

---

## 1. Application Overview

- **App Name**: SOE School Visit (Launcher name: `SOE School Visit`, Android Application ID: `com.aistudio.soeschoolvisit.app`)
- **Primary Purpose**: Field management application designed for School of Excellence (SOE) / Mission Gyan field officers and administrators to track, assign, verify, and document school visits across districts and blocks in Rajasthan.
- **Key Capabilities**:
  - **Role-Based Authentication**: Admin and Field Officer (Employee) login with session recovery and temporary password provisioning.
  - **School Roster Management**: In-app CRUD, search/filtering, and bulk Excel/CSV import and export.
  - **Task Assignment Engine**: Admins assign schools and target visit dates to employees; employees receive real-time task lists.
  - **14-Category Visit Inspection Form**: Rich structured assessment covering hardware, infrastructure, smart classrooms, student metrics, and photos with watermark timestamps.
  - **Photo Storage Pipeline**: Uploads optimized compressed images to Cloudinary via a secure Vercel backend signature service, with fallback to Firebase Storage.
  - **Offline-First Resilience (`SyncManager`)**: Complete local persistence in SQLite via Room DB. Visits submitted offline queue as `PENDING` with auto-sync when network returns, protected by SharedPreferences backup during migrations and serialized via coroutine mutexes to eliminate duplicate writes.
  - **Real-Time Synchronization & Push/In-App Alerts**: Firestore real-time snapshot listeners for schools, tasks, visits, and notifications.

---

## 2. Repository & Directory Structure

```
/
├── app/
│   ├── build.gradle.kts                      # Module build config, plugins, dependencies, signing
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml           # Manifest permissions, Application declaration, MainActivity
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt           # App entry point, session routing, global state observation
│       │   │   ├── data/
│       │   │   │   ├── local/                # Room DB Persistence Layer
│       │   │   │   │   ├── AppDatabase.kt    # Room database declaration (entities & migration logic)
│       │   │   │   │   ├── AppNotificationDao.kt # Notification DAO
│       │   │   │   │   ├── SchoolDao.kt      # School DAO (CRUD, search, soft-delete)
│       │   │   │   │   ├── TaskDao.kt        # Task DAO (assignment, status updates)
│       │   │   │   │   ├── UserDao.kt        # User/Employee DAO
│       │   │   │   │   └── VisitDao.kt       # Visit report DAO (sync status queries)
│       │   │   │   ├── model/                # Domain and Entity Data Models
│       │   │   │   │   ├── AppNotification.kt# In-app notifications model & entity
│       │   │   │   │   ├── PhotoCategory.kt  # 14 photo categories & metadata definitions
│       │   │   │   │   ├── School.kt         # School entity & domain model
│       │   │   │   │   ├── Task.kt           # Assigned task entity & domain model
│       │   │   │   │   ├── User.kt           # User profile & entity (UserRole, UserStatus)
│       │   │   │   │   ├── Visit.kt          # Visit report entity (VisitStatus, SyncStatus)
│       │   │   │   │   └── VisitAnswers.kt   # Serialized survey questionnaire data structures
│       │   │   │   └── repository/           # Single-source-of-truth Repositories
│       │   │   │       ├── AuthRepository.kt # Login, sessions, employee CRUD, temp passwords
│       │   │   │       ├── NotificationRepository.kt # In-app/system alerts & Firestore sync
│       │   │   │       ├── SchoolRepository.kt# Schools sync, search, import, and CRUD
│       │   │   │       ├── TaskRepository.kt # Task creation, status updates, employee assignment
│       │   │   │       └── VisitRepository.kt# Visit submission, duplicate prevention, sync
│       │   │   ├── ui/
│       │   │   │   ├── admin/                # Admin Panel UI Tabs
│       │   │   │   │   ├── AdminDashboardTab.kt   # High-level metrics, summary cards, recent visits
│       │   │   │   │   ├── AdminMainScreen.kt     # TopAppBar, NavigationBar, notification bell
│       │   │   │   │   ├── AssignVisitsTab.kt     # Assign schools to employees with filters
│       │   │   │   │   ├── EmployeeManagementTab.kt# Add/Edit/Soft-Delete/Restore officers
│       │   │   │   │   ├── PhotoGalleryTab.kt     # Photo stream across all inspections
│       │   │   │   │   ├── ReportsTab.kt          # Full inspection reports & Excel export
│       │   │   │   │   ├── SchoolManagementTab.kt # School roster, manual add, Excel import
│       │   │   │   │   └── SettingsTab.kt         # Admin password change & system settings
│       │   │   │   ├── auth/
│       │   │   │   │   └── LoginScreen.kt         # Clean, high-contrast M3 login interface
│       │   │   │   ├── components/            # Reusable UI widgets & custom cards
│       │   │   │   ├── employee/              # Field Officer UI
│       │   │   │   │   ├── EmployeeMainScreen.kt  # Assigned tasks, history, sync status badge
│       │   │   │   │   └── VisitFormScreen.kt     # 14-category multi-step inspection form
│       │   │   │   └── theme/                 # Material 3 Color Schemes & Typography
│       │   │   │       ├── Color.kt
│       │   │   │       ├── Theme.kt
│       │   │   │       └── Type.kt
│       │   │   └── util/                      # Utilities & Infrastructure
│       │   │       ├── AppNotificationHelper.kt# System tray notifications & channels
│       │   │       ├── CloudinaryUploader.kt  # Vercel backend signature & direct Cloudinary API
│       │   │       ├── ExcelHelper.kt         # CSV/XLSX parser & report generator
│       │   │       ├── FirebaseUtils.kt       # Firebase Auth, Firestore, Storage singletons
│       │   │       ├── MediaStorageHelper.kt  # Photo compression, local caching, watermarking
│       │   │       ├── SyncManager.kt         # Network monitor, background sync mutex, backup
│       │   │       └── ZipHelper.kt           # ZIP utility for media downloads/exports
│       │   └── res/                           # Android drawables, layouts, mipmaps, strings
├── firestore.rules                            # Firestore Security Rules
├── metadata.json                              # AI Studio Application metadata
└── settings.gradle.kts                        # Root project configuration
```

---

## 3. High-Level Architecture

The project follows a modified **MVVM (Model-View-ViewModel / Repository)** architecture with **Offline-First** durability:

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
       │    SQLite DB   │              │ Cloud Database │
       └────────────────┘              └────────────────┘
               ▲                               ▲
               └─────────── SyncManager ───────┘
                     (Mutex-Protected Auto-Sync)
```

1. **UI Layer**: Compose-based reactive screens observing StateFlows and Room Flows.
2. **Repository Layer**: Coordinates local cache (Room) and cloud sync (Firestore). All user mutations write to Room first to ensure offline responsiveness.
3. **Background Sync Engine (`SyncManager`)**: Listens to Android network connectivity via `ConnectivityManager.NetworkCallback`. When online, systematically drains pending visits and media uploads in sequential mutex locks to prevent server race conditions.
4. **Media Pipeline**: Photos captured by the device are compressed locally, watermarked with GPS coordinates/timestamps, stored in local app storage, and uploaded asynchronously to Cloudinary via Vercel sign API.

---

## 4. Authentication & User Roles

- **Role Definitions**:
  - `UserRole.ADMIN`: Full administrative control (School management, task assignment, officer management, photo gallery, Excel imports/exports).
  - `UserRole.EMPLOYEE`: Field Officer access (View assigned tasks, perform and submit visit inspections, view personal visit history, sync offline data).
- **Authentication Flow**:
  1. `AuthRepository.login(email, password)` verifies credentials against Firebase Authentication.
  2. If successful, user document `/users/{uid}` is fetched from Firestore to establish role, status, and profile details.
  3. If status is `INACTIVE` or `isDeleted == true`, session is terminated and login rejected with a localized message.
  4. Local Room user cache is populated, and `_currentUser` StateFlow emits the active user.
  5. **Offline Login Fallback**: If the network is unavailable, credentials and profile are checked against local Room `UserEntity` cache.
- **Admin Password Reset & Officer Creation**:
  - Admin can provision new employees using `AuthRepository.saveEmployee()`. A secondary `FirebaseApp` instance (`SecondaryAuthApp`) registers the Firebase Auth account with a secure temporary password without logging out the active admin.
  - Newly created employees receive `mustChangePassword = true`.

---

## 5. Screen & Navigation Flow

The app utilizes state-driven navigation inside `MainActivity.kt` via `ScreenState`:

```
                    ┌─────────────────┐
                    │ ScreenState.    │
                    │      Login      │
                    └────────┬────────┘
                             │ (Authenticated)
              ┌──────────────┴──────────────┐
              ▼                             ▼
   ┌────────────────────┐        ┌────────────────────┐
   │    ScreenState.    │        │    ScreenState.    │
   │       Admin        │        │      Employee      │
   └──────────┬─────────┘        └──────────┬─────────┘
              │                             │
    Admin Tab Navigation:           Employee Actions:
    0: Dashboard                    - Task list / Status
    1: Employees                    - Offline sync indicator
    2: Schools (Excel Import)       - Start Inspection ──┐
    3: Assign Visits                - View Past Visits   │
    4: Reports (Excel Export)                            │
    5: Photo Gallery                                     ▼
    6: Settings                           ┌────────────────────┐
                                          │    ScreenState.    │
                                          │     VisitForm      │
                                          └────────────────────┘
```

---

## 6. Data Models & Entities

### 6.1 User (`User.kt` & `UserEntity`)
- `userId: String` (Primary Key, Firebase UID)
- `name: String`, `email: String`, `mobile: String`, `state: String`, `district: String`
- `role: UserRole` (`ADMIN` or `EMPLOYEE`)
- `status: UserStatus` (`ACTIVE` or `INACTIVE`)
- `isDeleted: Boolean`, `deletedAt: Long`, `mustChangePassword: Boolean`, `createdAt: Long`

### 6.2 School (`School.kt` & `SchoolEntity`)
- `schoolId: String` (Primary Key, e.g., `sch_123456`)
- `sr: String` (Serial number from Excel)
- `schoolName: String`, `schoolType: String`, `stateName: String`, `districtName: String`, `blockName: String`, `villageName: String`
- `principalName: String`, `principalMobile: String`
- `visitDate: String` (Populated if visit is completed, e.g. `2025-02-15`)
- `isDeleted: Boolean`, `deletedAt: Long`, `createdAt: Long`, `updatedAt: Long`

### 6.3 Task (`Task.kt` & `TaskEntity`)
- `taskId: String` (Primary Key, e.g., `task_abcdef123`)
- `schoolId: String`, `schoolName: String`, `district: String`, `block: String`
- `employeeId: String`, `employeeName: String`, `employeeEmail: String`
- `visitDate: String` (Target scheduled date)
- `status: VisitStatus` (`ASSIGNED`, `STARTED`, `IN_PROGRESS`, `SUBMITTED`, `REVIEWED`, `CANCELLED`)
- `visitId: String` (Linked once submitted)
- `notes: String`, `isDeleted: Boolean`, `createdAt: Long`, `updatedAt: Long`

### 6.4 Visit (`Visit.kt` & `VisitEntity`)
- `visitId: String` (Primary Key, e.g., `vst_987654`)
- `taskId: String`, `schoolId: String`, `employeeId: String`, `employeeName: String`, `schoolName: String`, `district: String`, `block: String`, `visitDate: String`
- **School Metadata Snapshot Fields**:
  - `udiseCode: String` (Immutable snapshot of School UDISE at visit time)
  - `schoolType: String`, `villageName: String`
  - `principalName: String`, `principalMobile: String`
- **Lifecycle & Geo Tracking Timestamps**:
  - `startedAt: Long?` (Epoch milliseconds when the officer began the visit)
  - `completedAt: Long?` (Epoch milliseconds when the form was finalized)
  - `submittedAt: Long?` (Epoch milliseconds when the report was submitted)
  - `startLatitude: Double?`, `startLongitude: Double?`
  - `submitLatitude: Double?`, `submitLongitude: Double?`
- **Review & Verification**:
  - `reviewedBy: String`, `reviewedAt: Long?`, `reviewNotes: String`, `rejectionReason: String`
- `status: VisitStatus` (`CREATED`, `ASSIGNED`, `STARTED`, `IN_PROGRESS`, `SUBMITTED`, `REVIEWED`, `REJECTED`, `CANCELLED`)
- `answersJson: String` (Moshi-serialized `VisitAnswers`)
- `photosJson: String` (Moshi-serialized `Map<String, List<String>>` mapping category IDs to photo URLs)
- `editCount: Int`
- `syncStatus: SyncStatus` (`PENDING`, `SYNCING`, `SYNCED`, `FAILED`)
- `appVersion: String`
- `createdAt: Long`, `updatedAt: Long`

### 6.5 VisitEvent Audit Trail (`VisitEvent.kt` & `VisitEventEntity`)
- `eventId: String` (Primary Key, e.g., `evt_visitId_timestamp_random`)
- `visitId: String`, `taskId: String`, `schoolId: String`
- `eventType: String` (`VISIT_CREATED`, `VISIT_STARTED`, `VISIT_AUTOSAVED`, `VISIT_SUBMITTED`, `VISIT_REVIEWED`, `VISIT_REJECTED`, `VISIT_EDITED`, `PHOTO_DELETED`)
- `actorId: String`, `actorRole: String`, `timestamp: Long`, `details: String`, `latitude: Double?`, `longitude: Double?`

---

## 7. Local Database (Room)

- **Database Class**: `AppDatabase` (`@Database(version = 10)`)
- **Key DAOs**:
  - `UserDao`: Handles employee caching and soft-delete queries.
  - `SchoolDao`: Full-text/LIKE search across names, blocks, and districts.
  - `TaskDao`: Filtering active tasks for current employee UID.
  - `VisitDao`: Queries visits by sync status (`PENDING`, `SYNCING`, `SYNCED`, `FAILED`), manages lifecycle updates, and selective deletion that **preserves unsynced offline records**.
  - `VisitEventDao`: Records and queries chronological audit events per visit ID.
  - `AppNotificationDao`: Unread counters and user alert lists.
- **Data Protection Invariant**: `deleteVisitsNotIn()` and cleanup methods in `VisitDao` and `VisitRepository` strictly preserve local rows where `syncStatus != SYNCED` to prevent deleting offline reports before upload. Automatic SharedPreferences fallback backup ensures offline draft preservation across Room migrations.

---

## 8. Cloud Firestore Architecture & Security Rules

### 8.1 Collections
- `/users/{userId}`: User accounts and role documents.
- `/schools/{schoolId}`: School records and completion statuses.
- `/tasks/{taskId}`: Task assignments mapped to employee IDs.
- `/visits/{visitId}`: Comprehensive visit reports with full historical school snapshot and lifecycle metadata.
- `/visits/{visitId}/visitEvents/{eventId}`: Sub-collection storing immutable audit trail entries for each lifecycle transition.
- `/notifications/{notifId}`: In-app system alerts.

### 8.2 Security Rules Overview (`firestore.rules`)
- **Admins**: Read and write access across all collections.
- **Employees**:
  - Can read school rosters.
  - Can read only tasks where `request.auth.uid == resource.data.employeeId` or `email == request.auth.token.email`.
  - Can create and update their own visits (`resource.data.employeeId == request.auth.uid`). Once marked `REVIEWED`, modifications are locked to admins only.
  - Cannot self-escalate roles in `/users/{userId}`.

---

## 9. Media & Photo Storage (Cloudinary + Local Cache)

1. **Capture & Compression**: `MediaStorageHelper.kt` compresses camera bitmaps to standard resolution (~1280x720 JPEG @ 80% quality) and writes dynamic watermark overlays (Date, Time, School Name, Lat/Lng).
2. **Signature & Upload**: `CloudinaryUploader.kt` requests a signed upload token from the Vercel signature endpoint (`https://cloudinary-server-six.vercel.app/api/sign-upload`) and posts the multipart image payload directly to Cloudinary.
3. **Fallback Strategy**: If Cloudinary upload fails, fallback to Firebase Cloud Storage is executed, returning the HTTPS public download URL.

---

## 10. Background Sync & Offline Durability (`SyncManager`)

- **Singleton Pattern**: Managed via `SyncManager.getInstance(context)` to maintain a single unified network listener.
- **Mutex Serialization**: `syncMutex` prevents parallel concurrent uploads of the same visit report during rapid network reconnection.
- **SharedPreferences Safeguard**: Pending visits are serialized to `SharedPreferences` as JSON (`backupPendingVisits()`). In the event of a database upgrade or Room recreation, `restorePendingVisitsFromBackup()` rehydrates pending submissions.
- **Automatic Status Transition**:
  ```
  [Offline Submit] ──► Room DB (syncStatus = PENDING)
                              │
                    (Network Detected)
                              │
                              ▼
                       (syncStatus = SYNCING)
                              │
                    ┌─────────┴─────────┐
                    ▼                   ▼
                [Success]           [Failure]
                    │                   │
                    ▼                   ▼
            (syncStatus = SYNCED)  (syncStatus = PENDING / FAILED)
  ```

---

## 11. Excel & CSV Import / Export Workflow

- **Import (`ExcelHelper.parseSchoolsFile()`)**:
  - Supports CSV and Excel (`.xlsx`/`.xls`) files.
  - Robust column header normalization: matches headers like `School Name`, `School`, `School_Name`, `District`, `Block`, `Principal`, `Mob`, `Visit Date`.
  - Auto-detects completed status: If a valid `Visit Date` column is present in the imported spreadsheet, the school is marked `COMPLETED` and a corresponding manual visit record is generated in both Room and Firestore.
  - Batched in chunks of 400 operations to respect Firestore's 500-operation transaction limits.
- **Export (`ExcelHelper.exportVisitsToExcel()`)**:
  - Produces formatted `.csv` reports containing all school information, inspection answers, compliance scores, and photo URLs for spreadsheet analysis.

---

## 12. Task Assignment & Visit Reporting Lifecycle

1. **Admin Assignment**: Admin chooses a school and employee in `AssignVisitsTab`. Firestore `/tasks/{taskId}` is created with `status = "ASSIGNED"`.
2. **Employee Task Notification**: Real-time snapshot listener on the employee's device detects the assignment and adds it to their task feed.
3. **Execution**: Employee opens `VisitFormScreen`, fills out all categories, captures required photos, and taps Submit.
4. **Completion Flow**:
   - Visit is saved locally in Room with `syncStatus = PENDING` or `SYNCED`.
   - School is marked as completed (`isCompleted = true`, `visitDate = today`).
   - Task status is updated to `SUBMITTED`.
   - Admin receives in-app alert via `NotificationRepository`.

---

## 13. Notifications Engine

- **Notification Model**: `AppNotification` with fields `title`, `message`, `type`, `recipientUserId`, `isRead`, `timestamp`.
- **Channels**: System notification channel `"soe_visits_channel"` created in `AppNotificationHelper.kt` for background alerts.
- **Permission Flow**: Runtime notification permission (`POST_NOTIFICATIONS` on Android 13+) is strictly requested **after** user authentication/session detection, ensuring the initial login screen is clean and free of unsolicited system dialogs.
- **Real-Time Sync**: Snapshot listener routes new alerts to system tray if the app is backgrounded or in-app bell menu if active.

---

## 14. Key Dependencies & Configuration

- **Kotlin & Compose**: Jetpack Compose with Material 3.
- **Room Database**: Version 5 with Kotlin Symbol Processing (`ksp`).
- **Firebase BOM**: Firestore, Auth, Storage, Functions.
- **Moshi**: JSON serialization for complex nested models (`VisitAnswers`).
- **OkHttp & Retrofit**: Media upload network handling.
- **Cloudinary Endpoint**: Configured via `VERCEL_API_BASE_URL` in `build.gradle.kts`.

---

## 15. Crucial Bug Fixes & Architectural Invariants

1. **No Destructive Wipe of Unsynced Work**:
   - `VisitRepository.syncVisitsFromFirestore()` MUST NEVER delete local rows with `syncStatus == PENDING` or `FAILED`.
2. **True Duplicate Resolution**:
   - Grouping visits during sync MUST use `taskId` or `${schoolId}_${employeeId}_${visitDate}` rather than only `${schoolId}_${employeeId}` to prevent merging different legitimate inspection visits.
3. **SyncManager Singleton**:
   - Always access `SyncManager` via `SyncManager.getInstance(context)`. Never instantiate multiple instances.
4. **Admin Bootstrap Security**:
   - One-time admin creation bootstrap in `AuthRepository` ONLY triggers if the `/users` collection is entirely empty.
5. **Post-Login Permission Request**:
   - Notification permissions are requested only after a valid user session is confirmed, not on initial application launch before login.
6. **No Synthetic / Fake Data**:
   - Do NOT inject sample or mock users/schools into production code. Use real repository calls and genuine Room/Firestore data.

---

## 16. Mandatory Rules for Future AIs

1. **Update `brain.md` on Architectural Changes**: Whenever you add a new screen, modify data models, alter sync logic, or add background services, immediately update this file.
2. **Maintain Scope Discipline**: Do not add unrequested features, sidebars, or external libraries.
3. **Keep `metadata.json` and `strings.xml` in sync**: The platform app name and Android `app_name` resource must always match.
4. **Verify via `compile_applet`**: Ensure the application compiles cleanly after any structural changes.
