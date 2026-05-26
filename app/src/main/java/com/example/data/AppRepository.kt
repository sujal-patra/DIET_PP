package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val pantryDao: PantryDao,
    private val dietLogDao: DietLogDao,
    private val userGoalDao: UserGoalDao,
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

    // AI Queries
    suspend fun generateRecipe(pantryItems: List<PantryItem>, preference: String): GeminiRecipe? {
        return geminiRepository.generateRecipe(pantryItems, preference)
    }

    suspend fun analyzeDiet(mealLogs: List<DietLog>, goal: UserGoal): GeminiAnalysis? {
        return geminiRepository.analyzeDietAndMeals(mealLogs, goal)
    }
}
