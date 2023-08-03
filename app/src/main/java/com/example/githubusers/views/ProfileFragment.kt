package com.example.githubusers.views

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.githubusers.MainActivity
import com.example.githubusers.R
import com.example.githubusers.interfaces.CanLoadData
import com.example.githubusers.models.Note
import com.squareup.picasso.Picasso
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ProfileFragment(private val username: String): Fragment(), CanLoadData {
    val Int.dp: Int
        get() = (this * resources.displayMetrics.density + 0.5f).toInt()

    override var failedLoad: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.profile_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainActivity = activity as MainActivity

        // Change go to list imageButton image to match the current theme.
         lifecycleScope.launch {
             repeatOnLifecycle(Lifecycle.State.STARTED) {
                 mainActivity.viewModel.isDarkTheme.collect {
                     if(it) {
                         view.findViewById<ImageButton>(R.id.goToUsers)
                             .setImageResource(R.drawable.go_back_button_white)
                     }
                     else {
                         view.findViewById<ImageButton>(R.id.goToUsers)
                             .setImageResource(R.drawable.go_back_button)
                     }
                 }
             }
         }

        // Network lost icon
        val connectionLostIcon = view.findViewById<ImageView>(R.id.connectionLostIconInProfile)
        lifecycleScope.launch {
            // Set color of internet connection lost icon to match the theme.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainActivity.viewModel.isDarkTheme.collect {
                    if(it) {
                        connectionLostIcon.setImageResource(R.drawable.connection_lost_white)
                    }
                }
            }
        }
        lifecycleScope.launch {
            // Set visibility of internet connection lost icon depend on networkLost.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainActivity.viewModel.networkLost.collect {
                    if(it) {
                        connectionLostIcon.visibility = View.VISIBLE
                    }
                    else {
                        connectionLostIcon.visibility = View.GONE
                    }
                }
            }
        }
        // Set username text
        view.findViewById<TextView>(R.id.userProfileLogin).text = username

        // Set action for go to user imageButton.
        view.findViewById<ImageButton>(R.id.goToUsers).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainContainer, MainFragment())
                .commit()
        }
        // To wait for user profile data to load.
        loadData()
    }

    override fun loadData() {
        lifecycleScope.launch {
            val mainActivity = activity as MainActivity
            val user = mainActivity.viewModel.getUserProfile(username)
            if(user != null) {
                failedLoad = false
                view?.run {
                    findViewById<View>(R.id.profileSecondSeparator).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.profileFollowers).let {t ->
                        t.text = getString(R.string.profile_followers, user.followers.toString())
                        t.visibility = View.VISIBLE
                    }
                    findViewById<TextView>(R.id.profileFollowing).let {t ->
                        t.text = getString(R.string.profile_following, user.following.toString())
                        t.visibility = View.VISIBLE
                    }
                    findViewById<LinearLayout>(R.id.userProfileData).let {ll ->
                        var userData: Map<String, String?> = mapOf(
                         "userName" to getString(R.string.profile_name, user.name, user.twitterUsername),
                         "userCompany" to getString(R.string.profile_company, user.company),
                         "userBlog" to getString(R.string.profile_blog, user.blog),
                         "userLocation" to getString(R.string.profile_location, user.location),
                         "userCreatedAt" to getString(R.string.profile_created_at, user.createdAt)
                        )

                        userData = userData.mapValues {
                            if(it.key == "userName") {
                                if(user.name == null || user.twitterUsername == null) {
                                    val userName =
                                        if (user.name.isNullOrEmpty()) getString(R.string.not_specified) else user.name
                                    val userTwitter = if (user.twitterUsername.isNullOrEmpty()) getString(R.string.not_specified) else user.twitterUsername
                                    getString(R.string.profile_name, userName, userTwitter)
                                }
                                else {
                                    it.value
                                }
                            }
                            else {
                                if (!it.value.isNullOrEmpty()) {
                                    it.value
                                } else {
                                    getString(R.string.not_specified)
                                }
                            }
                        }



                        ll.findViewById<TextView>(R.id.profileUserName,).text = userData["userName"]
                        ll.findViewById<TextView>(R.id.profileUserCompany).text = userData["userCompany"]
                        ll.findViewById<TextView>(R.id.profileUserBlog).text = userData["userBlog"]
                        ll.findViewById<TextView>(R.id.profileUserLocation).text = userData["userLocation"]
                        ll.findViewById<TextView>(R.id.profileUserCreatedAt).text = userData["userCreatedAt"]
                        ll.visibility = View.VISIBLE
                    }
                    findViewById<TextView>(R.id.notesText).visibility = View.VISIBLE
                    val notesTextLayout =  findViewById<EditText>(R.id.notesEditText).also { et ->
                        et.visibility = View.VISIBLE
                        val text = mainActivity.viewModel.database?.getNoteDao()?.getNotesByLogin(user.login)?.firstOrNull()?.text
                        et.text.append(text ?: "")
                    }
                    findViewById<Button>(R.id.saveNotesButton).let {b ->
                        b.visibility = View.VISIBLE
                        b.setOnClickListener {
                            lifecycleScope.launch {
                                val databaseDao = mainActivity.viewModel.database?.getNoteDao()
                                if(databaseDao != null) {
                                    val notes = databaseDao.getNotesByLogin(username)
                                    if(notes.isEmpty()) {
                                        databaseDao.insert(Note(username, notesTextLayout.text.toString()))
                                    }
                                    else {
                                        databaseDao.update(Note(username, notesTextLayout.text.toString()))
                                    }
                                }
                                Toast.makeText(context, "Notes saved!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    findViewById<ImageView>(R.id.userProfileAvatar).let {
                        Picasso
                            .get()
                            .load(user.avatarUrl)
                            .into(it)
                        it.visibility = View.VISIBLE
                    }

                    findViewById<ProgressBar>(R.id.profileProgressBar).visibility = View.GONE
                }
            }
            else {
                Toast.makeText(context, "Unable to load user data.", Toast.LENGTH_SHORT).show()
                failedLoad = true
                mainActivity.viewModel.setNetworkState(true)
            }
        }
    }
}