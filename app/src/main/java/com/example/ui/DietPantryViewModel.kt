package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed interface RecipeUiState {
    object Idle : RecipeUiState
    object Loading : RecipeUiState
    data class Success(val recipe: GeminiRecipe) : RecipeUiState
    data class Error(val message: String) : RecipeUiState
}

sealed interface AnalysisUiState {
    object Idle : AnalysisUiState
    object Loading : AnalysisUiState
    data class Success(val analysis: GeminiAnalysis) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

class DietPantryViewModel(private val repository: AppRepository) : ViewModel() {

    // Helper to format date
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val todayDateString: String = dateFormatter.format(Date())

    // Selected Date for meal tracking
    private val _selectedDate = MutableStateFlow(todayDateString)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Pantry Inventory
    val pantryItems: StateFlow<List<PantryItem>> = repository.allPantryItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive Diet Logs matching selected date
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDietLogs: StateFlow<List<DietLog>> = _selectedDate
        .flatMapLatest { date ->
            repository.getDietLogsByDate(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User Goals & Profile configuration
    val userGoal: StateFlow<UserGoal> = repository.userGoalFlow
        .map { it ?: UserGoal() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserGoal()
        )

    val loggedInUser: StateFlow<UserAccount?> = repository.loggedInUserFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // AI States
    private val _recipeUiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Idle)
    val recipeUiState: StateFlow<RecipeUiState> = _recipeUiState.asStateFlow()

    private val _analysisUiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisUiState: StateFlow<AnalysisUiState> = _analysisUiState.asStateFlow()

    init {
        // Initialize user goal record in database so there's always one ready
        viewModelScope.launch {
            val existing = repository.getUserGoal()
            if (existing == null) {
                repository.insertUserGoal(UserGoal())
            }
        }
    }

    fun setSelectedDate(date: Date) {
        _selectedDate.value = dateFormatter.format(date)
        // Reset analysis when date shifts
        _analysisUiState.value = AnalysisUiState.Idle
    }

    fun setSelectedDateString(dateString: String) {
        _selectedDate.value = dateString
        // Reset analysis when date shifts
        _analysisUiState.value = AnalysisUiState.Idle
    }

    // Pantry Actions
    fun addPantryItem(name: String, quantity: Double, unit: String, category: String, expiryDate: Long?) {
        viewModelScope.launch {
            repository.insertPantryItem(
                PantryItem(
                    name = name.trim(),
                    quantity = quantity,
                    unit = unit.trim(),
                    category = category.trim(),
                    expiryDate = expiryDate
                )
            )
        }
    }

    fun deletePantryItem(item: PantryItem) {
        viewModelScope.launch {
            repository.deletePantryItem(item)
        }
    }

    // Diet Log Actions
    fun addDietLog(mealType: String, foodName: String, calories: Int, protein: Double, carbs: Double, fat: Double) {
        viewModelScope.launch {
            repository.insertDietLog(
                DietLog(
                    mealType = mealType,
                    foodName = foodName.trim(),
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    dateString = _selectedDate.value
                )
            )
        }
    }

    fun deleteDietLog(log: DietLog) {
        viewModelScope.launch {
            repository.deleteDietLog(log)
        }
    }

    // Goal & Profile Actions
    fun updateUserGoals(dailyCalorieGoal: Int, dailyProteinGoal: Double, dailyCarbsGoal: Double, dailyFatGoal: Double, dietPreference: String) {
        viewModelScope.launch {
            repository.insertUserGoal(
                UserGoal(
                    dailyCalorieGoal = dailyCalorieGoal,
                    dailyProteinGoal = dailyProteinGoal,
                    dailyCarbsGoal = dailyCarbsGoal,
                    dailyFatGoal = dailyFatGoal,
                    dietPreference = dietPreference
                )
            )
        }
    }

    fun signUp(username: String, passwordPlaintext: String, fullName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (username.isBlank() || passwordPlaintext.isBlank() || fullName.isBlank()) {
                onResult(false, "Please fill in all fields.")
                return@launch
            }
            val success = repository.signUpUser(username, passwordPlaintext, fullName)
            if (success) {
                onResult(true, "Successfully registered!")
            } else {
                onResult(false, "Username already exists.")
            }
        }
    }

    fun login(username: String, passwordPlaintext: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (username.isBlank() || passwordPlaintext.isBlank()) {
                onResult(false, "Please enter your username and password.")
                return@launch
            }
            val success = repository.loginUser(username, passwordPlaintext)
            if (success) {
                onResult(true, "Welcome back!")
            } else {
                onResult(false, "Invalid username or password.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logoutUser()
        }
    }

    fun updateProfile(fullName: String, avatarUrl: String, aboutMe: String, information: String) {
        val currentUser = loggedInUser.value ?: return
        viewModelScope.launch {
            repository.updateUserProfile(
                currentUser.copy(
                    fullName = fullName.trim(),
                    avatarUrl = avatarUrl,
                    aboutMe = aboutMe.trim(),
                    information = information.trim()
                )
            )
        }
    }

    // AI Actions
    fun generateRecipeFromPantry() {
        viewModelScope.launch {
            _recipeUiState.value = RecipeUiState.Loading
            try {
                // Trigger recipe call
                val currentPantry = pantryItems.value
                val preference = userGoal.value.dietPreference
                val result = repository.generateRecipe(currentPantry, preference)
                if (result != null) {
                    _recipeUiState.value = RecipeUiState.Success(result)
                } else {
                    _recipeUiState.value = RecipeUiState.Error("Failed to generate recipe. Check internet connectivity.")
                }
            } catch (e: Exception) {
                _recipeUiState.value = RecipeUiState.Error(e.localizedMessage ?: "Unknown error occurred.")
            }
        }
    }

    fun analyzeTodayDiet() {
        viewModelScope.launch {
            _analysisUiState.value = AnalysisUiState.Loading
            try {
                val currentLogs = currentDietLogs.value
                val goal = userGoal.value
                val result = repository.analyzeDiet(currentLogs, goal)
                if (result != null) {
                    _analysisUiState.value = AnalysisUiState.Success(result)
                } else {
                    _analysisUiState.value = AnalysisUiState.Error("Could not retrieve AI nutritional analysis.")
                }
            } catch (e: Exception) {
                _analysisUiState.value = AnalysisUiState.Error(e.localizedMessage ?: "Analysis server error.")
            }
        }
    }

    fun clearRecipeState() {
        _recipeUiState.value = RecipeUiState.Idle
    }

    fun clearAnalysisState() {
        _analysisUiState.value = AnalysisUiState.Idle
    }
}

class DietPantryViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DietPantryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DietPantryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
