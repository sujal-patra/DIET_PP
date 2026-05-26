package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.GeminiRepository
import com.example.ui.DietPantryViewModel
import com.example.ui.DietPantryViewModelFactory
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup Database & DAOs
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AppRepository(
            pantryDao = database.pantryDao(),
            dietLogDao = database.dietLogDao(),
            userGoalDao = database.userGoalDao(),
            geminiRepository = GeminiRepository()
        )

        // Initialize Diet & Pantry ViewModel
        val viewModel: DietPantryViewModel by viewModels {
            DietPantryViewModelFactory(repository)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
