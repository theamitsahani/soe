package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.JsonHelper
import com.example.data.model.Visit
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExportHelper {

    fun exportVisitsToExcel(context: Context, visits: List<Visit>): File? {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("School Visits Report")

            // Header Row
            val headerRow = sheet.createRow(0)
            val headers = listOf(
                "Visit ID",
                "School Name",
                "District",
                "Block",
                "Field Officer",
                "Visit Date",
                "Visit Time",
                "UDISE Code",
                "Met Principal",
                "Mission Gyan Aware",
                "Classes Participated",
                "Students Attended",
                "BCI Teacher",
                "BCI Mobile",
                "WhatsApp Group",
                "Poster Installed",
                "Smart Class Status",
                "Observations",
                "Problems / Assistance",
                "Followup Required",
                "Photos Count",
                "Sync Status"
            )

            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
            }

            // Data Rows
            visits.forEachIndexed { index, visit ->
                val row = sheet.createRow(index + 1)
                val answers = JsonHelper.fromJson(visit.answersJson)
                val photos = JsonHelper.photosFromJson(visit.photosJson)

                row.createCell(0).setCellValue(visit.visitId)
                row.createCell(1).setCellValue(visit.schoolName)
                row.createCell(2).setCellValue(visit.district)
                row.createCell(3).setCellValue(visit.block)
                row.createCell(4).setCellValue(visit.employeeName)
                row.createCell(5).setCellValue(visit.visitDate)
                row.createCell(6).setCellValue(visit.visitTime)
                row.createCell(7).setCellValue(answers.udiseCode)
                row.createCell(8).setCellValue(answers.metPrincipal)
                row.createCell(9).setCellValue(answers.missionGyanAwareness)
                row.createCell(10).setCellValue(answers.participatingClasses.joinToString(", "))
                row.createCell(11).setCellValue(answers.totalStudentsAttended.toDouble())
                row.createCell(12).setCellValue(answers.bciTeacherName)
                row.createCell(13).setCellValue(answers.bciMobile)
                row.createCell(14).setCellValue(answers.whatsappGroupAdded)
                row.createCell(15).setCellValue(answers.posterInstalled)
                row.createCell(16).setCellValue(answers.smartClassStatus)
                row.createCell(17).setCellValue(answers.keyObservations)
                row.createCell(18).setCellValue(answers.problemsOrAssistance)
                row.createCell(19).setCellValue(answers.followupRequired)
                row.createCell(20).setCellValue(photos.size.toDouble())
                row.createCell(21).setCellValue(if (visit.isSynced) "Synced" else "Pending")
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "SOE_Visits_Report_$timeStamp.xlsx")

            FileOutputStream(file).use { out ->
                workbook.write(out)
            }
            workbook.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareExcelFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share SOE Visits Excel Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
