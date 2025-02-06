package com.example.coffeetimer

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.recyclerview.widget.RecyclerView
import com.example.coffeetimer.data.Recipe
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import net.cachapa.expandablelayout.ExpandableLayout

class RecyclerAdapter(private val recipes: ArrayList<Recipe>) : RecyclerView.Adapter<RecyclerAdapter.ViewHolderItem>() {
    private lateinit var listener: OnButtonClickListener

    // リストに表示するアイテムの表示内容
    inner class ViewHolderItem(v: View) : RecyclerView.ViewHolder(v) {
        var expanded: Boolean = false
        val drawableOpen: Drawable? = getDrawable(v.context, R.drawable.open)
        val drawableClose: Drawable? = getDrawable(v.context, R.drawable.close)

        val recipeTitleHolder: TextView = v.findViewById(R.id.recipe_title)
        val beansNameHolder: TextInputEditText = v.findViewById(R.id.beans_name)
        val roastLevelHolder: AutoCompleteTextView = v.findViewById(R.id.roast_level)
        val grindHolder: TextInputEditText = v.findViewById(R.id.grind)
        val temperatureHolder: TextInputEditText = v.findViewById(R.id.temperature)
        val memoHolder: TextInputEditText = v.findViewById(R.id.memo)

        val recipeTitleLayout: LinearLayout = v.findViewById(R.id.recipe_title_layout)
        val expandableLayout: ExpandableLayout = v.findViewById(R.id.expandable_layout)
        val saveButtonHolder: MaterialButton = v.findViewById(R.id.save_button)
        val cancelButtonHolder: MaterialButton = v.findViewById(R.id.cancel_button)
        val deleteButtonHolder: ImageButton = v.findViewById(R.id.delete_button)

        init {
            val adapter = ArrayAdapter(
                v.context,
                android.R.layout.simple_dropdown_item_1line,
                listOf("ライト", "シナモン", "ミディアム", "ハイ", "シティ", "フルシティ", "フレンチ", "イタリアン")
            )
            roastLevelHolder.setAdapter(adapter)

            recipeTitleLayout.setOnClickListener {
                if(expanded){
                    expandableLayout.collapse(false)
                    recipeTitleHolder.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawableOpen, null)
                } else {
                    expandableLayout.expand(false)
                    recipeTitleHolder.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawableClose, null)
                }
                expanded = !expanded
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderItem {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recipecard, parent, false)
        return ViewHolderItem(view)
    }

    override fun onBindViewHolder(holder: ViewHolderItem, position: Int) {
        val currentItem = recipes[position]
        var recipeTitle = currentItem.beansName ?: ""
        if( recipeTitle.isNotEmpty() ){
            recipeTitle += if(currentItem.roastLevel == null) "" else "（${currentItem.roastLevel}）"
        }
        // TODO: 豆の名前が長いと表示しきれない問題を解決したい。改行できるようにする？文字数に応じて文字を小さくする？
        holder.recipeTitleHolder.setText(recipeTitle)
        holder.beansNameHolder.setText(currentItem.beansName)
        holder.roastLevelHolder.setText(currentItem.roastLevel ?: "ミディアム", false)
        holder.grindHolder.setText(currentItem.grind?.toString() ?: "")
        holder.temperatureHolder.setText(currentItem.temperature?.toString() ?: "")
        holder.memoHolder.setText(currentItem.memo)

        // 更新通知を送る度に開閉フラグがデフォルト値に戻ってしまうので、メンバ変数を基に開閉状態を決める。
        if(currentItem.id == 0){
            holder.expanded = true
        }
        if(holder.expanded){
            holder.expandableLayout.expand(false)
            holder.recipeTitleHolder.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, holder.drawableClose, null)
        }

        // 呼ばれる度に上書きされるので問題ない
        holder.saveButtonHolder.setOnClickListener {
            listener.onSaveClick(position, recipes[position].id)
        }
        holder.cancelButtonHolder.setOnClickListener {
            listener.onCancelClick(position, recipes[position].id)
        }
        holder.deleteButtonHolder.setOnClickListener {
            listener.onDeleteClick(position, recipes[position].id)
        }
    }

    // notifyItemChangedの初回実行時のみ、なぜかonCreateViewHolder()が呼び出されてしまい、expandedが初期化されてしまうので、
    // notifyItemChangedからレシピを閉じさせない指定ができるようにする（第2引数が指定されると、それをpayloadsとして受け取るこのメソッドが呼び出される）。
    override fun onBindViewHolder(holder: ViewHolderItem, position: Int, payloads: List<Any>) {
        if(payloads.any()){ // 1つ以上の要素があればtrueを返す
            holder.expanded = true
        }
        onBindViewHolder(holder, position)
    }

    // Fragmentで実装する
    interface OnButtonClickListener {
        fun onSaveClick(position: Int, recipeId: Int)
        fun onCancelClick(position: Int, recipeId: Int)
        fun onDeleteClick(position: Int, recipeId: Int)
    }

    fun setOnButtonClickListener(listener: OnButtonClickListener){
        this.listener = listener
    }

    // リストサイズを取得する用のメソッド
    override fun getItemCount(): Int = recipes.size

    // ビューに一意の値を割り当てることで、スクロール時にユーザー入力値を破棄させないようにする
    override fun getItemViewType(position: Int) = position
    override fun getItemId(position: Int) = position.toLong()
}