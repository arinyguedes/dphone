package app.dphone

import org.json.JSONObject
import java.util.*

class CallDetailsModel(private val jsonObject: JSONObject) {

    var user: ContactModel?
        get() {
            var user = jsonObject.optString("user")
            if(user!=null && !user.isNullOrBlank()) {
                return ContactModel(JSONObject(user))
            }
            return null
        }
        set(value) {
            jsonObject.putOpt("user", value.toString())
        }

    var callDate: Date?
        get() {
            return Date(jsonObject.optLong("callDate"))
        }
        set(value) {
            jsonObject.putOpt("callDate", value?.time)
        }

    var startDate: Date?
        get() {
            return Date(jsonObject.optLong("startDate"))
        }
        set(value) {
            jsonObject.putOpt("startDate", value?.time)
        }

    var endDate: Date?
        get() {
            return Date(jsonObject.optLong("endDate"))
        }
        set(value) {
            jsonObject.putOpt("endDate", value?.time)
        }

    var type: Int?
        get() {
            return jsonObject.optInt("type")
        }
        set(value) {
            jsonObject.putOpt("type", value)
        }

    var status: Int?
        get() {
            return jsonObject.optInt("status")
        }
        set(value) {
            jsonObject.putOpt("status", value)
        }

    override fun toString(): String {
        return jsonObject.toString()
    }

    fun getJson(): JSONObject {
        return jsonObject
    }

    companion object{
        const val INCOMING_CALL_TYPE = 0
        const val OUTGOING_CALL_TYPE = 1

        const val STATUS_NOT_ANSWERED = 0
        const val STATUS_ANSWERED = 1
    }
}