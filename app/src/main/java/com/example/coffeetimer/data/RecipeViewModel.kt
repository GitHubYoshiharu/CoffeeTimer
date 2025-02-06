package com.example.coffeetimer.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecipeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao: RecipeDao
    // idをautoGenerateにしていると、一度DBにデータを入れないとidが生成されず、updateできない。
    // FlowはDBが更新されたら自動で同期してくれる。
    private val _recipes = MutableStateFlow<List<Recipe>>(listOf()) // 更新可能な内部値。
    val recipes: StateFlow<List<Recipe>> = _recipes // 外部に公開される値。内部値が更新されると連動して更新される。
    init {
        val db = RecipeDatabase.getDatabase(application)
        dao = db.recipeDao()
    }

    fun getAllRecipes() {
        viewModelScope.launch(Dispatchers.IO) {
            _recipes.value = dao.selectAll()
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(id)
        }
    }
}