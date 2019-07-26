package app.dphone

import android.content.Intent
import android.os.Handler
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray


class MainActivity : BaseActivity(), OnUpdatedHistoryListener {

    override fun getContentLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun onCreateContent(content: View) {
        MyInstanceMessagingService.createChannel(applicationContext)
        if (blockstack().isUserSignedIn()) {
			loadAdapter()
			getContacts { contacts ->
                updateContacts(contacts)
			}
			getCallHistory { result ->
                updateCallHistory(result)
			}
            Handler().postDelayed({
                            setHubRegistration(loggedUsername())
                    }, 1000)
        }
    }

    private fun updateContacts(contacts: JSONArray?){
        var notNullContacts:JSONArray = contacts?:JSONArray()

        (pager.adapter as (TabPagerAdapter)).updateContacts(notNullContacts)
        Intent().also { intent ->
            intent.action = "android.intent.action.updateContacts"
            intent.putExtra("contacts", notNullContacts.toString())
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    private fun updateCallHistory(callHistory:JSONArray?){
        var notNullCallHistory:JSONArray = callHistory?:JSONArray()
        (pager.adapter as (TabPagerAdapter)).updateCallHistory(notNullCallHistory)
        Intent().also { intent ->
            intent.action = "android.intent.action.updateCallHistory"
            intent.putExtra("callHistory", notNullCallHistory.toString())
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    override fun saveUpdatedCallHistory(callHistory:JSONArray?, callback: (String?) -> Unit){
        saveCallHistory(Util.ConvertJSONArrayToArrayOfJSONObject(callHistory), callback)
    }

    override fun checkLoggedUser() {
    }

    override fun onResume() {
        super.onResume()
        super.checkLoggedUser()
    }

	private fun loadAdapter(){
        val fragmentAdapter = TabPagerAdapter(supportFragmentManager, this.baseContext, loggedUsername(), this)
        pager.adapter = fragmentAdapter
        tabs_main.setupWithViewPager(pager)
    }
}

interface OnUpdatedHistoryListener {
    fun saveUpdatedCallHistory(callHistory:JSONArray?, callback: (String?) -> Unit)
}
