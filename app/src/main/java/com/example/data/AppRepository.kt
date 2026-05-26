package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val pantryDao: PantryDao,
    private val dietLogDao: DietLogDao,
    private val userGoalDao: UserGoalDao,
    private val userAccountDao: UserAccountDao,
    private val geminiRepository: GeminiRepository
) {
    // Pantry items queries
    val allPantryItems: Flow<List<PantryItem>> = pantryDao.getAllPantryItems()

    suspend fun insertPantryItem(item: PantryItem) {
        pantryDao.insertPantryItem(item)
    }

    suspend fun deletePantryItem(item: PantryItem) {
        pantryDao.deletePantryItem(item)
    }

    suspend fun deletePantryItemById(id: Int) {
        pantryDao.deleteById(id)
    }

    // Diet/Meal Logs queries
    fun getDietLogsByDate(date: String): Flow<List<DietLog>> = dietLogDao.getDietLogsByDate(date)

    val allDietLogs: Flow<List<DietLog>> = dietLogDao.getAllDietLogs()

    suspend fun insertDietLog(log: DietLog) {
        dietLogDao.insertDietLog(log)
    }

    suspend fun deleteDietLog(log: DietLog) {
        dietLogDao.deleteDietLog(log)
    }

    suspend fun deleteDietLogById(id: Int) {
        dietLogDao.deleteById(id)
    }

    // User goals queries
    val userGoalFlow: Flow<UserGoal?> = userGoalDao.getUserGoalFlow()

    suspend fun getUserGoal(): UserGoal {
        return userGoalDao.getUserGoal() ?: UserGoal()
    }

    suspend fun insertUserGoal(goal: UserGoal) {
        userGoalDao.insertUserGoal(goal)
    }

    // User Account / Authentication / Profile methods
    val loggedInUserFlow: Flow<UserAccount?> = userAccountDao.getLoggedInUserFlow()

    suspend fun getLoggedInUser(): UserAccount? {
        return userAccountDao.getLoggedInUser()
    }

    suspend fun signUpUser(username: String, passwordPlaintext: String, fullName: String): Boolean {
        val existing = userAccountDao.getUserByUsername(username.lowercase().trim())
        if (existing != null) {
            return false // Username already taken
        }
        val newUser = UserAccount(
            username = username.lowercase().trim(),
            passwordHash = passwordPlaintext, // Plaintext/simple storage for local sandbox representation of signup
            fullName = fullName.trim(),
            isLoggedIn = true // Log them in immediately upon signup
        )
        // Ensure only one is logged in at a time
        userAccountDao.logoutAll()
        userAccountDao.insertUser(newUser)
        return true
    }

    suspend fun loginUser(username: String, passwordPlaintext: String): Boolean {
        val user = userAccountDao.getUserByUsername(username.lowercase().trim())
        if (user != null && user.passwordHash == passwordPlaintext) {
            userAccountDao.logoutAll()
            userAccountDao.setLoggedIn(user.id)
            return true
        }
        return false
    }

    suspend fun logoutUser() {
        userAccountDao.logoutAll()
    }

    suspend fun updateUserProfile(user: UserAccount) {
        userAccountDao.insertUser(user)
    }

    // AI Queries
    suspend fun generateRecipe(pantryItems: List<PantryItem>, preference: String): GeminiRecipe? {
        return geminiRepository.generateRecipe(pantryItems, preference)
    }

    suspend fun analyzeDiet(mealLogs: List<DietLog>, goal: UserGoal): GeminiAnalysis? {
        return geminiRepository.analyzeDietAndMeals(mealLogs, goal)
    }
}
