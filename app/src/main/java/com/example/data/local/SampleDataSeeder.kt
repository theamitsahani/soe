package com.example.data.local

import com.example.data.model.School
import com.example.data.model.Task
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.VisitStatus

object SampleDataSeeder {

    val sampleUsers = listOf(
        User(
            uid = "admin_01",
            name = "Suresh Meena (Admin)",
            email = "admin@missiongyan.org",
            mobile = "9829012345",
            role = UserRole.ADMIN,
            designation = "State Coordinator",
            isActive = true
        ),
        User(
            uid = "emp_01",
            name = "Rajesh Sharma",
            email = "rajesh.sharma@soe.org",
            mobile = "9829123456",
            role = UserRole.EMPLOYEE,
            designation = "Senior Field Officer",
            isActive = true
        ),
        User(
            uid = "emp_02",
            name = "Priya Verma",
            email = "priya.verma@soe.org",
            mobile = "9829234567",
            role = UserRole.EMPLOYEE,
            designation = "Field Officer",
            isActive = true
        ),
        User(
            uid = "emp_03",
            name = "Anil Kumar",
            email = "anil.kumar@soe.org",
            mobile = "9829345678",
            role = UserRole.EMPLOYEE,
            designation = "Field Officer",
            isActive = true
        )
    )

    val sampleSchools = listOf(
        School(
            schoolId = "SCH_001",
            schoolName = "Govt. Mahatma Gandhi English Medium School",
            stateName = "Rajasthan",
            districtName = "Jaipur",
            blockName = "Sanganer",
            udiseCode = "08120104502",
            schoolType = "Sr. Secondary",
            principalName = "Dr. Rameshwar Dayal",
            principalMobile = "9414011223",
            address = "Near Bus Stand, Sanganer, Jaipur",
            pincode = "302029"
        ),
        School(
            schoolId = "SCH_002",
            schoolName = "Govt. Sr. Sec. School Mansarovar",
            stateName = "Rajasthan",
            districtName = "Jaipur",
            blockName = "Jaipur City",
            udiseCode = "08120108912",
            schoolType = "Sr. Secondary",
            principalName = "Smt. Sunita Choudhary",
            principalMobile = "9414022334",
            address = "Sector 3, Mansarovar, Jaipur",
            pincode = "302020"
        ),
        School(
            schoolId = "SCH_003",
            schoolName = "Govt. Model School Mandore",
            stateName = "Rajasthan",
            districtName = "Jodhpur",
            blockName = "Mandore",
            udiseCode = "08150201103",
            schoolType = "Sr. Secondary",
            principalName = "Shri Mahendra Singh",
            principalMobile = "9414033445",
            address = "Main Road, Mandore, Jodhpur",
            pincode = "342007"
        ),
        School(
            schoolId = "SCH_004",
            schoolName = "Govt. Girls Sr. Sec. School City",
            stateName = "Rajasthan",
            districtName = "Udaipur",
            blockName = "Girwa",
            udiseCode = "08190302214",
            schoolType = "Sr. Secondary",
            principalName = "Dr. Rekha Sharma",
            principalMobile = "9414044556",
            address = "Surajpole, Girwa, Udaipur",
            pincode = "313001"
        ),
        School(
            schoolId = "SCH_005",
            schoolName = "Govt. Excellence Secondary School",
            stateName = "Rajasthan",
            districtName = "Kota",
            blockName = "Ladpura",
            udiseCode = "08170105521",
            schoolType = "Secondary",
            principalName = "Shri Harish Gupta",
            principalMobile = "9414055667",
            address = "Talwandi, Ladpura, Kota",
            pincode = "324005"
        ),
        School(
            schoolId = "SCH_006",
            schoolName = "Govt. Sr. Sec. School Pushkar",
            stateName = "Rajasthan",
            districtName = "Ajmer",
            blockName = "Pushkar",
            udiseCode = "08010403310",
            schoolType = "Sr. Secondary",
            principalName = "Shri R.K. Rathore",
            principalMobile = "9414066778",
            address = "Brahma Temple Road, Pushkar, Ajmer",
            pincode = "305022"
        ),
        School(
            schoolId = "SCH_007",
            schoolName = "Govt. Mahatma Gandhi English Medium School",
            stateName = "Rajasthan",
            districtName = "Alwar",
            blockName = "Tijara",
            udiseCode = "08020504422",
            schoolType = "Sr. Secondary",
            principalName = "Smt. Manju Yadav",
            principalMobile = "9414077889",
            address = "Bhiwadi Road, Tijara, Alwar",
            pincode = "301411"
        ),
        School(
            schoolId = "SCH_008",
            schoolName = "Govt. Sr. Sec. School Nokha",
            stateName = "Rajasthan",
            districtName = "Bikaner",
            blockName = "Nokha",
            udiseCode = "08030206633",
            schoolType = "Sr. Secondary",
            principalName = "Shri Kishna Ram",
            principalMobile = "9414088990",
            address = "Railway Station Road, Nokha, Bikaner",
            pincode = "334803"
        )
    )

