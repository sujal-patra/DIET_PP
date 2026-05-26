package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(viewModel: DietPantryViewModel) {
    val context = LocalContext.current
    val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()
    var showProfileDialog by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(0) }

    val pantryList by viewModel.pantryItems.collectAsStateWithLifecycle()
    val dietLogs by viewModel.currentDietLogs.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val goal by viewModel.userGoal.collectAsStateWithLifecycle()

    val recipeState by viewModel.recipeUiState.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisUiState.collectAsStateWithLifecycle()

    if (loggedInUser == null) {
        AuthScreen(viewModel = viewModel)
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("main_navigation_bar"),
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Kitchen, contentDescription = "Pantry Inventory") },
                        label = { Text("Pantry") },
                        modifier = Modifier.testTag("tab_pantry")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Restaurant, contentDescription = "Diet Logs") },
                        label = { Text("Diet Log") },
                        modifier = Modifier.testTag("tab_diet")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Magic Chef") },
                        label = { Text("AI Chef") },
                        modifier = Modifier.testTag("tab_ai_chef")
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Personalized Coaching") },
                        label = { Text("Coach & Goals") },
                        modifier = Modifier.testTag("tab_coach")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentTab) {
                    0 -> PantryTab(
                        pantryList = pantryList,
                        loggedInUser = loggedInUser,
                        onAvatarClick = { showProfileDialog = true },
                        onAddItem = { name, quantity, unit, category, expiryDays ->
                            val expiryTimestamp = if (expiryDays != null) {
                                System.currentTimeMillis() + (expiryDays * 24L * 60L * 60L * 1000L)
                            } else null
                            viewModel.addPantryItem(name, quantity, unit, category, expiryTimestamp)
                        },
                        onDeleteItem = { item ->
                            viewModel.deletePantryItem(item)
                            Toast.makeText(context, "${item.name} removed from pantry", Toast.LENGTH_SHORT).show()
                        }
                    )
                    1 -> DietTab(
                        selectedDate = selectedDate,
                        mealLogs = dietLogs,
                        goal = goal,
                        loggedInUser = loggedInUser,
                        onAvatarClick = { showProfileDialog = true },
                        onAddLog = { type, name, cal, p, c, f ->
                            viewModel.addDietLog(type, name, cal, p, c, f)
                        },
                        onDeleteLog = { log ->
                            viewModel.deleteDietLog(log)
                        },
                        onPrevDay = {
                            changeSelectedDateByDays(viewModel, -1)
                        },
                        onNextDay = {
                            changeSelectedDateByDays(viewModel, 1)
                        },
                        onSelectToday = {
                            viewModel.setSelectedDateString(viewModel.todayDateString)
                        }
                    )
                    2 -> ChefTab(
                        pantryList = pantryList,
                        recipeState = recipeState,
                        loggedInUser = loggedInUser,
                        onAvatarClick = { showProfileDialog = true },
                        onGenerate = {
                            viewModel.generateRecipeFromPantry()
                        },
                        onCookRecipe = { recipe ->
                            viewModel.addDietLog("Dinner", recipe.recipeName, recipe.estimatedCalories, recipe.protein, recipe.carbs, recipe.fat)
                            viewModel.clearRecipeState()
                            Toast.makeText(context, "Logged ${recipe.recipeName} to dinner list!", Toast.LENGTH_LONG).show()
                        },
                        onCancelRecipe = {
                            viewModel.clearRecipeState()
                        }
                    )
                    3 -> CoachTab(
                        goal = goal,
                        mealLogs = dietLogs,
                        analysisState = analysisState,
                        loggedInUser = loggedInUser,
                        onAvatarClick = { showProfileDialog = true },
                        onSaveGoals = { cal, p, c, f, pref ->
                            viewModel.updateUserGoals(cal, p, c, f, pref)
                            Toast.makeText(context, "Goals saved securely!", Toast.LENGTH_SHORT).show()
                        },
                        onTriggerAnalysis = {
                            viewModel.analyzeTodayDiet()
                        },
                        onClearAnalysis = {
                            viewModel.clearAnalysisState()
                        }
                    )
                }
            }
        }
    }

    if (showProfileDialog && loggedInUser != null) {
        EditProfileDialog(
            user = loggedInUser!!,
            onDismiss = { showProfileDialog = false },
            onSave = { name, avatar, about, info ->
                viewModel.updateProfile(name, avatar, about, info)
                Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
            },
            onLogout = {
                viewModel.logout()
                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// Helper function to shift dates smoothly in calendar logs
private fun changeSelectedDateByDays(viewModel: DietPantryViewModel, days: Int) {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    try {
        val currentDate = formatter.parse(viewModel.selectedDate.value) ?: Date()
        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        calendar.add(Calendar.DAY_OF_YEAR, days)
        viewModel.setSelectedDateString(formatter.format(calendar.time))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


// ==========================================
// PANTRY TAB VIEW
// ==========================================

@Composable
fun PantryTab(
    pantryList: List<PantryItem>,
    loggedInUser: UserAccount?,
    onAvatarClick: () -> Unit,
    onAddItem: (String, Double, String, String, Int?) -> Unit,
    onDeleteItem: (PantryItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "Produce", "Dairy", "Grains", "Bakery", "Meat & Fish", "Cans & Jars", "Spices", "Other")

    val filteredList = pantryList.filter { item ->
        val matchesSearch = item.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryFilter == "All" || item.category == selectedCategoryFilter
        matchesSearch && matchesCategory
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header Title
            MinimalGreetingHeader(
                title = "Pantry Inventory",
                subtitle = "Welcome, ${loggedInUser?.fullName ?: ""}",
                loggedInUser = loggedInUser,
                onAvatarClick = onAvatarClick
            )
            Text(
                text = "Keep track of items you have in stock to formulate custom recipes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search pantry items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("pantry_search"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Categories Filter Scroll Bar
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategoryFilter).coerceAtLeast(0),
                edgePadding = 0.dp,
                modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth().testTag("category_filter_row"),
                indicator = {}
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategoryFilter == category
                    Tab(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = category },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).testTag("filter_chip_$category"),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Pantry Items List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AllInbox,
                            contentDescription = "Pantry Empty",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedCategoryFilter != "All") "No matches found" else "Your pantry is empty!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap the + button to build up your ingredients list.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("pantry_items_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        PantryItemCard(item = item, onDelete = { onDeleteItem(item) })
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_pantry_item_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Pantry Item")
        }
    }

    if (showAddDialog) {
        AddPantryItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, qty, unit, cat, expDays ->
                onAddItem(name, qty, unit, cat, expDays)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CategoryItemIcon(category: String) {
    val (icon, bgColor, labelColor) = when (category) {
        "Produce" -> Triple(Icons.Default.Eco, Color(0xFFE9EFD1), Color(0xFF5C940D))
        "Dairy" -> Triple(Icons.Default.Opacity, Color(0xFFEFF6FF), Color(0xFF2563EB))
        "Grains", "Bakery" -> Triple(Icons.Default.BakeryDining, Color(0xFFFFF7ED), Color(0xFFEA580C))
        "Meat & Fish" -> Triple(Icons.Default.SetMeal, Color(0xFFFEF2F2), Color(0xFFEF4444))
        "Cans & Jars" -> Triple(Icons.Default.Widgets, Color(0xFFF3E8FF), Color(0xFF9333EA))
        "Spices" -> Triple(Icons.Default.Grain, Color(0xFFFEF3C7), Color(0xFFD97706))
        else -> Triple(Icons.Default.List, Color(0xFFF1F5F9), Color(0xFF475569))
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = labelColor
        )
    }
}

@Composable
fun PantryItemCard(item: PantryItem, onDelete: () -> Unit) {
    val expiryDaysRemaining = remember(item.expiryDate) {
        item.expiryDate?.let { date ->
            val diff = date - System.currentTimeMillis()
            (diff / (24L * 60L * 60L * 1000L)).toInt()
        }
    }

    val warningColor = when {
        expiryDaysRemaining == null -> Color.Gray
        expiryDaysRemaining < 0 -> Color(0xFFD32F2F) // Red - expired
        expiryDaysRemaining <= 3 -> Color(0xFFED6C02) // Orange - critical
        else -> Color(0xFF2E7D32) // Green - fresh
    }

    val warningText = when {
        expiryDaysRemaining == null -> "No Expiry Tracked"
        expiryDaysRemaining < 0 -> "Expired"
        expiryDaysRemaining == 0 -> "Expires Today!"
        expiryDaysRemaining == 1 -> "Expires Tomorrow!"
        else -> "Expires in $expiryDaysRemaining days"
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pantry_item_card_${item.name.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CategoryItemIcon(category = item.category)

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text(item.category, fontSize = 10.sp) },
                        modifier = Modifier.height(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.quantity} ${item.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(warningColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = warningText,
                        style = MaterialTheme.typography.bodySmall,
                        color = warningColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_item_button_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete pantry item",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPantryItemDialog(onDismiss: () -> Unit, onConfirm: (String, Double, String, String, Int?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var selectedUnit by remember { mutableStateOf("pcs") }
    var selectedCategory by remember { mutableStateOf("Produce") }
    var expiryDaysText by remember { mutableStateOf("") }

    val units = listOf("pcs", "g", "kg", "ml", "liters", "cans", "boxes", "cups", "tablespoons")
    val categories = listOf("Produce", "Dairy", "Grains", "Bakery", "Meat & Fish", "Cans & Jars", "Spices", "Other")

    var unitExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var formError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Pantry Item", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toDoubleOrNull()
                    if (name.trim().isEmpty() || qty == null || qty <= 0.0) {
                        formError = true
                    } else {
                        val expiryDays = expiryDaysText.toIntOrNull()
                        onConfirm(name.trim(), qty, selectedUnit, selectedCategory, expiryDays)
                    }
                },
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (formError) {
                    Text(
                        text = "Please enter a valid item name and numeric quantity.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        formError = false
                    },
                    label = { Text("Ingredient name (e.g. Tomato)") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_input_name"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            quantityText = it
                            formError = false
                        },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("dialog_input_quantity"),
                        singleLine = true
                    )

                    // Unit dropdown
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            units.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        selectedUnit = unit
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = expiryDaysText,
                    onValueChange = { expiryDaysText = it },
                    label = { Text("Estimated shelf life (in days, optional)") },
                    placeholder = { Text("e.g. 5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_input_expiry"),
                    singleLine = true
                )

                // Quick add helpers for expiry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(3, 7, 14, 30).forEach { days ->
                        InputChip(
                            selected = expiryDaysText == days.toString(),
                            onClick = { expiryDaysText = days.toString() },
                            label = { Text("+$days d") }
                        )
                    }
                }
            }
        }
    )
}


