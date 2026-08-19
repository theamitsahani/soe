package com.example.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700

object IndiaLocationData {

    val STATES_MAP: Map<String, List<String>> = linkedMapOf(
        "Rajasthan" to listOf(
            "Ajmer", "Alwar", "Anupgarh", "Balotra", "Banswara", "Baran", "Barmer", "Beawar",
            "Bharatpur", "Bhilwara", "Bikaner", "Bundi", "Chittorgarh", "Churu", "Dausa", "Deeg",
            "Dholpur", "Didwana-Kuchaman", "Dudu", "Dungarpur", "Gangapur City", "Hanumangarh",
            "Jaipur", "Jaipur Rural", "Jaisalmer", "Jalore", "Jhalawar", "Jhunjhunu", "Jodhpur",
            "Jodhpur Rural", "Karauli", "Kekri", "Khairthal-Tijara", "Kota", "Kotputli-Behror",
            "Nagaur", "Neem Ka Thana", "Pali", "Phalodi", "Pratapgarh", "Rajsamand", "Salumber",
            "Sanchore", "Sawai Madhopur", "Shahpura", "Sikar", "Sirohi", "Sri Ganganagar", "Tonk",
            "Udaipur"
        ),
        "Madhya Pradesh" to listOf(
            "Agar Malwa", "Alirajpur", "Anuppur", "Ashoknagar", "Balaghat", "Barwani", "Betul",
            "Bhind", "Bhopal", "Burhanpur", "Chhatarpur", "Chhindwara", "Damoh", "Datia", "Dewas",
            "Dhar", "Dindori", "Guna", "Gwalior", "Harda", "Hoshangabad", "Indore", "Jabalpur",
            "Jhabua", "Katni", "Khandwa", "Khargone", "Mandla", "Mandsaur", "Morena", "Narsinghpur",
            "Neemuch", "Niwari", "Panna", "Raisen", "Rajgarh", "Ratlam", "Rewa", "Sagar", "Satna",
            "Sehore", "Seoni", "Shahdol", "Shajapur", "Sheopur", "Shivpuri", "Sidhi", "Singrauli",
            "Tikamgarh", "Ujjain", "Umaria", "Vidisha"
        ),
        "Uttar Pradesh" to listOf(
            "Agra", "Aligarh", "Ambedkar Nagar", "Amethi", "Amroha", "Auraiya", "Ayodhya",
            "Azamgarh", "Baghpat", "Bahraich", "Ballia", "Balrampur", "Banda", "Barabanki",
            "Bareilly", "Basti", "Bhadohi", "Bijnor", "Budaun", "Bulandshahr", "Chandauli",
            "Chitrakoot", "Deoria", "Etah", "Etawah", "Farrukhabad", "Fatehpur", "Firozabad",
            "Gautam Buddha Nagar", "Ghaziabad", "Ghazipur", "Gonda", "Gorakhpur", "Hamirpur",
            "Hapur", "Hardoi", "Hathras", "Jalaun", "Jaunpur", "Jhansi", "Kannauj", "Kanpur Dehat",
            "Kanpur Nagar", "Kasganj", "Kaushambi", "Kheri", "Kushinagar", "Lalitpur", "Lucknow",
            "Maharajganj", "Mahoba", "Mainpuri", "Mathura", "Mau", "Meerut", "Mirzapur",
            "Moradabad", "Muzaffarnagar", "Pilibhit", "Pratapgarh", "Prayagraj", "Raebareli",
            "Rampur", "Saharanpur", "Sambhal", "Sant Kabir Nagar", "Shahjahanpur", "Shamli",
            "Shravasti", "Siddharthnagar", "Sitapur", "Sonbhadra", "Sultanpur", "Unnao", "Varanasi"
        ),
        "Haryana" to listOf(
            "Ambala", "Bhiwani", "Charkhi Dadri", "Faridabad", "Fatehabad", "Gurugram", "Hisar",
            "Jhajjar", "Jind", "Kaithal", "Karnal", "Kurukshetra", "Mahendragarh", "Nuh",
            "Palwal", "Panchkula", "Panipat", "Rewari", "Rohtak", "Sirsa", "Sonipat", "Yamunanagar"
        ),
        "Delhi" to listOf(
            "Central Delhi", "East Delhi", "New Delhi", "North Delhi", "North East Delhi",
            "North West Delhi", "Shahdara", "South Delhi", "South East Delhi", "South West Delhi", "West Delhi"
        ),
        "Gujarat" to listOf(
            "Ahmedabad", "Amreli", "Anand", "Aravalli", "Banaskantha", "Bharuch", "Bhavnagar",
            "Botad", "Chhota Udaipur", "Dahod", "Dang", "Devbhoomi Dwarka", "Gandhinagar",
            "Gir Somnath", "Jamnagar", "Junagadh", "Kheda", "Kutch", "Mahisagar", "Mehsana",
            "Morbi", "Narmada", "Navsari", "Panchmahal", "Patan", "Porbandar", "Rajkot",
            "Sabarkantha", "Surat", "Surendranagar", "Tapi", "Vadodara", "Valsad"
        ),
        "Punjab" to listOf(
            "Amritsar", "Barnala", "Bathinda", "Faridkot", "Fatehgarh Sahib", "Fazilka", "Ferozepur",
            "Gurdaspur", "Hoshiarpur", "Jalandhar", "Kapurthala", "Ludhiana", "Malerkotla", "Mansa",
            "Moga", "Muktsar", "Pathankot", "Patiala", "Rupnagar", "Sahibzada Ajit Singh Nagar",
            "Sangrur", "Shahid Bhagat Singh Nagar", "Tarn Taran"
        ),
        "Bihar" to listOf(
            "Araria", "Arwal", "Aurangabad", "Banka", "Begusarai", "Bhagalpur", "Bhojpur", "Buxar",
            "Darbhanga", "East Champaran", "Gaya", "Gopalganj", "Jamui", "Jehanabad", "Kaimur",
            "Katihar", "Khagaria", "Kishanganj", "Lakhisarai", "Madhepura", "Madhubani", "Munger",
            "Muzaffarpur", "Nalanda", "Nawada", "Patna", "Purnia", "Rohtas", "Saharsa", "Samastipur",
            "Saran", "Sheikhpura", "Sheohar", "Sitamarhi", "Siwan", "Supaul", "Vaishali", "West Champaran"
        ),
        "Maharashtra" to listOf(
            "Ahmednagar", "Akola", "Amravati", "Aurangabad", "Beed", "Bhandara", "Buldhana",
            "Chandrapur", "Dhule", "Gadchiroli", "Gondia", "Hingoli", "Jalgaon", "Jalna",
            "Kolhapur", "Latur", "Mumbai City", "Mumbai Suburban", "Nagpur", "Nanded", "Nandurbar",
            "Nashik", "Osmanabad", "Palghar", "Parbhani", "Pune", "Raigad", "Ratnagiri", "Sangli",
            "Satara", "Sindhudurg", "Solapur", "Thane", "Wardha", "Washim", "Yavatmal"
        ),
        "Himachal Pradesh" to listOf(
            "Bilaspur", "Chamba", "Hamirpur", "Kangra", "Kinnaur", "Kullu", "Lahaul and Spiti",
            "Mandi", "Shimla", "Sirmaur", "Solan", "Una"
        ),
        "Uttarakhand" to listOf(
            "Almora", "Bageshwar", "Chamoli", "Champawat", "Dehradun", "Haridwar", "Nainital",
            "Pauri Garhwal", "Pithoragarh", "Rudraprayag", "Tehri Garhwal", "Udham Singh Nagar", "Uttarkashi"
        ),
        "Chhattisgarh" to listOf(
            "Balod", "Baloda Bazar", "Balrampur", "Bastar", "Bemetara", "Bijapur", "Bilaspur",
            "Dantewada", "Dhamtari", "Durg", "Gariaband", "Gaurela-Pendra-Marwahi", "Janjgir-Champa",
            "Jashpur", "Kabirdham", "Kanker", "Kondagaon", "Korba", "Koriya", "Mahasamund",
            "Manendragarh-Chirmiri-Bharatpur", "Mohla-Manpur-Ambagarh Chowki", "Mungeli", "Narayanpur",
            "Raigarh", "Raipur", "Rajnandgaon", "Sarangarh-Bilaigarh", "Sakti", "Sukma", "Surajpur",
            "Surguja"
        ),
        "Jharkhand" to listOf(
            "Bokaro", "Chatra", "Deoghar", "Dhanbad", "Dumka", "East Singhbhum", "Garhwa",
            "Giridih", "Godda", "Gumla", "Hazaribagh", "Jamtara", "Khunti", "Koderma", "Latehar",
            "Lohardaga", "Pakur", "Palamu", "Ramgarh", "Ranchi", "Sahebganj", "Seraikela Kharsawan",
            "Simdega", "West Singhbhum"
        )
    )

