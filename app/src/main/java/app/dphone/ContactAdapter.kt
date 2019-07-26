package app.dphone

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.net.URLEncoder
import kotlin.collections.ArrayList


class ContactAdapter(private val mContext: Context, private val parent: Any, private val apiFilter: Boolean, private val threshold: Int, private val loggedusername: String?) : BaseAdapter(), Filterable {

    private val baseUrl = "https://core.blockstack.org/v1/search?query="
    private var resultList: MutableList<ContactModel> = ArrayList()
    private var filteredList: MutableList<ContactModel> = ArrayList()
    private var invalidList: MutableList<ContactModel> = ArrayList()

    interface OnFilterListener {
        fun onFilter(constraint: CharSequence?, result: List<ContactModel>)
        fun onStartFilter(constraint: CharSequence?)
    }

    fun setInvalidContacts(contacts: JSONArray?, justNewContact: ContactModel?) {
        invalidList = ArrayList()
        if (contacts != null) {
            for(i in 0 until contacts.length()) {
                val contactModel = ContactModel(contacts.get(i) as JSONObject)
                invalidList.add(i, contactModel)
            }
        }
        if (justNewContact != null && !TextUtils.isEmpty(justNewContact.username) && filteredList.isNotEmpty()) {
            for(i in 0 until filteredList.size) {
                if (filteredList.get(i).username.equals(justNewContact.username)) {
                    filteredList.removeAt(i)
                    notifyChange("", filteredList)
                    break
                }
            }
        }
    }

    fun setContacts(contacts: JSONArray?) {
        resultList = ArrayList()
        filteredList = ArrayList()
        if (contacts != null) {
            for(i in 0 until contacts.length()) {
                val contactModel = ContactModel(contacts.get(i) as JSONObject)
                resultList.add(i, contactModel)
                filteredList.add(i, contactModel)
            }
        }
    }

    override fun getCount(): Int {
        return filteredList.size
    }

    override fun getItem(index: Int): ContactModel {
        return filteredList[index]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertedView: View?, parent: ViewGroup): View {
        var convertView = convertedView
        if (convertView == null) {
            val inflater = mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.fragment_user_info, parent, false)
        }

