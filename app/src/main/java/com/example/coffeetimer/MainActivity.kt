package com.example.coffeetimer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_UNLABELED

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = fragment.navController
        val navbarView = findViewById<BottomNavigationView>(R.id.navbar_view)
        navbarView.labelVisibilityMode = LABEL_VISIBILITY_UNLABELED // テキストを非表示にする
        navbarView.setupWithNavController(navController)
    }
}