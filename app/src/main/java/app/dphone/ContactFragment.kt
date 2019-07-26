package app.dphone

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso


private const val ARG_USERNAME = "username"
private const val ARG_NAME = "name"
private const val ARG_AVATAR_URL = "avatarUrl"
private const val ARG_SHOW_BACK_BTN = "showBackButton"

class ContactFragment : Fragment() {
    private var username: String? = null
    private var name: String? = null
    private var avatarUrl: String? = null
    private var showBackButton: Boolean = false

    private var listener: OnFragmentBackListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            username = it.getString(ARG_USERNAME)
            name = it.getString(ARG_NAME, "")
            avatarUrl = it.getString(ARG_AVATAR_URL, "")
            showBackButton = it.getBoolean(ARG_SHOW_BACK_BTN, false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_contact, container, false)

        (view!!.findViewById<View>(R.id.contactOptionalText) as TextView).text = username
        if (!TextUtils.isEmpty(name)) {
            (view.findViewById<View>(R.id.contactMainText) as TextView).text = name
        } else {
            (view.findViewById<View>(R.id.contactMainText) as TextView).text = username
            (view.findViewById<View>(R.id.contactOptionalText) as TextView).visibility = View.GONE
        }
        if (!TextUtils.isEmpty(avatarUrl)) {
            Picasso.get().load(avatarUrl).into((view.findViewById<View>(R.id.contactAvatarView) as ImageView))
        } else {
            (view.findViewById<View>(R.id.contactAvatarView) as ImageView).setImageResource(R.drawable.default_avatar)
        }
        val backButton = (view.findViewById<View>(R.id.contactBackButton) as MaterialButton)
        if (showBackButton) {
            backButton.visibility = View.VISIBLE
            backButton.setSafeOnClickListener {
                listener?.onBackClick()
            }
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentBackListener) {
            listener = context
        } else if (showBackButton) {
            throw RuntimeException(context.toString() + " must implement OnFragmentBackListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentBackListener {
        fun onBackClick()
    }

    companion object {
        @JvmStatic
        fun newInstance(username: String, name: String?, avatarUrl: String?, showBackButton: Boolean) =
                ContactFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_USERNAME, username)
                        putString(ARG_NAME, name)
                        putString(ARG_AVATAR_URL, avatarUrl)
                        putBoolean(ARG_SHOW_BACK_BTN, showBackButton)
                    }
                }
    }
}
