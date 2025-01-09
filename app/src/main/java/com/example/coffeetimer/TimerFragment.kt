package com.example.coffeetimer

import android.content.Context.MODE_PRIVATE
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt


class TimerFragment : Fragment() {
    // 変数
    private var startTime : Long? = null
    private var isStarted = false
    private var isCountdown = true // 本当は初期化したくない
    private var canNavigate = false
    // まだViewが生成されていないので、インスタンスの代入はonCreateで行う必要がある
    private lateinit var countText : TextView
    private lateinit var timeTable : TableLayout
    private lateinit var toggleUnit : TextView
    private lateinit var totalAmountOfWater : TextView
    private lateinit var grind : TextView
    private lateinit var temperature : TextView
    private lateinit var startResetButton : ImageButton
    private lateinit var addRowButton : Button
    private lateinit var deleteRowButton : Button

    // 定数
    private val countdownTime = 3000L
    private val intervalMs = 10L
    private val dataFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimer = object : Runnable {
        override fun run() {
            if (startTime == null) {
                startTime = SystemClock.uptimeMillis()
            }
            val diffTime = SystemClock.uptimeMillis() - startTime!!
            if (isCountdown) {
                // 正確に0.1秒後に呼ばれるわけではないので、「==」で判定できない
                if (diffTime >= countdownTime) {
                    countText.setTextColor( Color.parseColor("#C0C0C0") )
                    isCountdown = !isCountdown
                }
                // カウントダウン中は切り上げ表示した方が自然に見える
                countText.text = dataFormat.format( (countdownTime - diffTime) + 1000L )
            } else {
                countText.text = dataFormat.format( abs(countdownTime - diffTime) )
            }
            // 終わった工程をグレーアウトする（最後の工程を除く）
            if(canNavigate && timeTable.childCount>2){
                for(i in 2 until timeTable.childCount){
                    val row = timeTable.getChildAt(i) as TableRow
                    val timeCell = row.getChildAt(0) as TextView
                    // CharSequence型は「==」などで比較できない
                    if(timeCell.text.contentEquals(countText.text)) {
                        // TableRowより、セルごとの背景色設定のほうが優先される
                        val rowBefore = timeTable.getChildAt(i-1) as TableRow
                        rowBefore.getChildAt(0).setBackgroundResource(R.color.finishedgrey)
                        rowBefore.getChildAt(1).setBackgroundResource(R.color.finishedgrey)
                        break
                    }
                }
            }
            // 0.1秒後に再度呼ばれるようにする（正確ではない）
            handler.postDelayed(this, intervalMs)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        countText = view.findViewById(R.id.count_label)
        timeTable = view.findViewById(R.id.time_table)
        toggleUnit = view.findViewById(R.id.toggle_unit)
        totalAmountOfWater = view.findViewById(R.id.total_amount_of_water)
        grind = view.findViewById(R.id.grind)
        temperature = view.findViewById(R.id.temperature)
        addRowButton = view.findViewById(R.id.add_row_button)
        deleteRowButton = view.findViewById(R.id.delete_row_button)

        // DBにデータが保存されているなら、読み込んでテーブルを作成する
        val sharedPref = activity?.getPreferences(MODE_PRIVATE)
        if (sharedPref != null) {
            with(sharedPref.edit()){
                if(sharedPref.all["unit_gram"]==null){
                    putBoolean("unit_gram", true)
                    apply()
                    toggleUnit.text = "湯量(g)"
                } else if(sharedPref.all["unit_gram"]==true){
                    toggleUnit.text = "湯量(g)"
                } else {
                    toggleUnit.text = "湯量(%)"
                }
            }

            if(sharedPref.all["amount_of_water"]==null || sharedPref.all["amount_of_water"]==-1){
                totalAmountOfWater.text = ""
            } else {
                totalAmountOfWater.text = sharedPref.all["amount_of_water"].toString()
            }
            if(sharedPref.all["grind"]==null || sharedPref.all["grind"]==-1){
                grind.text = ""
            } else {
                grind.text = sharedPref.all["grind"].toString()
            }
            if(sharedPref.all["temperature"]==null || sharedPref.all["temperature"]==-1){
                temperature.text = ""
            } else {
                temperature.text = sharedPref.all["temperature"].toString()
            }

            // データはMap構造で保存されているので、参照する順番を合わせる必要がある
            for(i in 1..sharedPref.all.size /* 終端を決めなきゃいけないのでテキトーに設定 */){
                if(sharedPref.all["time_$i"]==null) break
                layoutInflater.inflate(R.layout.tablerow, timeTable)
                val row = timeTable.getChildAt(i) as TableRow
                val timeCell = row.getChildAt(0) as TextView
                val waterCell = row.getChildAt(1) as TextView
                timeCell.text = sharedPref.all["time_$i"].toString()
                if(sharedPref.all["water_$i"]==-1) {
                    waterCell.text = ""
                } else {
                    waterCell.text = sharedPref.all["water_$i"].toString()
                }
            }
        }
        // 項目名を表示しているRowもchildとしてカウントされる
        deleteRowButton.isEnabled = timeTable.childCount>1

        startResetButton = view.findViewById(R.id.start_reset_button)
        startResetButton.setOnClickListener {
            if (!isStarted) {
                startResetButton.setImageResource(R.drawable.reset)
                countText.setTextColor( Color.parseColor("#ea3323") )
                isCountdown = true
                canNavigate = writeTimeTable()
                if(canNavigate){
                    toggleUnit.text = "湯量(g)"
                    val sharedPref = activity?.getPreferences(MODE_PRIVATE)
                    val doConvert = sharedPref!=null
                            && totalAmountOfWater.text.toString().isNotEmpty()
                            && !sharedPref.getBoolean("unit_gram", false)
                    for(i in 1 until timeTable.childCount){
                        val row = timeTable.getChildAt(i) as TableRow
                        val timeCell = row.getChildAt(0) as TextView
                        val waterCell = row.getChildAt(1) as TextView
                        // %表記をグラム単位に変換する
                        if (doConvert && waterCell.text.toString().isNotEmpty()) {
                            val gramWater = totalAmountOfWater.text.toString().toInt() * (waterCell.text.toString().toInt()/100.0)
                            waterCell.text = gramWater.toInt().toString()
                        }
                        if(timeCell.text.toString().isNotEmpty()) {
                            // 秒数表記を時間表記に変換する（引数はミリ秒）
                            timeCell.text = dataFormat.format( timeCell.text.toString().toLong() * 1000L )
                        }
                        // セルを編集不可にする（enableをいじるとグレーアウトしてしまう）
                        // 両方の値を変更する必要がある
                        timeCell.isFocusable = false
                        timeCell.isFocusableInTouchMode = false
                        waterCell.isFocusable = false
                        waterCell.isFocusableInTouchMode = false
                    }
                }
                addRowButton.isEnabled = false
                deleteRowButton.isEnabled = false
                handler.post(updateTimer)
            } else {
                startResetButton.setImageResource(R.drawable.start)
                handler.removeCallbacks(updateTimer)
                startTime = null
                // カウントダウン中にリセットが押されると、時間表示が赤色のままになる
                countText.setTextColor( Color.parseColor("#C0C0C0") )
                countText.text = dataFormat.format(0)
                if(canNavigate && timeTable.childCount>1) {
                    val sharedPref = activity?.getPreferences(MODE_PRIVATE)
                    if (sharedPref != null) {
                        if( !sharedPref.getBoolean("unit_gram", false) ){
                            toggleUnit.text = "湯量(%)"
                        }
                        for(i in 1 until timeTable.childCount){
                            val row = timeTable.getChildAt(i) as TableRow
                            val timeCell = row.getChildAt(0) as TextView
                            val waterCell = row.getChildAt(1) as TextView
                            // 時間表記から秒数表記に書き直す
                            timeCell.text = sharedPref.getInt("time_$i",0).toString()
                            // 湯量の表記をもとに戻す
                            if(sharedPref.getInt("water_$i",0) != -1){
                                waterCell.text = sharedPref.getInt("water_$i",0).toString()
                            }
                            // 背景色を戻す
                            timeCell.setBackgroundResource(R.color.cellgrey)
                            waterCell.setBackgroundResource(R.color.cellgrey)
                            // セルを編集可能にする
                            timeCell.isFocusable = true
                            timeCell.isFocusableInTouchMode = true
                            waterCell.isFocusable = true
                            waterCell.isFocusableInTouchMode = true
                        }
                    }
                }
                addRowButton.isEnabled = true
                deleteRowButton.isEnabled = timeTable.childCount>1
            }
            isStarted = !isStarted
        }

        toggleUnit.setOnClickListener {
            val sharedPref = activity?.getPreferences(MODE_PRIVATE)
            if (sharedPref != null) {
                with(sharedPref.edit()) {
                    if ( sharedPref.getBoolean("unit_gram", false) ) {
                        putBoolean("unit_gram", false)
                        apply()
                        toggleUnit.text = "湯量(%)";
                    } else {
                        putBoolean("unit_gram", true)
                        apply()
                        toggleUnit.text = "湯量(g)";
                    }
                }
            }
        }

        addRowButton.setOnClickListener {
            // 画面に収まる行数に制限する
            if(timeTable.childCount-1 >= 7) return@setOnClickListener
            layoutInflater.inflate(R.layout.tablerow, timeTable)
            deleteRowButton.isEnabled = true
        }

        deleteRowButton.setOnClickListener {
            val removeIdx = timeTable.childCount - 1
            val sharedPref = activity?.getPreferences(MODE_PRIVATE)
            if (sharedPref != null) {
                with(sharedPref.edit()){
                    // DBからもデータを消す
                    remove("time_$removeIdx")
                    remove("water_$removeIdx")
                }
            }
            timeTable.removeViewAt(removeIdx)
            deleteRowButton.isEnabled = timeTable.childCount>1
        }
    }

    // applyを呼び出さないことでロールバックされる
    private fun writeTimeTable(): Boolean{
        if(timeTable.childCount == 1) return false
        val sharedPref = activity?.getPreferences(MODE_PRIVATE) ?: return false
        with(sharedPref.edit()){
            // 右のフォームはすべて空欄を許可する
            if(totalAmountOfWater.text.toString().isNotEmpty()){
                putInt("amount_of_water", totalAmountOfWater.text.toString().toInt())
            } else {
                putInt("amount_of_water", -1)
            }
            if(grind.text.toString().isNotEmpty()){
                putInt("grind", grind.text.toString().toInt())
            } else {
                putInt("grind", -1)
            }
            if(temperature.text.toString().isNotEmpty()){
                putInt("temperature", temperature.text.toString().toInt())
            } else {
                putInt("temperature", -1)
            }

            var timeIntBefore : Int? = null
            for(i in 1 until timeTable.childCount){
                val row = timeTable.getChildAt(i) as TableRow
                val timeCell = row.getChildAt(0) as TextView
                val waterCell = row.getChildAt(1) as TextView
                // バリデーションチェック
                if(timeCell.text.toString().isNotEmpty()) {
                    val timeInt = timeCell.text.toString().toInt()
                    if(timeIntBefore == null || timeInt > timeIntBefore){
                        putInt("time_$i", timeCell.text.toString().toInt())
                        timeIntBefore = timeInt
                    } else {
                        return false
                    }
                } else {
                    return false
                }
                // 湯量は空欄の場合も登録する（空欄で抽出終了を示せるので）。空欄は「-1」で表す
                if(waterCell.text.toString().isNotEmpty()) {
                    putInt("water_$i", waterCell.text.toString().toInt())
                } else {
                    putInt("water_$i", -1)
                }
            }
            apply()
            return true
        }
    }
}