    val sampleTasks = listOf(
        Task(
            taskId = "TASK_001",
            schoolId = "SCH_001",
            employeeId = "emp_01",
            employeeName = "Rajesh Sharma",
            schoolName = "Govt. Mahatma Gandhi English Medium School",
            state = "Rajasthan",
            district = "Jaipur",
            block = "Sanganer",
            assignedBy = "Admin",
            visitDate = "2026-08-16",
            status = VisitStatus.ASSIGNED,
            notes = "Conduct Mission Gyan app orientation for class 9-12 students and check smart TV functionality.",
            createdAt = System.currentTimeMillis() - 86400000L * 2
        ),
        Task(
            taskId = "TASK_002",
            schoolId = "SCH_002",
            employeeId = "emp_01",
            employeeName = "Rajesh Sharma",
            schoolName = "Govt. Sr. Sec. School Mansarovar",
            state = "Rajasthan",
            district = "Jaipur",
            block = "Jaipur City",
            assignedBy = "Admin",
            visitDate = "2026-08-17",
            status = VisitStatus.ASSIGNED,
            notes = "Install SOE promotional posters, meet Principal and create WhatsApp group with BCI teacher.",
            createdAt = System.currentTimeMillis() - 86400000L
        ),
        Task(
            taskId = "TASK_003",
            schoolId = "SCH_003",
            employeeId = "emp_02",
            employeeName = "Priya Verma",
            schoolName = "Govt. Model School Mandore",
            state = "Rajasthan",
            district = "Jodhpur",
            block = "Mandore",
            assignedBy = "Admin",
            visitDate = "2026-08-16",
            status = VisitStatus.ASSIGNED,
            notes = "Follow up on student adoption metrics and smart classroom setup.",
            createdAt = System.currentTimeMillis() - 86400000L * 3
        ),
        Task(
            taskId = "TASK_004",
            schoolId = "SCH_004",
            employeeId = "emp_02",
            employeeName = "Priya Verma",
            schoolName = "Govt. Girls Sr. Sec. School City",
            state = "Rajasthan",
            district = "Udaipur",
            block = "Girwa",
            assignedBy = "Admin",
            visitDate = "2026-08-18",
            status = VisitStatus.ASSIGNED,
            notes = "Special digital awareness session for girl students in ICT lab.",
            createdAt = System.currentTimeMillis() - 86400000L * 2
        )
    )

    suspend fun seedIfNeeded(db: AppDatabase) {
        val userCount = db.userDao().getUserById("emp_01")
        if (userCount == null) {
            db.userDao().insertUsers(sampleUsers)
        }
        val school = db.schoolDao().getSchoolById("SCH_001")
        if (school == null) {
            db.schoolDao().insertSchools(sampleSchools)
        }
        val task = db.taskDao().getTaskById("TASK_001")
        if (task == null) {
            db.taskDao().insertTasks(sampleTasks)
        }
    }
}
