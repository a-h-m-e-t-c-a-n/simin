package ahmetcan.simin

import ahmetcan.simin.Discovery.Model.persistent.Language
import ahmetcan.simin.Discovery.Real.DiscoveryRepository
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_language.*

class LanguageActivity : AppCompatActivity() {
    lateinit var listAvailableAdapter: ArrayAdapter<String>
    lateinit var listRestAdapter: ArrayAdapter<String>
    var availables: List<Language>?=null
    var rest:List<Language>?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)
        //setSupportActionBar(toolbar)

        listAvailableAdapter =ArrayAdapter<String>(this, R.layout.language_listitem, arrayListOf(""))
        availableList.adapter=listAvailableAdapter
        availableList.setOnItemClickListener(object : AdapterView.OnItemClickListener{
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                availables?.let {
                    if(p2==0){
                        var resultIntent=Intent()
                        setResult(2,resultIntent)
                        finish()
                        return
                    }
                    var item=it[p2-1]
                    var resultIntent=Intent()
                    resultIntent.putExtra("iso",item.isoCode)
                    setResult(1,resultIntent)
                    finish()
                }
            }

        })
        listRestAdapter =ArrayAdapter<String>(this, R.layout.language_listitem, arrayListOf(""))
        restList.adapter=listRestAdapter
        restList.setOnItemClickListener(object : AdapterView.OnItemClickListener{
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                rest?.let {
                    var item = it[p2]
                    var resultIntent = Intent()
                    resultIntent.putExtra("iso", item.isoCode)
                    setResult(1,resultIntent)
                    finish()
                }
            }

        })
        backButton.setOnClickListener {
            setResult(0)
            finish()
        }
        loadIso()

    }
    fun loadIso()= logAsync{
        var videoId=intent.extras["videoid"] as String
        var languages=DiscoveryRepository.allLanguageges(videoId).sortedBy { it.DisplayName }
        availables=languages.filter { it.available==true }.toList();//.map { it.DisplayName }
        rest=languages.filter { it.available==false };//.map { it.DisplayName }
        onUI {
            listAvailableAdapter.clear()
            listRestAdapter.clear()
            listAvailableAdapter.add(getString(R.string.none))
            listAvailableAdapter.addAll(availables?.map { it.DisplayName })
            listRestAdapter.addAll(rest?.map { it.DisplayName })
        }

    }

}