// ==========================================
// DIET TAB & CALENDAR VIEW
// ==========================================

@Composable
fun DietTab(
    selectedDate: String,
    mealLogs: List<DietLog>,
    goal: UserGoal,
    loggedInUser: UserAccount?,
    onAvatarClick: () -> Unit,
    onAddLog: (String, String, Int, Double, Double, Double) -> Unit,
    onDeleteLog: (DietLog) -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onSelectToday: () -> Unit
) {
    var showAddMealDialog by remember { mutableStateOf(false) }

    // Aggregate values
    val totalCalories = mealLogs.sumOf { it.calories }
    val totalProtein = mealLogs.sumOf { it.protein }
    val totalCarbs = mealLogs.sumOf { it.carbs }
    val totalFat = mealLogs.sumOf { it.fat }

    val caloriePercentage = if (goal.dailyCalorieGoal > 0) (totalCalories.toFloat() / goal.dailyCalorieGoal.toFloat()).coerceAtMost(1f) else 0f
    val proteinPercentage = if (goal.dailyProteinGoal > 0.0) (totalProtein / goal.dailyProteinGoal).coerceAtMost(1.0).toFloat() else 0f
    val carbsPercentage = if (goal.dailyCarbsGoal > 0.0) (totalCarbs / goal.dailyCarbsGoal).coerceAtMost(1.0).toFloat() else 0f
    val fatPercentage = if (goal.dailyFatGoal > 0.0) (totalFat / goal.dailyFatGoal).coerceAtMost(1.0).toFloat() else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp).testTag("diet_logs_column"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MinimalGreetingHeader(
                title = "Nutritional Analysis",
                subtitle = "Welcome, ${loggedInUser?.fullName ?: ""}",
                loggedInUser = loggedInUser,
                onAvatarClick = onAvatarClick
            )
        }
        // Date Selector Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onPrevDay, modifier = Modifier.testTag("date_prev_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Day")
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSelectToday() }
                    ) {
                        Text(
                            text = getFancyDateFormatted(selectedDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tap to jump back to today",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(onClick = onNextDay, modifier = Modifier.testTag("date_next_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                    }
                }
            }
        }

        // Nutrient Dashboard Panel
        item {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE9EFD1)
                ),
                modifier = Modifier.fillMaxWidth().testTag("diet_goals_card")
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Daily Goal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF2D331C)
                            )
                            Text(
                                text = "$totalCalories / ${goal.dailyCalorieGoal} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF5C624D),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = Color(0xFF2D331C)
                            )
                        }
                    }

                    // Progress bar track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = caloriePercentage)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF5C940D))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Column / Row distribution of nutrients
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "PROTEIN",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5C624D)
                            )
                            Text(
                                text = "${totalProtein.toInt()}g / ${goal.dailyProteinGoal.toInt()}g",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D331C)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CARBS",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5C624D)
                            )
                            Text(
                                text = "${totalCarbs.toInt()}g / ${goal.dailyCarbsGoal.toInt()}g",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D331C)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "FAT",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5C624D)
                            )
                            Text(
                                text = "${totalFat.toInt()}g / ${goal.dailyFatGoal.toInt()}g",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D331C)
                            )
                        }
                    }
                }
            }
        }

        // Add Meal Button CTA
        item {
            Button(
                onClick = { showAddMealDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_meal_log_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.RestaurantMenu, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Consumed Meal / Food", fontWeight = FontWeight.Bold)
            }
        }

        // Meals List Section
        val mealGroups = listOf("Breakfast", "Lunch", "Dinner", "Snack")
        mealGroups.forEach { mealType ->
            val logsInGroup = mealLogs.filter { it.mealType.lowercase() == mealType.lowercase() }
            if (logsInGroup.isNotEmpty()) {
                item {
                    Text(
                        text = mealType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(logsInGroup, key = { it.id }) { log ->
                    MealLogItemCard(log = log, onDelete = { onDeleteLog(log) })
                }
            }
        }

        if (mealLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NoFood,
                            contentDescription = "No Food Logs",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No meals logged yet for this date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
    }

    if (showAddMealDialog) {
        AddMealLogDialog(
            onDismiss = { showAddMealDialog = false },
            onConfirm = { type, name, cal, p, c, f ->
                onAddLog(type, name, cal, p, c, f)
                showAddMealDialog = false
            }
        )
    }
}

@Composable
fun MealLogItemCard(log: DietLog, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("meal_log_card_${log.foodName.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.foodName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${log.calories} kcal  |  P: ${log.protein}g, C: ${log.carbs}g, F: ${log.fat}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_meal_log_${log.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete meal record",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun MacroLinearUnit(title: String, valueDouble: Double, goalDouble: Double, percentage: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            Text(
                "${valueDouble.toInt()}g / ${goalDouble.toInt()}g",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = percentage,
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// Convert "YYYY-MM-DD" to human readable like "Tuesday, May 26"
fun getFancyDateFormatted(dateString: String): String {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outputFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
    return try {
        val parsed = inputFormat.parse(dateString) ?: Date()
        val today = inputFormat.format(Date())
        if (dateString == today) {
            "Today, " + SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(parsed)
        } else {
            outputFormat.format(parsed)
        }
    } catch (e: Exception) {
        dateString
    }
}


@Composable
fun AddMealLogDialog(onDismiss: () -> Unit, onConfirm: (String, String, Int, Double, Double, Double) -> Unit) {
    var foodName by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf("Breakfast") }
    var caloriesText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var isEstimating by remember { mutableStateOf(false) }

    var formError by remember { mutableStateOf(false) }

    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Meal / Diet Entry", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    val cal = caloriesText.toIntOrNull()
                    val p = proteinText.toDoubleOrNull() ?: 0.0
                    val c = carbsText.toDoubleOrNull() ?: 0.0
                    val f = fatText.toDoubleOrNull() ?: 0.0

                    if (foodName.trim().isEmpty() || cal == null || cal < 0) {
                        formError = true
                    } else {
                        onConfirm(selectedMealType, foodName.trim(), cal, p, c, f)
                    }
                },
                modifier = Modifier.testTag("meal_dialog_confirm_button")
            ) {
                Text("Add Meal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (formError) {
                    Text(
                        text = "Food Name and Calories (valid positive integer) are required fields.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = foodName,
                    onValueChange = {
                        foodName = it
                        formError = false
                    },
                    label = { Text("Food Name (e.g. Avocado Toast)") },
                    modifier = Modifier.fillMaxWidth().testTag("meal_dialog_food_name"),
                    singleLine = true
                )

                // Meal Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    mealTypes.forEach { type ->
                        val isSel = selectedMealType == type
                        ElevatedFilterChip(
                            selected = isSel,
                            onClick = { selectedMealType = type },
                            label = { Text(type, fontSize = 11.sp) },
                            modifier = Modifier.testTag("chip_meal_$type")
                        )
                    }
                }

                // AI Estimator Button
                val canTriggerAi = foodName.trim().isNotEmpty()
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isEstimating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Consulting Gemini AI Chef...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                if (canTriggerAi) {
                                    isEstimating = true
                                    coroutineScope.launch {
                                        val estimate = autoEstimateNutrition(foodName.trim())
                                        if (estimate != null) {
                                            caloriesText = estimate.calories.toString()
                                            proteinText = estimate.protein.toString()
                                            carbsText = estimate.carbs.toString()
                                            fatText = estimate.fat.toString()
                                        }
                                        isEstimating = false
                                    }
                                }
                            },
                            enabled = canTriggerAi,
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto-Estimate Nutrition with AI", fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = {
                        caloriesText = it
                        formError = false
                    },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("meal_dialog_calories"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("meal_dialog_protein"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("meal_dialog_carbs"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("meal_dialog_fat"),
                        singleLine = true
                    )
                }
            }
        }
    )
}

