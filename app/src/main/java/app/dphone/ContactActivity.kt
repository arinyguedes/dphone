package app.dphone

import android.app.AlertDialog
import android.content.Intent
import android.text.Html
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.ArrayList


class ContactActivity : BaseActivity(), ContactFragment.OnFragmentBackListener {
    private var contact: ContactModel? = null
    private var contacts: JSONArray? = null

    override fun getContentLayoutId(): Int {
        return R.layout.activity_contact
    }

    override fun onCreateContent(content: View) {
        contact = ContactModel(JSONObject(intent.getStringExtra("contact")))
        contacts = JSONArray(intent.getStringExtra("contacts"))

        if (intent.getBooleanExtra("updated", false)) {
            handleViewCreation()
        } else {
            var shouldUpdateContact = false
            if (contact!!.publicKey == null || TextUtils.isEmpty(contact!!.publicKey)) {
                getPublicKey(contact!!.username!!) { publicKey ->
                    if (!TextUtils.isEmpty(publicKey)) {
                        contact!!.publicKey = publicKey
                        shouldUpdateContact = true
                    }
                    checkCurrentProfile(shouldUpdateContact)
                }
            } else {
                checkCurrentProfile(shouldUpdateContact)
            }
        }
    }

    private fun checkCurrentProfile(updatedPublicKey: Boolean) {
        var shouldUpdateContact = updatedPublicKey
        blockstack().getProfile(contact!!.username!!) { profile ->
            if (profile != null) {
                if ((profile.name != null && contact!!.name == null) || (profile.name == null && contact!!.name != null)
                        || (profile.name != null && contact!!.name != null && !profile.name.equals(contact!!.name))) {
                    contact!!.name = profile.name
                    shouldUpdateContact = true
                }
                if ((profile.avatarImage != null && contact!!.avatarUrl == null) || (profile.avatarImage == null && contact!!.avatarUrl != null)
                        || (profile.avatarImage != null && contact!!.avatarUrl != null && !profile.avatarImage.equals(contact!!.avatarUrl))) {
                    contact!!.avatarUrl = profile.avatarImage
                    shouldUpdateContact = true
                }
            }
            if (shouldUpdateContact) {

                val jsonArrayAsList = ArrayList<JSONObject>()
                for (i in 0 until contacts!!.length()) {
                    val element = contacts!!.get(i) as JSONObject
                    if (element.getString("username").equals(contact!!.username)) {
                        jsonArrayAsList.add(contact!!.getJson())
                    } else {
                        jsonArrayAsList.add(element)
                    }
                }

                val stringyResult = Util.StringifyJsonArrayAsList(jsonArrayAsList)

                blockstack().putFile(getString(R.string.contacts_file), stringyResult,  true) {
                    handleViewCreation()
                    Intent().also { intent ->
                        intent.action = "android.intent.action.updateContacts"
                        intent.putExtra("contacts", stringyResult)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }
                }
            } else {
                handleViewCreation()
            }
        }
    }

    private fun handleViewCreation() {
        findViewById<TextView>(R.id.contactLoading).visibility = View.GONE
        val contractFragment = ContactFragment.newInstance(contact!!.username!!, contact!!.name, contact!!.avatarUrl, true)
        val manager = supportFragmentManager
        val transaction = manager.beginTransaction()
        transaction.replace(R.id.contactInfoWrapper, contractFragment)
        transaction.commit()

        findViewById<TextView>(R.id.contactRemoveText).visibility = View.VISIBLE
        findViewById<FloatingActionButton>(R.id.contactRemoveButton).show()
        if (contact!!.publicKey == null || TextUtils.isEmpty(contact!!.publicKey)) {
            findViewById<TextView>(R.id.contactCallText).visibility = View.GONE
            findViewById<FloatingActionButton>(R.id.contactCallButton).hide()
            findViewById<TextView>(R.id.contactInviteText).visibility = View.VISIBLE
            val inviteBtn = findViewById<FloatingActionButton>(R.id.contactInviteButton)
            inviteBtn.show()
            inviteBtn.setSafeOnClickListener {
                var intent = Intent()
                intent.action = Intent.ACTION_SEND
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text_content))
                startActivity(Intent.createChooser(intent, "Share"))
            }
            val alertText = findViewById<TextView>(R.id.contactAlertText)
            val alertHtml = "<p>Contact doesn't have <font color='#fa1d49'><b>d</b></font><b>Phone</b></font> yet.</p><p>Invite :)</p>"
            alertText.text = Html.fromHtml(alertHtml, Html.FROM_HTML_MODE_COMPACT)
            alertText.visibility = View.VISIBLE
        } else {
            findViewById<TextView>(R.id.contactAlertText).visibility = View.GONE
            findViewById<TextView>(R.id.contactInviteText).visibility = View.GONE
            findViewById<FloatingActionButton>(R.id.contactInviteButton).hide()
            findViewById<TextView>(R.id.contactCallText).visibility = View.VISIBLE
            val callBtn = findViewById<FloatingActionButton>(R.id.contactCallButton)
            callBtn.show()
            callBtn.setSafeOnClickListener {
                val intent = Intent(this, CallActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                intent.putExtra("contact", contact.toString())
                startActivity(intent)
                finish()
            }
        }
        findViewById<FloatingActionButton>(R.id.contactRemoveButton).setSafeOnClickListener {
            showRemoveContactConfirmation()
        }
    }

    private fun showRemoveContactConfirmation(){
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Delete this contact?")
        builder.setPositiveButton("DELETE") { _, _ ->
            removeContact()
        }
        builder.setNeutralButton("CANCEL") { _, _ ->
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun onBackClick() {
        finish()
    }

    private fun removeContact() {
        showLoading()

        val jsonArrayAsList = ArrayList<JSONObject>()
        for (i in 0 until contacts!!.length()) {
            val element = contacts!!.get(i) as JSONObject
            if (!element.getString("username").equals(contact!!.username)) {
                jsonArrayAsList.add(element)
            }
        }

        val stringyResult = Util.StringifyJsonArrayAsList(jsonArrayAsList)

        blockstack().putFile(getString(R.string.contacts_file), stringyResult, true) { result ->
            val toastmessage: String
            if (!result.isNullOrEmpty()) {
                toastmessage = "Contact deleted."
            } else {
                toastmessage = "Error on delete contact."
            }
            Intent().also { intent ->
                intent.action = "android.intent.action.updateContacts"
                intent.putExtra("contacts", stringyResult)
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }
            removeLoading()
            toastNotify(toastmessage)
            finish()
        }
    }
}