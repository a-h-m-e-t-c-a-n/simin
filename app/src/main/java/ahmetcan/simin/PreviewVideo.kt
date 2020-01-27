package ahmetcan.simin

import ahmetcan.echo.ACPremium
import ahmetcan.simin.Api.Text
import ahmetcan.simin.Api.Transcript
import ahmetcan.simin.Discovery.Model.persistent.Language
import ahmetcan.simin.Discovery.Model.persistent.VideoViewState
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import ahmetcan.simin.Discovery.Real.DiscoveryRepository.allLanguageges
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.core.graphics.drawable.DrawableCompat
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_preview_video.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlin.random.Random
import android.util.StatsLog.logEvent
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.tooltip.Tooltip
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.TimeUnit


class PreviewVideo : YouTubeBaseActivity(), YouTubePlayer.OnInitializedListener, YouTubePlayer.OnFullscreenListener {
    var billing: ACPremium?=null

    fun saveSubscriptionState(has: Boolean) {
        val subscription = getSharedPreferences("subscription", Context.MODE_PRIVATE)
        val edit = subscription.edit()
        edit.putBoolean("has", has)
        edit.commit()
    }

    private var state: VideoViewState? = null
    private var fullscreen: Boolean = false
    private var player: YouTubePlayer? = null
    private var listenPlayerJob: Job? = null
    private var videoId: String = ""
    private lateinit var languages: List<Language>
    private var defaultLanguge: Language = Language()
    private var secondaryLanguge: Language? = null
    var primaryCaptionList: Transcript? = null
    var secondaryCaptionList: Transcript? = null
    var currentPrimaryText: Text? = null
    var currentSecondaryText: Text? = null
    private var captionOffMode: Boolean = false
    private var showCaption: Int = 0
    private var captionIndex = 0
    lateinit var listAdapter: ArrayAdapter<String>
    private var showSecondSubtitle: Boolean = false
    private var syncSubtitle: Boolean = false
    private var title: String = ""
    private var description = ""
    private var cover = ""

    protected val RESULT_SPEECH = 2
    protected val RESULT_TRANSLATE = 3

    companion object {
        const val RECOVERY_DIALOG_REQUEST = 1;
    }

    fun fetchSubscriptionState(): Boolean {
        val subscription = getSharedPreferences("subscription", Context.MODE_PRIVATE)
        val has: Boolean = subscription.getBoolean("has", false)
        return has;
    }

    fun listenPlayer(player: YouTubePlayer) = async {
        var currentTime: Int = 0
        while (this@PreviewVideo.playerView != null) {
            if (player.isPlaying) {
                var playerTime = player.currentTimeMillis
                if (playerTime != currentTime) {
                    currentTime = playerTime
                    onUI {
                        onPlayerTimeChanged(currentTime)
                    }

                }
            }
            delay(5)
        }
    }


