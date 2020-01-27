package ahmetcan.simin

import ahmetcan.simin.Discovery.Model.persistent.YoutubeSubscriptionResult
import android.app.Application
import android.util.Log
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.realm.Realm
import io.realm.RealmConfiguration


open class ACApplication : Application() {

    init {
        instance=this

    }

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        Fresco.initialize(this);

        Realm.init(this);
        val config = RealmConfiguration.Builder()
                .name("ahmetcan3.realm")
                .schemaVersion(1)
                .deleteRealmIfMigrationNeeded()
                .build()
        Realm.setDefaultConfiguration(config)

        FirebaseAnalytics.getInstance(this).setUserId(DeviceId(this).getId())
        FirebaseAnalytics.getInstance(this).setUserProperty("ACUserId",DeviceId(this).getId())
        FirebaseCrashlytics.getInstance().setUserId(DeviceId(this).getId());

    }


    companion object {
        lateinit var instance:ACApplication
    }
}