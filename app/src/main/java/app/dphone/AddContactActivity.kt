package app.dphone

import android.content.Intent
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_add_contact.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList


class AddContactActivity : BaseActivity(), ContactAdapter.OnFilterListener {

    private val threshold: Int = 3
    private var contacts: JSONArray? = null
    private var loggedusername: String? = null
    private var contactAdapter: ContactAdapter? = null

    override fun getContentLayoutId(): Int {
        return R.layout.activity_add_contact
    }

    override fun onCreateContent(content: View) {
        loggedusername = intent.getStringExtra("loggeduser")
        val stringContacts = intent.getStringExtra("contacts")
        if (stringContacts != null && !TextUtils.isEmpty(stringContacts)) {
            contacts = JSONArray(stringContacts)
        }
        back_button.setSafeOnClickListener {
            finish()
        }
        inviteButton.setSafeOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text_content))
            startActivity(Intent.createChooser(intent, "Share"))
        }
        contactAdapter = ContactAdapter(this, this, true, threshold, loggedusername)
        contactAdapter!!.setInvalidContacts(contacts, null)
        list.isTextFilterEnabled = true
        list.adapter = contactAdapter
        filterText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) { }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                contactAdapter!!.filter.filter(p0.toString())
            }
        })
        list.setOnItemClickListener { _, _, position, _ ->
            this.showLoading()
            try {
                val contact = this.contactAdapter!!.getItem(position)
                if (contact.notListed != null && contact.notListed!!) {
                    blockstack().getProfile(contact.username!!) { profile ->
                        if (profile != null) {
                            if (profile.name != null) {
                                contact.name = profile.name
                            }
                            if (profile.avatarImage != null) {
                                contact.avatarUrl = profile.avatarImage
                            }
                            handleNewContact(contact)
                        } else {
                            this.removeLoading()
                            toastNotify("Blockstack account not found.")
                        }
                    }
                } else {
                    handleNewContact(contact)
                }
            } catch (e: Exception) {
                Log.e("Error on add contact", e.message)
                this.removeLoading()
            }
        }
    }

    private fun handleNewContact(contact: ContactModel) {
        getPublicKey(contact.username!!) { publicKey ->
            if (!TextUtils.isEmpty(publicKey)) {
                contact.publicKey = publicKey
            }
            if (this.contacts != null) {
                this.contacts!!.put(contact.getJson())
                setContacts(this.contacts!!, contact)
            } else {
                val newContact = JSONArray()
                newContact.put(contact.getJson())
                this.contacts = newContact
                setContacts(newContact, contact)
            }
        }
    }

    override fun onFilter(constraint: CharSequence?, result: List<ContactModel>) {
        if (result.isNotEmpty() || constraint == null || constraint.length < threshold) {
            runOnUiThread {
                noDataText.visibility = View.GONE
            }
        } else {
            runOnUiThread {
                noDataText.setText(R.string.no_filtered_contacts)
                noDataText.visibility = View.VISIBLE
            }
        }
    }

    override fun onStartFilter(constraint: CharSequence?) {
    }

    private fun setContacts(contacts: JSONArray, newContact: ContactModel) {
        val jsonArrayAsList = ArrayList<JSONObject>()
        for (i in 0 until contacts.length()) {
            jsonArrayAsList.add(contacts.get(i) as JSONObject)
        }

        Collections.sort(jsonArrayAsList, object : Comparator<JSONObject> {
            override fun compare(jsonObjectA: JSONObject?, jsonObjectB: JSONObject?): Int {
                var compare = 0
                try {
                    var stringA = jsonObjectA?.optString("name")
                    if (stringA == null || TextUtils.isEmpty(stringA)) {
                        stringA = jsonObjectA?.getString("username")
                    }
                    var stringB = jsonObjectB?.optString("name")
                    if (stringB == null || TextUtils.isEmpty(stringB)) {
                        stringB = jsonObjectB?.getString("username")
                    }
                    compare = stringA!!.compareTo(stringB!!)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                return compare
            }
        })

        val stringyResult = Util.StringifyJsonArrayAsList(jsonArrayAsList)

        blockstack().putFile(getString(R.string.contacts_file), stringyResult, true) { result ->
            val success = !result.isNullOrEmpty()
            var textResource = R.string.contact_added
            if (success) {
                contactAdapter!!.setInvalidContacts(contacts, newContact)
            } else {
                textResource = R.string.error_on_add_contact
            }
            this.removeLoading()
            this.toastNotify(getString(textResource))
            if (success) {
                Intent().also { intent ->
                    intent.action = "android.intent.action.updateContacts"
                    intent.putExtra("contacts", stringyResult)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                }
                val intent = Intent(this, ContactActivity::class.java)
                intent.putExtra("contact", newContact.toString())
                intent.putExtra("contacts", contacts.toString())
                intent.putExtra("updated", true)
                startActivity(intent)
            }
        }
    }

}