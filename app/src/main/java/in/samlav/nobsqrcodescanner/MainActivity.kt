package `in`.samlav.nobsqrcodescanner

import android.os.Bundle
import android.view.Menu
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.nobsqrcodescanner.R

class MainActivity : AppCompatActivity()
{

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean
    {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}

object Constants
{
    const val PICK_IMAGE = 100
    const val REQUEST_PERMS = 250
    const val WIFI_CODE = 555
    const val ABOUT_INTENT = 10
    const val EXECUTE_INTENTS = "execute_intents"
    const val SHARED_PREF = "pref"
    const val FROM_SHORTCUT = "from_shortcut"
    const val TITLE = "Color Finder"
    const val ABOUT_TITLE = "About Color Finder"
    const val PERSONAL_WEBSITE = "https://samlav.in"
    const val REPOSITORY = "https://github.com/Salavin/No-BS-QR-Code-Scanner"
    const val GOOGLE_PAY = "https://gpay.app.goo.gl/pay-9su9NF43f9N"
}