    val ALL_STATES: List<String> = STATES_MAP.keys.toList()

    /**
     * Normalizes state input to standard format. Default fallback is "Rajasthan".
     */
    fun normalizeState(rawState: String?): String {
        val trimmed = rawState?.trim() ?: ""
        if (trimmed.isBlank()) return "Rajasthan"

        val matched = ALL_STATES.find { it.equals(trimmed, ignoreCase = true) }
        if (matched != null) return matched

        return toTitleCase(trimmed)
    }

    /**
     * Normalizes district input against official list for given state.
     * Prevents duplicate groups for "jaipur", "JAIPUR", " Jaipur ", etc.
     */
    fun normalizeDistrict(state: String?, rawDistrict: String?): String {
        val cleanDistrict = rawDistrict?.trim() ?: ""
        if (cleanDistrict.isBlank()) return ""

        val validState = normalizeState(state)
        val districtsForState = STATES_MAP[validState] ?: emptyList()

        val matched = districtsForState.find { it.equals(cleanDistrict, ignoreCase = true) }
        if (matched != null) return matched

        // Search across all districts in case state was misclassified
        for ((_, districts) in STATES_MAP) {
            val globalMatch = districts.find { it.equals(cleanDistrict, ignoreCase = true) }
            if (globalMatch != null) return globalMatch
        }

        return toTitleCase(cleanDistrict)
    }

