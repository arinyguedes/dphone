package app.dphone

import android.content.Context
import android.content.Intent
import android.view.View
import org.blockstack.android.sdk.model.UserData
import kotlinx.android.synthetic.main.activity_signin.*


class SignInActivity : BaseActivity() {

    override fun getContentLayoutId(): Int {
        return R.layout.activity_signin
    }

    override fun onCreateContent(content: View) {
        hideToolbar()
        if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        } else {
            setLogInButton()
        }
    }

    private fun setLogInButton() {
        signInButton.setSafeOnClickListener {
            blockstack().redirectUserToSignIn { errorResult ->
                if (!errorResult.isNullOrEmpty()) {
                    toastNotify(errorResult.toString())
                }
            }
        }
        runOnUiThread {
            signInButton.isEnabled = true
            signInButton.visibility = View.VISIBLE
        }
        removeLoading()
    }

    private fun setLoading() {
        runOnUiThread {
            signInButton.isEnabled = false
        }
        showLoading()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.action == Intent.ACTION_VIEW) {
            handleAuthResponse(intent)
        }
    }

    override fun checkLoggedUser() {
    }

    override fun onResume() {
        super.onResume()
        if (blockstack().isUserSignedIn()) {
            finish()
            navigateToHome()
        }
    }

    private fun onSignIn(userData: UserData, authResponse: String) {
        val identifier = userData.json.getString("username")
        val publicKey = Keys.GetPublicKey(userData.appPrivateKey)
        registerWithNotificationHubs(identifier, publicKey)
        val sharedPref = getSharedPreferences(getString(R.string.auth_token), Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString(getString(R.string.auth_token), authResponse)
            apply()
        }
        finish()
        navigateToHome()
    }

    private fun handleAuthResponse(intent: Intent) {
        setLoading()
        val response = intent.data?.query
        if (response != null) {
            val authResponseTokens = response.split('=')
            if (authResponseTokens.size > 1) {
                val authResponse = authResponseTokens[1]
                blockstack().handlePendingSignIn(authResponse) { userData ->
                    if (userData != null) {
                        onSignIn(userData, authResponse)
                    } else {
                        setLogInButton()
                    }
                }
            } else {
                setLogInButton()
            }
        } else {
            setLogInButton()
        }
    }
}