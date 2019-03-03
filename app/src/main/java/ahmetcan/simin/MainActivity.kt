package ahmetcan.simin

import ahmetcan.echo.ACPremium
import ahmetcan.simin.Discovery.DiscoveryFragment
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import ahmetcan.simin.Discovery.SearchActivity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.Window.FEATURE_NO_TITLE
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.*
import com.tooltip.Tooltip
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity() : AppCompatActivity(){
    lateinit var billing:ACPremium
    var isSubscripted: Boolean = false



    fun saveSubscriptionState(has: Boolean) {
        val subscription = getSharedPreferences("subscription", Context.MODE_PRIVATE)
        val edit = subscription.edit()
        edit.putBoolean("has", has)
        edit.commit()
    }
    fun checkCache() {
        val subscription = getSharedPreferences("cache", Context.MODE_PRIVATE)
        var introTimeMs = subscription.getLong("time", 0)

        if (introTimeMs > 0) {
            var asDay = TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().timeInMillis - introTimeMs)
            if (asDay > 1) {

                val edit = subscription.edit()
                edit.putLong("time", Calendar.getInstance().timeInMillis)
                edit.commit()

                DiscoveryRepository.invalidateChannelLists()
                DiscoveryRepository.invalidateLists()

            }
        }
        else{
            val edit = subscription.edit()
            edit.putLong("time", Calendar.getInstance().timeInMillis)
            edit.commit()
        }

    }
    fun fetchSubscriptionState(): Boolean {
        val subscription = getSharedPreferences("subscription", Context.MODE_PRIVATE)
        val has: Boolean = subscription.getBoolean("has", false)
        return has;
    }

    fun doIShowIntro(): Boolean {
        val subscription = getSharedPreferences("subscription", Context.MODE_PRIVATE)
        var introTimeMs = subscription.getLong("introtime", 0)

        if (introTimeMs > 0) {
            var asDay = TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().timeInMillis - introTimeMs)
            if (asDay <= 3) {
                return false;
            }
        }
        val edit = subscription.edit()
        edit.putLong("introtime", Calendar.getInstance().timeInMillis)
        edit.commit()
        return true

    }

    fun fetchSubscriptionState(context: Context): Boolean {
        val subscription = context.getSharedPreferences("subscription", Context.MODE_PRIVATE)
        val has: Boolean = subscription.getBoolean("has", false)
        return has;
    }

    @IgnoreExtraProperties
    data class Device(
            var NetworkCountryIso: String? = "",
            var NetworkCountryOperator: String? = "",
            var LocaleCountyIso:String="",
            var LocaleLanguage:String="",
            var packageList: MutableList<PackageData> = mutableListOf<PackageData>(),
            var checkDates: MutableList<String> = mutableListOf<String>()
    )
    @IgnoreExtraProperties
    data class PackageData(
            var Name: String? = "",
            var InstallDate: String? = null,
            var RemoveDate:String?=null
    )
    /*fun startAlarm(){
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val alarmIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(alarmIntent);

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                SystemClock.currentThreadTimeMillis(),
                60000,
                alarmIntent
        )

    }*/
    fun syncStatisticData(){
        var database = FirebaseDatabase.getInstance()
        database.setPersistenceEnabled(true)

        val install = getSharedPreferences("install", Context.MODE_PRIVATE)

        var deviceId  = install.getString("deviceid", null)
        if(deviceId==null){
            deviceId=UUID.randomUUID().toString()
            var ref=database.getReference("/device/"+deviceId);
            ref.keepSynced(true)

            var telephony=applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            var device=Device();
            device.NetworkCountryIso=telephony.networkCountryIso
            device.NetworkCountryOperator=telephony.networkOperator
            device.LocaleCountyIso=Locale.getDefault().country
            device.LocaleLanguage=Locale.getDefault().language

            ref.setValue(device)
            ref.push()

            val edit = install.edit()
            edit.putString("deviceid", deviceId)
            edit.commit()

        }

        val pm = getPackageManager()
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val packages = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
        var ref=database.getReference("/device/"+deviceId);
        ref.addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                Log.e("ahmetcan","firebase cancelll")
            }
            val format = SimpleDateFormat("yyyy:MM:dd:HH:mm:ss ")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var deviceModel=dataSnapshot.getValue(Device::class.java)
                deviceModel?.let {
                    for(item in packages){
                        if(it.packageList.count {f->f.Name== item.activityInfo.packageName && f.RemoveDate==null}==0){
                            //add
                            it.packageList.add(PackageData(item.activityInfo.packageName,format.format(Date())))
                        }
                    }
                    for (packageInfo in deviceModel.packageList.filter { f->f.RemoveDate==null }) {
                        if(packages.count {f->f.activityInfo.packageName== packageInfo.Name }==0){
                            //add
                           packageInfo.RemoveDate=format.format(Date());
                        }

                    }

                    it.checkDates.add(format.format(Date()))

                    ref.setValue(it)
                    ref.push()
                }

            }

        });


    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GlobalScope.async {
            try {
                syncStatisticData()
            }
            catch (ex:java.lang.Exception){
                Log.e("ahmetcan",ex.toString())
            }
        }


        billing=ACPremium(this,object :ACPremium.IState{
            override fun onError() {
                onUI {
                    saveSubscriptionState(true)
                    main_buyButton.visibility = View.GONE

                    try{
                        FirebaseAnalytics.getInstance(this@MainActivity).logEvent("BillingError", null)

                    }
                    catch (ex:Exception){


                    }
                }
            }

            override fun onUserCancelFlow() {
                try{
                    var firebaseAnalytics = FirebaseAnalytics.getInstance(this@MainActivity)
                    val params = Bundle()
                    params.putString("BillingUserCancelStr","BillingUserCancelStrValu")
                    firebaseAnalytics.logEvent("BillingUserCancelEvent", params)

                }catch (ex:Exception){}
            }

            override fun onPremiumChanged(isPremium: Boolean) {
                onUI {
                    if (isPremium) {
                        saveSubscriptionState(true)
                        main_buyButton.visibility = View.GONE
                    } else {
                        saveSubscriptionState(false)
                        if (doIShowIntro()) {
                            try {
                                val tooltip = Tooltip.Builder(this@MainActivity, main_buyButton)
                                        .setText(R.string.subscription_intro)
                                        .setPadding(30f)
                                        .setCornerRadius(10f)
                                        .setTextSize(13f)
                                        .setBackgroundColor(Color.rgb(170, 60, 57))
                                        .setDismissOnClick(true)
                                        .setCancelable(true)
                                        .show()
                            }
                            catch(ex:Exception){

                            }


                        }

                    }
                }

            }

        })


        requestWindowFeature(FEATURE_NO_TITLE)

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false);

        checkCache();


        tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.icon?.clearColorFilter()
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    tab.icon?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
                    when (it.position) {
                        0 -> changeFragment(DiscoveryFragment())
                        1 -> changeFragment(ChannelFragment())
                        2 -> changeFragment(FavoriesFragment())
                    }

                }

            }

        })
        tab_layout.getTabAt(0)?.icon?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
        changeFragment(DiscoveryFragment())

        fab.setOnClickListener {
            var intent: Intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }


        isSubscripted = fetchSubscriptionState(this)
        if (isSubscripted) {
            main_buyButton.visibility = View.GONE
        }

        main_buyButton.setOnClickListener {
            try{
                var firebaseAnalytics = FirebaseAnalytics.getInstance(this)
                val params = Bundle()
                params.putString("MainBuyClickedStr","strMainBuy")
                firebaseAnalytics.logEvent("MainBuyEvent", params)

            }catch (ex:Exception){}


            billing.buyPremium()
        }

        button_privacypolicy.setOnClickListener {
            AlertDialog.Builder(this)
                    .setMessage("would you like to see the privacy policy ?")
                    .setNegativeButton("Close",object:DialogInterface.OnClickListener{
                        override fun onClick(p0: DialogInterface?, p1: Int) {

                        }


                    })
                    .setPositiveButton("Go",object :DialogInterface.OnClickListener{
                        override fun onClick(p0: DialogInterface?, p1: Int) {
                            try {
                                val myIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.freeprivacypolicy.com/privacy/view/cf6bb63fe3af021f517bc52e9b40c4f4"))
                                startActivity(myIntent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(this@MainActivity, "No application can handle this request." + " Please install a webbrowser", Toast.LENGTH_LONG).show()
                                e.printStackTrace()
                            }

                        }


                    }).show()

        }
    }

    override fun onStart() {
        super.onStart()


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.main, menu)

        return true
    }


    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.contentFragment, fragment)
                .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        try{
            billing.destroy()
        }
        catch (ex:Exception){
            Log.e("Simin",ex.toString())
        }

    }

    //InAppBilling


}
