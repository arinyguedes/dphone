package app.dphone

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import org.json.JSONObject


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_USERNAME = "username"
private const val ARG_NAME = "name"
private const val ARG_AVATAR_URL = "avatarUrl"
private const val ARG_DARK_LAYOUT = "darkLayout"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [UserInfoFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [UserInfoFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class UserInfoFragment : Fragment() {
    private var username: String? = null
    private var name: String? = null
    private var avatarUrl: String? = null
    private var darkLayout: Boolean = false

    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            username = it.getString(ARG_USERNAME)
            name = it.getString(ARG_NAME, "")
            avatarUrl = it.getString(ARG_AVATAR_URL, "")
            darkLayout = it.getBoolean(ARG_DARK_LAYOUT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_user_info, container, false)

        (view!!.findViewById<View>(R.id.userDataTextViewOptional) as TextView).text = username
        if (!TextUtils.isEmpty(name)) {
            (view.findViewById<View>(R.id.userDataTextView) as TextView).text = name
        } else {
            (view.findViewById<View>(R.id.userDataTextView) as TextView).text = username
            (view.findViewById<View>(R.id.userDataTextViewOptional) as TextView).visibility = View.GONE
        }
        if (darkLayout) {
            (view.findViewById<View>(R.id.userDataTextViewOptional) as TextView).setTextColor(Color.WHITE)
            (view.findViewById<View>(R.id.userDataTextView) as TextView).setTextColor(Color.WHITE)
        }
        if (!TextUtils.isEmpty(avatarUrl)) {
            Picasso.get().load(avatarUrl).into((view.findViewById<View>(R.id.avatarView) as ImageView))
        } else {
            (view.findViewById<View>(R.id.avatarView) as ImageView).setImageResource(R.drawable.default_avatar)
        }

        view.setSafeOnClickListener {
            val contact = JSONObject()
            contact.put("username", username)
            contact.put("name", name)
            contact.put("avatarUrl", avatarUrl)
            listener?.onFragmentClick(ContactModel(contact))
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentClick(model: ContactModel)
    }

    companion object {
        @JvmStatic
        fun newInstance(username: String, name: String?, avatarUrl: String?, darkLayout: Boolean = false) =
                UserInfoFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_USERNAME, username)
                        putString(ARG_NAME, name)
                        putString(ARG_AVATAR_URL, avatarUrl)
                        putBoolean(ARG_DARK_LAYOUT, darkLayout)
                    }
                }
    }
}
