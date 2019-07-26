package app.dphone

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.json.JSONArray


class TabPagerAdapter(private val fm: FragmentManager, private val mainContext: Context, private val loggeduser: String, private val listener: OnUpdatedHistoryListener) : FragmentPagerAdapter(fm) {
    private var callHistory: JSONArray? = null
    private var contacts:JSONArray? = null

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                ListContactFragment.newInstance(loggeduser,contacts)
            }
            else -> {
                ListHistoryFragment.newInstance(callHistory, contacts, listener)
            }
        }
    }

    fun updateContacts(contacts: JSONArray){
        this.contacts = contacts
    }

    fun updateCallHistory(callHistory: JSONArray){
        this.callHistory = callHistory
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> mainContext.getString(R.string.contacts_tab)
            else -> mainContext.getString(R.string.history_tab)
        }
    }
}