package ahmetcan.simin

import ahmetcan.simin.Api.Text
import ahmetcan.simin.Api.Track
import ahmetcan.simin.Api.Transcript
import ahmetcan.simin.Api.TranscriptList
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import kotlinx.android.synthetic.main.activity_preview_video.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import android.content.ActivityNotFoundException
import android.graphics.Color
import android.speech.RecognizerIntent
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import android.graphics.PorterDuff
import android.R.attr.checked
import android.content.res.ColorStateList
import android.provider.Settings
import android.widget.*
import android.widget.AbsListView




class PreviewVideo : YouTubeBaseActivity(),  YouTubePlayer.OnInitializedListener , YouTubePlayer.OnFullscreenListener {
    private var fullscreen: Boolean = false
    private var player:YouTubePlayer?=null
    private var listenPlayerJob: Job?=null
    private var videoId:String=""
    private var transcriptList: TranscriptList?=null
    private var defaultLanguge: Track?=null
    private var secondaryLanguge: Track?=null
    var primaryCaptionList: Transcript?=null
    var currentPrimaryText: Text?=null
    private var captionOffMode:Boolean=false
    private var showCaption:Boolean=false
    private var captionIndex=0
    lateinit var listAdapter: ArrayAdapter<String>


    protected val RESULT_SPEECH = 2
    companion object {
        const val  RECOVERY_DIALOG_REQUEST = 1;
    }
    fun listenPlayer(player:YouTubePlayer)= async{
        var currentTime:Int=0
        while(this@PreviewVideo.playerView!=null){
            if(player.isPlaying){
                var playerTime=player.currentTimeMillis
                if(playerTime!=currentTime){
                    currentTime=playerTime
                    onUI {
                        onPlayerTimeChanged(currentTime)
                    }

                }
            }
            delay(5)
        }
    }


