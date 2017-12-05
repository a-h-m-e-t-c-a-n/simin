package ahmetcan.simin

import android.app.Application
import android.util.Log
import com.facebook.drawee.backends.pipeline.Fresco
import io.realm.Realm
import io.realm.RealmConfiguration


open class ACApplication : Application() {

//    private var sTracker: Tracker? = null

    init {
        instance=this

    }

    override fun onCreate() {
        super.onCreate()
        Fresco.initialize(this);

        Realm.init(this);
        val config = RealmConfiguration.Builder()
                .name("ahmetcanreal")
                .deleteRealmIfMigrationNeeded()
                .build()
        Realm.setDefaultConfiguration(config)


//        Thread.setDefaultUncaughtExceptionHandler(object: Thread.UncaughtExceptionHandler{
//            override fun uncaughtException(p0: Thread?, p1: Throwable?) {
//                Log.e("Simin App Uncaught:",p1.toString())
//            }
//
//        })
    }

//    @Synchronized
//    fun getDefaultTracker(): Tracker {
//        // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
//        if (sTracker == null) {
//            sTracker = sAnalytics?.newTracker(R.xml.global_tracker)
//        }
//
//        return sTracker
//    }
    companion object {
        lateinit var instance:ACApplication
    }
}