package com.example.githubusers.recyclerList

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.githubusers.R
import com.example.githubusers.models.User
import com.example.githubusers.models.databases.daos.NotesDao
import com.example.githubusers.viewModels.MainViewModel
import com.example.githubusers.views.ProfileFragment
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class MainRecyclerListAdapter(private val viewModel: MainViewModel):
    ListAdapter<User?, MainRecyclerListAdapter.MainViewHolder>(
    object : DiffUtil.ItemCallback<User?>() {
        override fun areItemsTheSame(
            oldItem: User, newItem: User,
        ): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(
            oldItem: User, newItem: User,
        ): Boolean =
            oldItem.id == newItem.id && oldItem.login == newItem.login

    }
    ) {

    private var defaultList: List<User?> = emptyList()

    init {
        submitList(defaultList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        return  if(viewType == 0) {
            MainViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.main_recycler_list_load_item, parent, false))
        }
        else {
            MainViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.main_recycler_list_item, parent, false))
        }
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        if(currentList[position] == null) {
            viewModel.getMoreUsers(whereAssign =  {
                add(it)
            }, ifFail = {
                viewModel.loadMoreUsersCallback = {
                    viewModel.getMoreUsers(whereAssign = { nu ->
                        add(nu)
                    })
                }
            })
        }
        currentList.let {
            val user = it[position]
            if(user != null) {
                holder.run {
                    username?.text = user.login
                    Picasso.get()
                        .load(user.avatarUrl)
                        .resize(200, 200)
                        .into(avatar)
                    itemView.setOnClickListener {
                        viewModel.fragmentManager?.beginTransaction()
                            ?.replace(R.id.mainContainer, ProfileFragment(user.login))
                            ?.commit()
                    }
                    val notesForCheck = viewModel.database?.getNoteDao()?.getNotesByLogin(user.login)
                    if(!notesForCheck.isNullOrEmpty()) {
                        notesIcon?.visibility = View.VISIBLE
                        if(viewModel.isDarkTheme.value) {
                            notesIcon?.setImageResource(R.drawable.notes_icon_white)
                        }
                    }
                    else {
                        notesIcon?.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int) = if(currentList[position] == null) 0 else 1


    fun add(newUsers: List<User>, submit: Boolean = true, checkIfLessThen30: Boolean = true) {
        Log.i("Add users", "New users count: ${newUsers.size}")
        defaultList = if(newUsers.isNotEmpty()) {
            if(itemCount > 2) {
                defaultList.subList(0, itemCount-1) + addNullIfSize30(newUsers, checkIfLessThen30)
            }
            else {
                defaultList + addNullIfSize30(newUsers, checkIfLessThen30)
            }
        }
        else {
            defaultList
        }
        if(submit) {
            submitList(defaultList)
        }
    }

    private fun addNullIfSize30(users: List<User>, checkIfLessThen30: Boolean) =
        if(users.size == 30 && !viewModel.isSearchMode.value || !checkIfLessThen30)
            users.filter {
                !defaultList.contains(it)
            } + null
        else
            users.filter {
                !defaultList.contains(it)
            }

    fun setDefaultList() {
        submitList(defaultList)
    }

    fun filter(query: String?) {
        val action: suspend () -> Unit = {
            Log.i("Search filter", "Start")
            val lowerQuery = query?.lowercase()
            val notesDao = viewModel.database?.getNoteDao()
            submitList(if(lowerQuery.isNullOrEmpty()) {
                defaultList
            }
            else {
                filterList(lowerQuery, notesDao)
            })

        }
        viewModel.lifecycleScope?.launch {
            action()
        }
    }

    private suspend fun filterList(query: String, notesDao: NotesDao?): List<User?> {
        var temp = defaultList.filter {
            it != null && it.login.lowercase().contains(query)
        }
        if (notesDao != null) {
            var tookNotes = notesDao.getNotesByQuery(query)
            val usernamesFromNotes = tookNotes.map { it.login }
            val needLoadUsers = usernamesFromNotes.filter { s ->
                defaultList.none { u ->
                    u?.login == s
                }
            }
            if (needLoadUsers.isNotEmpty()) {
                val loadedUsers = viewModel.getUsersByLogins(needLoadUsers).filterNotNull()
                add(loadedUsers, submit = false, checkIfLessThen30 = false)
                temp = temp + loadedUsers
            }
            tookNotes = tookNotes.filter {
                needLoadUsers.none { s ->
                    s == it.login
                }
            }
            temp = temp + defaultList.filter {
                it != null && tookNotes.any { n ->
                    it.login == n.login
                }
            }
        }
        if(temp.none {query == it?.login
                    || it?.login?.startsWith(query) == true}) {
            val newUser = tryGetUserByProfile(query)
            if(newUser != null) temp = temp + newUser
        }
        return temp
    }

    private suspend fun tryGetUserByProfile(query: String): User? {
        val user = viewModel.getUserProfile(query)
        return user?.toUser()
    }

    class MainViewHolder(view: View) : ViewHolder(view) {
        val username: TextView? = view.findViewById(R.id.userUsername)
        val avatar: ImageView? = view.findViewById(R.id.userAvatar)
        val notesIcon: ImageView? = view.findViewById(R.id.notesIcon)
    }
}