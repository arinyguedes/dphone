package app.dphone

import org.json.JSONObject

class ContactModel(private val jsonObject: JSONObject) {

    var username: String?
        get() {
            return jsonObject.getString("username")
        }
        set(value) {
            jsonObject.putOpt("username", value)
        }

    var name: String?
        get() {
            return jsonObject.optString("name")
        }
        set(value) {
            jsonObject.putOpt("name", value)
        }

    var avatarUrl: String?
        get() {
            return jsonObject.optString("avatarUrl")
        }
        set(value) {
            jsonObject.putOpt("avatarUrl", value)
        }

    var publicKey: String?
        get() {
            return jsonObject.optString("publicKey")
        }
        set(value) {
            jsonObject.putOpt("publicKey", value)
        }

    var notListed: Boolean?
        get() {
            return jsonObject.optBoolean("notListed")
        }
        set(value) {
            jsonObject.putOpt("notListed", value)
        }

    override fun toString(): String {
        return jsonObject.toString()
    }

    fun getJson(): JSONObject {
        return jsonObject
    }
}