    fun onPlayerTimeChanged(time: Int) {
        var primaryTextExist = false
        var secondaryTextExist = false
        primaryCaptionList.let {
            it?.texts?.let {
                for ((index, value) in it.withIndex()) {
                    if (time >= value.start && time <= value.start + value.duration) {
                        primaryTextExist = true
                        if (value?.sentence.toString().compareTo(currentPrimaryText?.sentence.toString()) != 0) {
                            currentPrimaryText = value
                            captionIndex = index
                            onCaptionChanged()
                            break;
                        }
                    }
                }
            }

            secondaryCaptionList.let {
                it?.texts?.let {
                    for ((index, value) in it.withIndex()) {
                        if (time >= value.start && time <= value.start + value.duration) {
                            secondaryTextExist = true
                            if (value?.sentence.toString().compareTo(currentSecondaryText?.sentence.toString()) != 0) {
                                currentSecondaryText = value
                                onSecondaryCaptionChanged()
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    fun onCaptionChanged() {
        if (currentPrimaryText == null) return
        if (captionOffMode) {
            if (showCaption > 0) {
                captionPrimary.setText(currentPrimaryText?.sentence ?: "")
                showCaption--;

            } else {
                captionPrimary.setText(captionIndex.toString() + " " + getString(R.string.clicktoshow))
            }

        } else {
            captionPrimary.setText(currentPrimaryText?.sentence ?: "")
        }
        if (syncSubtitle) {
            primaryCaptionList?.let {
                if (captionIndex + 1 < it.texts.count()) allcaptions.setSelection(captionIndex + 1)
            }
        }
    }

    fun onSecondaryCaptionChanged() {
        if (currentSecondaryText == null) return
        captionSecondary.setText(currentSecondaryText?.sentence ?: "")
    }

    override fun onInitializationSuccess(p0: YouTubePlayer.Provider?, pl: YouTubePlayer?, wasRestored: Boolean) {
        player = pl
        player?.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE)
        player?.setOnFullscreenListener(this)
        player?.let {
            listenPlayerJob = listenPlayer(it)
        }
        if (!wasRestored) {
            player?.loadVideo(videoId as? String);
        }
        player?.setPlaybackEventListener(object : YouTubePlayer.PlaybackEventListener {
            override fun onSeekTo(p0: Int) {
            }

            override fun onBuffering(p0: Boolean) {
            }

            override fun onPlaying() {
                video_playButton.setImageResource(R.drawable.ic_pause_black_48dp)
            }

            override fun onStopped() {
            }

            override fun onPaused() {
                video_playButton.setImageResource(R.drawable.ic_play_arrow_black_48dp)
            }

        })
        //startActivity(Intent(Settings.ACTION_CAPTIONING_SETTINGS));

    }


    override fun onInitializationFailure(provider: YouTubePlayer.Provider,
                                         errorReason: YouTubeInitializationResult) {
        if (errorReason.isUserRecoverableError) {
            errorReason.getErrorDialog(this, RECOVERY_DIALOG_REQUEST).show()
        } else {
            val errorMessage = String.format(getString(R.string.error_player), errorReason.toString())
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            RECOVERY_DIALOG_REQUEST -> {
                // Retry initialization if user performed a recovery action
                playerView.initialize(ApiKey.YOUTUBEDATAAPIV3_KEY, this)
            }
            RESULT_SPEECH -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.let {
                        val text = it.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        if (text.count() > 0) {
                            speakMatch.setText(text[0])
                        }
                        speakMatch.visibility = View.VISIBLE
                    }
                }
            }
            RESULT_TRANSLATE -> {
                data?.let {
                    if (resultCode == 2) {
                        secondaryLanguge = null
                        captionSecondary.visibility = View.GONE
                        video_secondSubTitleShow.visibility = View.GONE
                        onUI {
                            if (showSecondSubtitle) {
                                video_secondSubTitleShow.callOnClick()
                            }

                        }
                    } else {
                        captionSecondary.visibility = View.VISIBLE
                        video_secondSubTitleShow.visibility = View.VISIBLE
                        var translateiso = it.extras["iso"]
                        secondaryLanguge = languages.filter { it.isoCode == translateiso }.firstOrNull()
                        onUI {
                            if (!showSecondSubtitle) {
                                video_secondSubTitleShow.callOnClick()
                            }
                        }
                        logAsync {
                            loadSecondaryCaption()
                        }
                    }

                }
            }
        }
    }

    fun doIShowIntro(): Boolean {
        val subscription = getSharedPreferences("subscription_preview", Context.MODE_PRIVATE)
        var introTimeMs = subscription.getLong("introtime", 0)

        if (introTimeMs > 0) {
            var asDay = TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().timeInMillis - introTimeMs)
            if (asDay <= 5) {
                return false;
            }
        }

        val edit = subscription.edit()
        edit.putLong("introtime", Calendar.getInstance().timeInMillis)
        edit.commit()
        return true

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        if (intent.action == Intent.ACTION_SEND) {

            var url = Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT))
            var youtubeurl = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (youtubeurl.contains("youtu.be", true)) {
                videoId = url.path.replace("/", "")
                title = ""
                description = ""
                cover = ""
            }

        } else {
            videoId = intent.extras["videoid"] as String
            title = intent.extras["title"] as String
            description = intent.extras["description"] as String
            cover = intent.extras["cover"] as String

        }

        setContentView(R.layout.activity_preview_video)


        playerView.initialize(ApiKey.YOUTUBEDATAAPIV3_KEY, this);

        video_SpeechTest.setOnClickListener {

            try {
                speakMatch.setText("")
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

                if (defaultLanguge.isoCode.isNotEmpty()) {
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, defaultLanguge.isoCode)
                    startActivityForResult(intent, RESULT_SPEECH)
                }


            } catch (a: ActivityNotFoundException) {
                val t = Toast.makeText(applicationContext, getString(R.string.speakerror), Toast.LENGTH_SHORT)
                t.show()
            }

        }
        backButton.setOnClickListener {
            finish()
        }
        buyButton.setOnClickListener{
            try{
                FirebaseAnalytics.getInstance(this).logEvent("PreviewBuyEvent",null)

            }catch (ex:Exception){}


            billing?.let { it.buyPremium()  }
        }


        if (!fetchSubscriptionState()) {
            buyButton.visibility=View.VISIBLE
//            if (doIShowIntro()) {
//                try {
//                    val tooltip = Tooltip.Builder(this@PreviewVideo, buyButton)
//                            .setText(R.string.subscription_intro)
//                            .setPadding(30f)
//                            .setCornerRadius(10f)
//                            .setTextSize(13f)
//                            .setBackgroundColor(Color.rgb(170, 60, 57))
//                            .setDismissOnClick(true)
//                            .setCancelable(true)
//                            .show()
//                } catch (ex: Exception) {
//
//                }
//            }




                billing = ACPremium(this, object : ACPremium.IState {
                    override fun onError() {
                        onUI {
                            saveSubscriptionState(true)
                          //  adsView.visibility = View.GONE

                            try {
                                FirebaseAnalytics.getInstance(this@PreviewVideo).logEvent("PreviewBillingError", null)

                            } catch (ex: Exception) {


                            }
                        }
                    }

                    override fun onUserCancelFlow() {
                        try {
                            FirebaseAnalytics.getInstance(this@PreviewVideo).logEvent("PreivewBillingUserCancel", null)

                        } catch (ex: Exception) {
                        }
                    }

                    override fun onPremiumChanged(isPremium: Boolean) {
                        onUI {
                            if (isPremium) {
                                saveSubscriptionState(true)
                                adsView.visibility = View.GONE
                            } else {
                                saveSubscriptionState(false)
                            }
                        }

                    }

                })


                adsView.visibility = View.VISIBLE
                MobileAds.initialize(this, ApiKey.ADMOB_APPID);
                val adRequest = AdRequest.Builder()
                adRequest.addTestDevice("0CCBF425EA2828FA093D1115E3C8A3F2")
                adView.setAdListener(object : AdListener() {
                    override fun onAdLoaded() {
                        // Code to be executed when an ad finishes loading.
                    }

                    override fun onAdFailedToLoad(errorCode: Int) {
                        // Code to be executed when an ad request fails.
                    }

                    override fun onAdOpened() {
                        // Code to be executed when an ad opens an overlay that
                        // covers the screen.
                    }

                    override fun onAdLeftApplication() {
                        // Code to be executed when the user has left the app.
                    }

                    override fun onAdClosed() {
                        // Code to be executed when when the user is about to return
                        // to the app after tapping on an ad.
                    }
                })
                adView.loadAd(adRequest.build())
            }



        captionPrimary.setOnClickListener {
            currentPrimaryText?.let {
                player?.seekToMillis(it.start.toInt())
                if (captionOffMode) {
                    showCaption = 3
                }
                onCaptionChanged();
            }

        }
//        captionPrimary.setOnLongClickListener {
//            currentPrimaryText?.let {
//                player?.seekToMillis(it.start.toInt())
//                if(captionOffMode){
//                    showCaption=true
//                }
//                onCaptionChanged();
//            }
//
//            true
//        }
        if (captionOffMode) {
            captionPrimary.setText(getString(R.string.clicktoshow))
            showCaption = 0
        }
        speakMatch.setOnClickListener {
            speakMatch.visibility = View.GONE
        }
        video_SkipPrevious.setOnClickListener {
            try {
                if (captionIndex > 0) {
                    captionIndex--
                    primaryCaptionList?.let {
                        var prev = it.texts[captionIndex]
                        player?.seekToMillis(prev.start.toInt())
                    }

                    // onCaptionChanged()
                }
            } catch (ex: Exception) {
                Log.w("Simin", ex.toString())
            }
        }
        video_skipNext.setOnClickListener {
            try {
                primaryCaptionList?.let {
                    if (captionIndex < it.texts.count()) {
                        captionIndex++
                        var next = it.texts[captionIndex]
                        player?.seekToMillis(next.start.toInt())
                        //  onCaptionChanged()

                    }
                }

            } catch (ex: Exception) {
                Log.w("Simin", ex.toString())
            }

        }
        video_playButton.setOnClickListener {
            try {
                player?.let {
                    if (it.isPlaying) {
                        it.pause()
                    } else {
                        it.play()
                    }
                }
            } catch (ex: Throwable) {

                Log.e("AHMETCAN error ABSORBE", ex.toString())
            }
        }
        DrawableCompat.setTint(video_hardmodeButton.background, Color.BLACK)


        video_hardmodeButton.setOnClickListener {
            try {
                if (captionOffMode) {
                    video_hardmodeButtonText?.setTextColor(Color.BLACK)
                    DrawableCompat.setTint(video_hardmodeButton.background, Color.BLACK)
                    //video_hardmodeButton.setColorFilter(Color.BLACK,PorterDuff.Mode.MULTIPLY)
                    captionOffMode = false
                } else {
                    video_hardmodeButtonText?.setTextColor(Color.RED)
                    DrawableCompat.setTint(video_hardmodeButton.background, Color.RED)
//                    video_hardmodeButton.setColorFilter(Color.RED,PorterDuff.Mode.MULTIPLY)
                    captionOffMode = true
                }

                fillCaptionList()
            } catch (ex: Throwable) {

                Log.e("AHMETCAN error ABSORBE", ex.toString())
            }
        }
        video_FavoryButton.setOnClickListener {
            try {
                if (state == null) {
                    video_FavoryButton.setColorFilter(Color.RED)
                    //DrawableCompat.setTint(video_FavoryButton.background,Color.RED)
                    state = VideoViewState()
                } else {
                    video_FavoryButton.setColorFilter(Color.BLACK)

                    //DrawableCompat.setTint(video_FavoryButton.background,Color.BLACK)
                    state = null;
                }
                statePersist()
            } catch (ex: Exception) {

            }

        }

        video_hardmodeButtonText.setOnClickListener {
            video_hardmodeButton.callOnClick()
        }
        if (captionOffMode) video_hardmodeButton.callOnClick()

        listAdapter = ArrayAdapter<String>(this, R.layout.transcript_listitem, arrayListOf(""))
        allcaptions.adapter = listAdapter
        allcaptions.setOnItemClickListener(object : AdapterView.OnItemClickListener {
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                primaryCaptionList?.let {
                    var next = it.texts[p2]
                    player?.seekToMillis(next.start.toInt())
                    onCaptionChanged()
                }

            }

        })
        video_translateButton.setOnClickListener {
            var intent = Intent(applicationContext, LanguageActivity::class.java)
            intent.putExtra("videoid", videoId)
            startActivityForResult(intent, RESULT_TRANSLATE)
        }
        video_secondSubTitleShow.setOnClickListener {
            if (showSecondSubtitle) {
                showSecondSubtitle = false
                video_secondSubTitleShow.setColorFilter(Color.BLACK)
                captionSecondary.visibility = View.GONE
            } else {
                if (secondaryLanguge != null) {
                    showSecondSubtitle = true
                    video_secondSubTitleShow.setColorFilter(Color.RED)
                    captionSecondary.visibility = View.VISIBLE

                }
            }
        }
        video_subTitleSync.setOnClickListener {
            if (syncSubtitle) {
                syncSubtitle = false
                video_subTitleSync.setColorFilter(Color.BLACK)
            } else {
                syncSubtitle = true
                video_subTitleSync.setColorFilter(Color.RED)
            }
        }



