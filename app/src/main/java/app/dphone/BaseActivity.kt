package app.dphone

import android.animation.Animator
import android.content.Context
import android.content.DialogInterface
import org.json.JSONObject
import android.graphics.Rect
import android.widget.EditText
import android.view.inputmethod.InputMethodManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import org.json.JSONException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.appbar.AppBarLayout
import org.json.JSONArray
import java.util.*
import kotlin.collections.HashMap


abstract class BaseActivity : AppCompatActivity(), UserInfoFragment.OnFragmentInteractionListener {

    private val _playServiceResolutionRequest = 9000

    @LayoutRes
    abstract fun getContentLayoutId(): Int

    abstract fun onCreateContent(content: View)

    protected fun blockstack(): BlockstackManager {
        return BlockstackManager.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
        checkLoggedUser()

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        val vs = findViewById<ViewStub>(R.id.stub)
        vs.layoutResource = getContentLayoutId()
        val mainContent = vs.inflate()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        onCreateContent(mainContent)
    }

    override fun onCreateOptionsMenu(menu: Menu) : Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) : Boolean {
        val id = item.itemId
        if (id == R.id.signOut) {
            blockstack().signUserOut()
            val sharedPref = getSharedPreferences(getString(R.string.auth_token), Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                remove(getString(R.string.auth_token))
                apply()
            }
            unregisterWithNotificationHubs()
            finish()
            navigateToStep1()
        } else if (id == R.id.inviteMenu) {
            var intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text_content))
            startActivity(Intent.createChooser(intent, "Share"))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    protected fun hideToolbar() {
        val toolbarWrapper = findViewById<AppBarLayout>(R.id.toolbarWrapper)
        runOnUiThread {
            val params = toolbarWrapper.layoutParams as CoordinatorLayout.LayoutParams
            params.height = 0
            toolbarWrapper.layoutParams = params
            toolbarWrapper.visibility = View.GONE
        }
    }

    private fun handleSignedIn() {
        val loggedUser = blockstack().getLoggedUser()
        if (loggedUser != null) {
            val bundle = Bundle()
            bundle.putString("username", loggedUser.json.getString("username"))
            bundle.putString("avatarUrl", loggedUser.profile?.avatarImage)
            bundle.putString("name", loggedUser.profile?.name)
            bundle.putBoolean("darkLayout", true)
            val userFragment = UserInfoFragment()
            userFragment.arguments = bundle
            val manager = supportFragmentManager
            val transaction = manager.beginTransaction()
            transaction.replace(R.id.logged_user_fragment, userFragment)
            transaction.commit()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_MAIN) {
            handleSignedIn()
        }
    }

    protected fun getPublicKey(username: String, callback: (String?) -> Unit) {
        blockstack().getPublicFile(getString(R.string.identifier_file), username) { response ->
            if (response != null) {
                callback.invoke(JSONObject(response.toString()).getString("publicKey"))
            } else {
                callback.invoke(null)
            }
        }
    }

    protected fun getContacts(callback: (JSONArray?) -> Unit) {
        blockstack().getPrivateFile(getString(R.string.contacts_file)) { file ->
            if (file != null) {
                callback.invoke(JSONArray(file.toString()))
            } else {
                callback.invoke(null)
            }
        }
    }

    protected fun getCallHistory(callback: (JSONArray?) -> Unit) {
        blockstack().getPrivateFile(getString(R.string.call_history_file)) { file ->
            if (file != null) {
                var result = getValidHistory(file)
                callback.invoke(result)
            } else {
                callback.invoke(null)
            }
        }
    }

    private fun getValidHistory(file: Any): JSONArray {
        var result = JSONArray()
        var callHistory = JSONArray(file.toString())
        for (i in 0 until callHistory.length()) {
            if (!(callHistory[i] as JSONObject).optString("user").isNullOrBlank()) {
                result.put(callHistory[i])
            }
        }
        return result
    }

    protected fun saveCallHistory(arrayOfJson: ArrayList<JSONObject>?, callback: ((String?) -> Unit)? = null) {
        val stringifyResult = Util.StringifyJsonArrayAsList(arrayOfJson)

        blockstack().putFile(getString(R.string.call_history_file), stringifyResult, true) {
            if(callback!=null){
                callback(stringifyResult)
            }
            else {
                Intent().also { intent ->
                    intent.action = "android.intent.action.updateCallHistory"
                    intent.putExtra("callHistory", stringifyResult)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                }
            }
        }
    }

    override fun onFragmentClick(model: ContactModel) {
    }

    protected fun validateLoggedUser(): Boolean {
        if (!blockstack().isUserSignedIn()) {
            navigateToStep1()
            return false
        }
        return true
    }

    protected open fun checkLoggedUser() {
        if (validateLoggedUser()) {
            handleSignedIn()
        }
    }

    protected fun loggedUsername():String{
        return blockstack().getLoggedUser()!!.json.getString("username")
    }

    protected fun loggedPrivateKey():String{
        return blockstack().getLoggedUser()!!.appPrivateKey
    }

    protected fun loggedPublicKey():String{
        return Keys.GetPublicKey(loggedPrivateKey())
    }

    protected fun decryptString(content: String): String {
        return blockstack().decryptAsString(content)
    }

    protected fun encryptString(content: String, publicKey:String): String {
        return blockstack().encryptAsString(content, publicKey)
    }

    protected fun decryptStringToByteArray(content: String): ByteArray {
        return blockstack().decryptAsByteArray(content)
    }

    protected fun encryptByteArray(content: ByteArray, publicKey: String): String {
        return blockstack().encryptAsByteArray(content, publicKey)
    }

    protected fun navigateToStep1() {
        startActivity(Intent(this, Step1Activity::class.java))
    }

    protected fun navigateToSignIn() {
        startActivity(Intent(this, SignInActivity::class.java))
    }

    protected fun navigateToHome() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    protected fun sendPush(to: String, payloadObject: JSONObject,
                           successListener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener) {
        sendPostRequest("v1/push/" + to, payloadObject, successListener, errorListener)
    }

    protected fun sendPostRequest(route: String, requestObject: JSONObject, successListener: Response.Listener<JSONObject>) {
        sendRequest(Request.Method.POST, route, requestObject, successListener)
    }

    private fun sendPostRequest(route: String, requestObject: JSONObject,
                                  successListener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener) {
        sendRequest(Request.Method.POST, route, requestObject, successListener, errorListener)
    }

    protected fun sendGetRequest(route: String, successListener: Response.Listener<JSONObject>) {
        sendRequest(Request.Method.GET, route, JSONObject(), successListener)
    }

    private fun sendRequest(requestMethod: Int, route: String, requestObject: JSONObject,
                            successListener: Response.Listener<JSONObject>) {
        sendRequest(requestMethod, route, requestObject, successListener, getErrorDialogListener())
    }

    private fun sendRequest(requestMethod: Int, route: String, requestObject: JSONObject,
                              successListener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener) {
        val storedToken = getSharedPreferences(getString(R.string.auth_token), Context.MODE_PRIVATE)
                .getString(getString(R.string.auth_token), "")
        if (storedToken == null || storedToken.equals("")) {
            blockstack().getAuthToken { authToken ->
                sendRequest(requestMethod, route, requestObject, authToken, successListener, errorListener)
            }
        } else {
            sendRequest(requestMethod, route, requestObject, storedToken, successListener, errorListener)
        }
    }

    private fun sendRequest(requestMethod: Int, route: String, requestObject: JSONObject, authToken: String?,
                            successListener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener) {

        val jsonRequest = object : JsonObjectRequest(requestMethod, "${BuildConfig.SERVER_URL}/api/${route}",
                requestObject, successListener, errorListener) {

            override fun parseNetworkError(volleyError: VolleyError?): VolleyError {
                if (volleyError?.networkResponse != null && volleyError.networkResponse.data != null) {
                    return VolleyError(String(volleyError.networkResponse.data))
                }
                return super.parseNetworkError(volleyError)
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["X-Mobile-App"] = BuildConfig.VERSION_CODE.toString()
                if (authToken != null && !authToken.equals("")) {
                    headers["blockstack-auth-token"] = authToken
                }
                return headers
            }
        }
        RequestQueueSingleton.getInstance(this.applicationContext).addToRequestQueue(jsonRequest)
    }

    protected fun getErrorDialogListener() : Response.ErrorListener {
        return Response.ErrorListener {
            val alertDialog = AlertDialog.Builder(this).create()
            alertDialog.setTitle("Alert")
            alertDialog.setMessage(trimMessage(it.message, "error"))
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    DialogInterface.OnClickListener { dialog, _ -> dialog.dismiss() })
            alertDialog.show()
        }
    }

    private fun trimMessage(json: String?, key: String): String? {
        val trimmedString: String?
        try {
            val obj = JSONObject(json)
            trimmedString = obj.getString(key)
        } catch (e: JSONException) {
            e.printStackTrace()
            return null
        }
        return trimmedString
    }

    protected fun registerWithNotificationHubs(userIdentifier: String, publicKey: String) {
        if (checkPlayServices()) {
            setHubRegistration(userIdentifier)
            val payload = JSONObject()
            payload.put("publicKey", publicKey)
            payload.put("referenceDate", Calendar.getInstance().time)
            blockstack().putFile(getString(R.string.identifier_file), payload.toString(), false) {
            }
        }
    }

    protected fun setHubRegistration(userIdentifier: String) {
        val intent = Intent(this, RegistrationIntentService::class.java)
        intent.putExtra("UserIdentifier", userIdentifier)
        startService(intent)
    }

    private fun unregisterWithNotificationHubs() {
        if (checkPlayServices()) {
            blockstack().deleteFile(getString(R.string.identifier_file)) {
            }
            val intent = Intent(this, RegistrationIntentService::class.java)
            intent.putExtra("Unregister", true)
            startService(intent)
        }
    }

    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, _playServiceResolutionRequest)
                        .show()
            } else {
                toastNotify("This device is not supported by Google Play Services.")
                finish()
            }
            return false
        }
        return true
    }

    protected fun toastNotify(notificationMessage: String) {
        runOnUiThread {
            Toast.makeText(this, notificationMessage, Toast.LENGTH_LONG).show()
        }
    }

    protected fun showLoading() {
        animateLoading(View.VISIBLE, 0.4f, 200)
    }

    protected fun removeLoading() {
        animateLoading(View.GONE, 0f, 200)
    }

    private fun animateLoading(toVisibility: Int, toAlpha: Float, duration: Long) {
        val view = findViewById<View>(R.id.progress_overlay)
        var alpha = 0f
        runOnUiThread {
            if (toVisibility == View.VISIBLE) {
                view.alpha = 0f
                alpha = toAlpha
            }
            view.visibility = View.VISIBLE
            view.animate()
                    .setDuration(duration)
                    .alpha(alpha)
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationEnd(animation: Animator?) {
                            view.visibility = toVisibility
                        }

                        override fun onAnimationStart(animation: Animator?) {}

                        override fun onAnimationRepeat(animation: Animator?) {}

                        override fun onAnimationCancel(animation: Animator?) {}
                    })
        }
    }
}
