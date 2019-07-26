package app.dphone

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_list_contact.*
import kotlinx.android.synthetic.main.fragment_list_contact.view.*
import org.json.JSONArray
import org.json.JSONObject
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class ListContactFragment : Fragment(), ContactAdapter.OnFilterListener {

    private var contacts: JSONArray? = null
    private var loggedusername: String? = null
    private var mReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerContactsBroadcastReceiver()
    }

    private fun registerContactsBroadcastReceiver() {
        val intentFilter = IntentFilter(
                "android.intent.action.updateContacts")

        mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val stringContacts = intent.getStringExtra("contacts")
                updateContactsFromString(stringContacts)
            }
        }
        LocalBroadcastManager.getInstance(context!!).registerReceiver(mReceiver!!, intentFilter)
    }

    fun updateContactsFromString(stringContacts: String?){
        if (stringContacts != null && !TextUtils.isEmpty(stringContacts)) {
            contacts = JSONArray(stringContacts)
        }
        else{
            contacts = null
        }
        onContactsUpdated()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(mReceiver!=null) {
            LocalBroadcastManager.getInstance(context!!).unregisterReceiver(mReceiver!!)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?  {
        val view = inflater.inflate(R.layout.fragment_list_contact, container, false)
        view.addContactButton.setSafeOnClickListener {
            onClickAddContact()
        }
        view.list.setOnItemClickListener { _, _, position, _ ->
            onContactClick(ContactModel(this.contacts!!.get(position) as JSONObject))
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val adapter = ContactAdapter(this.context!!, this, false, 1, loggedusername)
        list.isTextFilterEnabled = true
        list.adapter = adapter
        var contactsString: String? = null
        arguments?.let {
            loggedusername = it.getString("loggeduser")
            contactsString = it.getString("contacts")
        }
        updateContactsFromString(contactsString)
        filterText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) { }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                adapter.filter.filter(p0.toString())
            }
        })
    }

    private fun onContactsUpdated(){
        if(list!= null) {
            val adapter = (list.adapter as (ContactAdapter))
            adapter.setContacts(contacts)
            when {
                this.contacts == null -> {
                    noDataText.setText(R.string.loading)
                    noDataText.visibility = View.VISIBLE
                }
                contacts!!.length() == 0 -> {
                    noDataText.setText(R.string.no_contacts)
                    noDataText.visibility = View.VISIBLE
                }
                else -> noDataText.visibility = View.GONE
            }
            adapter.filter.filter(filterText.text)
        }
    }

    private fun onContactClick(contact: ContactModel) {
        val intent = Intent(this.context, ContactActivity::class.java)
        intent.putExtra("contact", contact.toString())
        intent.putExtra("contacts", this.contacts!!.toString())
        intent.putExtra("updated", false)
        startActivity(intent)
    }

    override fun onFilter(constraint: CharSequence?, result: List<ContactModel>) {
        if (this.contacts != null && this.contacts!!.length() > 0) {
            if (result.isNotEmpty()) {
                noDataText.setText(R.string.no_contacts)
                noDataText.visibility = View.GONE
            } else {
                noDataText.setText(R.string.no_filtered_contacts)
                noDataText.visibility = View.VISIBLE
            }
        }
    }

    override fun onStartFilter(constraint: CharSequence?) {
    }

    private fun onClickAddContact() {
        addContactButton.isEnabled = false
        val intent = Intent(this.context, AddContactActivity::class.java)
        intent.putExtra("contacts", contacts?.toString())
        intent.putExtra("loggeduser", loggedusername)
        startActivity(intent)
        addContactButton.isEnabled = true
    }

    companion object {
        @JvmStatic
        fun newInstance(loggeduser: String, contacts: JSONArray?) =
                ListContactFragment().apply {
                    arguments = Bundle().apply {
                        putString("loggeduser", loggeduser)
                        putString("contacts", contacts?.toString())
                    }
                }
    }
}
