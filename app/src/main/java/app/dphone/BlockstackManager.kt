package app.dphone

import android.content.Context
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.Result
import org.blockstack.android.sdk.Scope
import org.blockstack.android.sdk.model.*
import org.json.JSONObject
import java.net.URL
import java.security.PrivateKey

class BlockstackManager private constructor(context: Context) {
    private val _authTokenFileName: String = "authToken"
    private val _appUrl: String = "https://" + context.getString(R.string.app_webpage)
    private val _blockstackSession: BlockstackSession

    init {
        _blockstackSession = BlockstackSession(context, (_appUrl).toBlockstackConfig(arrayOf(Scope.StoreWrite, Scope.PublishData, Scope.Email)))
    }
    companion object : SingletonHolder<BlockstackManager, Context>(::BlockstackManager)

    private var _loggedUser : UserData? = null
    fun getLoggedUser() : UserData? {
        if (_loggedUser != null) {
            return _loggedUser
        } else {
            _loggedUser = _blockstackSession.loadUserData()
            return _loggedUser
        }
    }

    private var _authToken : String? = null
    fun getAuthToken(callback: (String?) -> Unit) {
        if (_authToken != null) {
            callback.invoke(_authToken)
        } else {
            getPrivateFile(_authTokenFileName) { authToken ->
                _authToken = authToken?.toString()
                callback.invoke(_authToken)
            }
        }
    }

    private fun getStringFromResult(result : Result<Any>): String {
        if(result.hasValue){
            if(result.value is CipherObject)
                return (result.value as CipherObject).json.toString()
            return result.value!! as String
        }
        throw Exception(result.error)
    }

    private fun getByteArrayFromResult(result : Result<Any>): ByteArray {
        if(result.hasValue && result.value is ByteArray){
            return result.value!! as ByteArray
        }
        throw Exception(result.error)
    }

    private fun clearUserData() {
        clearStoredUser()
        _authToken = null
    }

    fun clearStoredUser() {
        _loggedUser = null
    }

    fun signUserOut() {
        clearUserData()
        deleteFile(_authTokenFileName) {}
        _blockstackSession.signUserOut()
    }

    fun redirectUserToSignIn(callbackError: (String?) -> Unit) {
        clearUserData()
        _blockstackSession.redirectUserToSignIn { errorResult ->
            if (errorResult.hasErrors) {
                callbackError.invoke(errorResult.error)
            } else {
                callbackError.invoke(null)
            }
        }
    }

    fun handlePendingSignIn(authResponse: String, callback: (UserData?) -> Unit) {
        _blockstackSession.handlePendingSignIn(authResponse) { userData ->

            if (userData.hasValue && userData.error == null) {
                callback.invoke(userData.value)
                putFile(_authTokenFileName, authResponse, true) {}
            } else {
                callback.invoke(null)
            }
        }
    }

    fun isUserSignedIn(): Boolean {
        return _blockstackSession.isUserSignedIn()
    }

    fun getProfile(username: String, callback: (Profile?) -> Unit){
        _blockstackSession.lookupProfile(username, URL(_blockstackSession.nameLookupUrl)) { response ->
            if (response.hasValue && response.error == null) {
                callback.invoke(response.value)
            } else {
                callback.invoke(null)
            }
        }
    }

    fun getPublicFile(fileName: String, username: String, callback: (Any?) -> Unit) {
        _blockstackSession.getFile(fileName,
                GetFileOptions(decrypt = false, username = username, app = _appUrl)) { file ->
            if (file.hasValue && file.error == null) {
                callback.invoke(file.value)
            } else {
                callback.invoke(null)
            }
        }
    }

    fun getPrivateFile(fileName: String, callback: (Any?) -> Unit) {
        _blockstackSession.getFile(fileName, GetFileOptions(decrypt = true, app = _appUrl)) { file ->
            if (file.hasValue && file.error == null) {
                callback.invoke(file.value)
            } else {
                callback.invoke(null)
            }
        }
    }

    fun putFile(fileName: String, payload: String, encrypt: Boolean, callback: (String?) -> Unit) {
        _blockstackSession.putFile(fileName, payload, PutFileOptions(encrypt = encrypt)) { result ->
            if (result.hasValue && result.error == null) {
                if (result.value.isNullOrEmpty()) {
                    callback.invoke("ok")
                } else {
                    callback.invoke(result.value)
                }
            } else {
                callback.invoke(null)
            }
        }
    }

    fun deleteFile(fileName: String, callback: (Boolean) -> Unit) {
        _blockstackSession.deleteFile(fileName) { result ->
            if (result.hasValue && result.error == null) {
                callback.invoke(true)
            } else {
                callback.invoke(false)
            }
        }
    }

    fun decryptAsString(content: String): String {
        return getStringFromResult(_blockstackSession.decryptContent(
                content, false, CryptoOptions(null, getLoggedUser()!!.appPrivateKey)))
    }

    fun encryptAsString(content: String, publicKey: String): String {
        return getStringFromResult(_blockstackSession.encryptContent(
                content, CryptoOptions(publicKey)) as Result<Any>)
    }

    fun decryptAsByteArray(content: String): ByteArray {
        return getByteArrayFromResult(_blockstackSession.decryptContent(
                content, true, CryptoOptions(null, getLoggedUser()!!.appPrivateKey)))
    }

    fun encryptAsByteArray(content: ByteArray, publicKey:String): String{
        return getStringFromResult(_blockstackSession.encryptContent(
                content, CryptoOptions(publicKey)) as Result<Any>)
    }

    fun signProfileToken(privateKey: String,publicKey: String):ProfileTokenPair{
        return _blockstackSession.signProfileToken(Profile(JSONObject()), privateKey, Entity.withKey(publicKey),Entity.withKey(publicKey))
    }

    fun verifyProfileToken(token: String,publicKey: String): ProfileToken{
        return _blockstackSession.verifyProfileToken(token, publicKey)
    }
}