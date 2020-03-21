package ahmetcan.simin

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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.activity_preview_video.*
import kotlinx.android.synthetic.main.activity_preview_video.allcaptions
import kotlinx.android.synthetic.main.activity_preview_video.backButton
import kotlinx.android.synthetic.main.activity_preview_video.captionPrimary
import kotlinx.android.synthetic.main.activity_preview_video.captionSecondary
import kotlinx.android.synthetic.main.activity_preview_video.info
import kotlinx.android.synthetic.main.activity_preview_video.progressBar1
import kotlinx.android.synthetic.main.activity_preview_video.speakMatch
import kotlinx.android.synthetic.main.activity_preview_video.video_FavoryButton
import kotlinx.android.synthetic.main.activity_preview_video.video_SkipPrevious
import kotlinx.android.synthetic.main.activity_preview_video.video_SpeechTest
import kotlinx.android.synthetic.main.activity_preview_video.video_hardmodeButton
import kotlinx.android.synthetic.main.activity_preview_video.video_hardmodeButtonText
import kotlinx.android.synthetic.main.activity_preview_video.video_playButton
import kotlinx.android.synthetic.main.activity_preview_video.video_secondSubTitleShow
import kotlinx.android.synthetic.main.activity_preview_video.video_skipNext
import kotlinx.android.synthetic.main.activity_preview_video.video_subTitleSync
import kotlinx.android.synthetic.main.activity_preview_video.video_translateButton
import kotlinx.android.synthetic.main.activity_preview_video_web.*
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit


class PreviewVideoWeb : ActivityBase() {
    var isPlaying: Boolean = false
    private var state: VideoViewState? = null
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

    var scope = MainScope() + CoroutineExceptionHandler { _, ex ->
        FirebaseCrashlytics.getInstance().recordException(ex)
        Log.e("ahmetcan", "PreviewVideoWeb main scope exception [blocked]", ex)
    }

    companion object {
        const val RECOVERY_DIALOG_REQUEST = 1;
    }

    fun fetchSubscriptionState(): Boolean {
        val subscription = getSharedPreferences("subscription", Context.MODE_PRIVATE)
        val has: Boolean = subscription.getBoolean("has", false)
        return has;
    }


    fun onPlayerTimeChanged(second: Double) {
        var primaryTextExist = false
        var secondaryTextExist = false
        primaryCaptionList.let {
            it?.texts?.let {
                for ((index, value) in it.withIndex()) {
                    if (second >= value.start && second <= value.start + value.duration) {
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
                        if (second >= value.start && second <= value.start + value.duration) {
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

    fun InitPlayer() {
        video_playButton.setImageResource(R.drawable.ic_play_arrow_black_48dp)

        player.loadFromHtml()
        player.Callbacks.apply {
            timeChanged = {
                scope.launch(Dispatchers.Main) {
                    onPlayerTimeChanged(it)
                }
            }
            playing = {
                isPlaying = true
                scope.launch(Dispatchers.Main) {
                    video_playButton.setImageResource(R.drawable.ic_pause_black_48dp)
                }
            }
            paused = {
                isPlaying = false
                scope.launch(Dispatchers.Main) {
                    video_playButton.setImageResource(R.drawable.ic_play_arrow_black_48dp)
                }
            }
            ready = {
                scope.launch(Dispatchers.Main) {
                    player.loadVideo(videoId)
                }
            }
            unstarted={
                Log.w("ahmetcan","unstarted")

            }
        }
        scope.launch {
            loadCaption()
            state = DiscoveryRepository.getVideoViewState(videoId)
            state?.let {
                if (captionOffMode) video_hardmodeButton.callOnClick()
                var secondaryIso = it.secondaryLanguageIso
                if (!secondaryIso.isNullOrEmpty()) {
                    secondaryLanguge = languages.filter { it.isoCode == secondaryIso }.firstOrNull()
                    video_secondSubTitleShow.visibility = View.VISIBLE
                }

                loadSecondaryCaption()


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

    override fun onResume() {
        super.onResume()
        var fvideoId=""
        if (intent.action == Intent.ACTION_SEND) {

            var url = Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT))
            var youtubeurl = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (youtubeurl.contains("youtu.be", true)) {
                fvideoId = url.path.replace("/", "")
                title = ""
                description = ""
                cover = ""
            }

        } else {
            fvideoId = intent.extras["videoid"] as String
            title = intent.extras["title"] as String
            description = intent.extras["description"] as String
            cover = intent.extras["cover"] as String

        }
       if(fetchSubscriptionState()==false){
                  return
       }
        if(videoId!=fvideoId){
            videoId=fvideoId
            InitPlayer()
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            RECOVERY_DIALOG_REQUEST -> {
                // Retry initialization if user performed a recovery action
                InitPlayer()
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
                        scope.launch(Dispatchers.Main) {
                            if (showSecondSubtitle) {
                                video_secondSubTitleShow.callOnClick()
                            }

                        }
                    } else {
                        captionSecondary.visibility = View.VISIBLE
                        video_secondSubTitleShow.visibility = View.VISIBLE
                        var translateiso = it.extras["iso"]
                        secondaryLanguge = languages.filter { it.isoCode == translateiso }.firstOrNull()
                        if (!showSecondSubtitle) {
                            video_secondSubTitleShow.callOnClick()
                        }
                       scope.launch {
                           loadSecondaryCaption()
                       }
                    }

                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        setContentView(R.layout.activity_preview_video_web)



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





        captionPrimary.setOnClickListener {
            currentPrimaryText?.let {
                player?.seekTo(it.start)
                if (captionOffMode) {
                    showCaption = 3
                }
                onCaptionChanged();
            }

        }

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
                        player?.seekTo(prev.start)
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
                        player?.seekTo(next.start)
                        //  onCaptionChanged()

                    }
                }

            } catch (ex: Exception) {
                Log.w("Simin", ex.toString())
            }

        }
        video_playButton.setOnClickListener {
            try {
                if (isPlaying) {
                    player?.pause()
                } else {
                    player?.play()
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
                    player?.seekTo(next.start)
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




    override fun onDestroy() {
        super.onDestroy()
        statePersist()
        scope.cancel("ondestroy")

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

    suspend fun loadCaption() {

        progressBar1.visibility = View.VISIBLE

        languages = withContext(Dispatchers.IO){allLanguageges(videoId)}
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


        primaryCaptionList = withContext(Dispatchers.IO) { DiscoveryRepository.caption(videoId, defaultLanguge.isoCode, "") }
        fillCaptionList()
        progressBar1.visibility = View.GONE
        if (captionIndex > 0) {
            primaryCaptionList?.let {
                var seekToCption = it.texts[captionIndex]
                player?.seekTo(seekToCption.start)
            }

        }


    }

    suspend fun loadSecondaryCaption() {
        progressBar1.visibility = View.VISIBLE
        secondaryLanguge?.let {
            if (it.available) {
                secondaryCaptionList = withContext(Dispatchers.IO) { DiscoveryRepository.caption(videoId, it.isoCode, "") }
            } else {
                secondaryCaptionList = withContext(Dispatchers.IO) { DiscoveryRepository.caption(videoId, defaultLanguge.isoCode, it.isoCode) }
            }

        }
        progressBar1.visibility = View.GONE
    }


}
