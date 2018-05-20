package com.example.changedetection

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem
import androidx.navigation.Navigation
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.orhanobut.logger.Logger

class HostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nav_frag)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.my_nav_host_fragment) as NavHostFragment
            findViewById<BottomNavigationView>(R.id.bottom_nav_view).let { bottomNavView ->
                bottomNavView.setOnNavigationItemSelectedListener {
                    println("rawr!")
                    true
                }

//            bottomNavView.setOnNavigationItemSelectedListener {
//                println(it.itemId)
//                NavigationUI.onNavDestinationSelected(
//                    it,
//                    Navigation.findNavController(this, R.id.my_nav_host_fragment)
//                )
//            }
//
            NavigationUI.setupWithNavController(bottomNavView, navHostFragment.navController)
        }

        Navigation.findNavController(navHostFragment.view!!).addOnNavigatedListener { controller, destination ->
            println("addOnNavigateListener")

        }
    }
}