        val username = getItem(position).username
        val name = getItem(position).name
        val avatarUrl = getItem(position).avatarUrl
          (convertView!!.findViewById<View>(R.id.userDataTextViewOptional) as TextView).text = username
        if (!TextUtils.isEmpty(name)) {
            (convertView.findViewById<View>(R.id.userDataTextView) as TextView).text = name
        } else {
            (convertView.findViewById<View>(R.id.userDataTextView) as TextView).text = username
            (convertView.findViewById<View>(R.id.userDataTextViewOptional) as TextView).visibility = View.GONE
        }
        if (!TextUtils.isEmpty(avatarUrl)) {
            Picasso.get().load(avatarUrl).into((convertView.findViewById<View>(R.id.avatarView) as ImageView))
        } else {
            (convertView.findViewById<View>(R.id.avatarView) as ImageView).setImageResource(R.drawable.default_avatar)
        }
        if (position % 2 == 1) {
            convertView.setBackgroundColor(Color.WHITE)
        } else {
            convertView.setBackgroundColor(Color.parseColor("#e6e6e6"))
        }
        return convertView
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                if (constraint != null && constraint.toString().length >= threshold) {
                    if (parent is OnFilterListener) {
                        parent.onStartFilter(constraint)
                    }
                    if (apiFilter) {
                        val contactList = findContact(constraint.toString())
                        filterResults.values = contactList
                        filterResults.count = contactList.size
                    } else {
                        filteredList = ArrayList<ContactModel>()
                        for (i in 0 until resultList.size) {
                            val data = resultList.get(i)
                            if (data.username!!.startsWith(constraint.toString(), true) ||
                                    !TextUtils.isEmpty(data.name) && data.name!!.startsWith(constraint.toString(), true)) {
                                filteredList.add(data)
                            }
                        }
                        filterResults.values = filteredList
                        filterResults.count = filteredList.size
                    }
                } else if (!apiFilter) {
                    filteredList = ArrayList<ContactModel>(resultList)
                    filterResults.values = filteredList
                    filterResults.count = filteredList.size
                } else {
                    resultList = ArrayList<ContactModel>()
                    filteredList = ArrayList<ContactModel>()
                    val possibleUser = getNotListedUser(constraint?.toString())
                    if (possibleUser != null) {
                        resultList.add(possibleUser)
                        filteredList.add(possibleUser)
                    }
                    filterResults.values = filteredList
                    filterResults.count = filteredList.size
                }
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyChange(constraint.toString(), filteredList)
            }
        }
    }

    private fun findContact(text: String): List<ContactModel> {

        val oRequest = object : JsonObjectRequest(Method.GET, baseUrl + URLEncoder.encode(text, "UTF-8"), JSONObject(),
            Response.Listener<JSONObject> { response ->
                val result = response.getJSONArray("results")
                resultList = ArrayList()

                val possibleUser = getNotListedUser(text)
                var addPossibleUser = possibleUser != null

                for (i in 0 until result.length()) {
                    val loc = result.get(i) as JSONObject
                    val username = loc.getString("username")
                    if (loggedusername != null && loggedusername.equals(username)) {
                        continue
                    }
                    var shouldContinue = true
                    if (invalidList.isNotEmpty()) {
                        for (j in 0 until invalidList.size) {
                            if (username.equals(invalidList[j].username)) {
                                shouldContinue = false
                                break
                            }
                        }
                    }
                    if (!shouldContinue) {
                        continue
                    }
                    if (possibleUser != null && possibleUser.username.equals(username)) {
                        addPossibleUser = false
                    }
                    val profile = loc.getJSONObject("profile")
                    val name = profile.optString("name")
                    val images = profile.optJSONArray("image")
                    var avatarUrl = ""
                    if (images != null && images.length() > 0) {
                        for (j in 0 until images.length()) {
                            val img = images.get(j) as JSONObject
                            val imgName = img.optString("name")
                            if (!TextUtils.isEmpty(imgName) && imgName.equals("avatar")) {
                                avatarUrl = img.optString("contentUrl")
                                break
                            }
                        }
                    }
                    val contact = JSONObject()
                    contact.put("username", username)
                    if (!TextUtils.isEmpty(name)) {
                        contact.put("name", name)
                    }
                    if (!TextUtils.isEmpty(avatarUrl)) {
                        contact.put("avatarUrl", avatarUrl)
                    }
                    resultList.add(ContactModel(contact))
                }
                if (addPossibleUser) {
                    resultList.add(0, possibleUser!!)
                }
                filteredList = ArrayList<ContactModel>(resultList)
                notifyChange(text, filteredList)
            },
            Response.ErrorListener { error ->
                val err = if (error.message == null) "VolleyError: Request loading failed" else error.message
                Log.d("ContactAdapter", err)
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["query"] = text
                return params
            }}

            val requestQ = Volley.newRequestQueue(this.mContext)
            requestQ.add(oRequest)
            return resultList
    }


    private fun getNotListedUser(constraint: String?) : ContactModel? {
        if (constraint != null && constraint.trim().isNotEmpty()) {
            val username = constraint.trim().toLowerCase() + ".id.blockstack"
            var shouldAdd = true
            if (invalidList.isNotEmpty()) {
                for (j in 0 until invalidList.size) {
                    if (username.equals(invalidList[j].username)) {
                        shouldAdd = false
                        break
                    }
                }
            }
            if (shouldAdd) {
                val contact = JSONObject()
                contact.put("username", username)
                contact.put("notListed", true)
                return ContactModel(contact)
            }
        }
        return null
    }

    private fun notifyChange(constraint: String, list: List<ContactModel>) {
        notifyDataSetChanged()
        if (parent is OnFilterListener) {
            parent.onFilter(constraint, list)
        }
    }
}