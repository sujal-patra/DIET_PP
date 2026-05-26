package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    val expiryDate: Long? = null,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "diet_logs")
data class DietLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snack"
    val foodName: String,
    val calories: Int,
    val protein: Double, // in grams
    val carbs: Double,   // in grams
    val fat: Double,     // in grams
    val dateString: String, // "YYYY-MM-DD" for filtering
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_goals")
data class UserGoal(
    @PrimaryKey val id: Int = 1, // Single row configuration
    val dailyCalorieGoal: Int = 2000,
    val dailyProteinGoal: Double = 120.0,
    val dailyCarbsGoal: Double = 220.0,
    val dailyFatGoal: Double = 70.0,
    val dietPreference: String = "None" // "None", "Keto", "Vegan", "Vegetarian", "Gluten Free", "Low Carb"
)

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val avatarUrl: String = "avatar_avocado", // Preset key or external URL
    val aboutMe: String = "Passionate about healthy living and clean nutrition.",
    val information: String = "Goal weight: 75kg • Height: 180cm",
    val isLoggedIn: Boolean = false
)
