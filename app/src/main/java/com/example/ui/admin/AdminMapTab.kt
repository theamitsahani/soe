package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.School
import com.example.data.model.Task
import com.example.data.model.Visit
import com.example.ui.components.SchoolInteractiveMapView
import com.example.ui.theme.Navy900

@Composable
fun AdminMapTab(
    schools: List<School>,
    tasks: List<Task>,
    visits: List<Visit>,
    onViewSchoolReport: ((String) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SchoolInteractiveMapView(
            schools = schools,
            tasks = tasks,
            visits = visits,
            onViewDetails = { item ->
                if (item.visitId.isNotBlank()) {
                    onViewSchoolReport?.invoke(item.visitId)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