        async(Dispatchers.IO) {
            loadCaption()

            onUI {
                state = DiscoveryRepository.getVideoViewState(videoId)
                state?.let {
                    if (captionOffMode) video_hardmodeButton.callOnClick()
                    var secondaryIso = it.secondaryLanguageIso
                    if (!secondaryIso.isNullOrEmpty()) {
                        secondaryLanguge = languages.filter { it.isoCode == secondaryIso }.firstOrNull()
                        video_secondSubTitleShow.visibility = View.VISIBLE
                    }
                    logAsync { loadSecondaryCaption() }

                    if (it.showCaption) video_secondSubTitleShow.callOnClick()
                    if (it.syncSubtitle) video_subTitleSync.callOnClick()
                    if (it.showSecondSubtitle) video_secondSubTitleShow.callOnClick()
                }
                if (state != null) {
                    video_FavoryButton.setColorFilter(Color.RED)
                } else if (syncSubtitle == false) {
                    video_subTitleSync.callOnClick()
                }
            }

        }
    }

    fun statePersist() {
        if (state == null) {
            DiscoveryRepository.deleteVideoState(videoId)
        } else {
            state?.videoId = videoId
            state?.captionOffMode = captionOffMode
            state?.captionIndex = captionIndex
            state?.secondaryLanguageIso = secondaryLanguge?.isoCode ?: ""
            //state?.showCaption=showCaption
            state?.syncSubtitle = syncSubtitle
            state?.showSecondSubtitle = showSecondSubtitle
            state?.title = title
            state?.description = description
            state?.cover = cover

            state?.let { DiscoveryRepository.persistVideoState(it) }
        }
    }

    private fun doLayout() {
        val playerParams = playerView.getLayoutParams() as LinearLayout.LayoutParams
        if (fullscreen) {
//            // When in fullscreen, the visibility of all other views than the player should be set to
//            // GONE and the player should be laid out across the whole screen.
//            playerParams.width = ViewGroup.LayoutParams.MATCH_PARENT
//            playerParams.height = ViewGroup.LayoutParams.MATCH_PARENT

            info.setVisibility(View.GONE)
        } else {
            // This layout is up to you - this is just a simple example (vertically stacked boxes in
            // portrait, horizontally stacked in landscape).
            info.setVisibility(View.VISIBLE)
//            val otherViewsParams = info.getLayoutParams()
//            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                otherViewsParams.width = 0
//                playerParams.width = otherViewsParams.width
//                playerParams.height = WRAP_CONTENT
//                otherViewsParams.height = MATCH_PARENT
//                playerParams.weight = 1f
//                baseLayout.setOrientation(LinearLayout.HORIZONTAL)
//            } else {
//                otherViewsParams.width = MATCH_PARENT
//                playerParams.width = otherViewsParams.width
//                playerParams.height = WRAP_CONTENT
//                playerParams.weight = 0f
//                otherViewsParams.height = 0
//                baseLayout.setOrientation(LinearLayout.VERTICAL)
//            }
        }


    }

    override fun onFullscreen(isFullscreen: Boolean) {
        fullscreen = isFullscreen;
        doLayout();
    }

    override fun onDestroy() {
        super.onDestroy()
        //  listenPlayerJob?.cancel()
        statePersist()

    }

    fun fillCaptionList() {
        primaryCaptionList?.let {
            listAdapter.clear()
            for ((index, item) in it.texts.withIndex()) {
                if (captionOffMode) {
                    listAdapter.add(index.toString())
                } else {
                    listAdapter.add(item.sentence)
                }

            }

        }

    }

    fun loadCaption() {

        onUI { progressBar1.visibility = View.VISIBLE }

        languages = allLanguageges(videoId)
        if (languages.count() == 0) {
            return;
        }
        var default = languages.filter { it.default == true }.firstOrNull()
        if (default == null) {
            default = languages.filter { it.available == true && it.isoCode == "en" }.firstOrNull()
            if (default == null) {
                languages.filter { it.available == true }.firstOrNull()?.let {
                    defaultLanguge = it
                }

            }
        } else {
            defaultLanguge = default
        }


        primaryCaptionList = DiscoveryRepository.caption(videoId, defaultLanguge.isoCode, "")
        onUI {
            fillCaptionList()
            progressBar1.visibility = View.GONE
            if (captionIndex > 0) {
                primaryCaptionList?.let {
                    var seekToCption = it.texts[captionIndex]
                    player?.seekToMillis(seekToCption.start.toInt())
                }

            }
        }


    }

    fun loadSecondaryCaption() {
        onUI { progressBar1.visibility = View.VISIBLE }
        secondaryLanguge?.let {
            if (it.available) {
                secondaryCaptionList = DiscoveryRepository.caption(videoId, it.isoCode, "")
            } else {
                secondaryCaptionList = DiscoveryRepository.caption(videoId, defaultLanguge.isoCode, it.isoCode)
            }

        }


        onUI { progressBar1.visibility = View.GONE }
    }

}