// Model representing AI estimates temporarily
private data class NutritionEstimate(val calories: Int, val protein: Double, val carbs: Double, val fat: Double)

// Direct suspend parsing of Gemini model for automatic dialog prefilling
private suspend fun autoEstimateNutrition(promptFood: String): NutritionEstimate? = withContext(Dispatchers.IO) {
    val apiKey = com.example.BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@withContext null

    val promptText = "Estimate the calorie count, protein, carbs, and fat in typical portion sizing of '$promptFood'. Give output strictly formatted as single JSON containing keys: 'calories' (integer limit), 'protein' (number), 'carbs' (number), 'fat' (number)."

    val schema = ResponseSchema(
        type = "OBJECT",
        properties = mapOf(
            "calories" to ResponseProperty("INTEGER"),
            "protein" to ResponseProperty("NUMBER"),
            "carbs" to ResponseProperty("NUMBER"),
            "fat" to ResponseProperty("NUMBER")
        ),
        required = listOf("calories", "protein", "carbs", "fat")
    )

    val request = GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(promptText)))),
        generationConfig = GenerationConfig(
            responseMimeType = "application/json",
            responseSchema = schema,
            temperature = 0.1f
        )
    )

    try {
        val response = GeminiClient.service.generateContent(apiKey, request)
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        if (text != null) {
            val json = JSONObject(text)
            NutritionEstimate(
                calories = json.optInt("calories", 100),
                protein = json.optDouble("protein", 0.0),
                carbs = json.optDouble("carbs", 0.0),
                fat = json.optDouble("fat", 0.0)
            )
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


// ==========================================
// AI MAGIC CHEF VIEW
// ==========================================

@Composable
fun ChefTab(
    pantryList: List<PantryItem>,
    recipeState: RecipeUiState,
    loggedInUser: UserAccount?,
    onAvatarClick: () -> Unit,
    onGenerate: () -> Unit,
    onCookRecipe: (GeminiRecipe) -> Unit,
    onCancelRecipe: () -> Unit
) {
    val animatedProgressFraction by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Chef Header Cards
        item {
            Column {
                MinimalGreetingHeader(
                    title = "AI Magic Chef",
                    subtitle = "Welcome, ${loggedInUser?.fullName ?: ""}",
                    loggedInUser = loggedInUser,
                    onAvatarClick = onAvatarClick
                )
                Text(
                    text = "Formulate high-cuisine meals instantly based on elements currently present in your Pantry list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        when (recipeState) {
            is RecipeUiState.Idle -> {
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("chef_generate_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Have ${pantryList.size} ingredients ready",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "The AI Chef will scan items in your pantry list like Vegetables, Dairy, and Grains to formulate a Michelin-star style custom recipe.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onGenerate,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("chef_submit_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Formulate Culinary Recipe ✨", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                // Show basic preview of items being utilized
                item {
                    Text(
                        text = "Ingredients scanning catalog:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (pantryList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "Your pantry is empty. Please add items in the Pantry tab first, so the AI has resources to combine!",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(pantryList) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text("${item.quantity} ${item.unit}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            is RecipeUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(54.dp),
                                strokeWidth = 5.dp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "AI Chef is Mixing & Simmering...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Constructing a balanced custom recipe matching your dietary configurations...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            is RecipeUiState.Success -> {
                val recipe = recipeState.recipe
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onCancelRecipe) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Formulate different recipe")
                        }
                    }
                }

                item {
                    RecipeResultDetailView(recipe = recipe, onCook = { onCookRecipe(recipe) })
                }
            }

            is RecipeUiState.Error -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Recipe Failure", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(recipeState.message, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onCancelRecipe) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeResultDetailView(recipe: GeminiRecipe, onCook: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("generated_recipe_container")
    ) {
        Column {
            // Header Image Box with custom styled Sage Brush gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = recipe.recipeName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Custom generated menu recommendation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Description
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Stats Dashboard Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    FlowRow(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NutrientStatBadge(icon = Icons.Default.Timer, label = recipe.prepTime)
                        NutrientStatBadge(icon = Icons.Default.LocalFireDepartment, label = "${recipe.estimatedCalories} kcal")
                        NutrientStatBadge(icon = null, label = "P: ${recipe.protein.toInt()}g")
                        NutrientStatBadge(icon = null, label = "C: ${recipe.carbs.toInt()}g")
                        NutrientStatBadge(icon = null, label = "F: ${recipe.fat.toInt()}g")
                    }
                }

                // Checklist of Ingredients
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Ingredients checklist:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    recipe.ingredients.forEach { ing ->
                        var isChecked by remember { mutableStateOf(false) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isChecked = !isChecked }.fillMaxWidth()
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = { isChecked = it })
                            Text(
                                text = ing,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.fillMaxWidth())

                // Instructions Timeline
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cooking directions:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    recipe.instructions.forEachIndexed { index, step ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onCook,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("cook_done_button")
                ) {
                    Icon(Icons.Default.Celebration, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I Cooked This! (Instant log meal)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NutrientStatBadge(icon: ImageVector?, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, size = 12.dp, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

// Inline size replacement for Icon
@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(size),
        tint = tint
    )
}

@Composable
fun AvatarDisplay(avatarId: String, size: androidx.compose.ui.unit.Dp = 44.dp) {
    val (emoji, bg) = when (avatarId) {
        "avatar_avocado" -> "🥑" to Color(0xFFE9EFD1)
        "avatar_apple" -> "🍎" to Color(0xFFFEF2F2)
        "avatar_water" -> "💧" to Color(0xFFEFF6FF)
        "avatar_chef" -> "🧑‍🍳" to Color(0xFFF3E8FF)
        "avatar_sun" -> "☀️" to Color(0xFFFEF3C7)
        "avatar_gym" -> "🏋️" to Color(0xFFF1F5F9)
        else -> "👤" to MaterialTheme.colorScheme.primaryContainer
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = (size.value * 0.5f).sp)
    }
}

@Composable
fun MinimalGreetingHeader(
    title: String,
    subtitle: String,
    loggedInUser: UserAccount?,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "GOOD MORNING",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Box(
            modifier = Modifier
                .clickable { onAvatarClick() }
                .clip(CircleShape)
        ) {
            if (loggedInUser != null) {
                AvatarDisplay(avatarId = loggedInUser.avatarUrl)
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: DietPantryViewModel,
    modifier: Modifier = Modifier
) {
    var isLogin by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFDFCF9))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE9EFD1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Kitchen,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF5C940D)
                )
            }

            Text(
                text = if (isLogin) "Welcome Back" else "Create Account",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Text(
                text = if (isLogin) "Log in to track your pantry, plan meals, and receive elite AI coaching." 
                       else "Join our minimalist wellness companion today.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!isLogin) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    placeholder = { Text("e.g. Alex Rivera") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("signup_name_input")
                )
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username or Email") },
                placeholder = { Text("e.g. alex_rivera") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("login_username_input")
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("login_password_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (isLogin) {
                        viewModel.login(username, password) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        viewModel.signUp(username, password, fullName) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) {
                                isLogin = true
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5C940D),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag(if (isLogin) "login_submit_button" else "signup_submit_button")
            ) {
                Text(
                    text = if (isLogin) "Log In" else "Sign Up",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = { isLogin = !isLogin },
                modifier = Modifier.testTag("toggle_auth_button")
            ) {
                Text(
                    text = if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Log In",
                    color = Color(0xFF5C940D),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    user: UserAccount,
    onDismiss: () -> Unit,
    onSave: (fullName: String, avatarUrl: String, aboutMe: String, information: String) -> Unit,
    onLogout: () -> Unit
) {
    var name by remember { mutableStateOf(user.fullName) }
    var avatar by remember { mutableStateOf(user.avatarUrl) }
    var about by remember { mutableStateOf(user.aboutMe) }
    var info by remember { mutableStateOf(user.information) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFCF9)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("edit_profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                AvatarDisplay(avatarId = avatar, size = 64.dp)

                Text(
                    text = "Choose Profile Picture Preset",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )

                val presets = listOf(
                    "avatar_avocado" to "🥑",
                    "avatar_apple" to "🍎",
                    "avatar_water" to "💧",
                    "avatar_chef" to "🧑‍🍳",
                    "avatar_sun" to "☀️",
                    "avatar_gym" to "🏋️"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    presets.forEach { (id, emoji) ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (avatar == id) Color(0xFFE9EFD1) else Color.Transparent)
                                .clickable { avatar = id }
                                .border(
                                    width = if (avatar == id) 2.dp else 0.dp,
                                    color = if (avatar == id) Color(0xFF5C940D) else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )

                OutlinedTextField(
                    value = about,
                    onValueChange = { about = it },
                    label = { Text("About Me") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_about_input")
                )

                OutlinedTextField(
                    value = info,
                    onValueChange = { info = it },
                    label = { Text("Information") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_info_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onSave(name, avatar, about, info)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C940D)),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button")
                ) {
                    Text("Save Customizations", fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = {
                        onLogout()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("profile_logout_button")
                ) {
                    Text("Log Out Account", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


// ==========================================
// GOALS & PROFILE & COACH TAB VIEW
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachTab(
    goal: UserGoal,
    mealLogs: List<DietLog>,
    analysisState: AnalysisUiState,
    loggedInUser: UserAccount?,
    onAvatarClick: () -> Unit,
    onSaveGoals: (Int, Double, Double, Double, String) -> Unit,
    onTriggerAnalysis: () -> Unit,
    onClearAnalysis: () -> Unit
) {
    var calText by remember(goal) { mutableStateOf(goal.dailyCalorieGoal.toString()) }
    var proteinText by remember(goal) { mutableStateOf(goal.dailyProteinGoal.toString()) }
    var carbsText by remember(goal) { mutableStateOf(goal.dailyCarbsGoal.toString()) }
    var fatText by remember(goal) { mutableStateOf(goal.dailyFatGoal.toString()) }
    var selectedPreference by remember(goal) { mutableStateOf(goal.dietPreference) }

    val options = listOf("None", "Keto", "Vegan", "Vegetarian", "Gluten Free", "Low Carb")

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp).testTag("coach_tab_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                MinimalGreetingHeader(
                    title = "Coach & Goals",
                    subtitle = "Welcome, ${loggedInUser?.fullName ?: ""}",
                    loggedInUser = loggedInUser,
                    onAvatarClick = onAvatarClick
                )
                Text(
                    text = "Track your daily nutrient parameters and invoke elite nutrition feedback with AI.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        // COACHING ADVICE PORTION
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "AI Health & Diet Coach 🩺",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    when (analysisState) {
                        is AnalysisUiState.Idle -> {
                            Text(
                                text = "Ask the AI Coach to analyze your current calorie and macro consumption on this date for recommendations.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onTriggerAnalysis,
                                modifier = Modifier.fillMaxWidth().testTag("coach_review_button")
                            ) {
                                Text("Trigger Diet Analysis 🪄", fontWeight = FontWeight.Bold)
                            }
                        }

                        is AnalysisUiState.Loading -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Coach is checking macros...", fontWeight = FontWeight.Bold)
                            }
                        }

                        is AnalysisUiState.Success -> {
                            val data = analysisState.analysis
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val badgeColor = if (data.isGoalAchieved) Color(0xFF2E7D32) else Color(0xFFED6C02)
                                    val badgeLabel = if (data.isGoalAchieved) "Target Maintained!" else "Corrections Advised"

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(badgeColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(badgeLabel, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    TextButton(onClick = onClearAnalysis) {
                                        Text("Reset Coach")
                                    }
                                }

                                Text(text = data.analysisText, style = MaterialTheme.typography.bodyMedium)

                                if (data.suggestions.isNotEmpty()) {
                                    Divider()
                                    Text("Actionable Tips:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    data.suggestions.forEach { tip ->
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                            Text("• ", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                            Text(tip, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        is AnalysisUiState.Error -> {
                            Text(analysisState.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = onClearAnalysis) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }
        }

        // GOAL CONFIGURATION PORTION
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Customize Goals & Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = calText,
                        onValueChange = { calText = it },
                        label = { Text("Daily Calories Goal (kcal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("profile_calories"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = proteinText,
                            onValueChange = { proteinText = it },
                            label = { Text("Protein (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("profile_protein"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = carbsText,
                            onValueChange = { carbsText = it },
                            label = { Text("Carbohydrates (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("profile_carbs"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = fatText,
                            onValueChange = { fatText = it },
                            label = { Text("Fat (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("profile_fat"),
                            singleLine = true
                        )
                    }

                    // Diet Preference Selector row
                    Text("Dietary Preference Style:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        options.forEach { opt ->
                            val isSel = selectedPreference == opt
                            InputChip(
                                selected = isSel,
                                onClick = { selectedPreference = opt },
                                label = { Text(opt, fontSize = 11.sp) },
                                modifier = Modifier.testTag("preference_chip_$opt")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val cal = calText.toIntOrNull() ?: 2000
                            val p = proteinText.toDoubleOrNull() ?: 120.0
                            val c = carbsText.toDoubleOrNull() ?: 220.0
                            val f = fatText.toDoubleOrNull() ?: 70.0
                            onSaveGoals(cal, p, c, f, selectedPreference)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_save_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Goal Metrics Parameters", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Custom flow inline row styling helper
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
fun rememberScrollState(): androidx.compose.foundation.ScrollState {
    return androidx.compose.foundation.rememberScrollState()
}
