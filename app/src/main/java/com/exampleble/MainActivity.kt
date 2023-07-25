package com.exampleble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.clj.fastble.BleManager
import com.clj.fastble.data.BleDevice
import com.desarollobluetooth.fragments.MainFragment
import com.exampleble.fragment.ShowChartFragment
import com.exampleble.observers.Observer
import com.exampleble.observers.ObserverManager
import com.google.android.material.navigation.NavigationView
import com.ibsalab.general.activity.LoginActivityPersonal
import com.ibsalab.general.util.AuthUtil
import io.realm.Realm
import kotlinx.android.synthetic.main.app_bar_main.*
import javax.annotation.Nullable


class MainActivity : AppCompatActivity(), MainFragment.OnFragmentInteractionListener, Observer {

    private var titles = arrayOfNulls<String>(3)
    private var bleDevice: BleDevice? = null
    private var bluetoothGattService: BluetoothGattService? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private var charaProp: Int = 0
    private var currentPage = 0
    private val fragments = ArrayList<Fragment>()
    private var toolbar: Toolbar? = null

    override fun disConnected(device: BleDevice?) {
        if (device != null && bleDevice != null && device.key == bleDevice!!.key) {
            //finish()
            Toast.makeText(
                this,
                "Disconnection",
                Toast.LENGTH_LONG
            ).show()
            changePage(1)
        }
    }

    fun changePage(page: Int) {
        currentPage = page
        toolbar!!.title = titles[page]
        updateFragment(page)

        /*
        if (currentPage == 1) {
            (fragments[1] as ShowChartFragment)
        }

         */
    }

    private fun initPage() {
       //prepareFragment()
        //onNotify()
        val fragmentTransaction = supportFragmentManager.beginTransaction()  //用於儲存指令
        fragmentTransaction.replace(R.id.frameLayout, fragments[0])   //呼叫剛才添加在fragmentList裡的第一個fragment，以第一個fragment取代現在的頁面，即為MainFragment
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)  //設定頁面轉跳的動畫
        fragmentTransaction.commit()  //(當現在的線程結束其他工作時，)執行這個指令

    }

    private fun prepareFragment() {
        fragments.add(ShowChartFragment.newInstance("",""))

        for (fragment in fragments) {
            if ( fragment !is MainFragment) {
                supportFragmentManager.beginTransaction().add(R.id.frameLayout, fragment)  //將fragment放到fragmentLayout裡面
                    .hide(fragment).commit()   //先不要顯示添加的fragment

            }
        }
    }


    private fun updateFragment(position: Int) {
        if (position > fragments.size - 1) {    //insurance for exceeding the size of fragments
            return
        }

        for (i in fragments.indices) {
            val transaction = supportFragmentManager.beginTransaction()
            val fragment = fragments[i]
            if (i == position) {
                transaction.show(fragment)

            }
            else {
                transaction.hide(fragment)
            }
            transaction.commit()

        }
    }

    override fun onFragmentInteraction(bledevice: BleDevice?) {   //Any change according to the parameter
        this.bleDevice = bledevice
        prepareFragment()
        changePage(1)
    }

    private fun intiActionBarDrawer(){
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_bar_main)  //Jump to a different layout(but the same activity)

        titles = arrayOf(                             //get strings from strings.xml
            getString(R.string.service_list),
            getString(R.string.characteristic_list),
            getString(R.string.console),
            getString(R.string.console)
        )

        /*****************************************
        ******************************************/
        Realm.init(this)
        /*****************************************
         ******************************************/
        toolbar = findViewById(R.id.toolbar)
        toolbar!!.title = titles[0]

        setSupportActionBar(toolbar)  //使用 toolbar 取代原本的 actionBar
        intiActionBarDrawer()
        fragments.add(MainFragment.newInstance("", "")) //instantiate a MainFragment
        setListener()
        initPage()
        ObserverManager.getInstance().addObserver(this)   //this refers to instantiate MainFragment

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val authUtil = AuthUtil(this, "personal", null)

        when (authUtil.accessToken == "") {
            true  -> btnLogin.text = "登入"
            false -> btnLogin.text = "登出"
        }

        btnLogin.setOnClickListener {
            if (authUtil.accessToken == "") {
                val loginIntent =
                    Intent(this, LoginActivityPersonal::class.java)
                startActivity(loginIntent)

            }
            else {
                authUtil.logout()
                btnLogin.text = "登入"
            }
        }
    }
    /**控制menu*/
    private fun setListener(){
        val manager = supportFragmentManager
        val drawerLayout= findViewById<DrawerLayout>(R.id.drawerLayout)
        val drawer: NavigationView= findViewById(R.id.navigation_view)
        drawer.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.action_home -> {
                    Toast.makeText(this,"home",Toast.LENGTH_LONG).show()
                    drawerLayout.closeDrawer(drawer)
                }
                R.id.action_old -> {


                    drawerLayout.closeDrawer(drawer)
                }
                /*R.id.action_settings -> {
                    startActivity(Intent(this, PieChartActivity::class.java))
                }
                R.id.action_about -> {
                    startActivity(Intent(this, SettingActivity::class.java))
                }*/
                else -> {}
            }
            return@setNavigationItemSelectedListener true
        }



    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {  // when pressing "back"
            if (currentPage != 0) {
                currentPage--
                BleManager.getInstance().disconnect(bleDevice)
                bleDevice = null
                changePage(currentPage)
                supportFragmentManager.beginTransaction().remove(fragments[1])
                fragments.removeAt(1)
                true

            }else if (drawerLayout.isDrawerOpen(GravityCompat.START)){
                drawerLayout.closeDrawers()
                true
            } else {
                onBackPressed()
                true
            }
        }
        else {
            super.onKeyDown(keyCode, event)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        BleManager.getInstance().clearCharacterCallback(bleDevice)
        ObserverManager.getInstance().deleteObserver(this)
    }

    fun getBleDevice(): BleDevice? {
        return this.bleDevice
    }

    fun getBluetoothGattService(): BluetoothGattService? {
        return bluetoothGattService
    }

    fun setBluetoothGattService(bluetoothGattService: BluetoothGattService?) {
        this.bluetoothGattService = bluetoothGattService
    }

    fun getCharacteristic(): BluetoothGattCharacteristic? {
        return characteristic
    }

    fun setCharacteristic(characteristic: BluetoothGattCharacteristic) {
        this.characteristic = characteristic
    }

    fun getCharaProp(): Int {
        return charaProp
    }

    fun setCharaProp(charaProp: Int) {
        this.charaProp = charaProp
    }

}
