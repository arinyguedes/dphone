package app.dphone

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.fragment_list_history.*
import kotlinx.android.synthetic.main.fragment_list_history.view.*
import org.json.JSONArray
import org.json.JSONObject


class ListHistoryFragment(private val listener: OnUpdatedHistoryListener?) : Fragment() {
    private var callHistory: JSONArray? = null
    private var contacts: JSONArray? = null
    private var mReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerCallHistoryBroadcastReceiver()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View?  {
        val view = inflater.inflate(R.layout.fragment_list_history, container, false)
        view.list.setOnItemLongClickListener{ _, _, position, _ ->
            onHistoryLongClick(position)
        }
        view.list.setOnItemClickListener{ _, _, position, _ ->
            onHistoryClick(position)
        }
        view.cancelSelectionButton.setOnClickListener {
            cancelSelection()
        }
        view.deleteButton.setOnClickListener {
            deleteSelected()
        }
        return view
    }

    private fun onHistoryLongClick(position: Int):Boolean{
        val adapter = (list.adapter as (CallHistoryAdapter))
        adapter.setChecked(position, true)
        adapter.showSelectionCheckboxes = true
        deleteHistory.visibility = View.VISIBLE
        adapter.notifyDataSetChanged()
        updateSelectionText()
        return true
    }

    private fun onHistoryClick(position: Int) {
        val adapter = (list.adapter as (CallHistoryAdapter))
        if(adapter.showSelectionCheckboxes) {
            adapter.toggleChecked(position)
            adapter.notifyDataSetChanged()
            updateSelectionText()
        }
        else if(contacts != null) {
            val callDetailsModel = CallDetailsModel(callHistory!!.get(position) as JSONObject)
            val intent = Intent(this.context, ContactActivity::class.java)
            intent.putExtra("contact", callDetailsModel.user.toString())
            intent.putExtra("contacts", contacts.toString())
            intent.putExtra("updated", false)
            startActivity(intent)
        }
    }

    private fun updateSelectionText(){
        val adapter = (list.adapter as (CallHistoryAdapter))
        selectionText.text = getString(R.string.total_selected, adapter.getCheckedCount())
    }

    private fun cancelSelection(){
        val adapter = (list.adapter as (CallHistoryAdapter))
        adapter.showSelectionCheckboxes = false
        adapter.resetChecked()
        deleteHistory.visibility = View.GONE
        adapter.notifyDataSetChanged()
    }

    private fun deleteSelected(){
        val adapter = (list.adapter as (CallHistoryAdapter))
        if(adapter.getCheckedCount() == 0){
            Toast.makeText(context, "Select an item to delete", Toast.LENGTH_LONG).show()
        }
        else {
            showDeleteConfirmation(adapter)
        }
    }

    private fun showDeleteConfirmation(adapter: CallHistoryAdapter) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage("Delete selected items?")
        builder.setPositiveButton("DELETE") { _, _ ->
            var updatedCallHistory = JSONArray()
            for (i in 0 until callHistory!!.length()) {
                if (!adapter.getChecked(i))
                    updatedCallHistory.put(callHistory!![i])
            }
            listener?.saveUpdatedCallHistory(updatedCallHistory) { stringCallHistory ->
                run {
                    cancelSelection()
                    updateCallHistoryFromString(stringCallHistory)
                }
            }
        }
        builder.setNeutralButton("CANCEL") { _, _ ->
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val adapter = CallHistoryAdapter(context!!)
        list.adapter = adapter
        var stringCallHistory: String? = null
        var stringContacts: String? = null
        arguments?.let {
            stringCallHistory = it.getString("callHistory")
            stringContacts = it.getString("contacts")
        }
        updateCallHistoryFromString(stringCallHistory)
        updateContactsFromString(stringContacts)
    }

    private fun registerCallHistoryBroadcastReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.intent.action.updateCallHistory")
        intentFilter.addAction("android.intent.action.updateContacts")

        mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val stringCallHistory = intent.getStringExtra("callHistory")
                if(stringCallHistory!=null) {
                    updateCallHistoryFromString(stringCallHistory)
                }
                val stringContact= intent.getStringExtra("contacts")
                if(stringContact!=null) {
                    updateContactsFromString(stringContact)
                }
            }
        }
        LocalBroadcastManager.getInstance(context!!).registerReceiver(mReceiver!!, intentFilter)
    }

    fun updateCallHistoryFromString(stringCallHistory:String?){
        if (stringCallHistory != null && !TextUtils.isEmpty(stringCallHistory)) {
            callHistory = JSONArray(stringCallHistory)
        } else {
            callHistory = null
        }
        onCallHistoryUpdated()
    }

    fun updateContactsFromString(stringContacts: String?){
        if (stringContacts != null && !TextUtils.isEmpty(stringContacts)) {
            contacts = JSONArray(stringContacts)
        }
        else{
            contacts = null
        }
    }

    private fun onCallHistoryUpdated(){
        if(list != null) {
            val adapter = (list.adapter as (CallHistoryAdapter))
            adapter.setCallHistory(callHistory)
            when {
                this.callHistory == null -> {
                    noDataText.setText(R.string.loading)
                    noDataText.visibility = View.VISIBLE
                }
                callHistory!!.length() == 0 -> {
                    noDataText.setText(R.string.no_calls_history)
                    noDataText.visibility = View.VISIBLE
                }
                else -> noDataText.visibility = View.GONE
            }
            if (callHistory != null) {
                adapter.notifyDataSetChanged()
            }
            updateSelectionText()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(callHistory: JSONArray?, contacts: JSONArray?, listener: OnUpdatedHistoryListener) =
                ListHistoryFragment(listener).apply {
                    arguments = Bundle().apply {
                        putString("callHistory", callHistory?.toString())
                        putString("contacts", contacts?.toString())
                    }
                }
    }
}