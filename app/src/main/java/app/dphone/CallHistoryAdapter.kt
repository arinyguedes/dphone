package app.dphone

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import kotlin.collections.ArrayList
import android.widget.CheckBox




class CallHistoryAdapter(private val mContext: Context) : BaseAdapter() {
    var showSelectionCheckboxes: Boolean= false
    private var resultList: MutableList<CallDetailsModel> = ArrayList()
    private var checked: BooleanArray? = null

    fun setCallHistory(callHistory: JSONArray?) {
        resultList = ArrayList()
        if (callHistory != null) {
            for(i in 0 until callHistory.length()) {
                val callDetailsModel = CallDetailsModel(callHistory.get(i) as JSONObject)
                resultList.add(i, callDetailsModel)
            }
        }
        checked = BooleanArray(resultList.size)
    }

    override fun getCount(): Int {
        return resultList.size
    }

    override fun getItem(index: Int): CallDetailsModel {
        return resultList[index]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun setChecked(position: Int, isChecked: Boolean) {
        checked!![position] = isChecked
    }

    fun getChecked(position: Int): Boolean {
        return checked!![position]
    }

    fun toggleChecked(position: Int) {
        setChecked(position, !getChecked(position))
    }

    fun getCheckedCount():Int{
        var count = 0
        for(i in 0 until checked!!.size) {
            if(checked!![i]){
                count++
            }
        }
        return count
    }

    fun resetChecked(){
        checked = BooleanArray(resultList.size)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var newConvertView = convertView
        if (newConvertView == null) {
            val inflater = mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            newConvertView = inflater.inflate(R.layout.fragment_call_info, parent, false)
        }

        val callDetails = getItem(position)
        val user = callDetails.user
        if(user != null) {
            val username = user.username
            val name = user.name
            val avatarUrl = user.avatarUrl

            (newConvertView!!.findViewById<View>(R.id.timeDataTextView) as TextView).text = DateFormat.getTimeInstance(DateFormat.SHORT).format(callDetails.callDate)
            (newConvertView.findViewById<View>(R.id.dateDataTextView) as TextView).text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(callDetails.callDate)
            if (!TextUtils.isEmpty(name)) {
                (newConvertView.findViewById<View>(R.id.userDataTextView) as TextView).text = name
            } else {
                (newConvertView.findViewById<View>(R.id.userDataTextView) as TextView).text = username
            }

            (newConvertView.findViewById<View>(R.id.statusIconImageView) as ImageView).setImageResource(getStatusIcon(callDetails))
            setStatusTextAndColor((newConvertView.findViewById<View>(R.id.statusDataTextView) as TextView), callDetails)

            if (!TextUtils.isEmpty(avatarUrl)) {
                Picasso.get().load(avatarUrl).into((newConvertView.findViewById<View>(R.id.avatarView) as ImageView))
            } else {
                (newConvertView.findViewById<View>(R.id.avatarView) as ImageView).setImageResource(R.drawable.default_avatar)
            }

            if (position % 2 == 1) {
                newConvertView.setBackgroundColor(Color.WHITE)
            } else {
                newConvertView.setBackgroundColor(Color.parseColor("#e6e6e6"))
            }

            if (showSelectionCheckboxes) {
                newConvertView.findViewById<View>(R.id.checkbox).visibility = View.VISIBLE
                (newConvertView.findViewById<View>(R.id.checkbox) as CheckBox).isChecked = checked!![position]
            } else {
                newConvertView.findViewById<View>(R.id.checkbox).visibility = View.GONE
            }
        }
        return newConvertView!!
    }

    private fun getStatusIcon(callDetails:CallDetailsModel):Int{
        return if(callDetails.type == CallDetailsModel.INCOMING_CALL_TYPE){
            if(callDetails.status == CallDetailsModel.STATUS_ANSWERED){
                R.drawable.ic_call_incoming
            } else{
                R.drawable.ic_call_missed
            }
        } else{
            if(callDetails.status == CallDetailsModel.STATUS_ANSWERED) {
                R.drawable.ic_call_outgoing
            }
            else{
                R.drawable.ic_call_outgoing_missed
            }
        }
    }

    private fun setStatusTextAndColor(textView: TextView, callDetails:CallDetailsModel){
        if(callDetails.status == CallDetailsModel.STATUS_ANSWERED){
            if(callDetails.startDate != null && callDetails.endDate != null) {
                textView.text = mContext.getString(R.string.call_duration, Util.GetElapsedTime(callDetails.startDate, callDetails.endDate))
            }
            if(callDetails.type == CallDetailsModel.INCOMING_CALL_TYPE) {
                textView.setTextColor(mContext.getColor(R.color.colorCallIncoming))
            }
            else{
                textView.setTextColor(mContext.getColor(R.color.colorCallOutgoing))
            }
        } else {
            if(callDetails.type == CallDetailsModel.INCOMING_CALL_TYPE) {
                textView.text = mContext.getString(R.string.call_missed)
            }else{
                textView.text = mContext.getString(R.string.call_not_answered)
            }
            textView.setTextColor(mContext.getColor(R.color.colorCallIncompleted))
        }
    }
}