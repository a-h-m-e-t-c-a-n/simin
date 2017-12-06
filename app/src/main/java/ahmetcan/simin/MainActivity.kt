package ahmetcan.simin

import InAppBilling.IabBroadcastReceiver
import ahmetcan.simin.Discovery.DiscoveryFragment
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import ahmetcan.simin.Discovery.SearchActivity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window.FEATURE_NO_TITLE
import com.google.firebase.crash.FirebaseCrash
import com.tooltip.Tooltip
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.async
import java.util.*
import java.util.concurrent.TimeUnit








class MainActivity() : AppCompatActivity(), IabBroadcastReceiver.IabBroadcastListener {
    var inappBillingNoAdv: InAppBillingNoAdv = InAppBillingNoAdv()
    var isSubscripted: Boolean = false
    override fun receivedBroadcast() {
        inappBillingNoAdv.receivedBroadcast()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        inappBillingNoAdv.onActivityResult(requestCode, resultCode, data)
    }

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseCrash.log("Activity created");

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
        inappBillingNoAdv.setOnSubscriptionStateChanged {
            if (it) {
                saveSubscriptionState(true)
            } else {
                saveSubscriptionState(false)
            }
        }
        inappBillingNoAdv.Init(this, this)

        isSubscripted = fetchSubscriptionState(this)

        main_buyButton.setOnClickListener {

            logAsync {

                inappBillingNoAdv.setOnBuyCompleted {
                    if (it) {
                        main_buyButton.visibility = View.GONE;
                    }
                }
                inappBillingNoAdv.Buy()


            }

            true
        }

        if (!isSubscripted) {
            if (doIShowIntro()) {
                val tooltip = Tooltip.Builder(this, main_buyButton)
                        .setText(R.string.subscription_intro)
                        .setPadding(30f)
                        .setCornerRadius(10f)
                        .setTextSize(13f)
                        .setBackgroundColor(Color.rgb(170, 60, 57))
                        .setDismissOnClick(true)
                        .setCancelable(true)
                        .show()

            }

        } else {
            main_buyButton.visibility = View.GONE
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
        inappBillingNoAdv?.destroy()
    }

    //InAppBilling


}