    fun getDistrictsForState(state: String?): List<String> {
        val validState = normalizeState(state)
        return STATES_MAP[validState] ?: STATES_MAP["Rajasthan"] ?: emptyList()
    }

    fun toTitleCase(text: String): String {
        if (text.isBlank()) return ""
        return text.split(Regex("\\s+")).joinToString(" ") { word ->
            if (word.isBlank()) ""
            else word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}

/**
 * Reusable Compose State & District Picker with Built-in Search Dialog.
 */
@Composable
fun StateDistrictSelector(
    selectedState: String,
    onStateSelected: (String) -> Unit,
    selectedDistrict: String,
    onDistrictSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var showStateDialog by remember { mutableStateOf(false) }
    var showDistrictDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // State Selector Field
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showStateDialog = true },
            shape = RoundedCornerShape(8.dp),
            color = Slate100
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("State (राज्य)", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Medium)
                        Text(
                            text = selectedState.ifBlank { "Select State (Rajasthan)" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    }
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate500)
            }
        }

        // District Selector Field
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDistrictDialog = true },
            shape = RoundedCornerShape(8.dp),
            color = if (isError && selectedDistrict.isBlank()) Color(0xFFFEE2E2) else Slate100
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationCity, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("District (ज़िला) *", fontSize = 10.sp, color = if (isError && selectedDistrict.isBlank()) Color(0xFFDC2626) else Slate500, fontWeight = FontWeight.Medium)
                        Text(
                            text = selectedDistrict.ifBlank { "Choose District (ज़िला चुनें)" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedDistrict.isBlank()) Slate500 else Navy900
                        )
                    }
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate500)
            }
        }
    }

    // State Search & Select Dialog
    if (showStateDialog) {
        var stateSearch by remember { mutableStateOf("") }
        val filteredStates = remember(stateSearch) {
            if (stateSearch.isBlank()) IndiaLocationData.ALL_STATES
            else IndiaLocationData.ALL_STATES.filter { it.contains(stateSearch, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = { showStateDialog = false },
            title = {
                Text("Select State (राज्य चुनें)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = stateSearch,
                        onValueChange = { stateSearch = it },
                        placeholder = { Text("Search state...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (stateSearch.isNotBlank()) {
                                IconButton(onClick = { stateSearch = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        items(filteredStates) { st ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onStateSelected(st)
                                        // Reset district if it doesn't belong to new state
                                        val newDistricts = IndiaLocationData.getDistrictsForState(st)
                                        if (!newDistricts.contains(selectedDistrict)) {
                                            onDistrictSelected("")
                                        }
                                        showStateDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = st,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedState == st) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedState == st) Indigo600 else Slate700
                                )
                                if (selectedState == st) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                                }
                            }
                            Divider(color = Slate100)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStateDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // District Search & Select Dialog
    if (showDistrictDialog) {
        var districtSearch by remember { mutableStateOf("") }
        val allDistricts = remember(selectedState) {
            IndiaLocationData.getDistrictsForState(selectedState)
        }
        val filteredDistricts = remember(districtSearch, allDistricts) {
            if (districtSearch.isBlank()) allDistricts
            else allDistricts.filter { it.contains(districtSearch, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = { showDistrictDialog = false },
            title = {
                Column {
                    Text("Select District (ज़िला चुनें)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
                    Text("State: ${selectedState.ifBlank { "Rajasthan" }}", fontSize = 12.sp, color = Indigo600)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = districtSearch,
                        onValueChange = { districtSearch = it },
                        placeholder = { Text("Search district (e.g. Jaipur, Alwar)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (districtSearch.isNotBlank()) {
                                IconButton(onClick = { districtSearch = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                        items(filteredDistricts) { dist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDistrictSelected(dist)
                                        showDistrictDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dist,
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedDistrict.equals(dist, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedDistrict.equals(dist, ignoreCase = true)) Indigo600 else Slate700
                                )
                                if (selectedDistrict.equals(dist, ignoreCase = true)) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Indigo600, modifier = Modifier.size(18.dp))
                                }
                            }
                            Divider(color = Slate100)
                        }

                        // Custom District Option if user searched something not in list
                        if (districtSearch.isNotBlank() && !allDistricts.any { it.equals(districtSearch.trim(), ignoreCase = true) }) {
                            item {
                                val customTitle = IndiaLocationData.toTitleCase(districtSearch.trim())
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDistrictSelected(customTitle)
                                            showDistrictDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+ Use \"$customTitle\"",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Indigo600
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDistrictDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