    fun onPlayerTimeChanged(time:Int){
        var primaryTextExist=false;
        primaryCaptionList.let {
            it?.texts?.let {
                for((index, value) in it.withIndex()){
                    if(time>=value.start&&time<=value.start+value.duration){
                        primaryTextExist=true
                        if(value?.sentence.toString().compareTo(currentPrimaryText?.sentence.toString())!=0){
                            currentPrimaryText=value
                            captionIndex=index
                            onCaptionChanged()
                        }
                    }

                }
            }
        }
        if(!primaryTextExist)captionPrimary.setText("")
    }
    fun onCaptionChanged(){
        if(currentPrimaryText==null)return
        if(captionOffMode){
            if(showCaption){
                captionPrimary.setText(currentPrimaryText?.sentence?:"")
            }
            else{
                captionPrimary.setText(captionIndex.toString()+" "+getString(R.string.clicktoshow))
            }
            showCaption=false
        }
        else{
            captionPrimary.setText(currentPrimaryText?.sentence?:"")
        }
        primaryCaptionList?.let {
            if(captionIndex+1<it.texts.count())allcaptions.setSelection(captionIndex+1)
        }

    }
    override fun onInitializationSuccess(p0: YouTubePlayer.Provider?, pl: YouTubePlayer?, wasRestored: Boolean) {
        player=pl
        player?.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE)
        player?.setOnFullscreenListener(this)
        player?.let {
            listenPlayerJob=listenPlayer(it)
        }
        if (!wasRestored) {
            player?.loadVideo(videoId as? String);
        }
        player?.setPlaybackEventListener(object :YouTubePlayer.PlaybackEventListener{
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
                        Log.e("AAAAA->>>>>>>>>>>>",text[0])
                        if(text.count()>0) {
                            speakMatch.setText(text[0])
                        }
                        speakMatch.visibility=View.VISIBLE

                    }


                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoId=intent.extras["videoid"] as String

        setContentView(R.layout.activity_preview_video)



        playerView.initialize(ApiKey.YOUTUBEDATAAPIV3_KEY, this);

        video_SpeechTest.setOnClickListener {
            speakMatch.setText("")
            val intent = Intent( RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, defaultLanguge.toString())

            try {
                startActivityForResult(intent, RESULT_SPEECH)

            } catch (a: ActivityNotFoundException) {
                val t = Toast.makeText(applicationContext,getString(R.string.speakerror),Toast.LENGTH_SHORT)
                t.show()
            }

        }
        backButton.setOnClickListener {
            finish()
        }
        MobileAds.initialize(this, ApiKey.ADMOB_APPID);
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        captionPrimary.setOnClickListener {
            currentPrimaryText?.let {
                player?.seekToMillis(it.start.toInt())
//                if(captionOffMode){
//                    showCaption=true
//                }
                onCaptionChanged();
            }

        }
        captionPrimary.setOnLongClickListener {
            currentPrimaryText?.let {
                player?.seekToMillis(it.start.toInt())
                if(captionOffMode){
                    showCaption=true
                }
                onCaptionChanged();
            }

            true
        }
        if(captionOffMode){
            captionPrimary.setText(getString(R.string.clicktoshow))
            showCaption=false
        }
        speakMatch.setOnClickListener {
            speakMatch.visibility=View.GONE
        }
        video_SkipPrevious.setOnClickListener {
            if(captionIndex>0){
                captionIndex--
                var prev=primaryCaptionList!!.texts[captionIndex]
                player?.seekToMillis(prev.start.toInt())
                onCaptionChanged()
            }
        }
        video_skipNext.setOnClickListener {
            if(captionIndex<primaryCaptionList!!.texts.count()){
                captionIndex++
                var next=primaryCaptionList!!.texts[captionIndex]
                player?.seekToMillis(next.start.toInt())
                onCaptionChanged()

            }
        }
        video_playButton.setOnClickListener {
            player?.let {
                if(it.isPlaying){
                    it.pause()
                }
                else {
                    it.play()
                }
            }
        }

        video_hardmodeButton.setOnCheckedChangeListener(object :CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(button: CompoundButton?, state: Boolean) {
                if(state){
                    video_hardmodeButtonText?.setTextColor(Color.RED)
                    captionOffMode=true
                }
                else{
                    video_hardmodeButtonText?.setTextColor(Color.BLACK)
                    captionOffMode=false
                }
                fillCaptionList()
            }
        })
        video_hardmodeButtonText.setOnClickListener {
            video_hardmodeButton.toggle()
        }
        if(captionOffMode)video_hardmodeButton.toggle()

        listAdapter =ArrayAdapter<String>(this, R.layout.transcript_listitem, arrayListOf(""))
        allcaptions.adapter=listAdapter
        allcaptions.setOnItemClickListener(object :AdapterView.OnItemClickListener{
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                var next=primaryCaptionList!!.texts[p2]
                player?.seekToMillis(next.start.toInt())
                onCaptionChanged()
            }

        })


        async {
            loadCaption()
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
    }
    fun fillCaptionList(){
        listAdapter.clear()
        for ((index,item) in primaryCaptionList!!.texts.withIndex()){
            if(captionOffMode){
                listAdapter.add(index.toString())
            }
            else{
                listAdapter.add(item.sentence)
            }

        }

    }

    fun loadCaption() {
        onUI { progressBar1.visibility=View.VISIBLE }
        transcriptList = DiscoveryRepository.captionList(videoId)
        onUI{ progressBar1.visibility=View.GONE }
        if (transcriptList == null) {
            TODO("load fail hatası verilecek")
            return;
        }
        if (transcriptList?.tracks == null) {
            TODO("CAPTİON NOT FOUND UYARISI VERİLECEK")
            return;
        }

        for (item in transcriptList?.tracks!!){
            if (item.langDefault == "true") {
                defaultLanguge=item;
                break;
            }
        }
        primaryCaptionList =DiscoveryRepository.caption(videoId,defaultLanguge!!.langCode,"")
        onUI {
            fillCaptionList()
        }

    }
}
