package com.example.coffeetimer

import android.content.DialogInterface
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeetimer.data.Recipe
import com.example.coffeetimer.data.RecipeDao
import com.example.coffeetimer.data.RecipeDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.time.Instant

class RecipeFragment : Fragment() {
    private lateinit var recipeDao: RecipeDao
    private lateinit var recipes: ArrayList<Recipe>

    private lateinit var faButton : FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerAdapter: RecyclerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        faButton = view.findViewById(R.id.fab)
        faButton.customSize = 140
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(view.context) // これが無いと表示されないっぽい？
        val itemDecoration = object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                // アイテムの上下左右にpaddingを入れる（単位：px）
                outRect.set(0, 0, 0, 20)
            }
        }
        recyclerView.addItemDecoration(itemDecoration)

        val db = RecipeDatabase.getDatabase( requireNotNull(this.activity).application )
        recipeDao = db.recipeDao()

        lifecycleScope.launch {
            recipes = recipeDao.selectAll() as ArrayList<Recipe> // 変更可能のリストに変換する
            recyclerAdapter = RecyclerAdapter(recipes)
            recyclerView.adapter = recyclerAdapter

            recyclerAdapter.setOnButtonClickListener(object:RecyclerAdapter.OnButtonClickListener{
                @RequiresApi(Build.VERSION_CODES.O) // Instant.now()を使うために必要
                override fun onSaveClick(position: Int, recipeId: Int) {
                    val holder = recyclerView.findViewHolderForAdapterPosition(position) as RecyclerAdapter.ViewHolderItem
                    val inputtedRecipe = Recipe(
                        id = recipeId,
                        beansName = holder.beansNameHolder.text?.toString(),
                        roastLevel = holder.roastLevelHolder.text?.toString(),
                        grind = holder.grindHolder.text.toString().toIntOrNull(),
                        temperature = holder.temperatureHolder.text.toString().toIntOrNull(),
                        memo = holder.memoHolder.text?.toString(),
                        modifiedDateTime = Instant.now()
                    )
                    lifecycleScope.launch {
                        var generatedId: Long? = null
                        if(recipeId == 0){
                            generatedId = recipeDao.insert(inputtedRecipe)
                        } else {
                            recipeDao.update(inputtedRecipe)
                        }

                        val idx = recipes.indexOfFirst { it.id == recipeId }
                        recipes[idx] = inputtedRecipe
                        // DB上の値を更新・削除できるようにするために、自動生成されたidをレシピにセットする
                        if(generatedId != null) {
                            recipes[idx].id = generatedId.toInt()
                            faButton.isEnabled = true
                        }
                        // レシピのタイトルを更新するため
                        recyclerAdapter.notifyItemChanged(position, "Do not close recipecard")
                    }
                    Snackbar.make(view, "レシピを保存しました", 1000/* ms */)
                        .show()
                }
                override fun onCancelClick(position: Int, recipeId: Int) {
                    if(recipeId == 0) {
                        val idx = recipes.indexOfFirst { it.id == 0 }
                        recipes.removeAt(idx)
                        recyclerAdapter.notifyItemRemoved(position)
                        faButton.isEnabled = true
                    } else {
                        // 現在recipesに入っている値で更新することでロールバックする
                        recyclerAdapter.notifyItemChanged(position, "Do not close recipecard")
                    }
                }
                override fun onDeleteClick(position: Int, recipeId: Int) {
                    android.app.AlertDialog.Builder(view.context)
                        .setTitle("このレシピを削除しますか？")
                        .setPositiveButton("はい") { dialog: DialogInterface, _: Int ->
                            try {
                                // AlertDialogを非表示にする
                                dialog.dismiss()
                                if(recipeId == 0) {
                                    faButton.isEnabled = true
                                } else {
                                    lifecycleScope.launch {
                                        recipeDao.delete(recipeId)
                                    }
                                }
                                val idx = recipes.indexOfFirst { it.id == recipeId }
                                recipes.removeAt(idx)
                                recyclerAdapter.notifyItemRemoved(position)
                            } catch (ignored: Exception) {
                            }
                        }.setNegativeButton("いいえ") { _: DialogInterface, _: Int ->
                            // DO NOTHING
                        }
                        .show()
                }
            })
        }

        faButton.setOnClickListener {
            // adapterにセットしたリストが監視されるので、recipesに新しいRecipeを追加する必要がある
            recipes.add(Recipe(
                id = 0, // idを0に設定してDBに登録すると、idが自動生成される
                beansName = null,
                roastLevel = null,
                grind = null,
                temperature = null,
                memo = null,
                modifiedDateTime = null
            ))
            // 表示するリストを更新
            recyclerAdapter.notifyItemInserted(recipes.lastIndex)
            faButton.isEnabled = false
        }
    }
}