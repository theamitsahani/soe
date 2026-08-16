package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SampleDataSeeder
import com.example.data.model.*
import com.example.data.repository.SchoolRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.VisitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val schoolRepo = SchoolRepository(db.schoolDao())
    private val taskRepo = TaskRepository(db.taskDao())
    private val visitRepo = VisitRepository(db.visitDao())
    private val userDao = db.userDao()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val allUsers: StateFlow<List<User>> = userDao.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSchools: StateFlow<List<School>> = schoolRepo.getAllSchools()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDistricts: StateFlow<List<String>> = schoolRepo.getAllDistricts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<Task>> = taskRepo.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVisits: StateFlow<List<Visit>> = visitRepo.getAllVisits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unsyncedCount: StateFlow<Int> = visitRepo.getUnsyncedVisitsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            SampleDataSeeder.seedIfNeeded(db)
            // Auto select default employee if not selected
            val defaultEmp = userDao.getUserById("emp_01")
            if (defaultEmp != null && _currentUser.value == null) {
                _currentUser.value = defaultEmp
            }
        }
    }

    fun selectUser(user: User) {
        _currentUser.value = user
    }

    fun logout() {
        _currentUser.value = null
    }

    fun quickAddUser(name: String, mobile: String, role: UserRole) {
        viewModelScope.launch {
            val newUser = User(
                uid = "user_${System.currentTimeMillis()}",
                name = name,
                email = "${name.lowercase().replace(" ", ".")}@soe.org",
                mobile = mobile,
                role = role,
                designation = if (role == UserRole.ADMIN) "Coordinator" else "Field Officer"
            )
            userDao.insertUser(newUser)
            _currentUser.value = newUser
        }
    }

    fun saveSchool(school: School) {
        viewModelScope.launch {
            schoolRepo.saveSchool(school)
        }
    }

    fun assignTask(task: Task) {
        viewModelScope.launch {
            taskRepo.saveTask(task)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepo.deleteTask(taskId)
        }
    }

    fun submitVisit(visit: Visit, associatedTaskId: String? = null) {
        viewModelScope.launch {
            visitRepo.saveVisit(visit)

            // If this visit was from an assigned task, mark task as completed
            if (!associatedTaskId.isNullOrBlank()) {
                val task = taskRepo.getTaskById(associatedTaskId)
                if (task != null) {
                    val updatedTask = task.copy(
                        status = VisitStatus.COMPLETED,
                        visitId = visit.visitId
                    )
                    taskRepo.saveTask(updatedTask)
                }
            }

            // Trigger remote sync
            syncAllUnsyncedVisits()
        }
    }

    fun syncAllUnsyncedVisits() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val unsynced = db.visitDao().getUnsyncedVisits()
                for (v in unsynced) {
                    val success = visitRepo.saveVisitToRemote(v)
                    if (success) {
                        db.visitDao().updateVisit(v.copy(isSynced = true))
                    }
                }
            } catch (_: Exception) {
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncRemote() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                schoolRepo.syncSchoolsFromRemote()
                taskRepo.syncTasksFromRemote()
                visitRepo.syncVisitsFromRemote()
            } catch (_: Exception) {
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
