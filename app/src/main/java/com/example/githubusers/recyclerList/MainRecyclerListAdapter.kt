package com.example.githubusers.recyclerList

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
    // Це список, який буде використовуватися для відновлення списку користувачів після виходу з режиму пошуку.
    private var defaultList: List<User?> = emptyList()
    // Ініціюємо список
    init {
        submitList(defaultList)
    }
    // Переписуємо цей метод так, щоб якщо тип представлення 0, то створюємо елемент з завантаженням,
    // а якщо ні, то звичайний елемент з фотографією і логіном користувача.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        return  if(viewType == 0) {
            // Елемент, що демонструє завантаження
            MainViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.main_recycler_list_load_item, parent, false))
        }
        else {
            // Звичайний елемент списку
            MainViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.main_recycler_list_item, parent, false))
        }
    }

    /** Отримуємо кількість елементів даного списку (навіть в режимі пошуку) */
    override fun getItemCount(): Int {
        return currentList.size
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        // Якщо останій елемент пустий, то завантажуємо нові.
        if(currentList[position] == null) {
            viewModel.getMoreUsers(whereAssign =  {
                add(it)
            },  // Якщо відбудеться помилка, то після вирішення проблеми спробуємо завантажити знову.
                ifFail = {
                viewModel.loadMoreUsersCallback = {
                    viewModel.getMoreUsers(whereAssign = { nu ->
                        add(nu)
                    })
                }
            })
        }
        // Заповнюємо елементи списку даними.
        currentList.let {
            val user = it[position]
            if(user != null) {
                holder.run {
                    // Логін користувача.
                    username?.text = user.login
                    // Зображення користувача.
                    Picasso.get()
                        .load(user.avatarUrl)
                        .resize(200, 200)
                        .into(avatar)
                    //      Робимо його видимим.
                    itemView.setOnClickListener {
                        viewModel.fragmentManager?.beginTransaction()
                            ?.replace(R.id.mainContainer, ProfileFragment(user.login))
                            ?.commit()
                    }
                    // Перевіряємо чи є нотатки про даного користувача.
                    val notesForCheck = viewModel.database?.getNoteDao()?.getNotesByLogin(user.login)
                    // Якщо є, то робимо значок нотатків видимим.
                    if(!notesForCheck.isNullOrEmpty()) {
                        notesIcon?.visibility = View.VISIBLE
                        if(viewModel.isDarkTheme.value) {
                            notesIcon?.setImageResource(R.drawable.notes_icon_white)
                        }
                    }
                    // Якщо ні, то невидимим.
                    else {
                        notesIcon?.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    /** Якщо елемент пустий, то тип представлення 0, а якщо ні, то 1. */
    override fun getItemViewType(position: Int) = if(currentList[position] == null) 0 else 1

    /** Виконує додавання елементів до списку. Має два параметри: submit та checkIfLessThen30.
     * @param newUsers список елементів, які потрібно додати.
     * @param submit запитує, чи потрібно оновлювати список і на екрані.
     * @param checkIfLessThen30 запитує, чи потрібно перевіряти, щоб розмір доданих елементів був рівний або більше ніж 30.
    */
    fun add(newUsers: List<User>, submit: Boolean = true, checkIfLessThen30: Boolean = true) {
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

    /**
     * Виконує додавання до [defaultList] елементи. Якщо [checkIfLessThen30] = true, то перевіряє чи кількість елементів більше або рівне 30.
     * @param users користувачі, котрі необхідно додати.
     * @param checkIfLessThen30 запитує, чи потрібно перевіряти, щоб розмір доданих елементів був рівний або більше ніж 30.
     */
    private fun addNullIfSize30(users: List<User>, checkIfLessThen30: Boolean) =
        if(users.size >= 30 && !viewModel.isSearchMode.value || !checkIfLessThen30)
            users.filter {
                !defaultList.contains(it)
            } + null
        else
            users.filter {
                !defaultList.contains(it)
            }

    /**
     * Повертає значення currentList до значення [defaultList]
     */
    fun setDefaultList() {
        submitList(defaultList)
    }

    /**
     * Під час режиму пошуку, фільтрує елементи, які не підходять під [query].
     * В цьому методі виконується лише перевірка чи [query] дорівнює null або пустий. Якщо так, то повертає [defaultList], а якщо ні, то виконується метод [filterList]
     * @param query рядок, по якому потрібно виконувати перевірку.
     */
    fun filter(query: String?) {
        val action: suspend () -> Unit = {
            // Перероблюємо в нижній регістр, щоб перевіряти незважаючи на нього.
            val lowerQuery = query?.lowercase()
            // Отримуємо дао об'єкт.
            val notesDao = viewModel.database?.getNoteDao()
            // Якщо query пустий, то повертаємо defaultList, інакше переходимо в filterList.
            submitList(if(lowerQuery.isNullOrEmpty()) {
                defaultList
            }
            else {
                filterList(lowerQuery, notesDao)
            })

        }
        // Це щоб виконувати ассинхронний код (отримання з БД користувачів, пошук користувача за login та інше).
        viewModel.lifecycleScope?.launch {
            action()
        }
    }

    /**
     * Метод виконує фільтрування списку у випадку, якщо [query] не пустий і не порожній.
     * Складається з декількох етапів:
     * 1) пошук користувачів в [defaultList];
     * 2) перевірка [notesDao] на порожність;
     * 3) пошук логінів, які потрібно довантажити;
     * 4) довантаження потрібних користувачів;
     * 5) пошук користувача за точним співпадінням логіну і [query] (якщо такого немає в [defaultList] з самого початку).
     *
     * Якщо [notesDao] порожній, то відбувається перехід до етапу №5.
     * @param query запит-рядок.
     * @param notesDao дао об'єкт для пошуку користувачів, якіх потрібно довантажити, з БП.
     * @return Список користувачів, який може бути порожнім, якщо немає нікого, хто б відповідав [query].
     */
    private suspend fun filterList(query: String, notesDao: NotesDao?): List<User?> {
        // Етап 1
        var temp = defaultList.filter {
            it != null && it.login.lowercase().contains(query)
        }
        // Етап 2
        if (notesDao != null) {
            // Етап 3
            var tookNotes = notesDao.getNotesByQuery(query)
            val loginFromNotes = tookNotes.map { it.login }
            val needLoadUsers = loginFromNotes.filter { s ->
                defaultList.none { u ->
                    u?.login == s
                }
            }
            // Етап 4
            if (needLoadUsers.isNotEmpty()) {
                val loadedUsers = viewModel.getUsersByLogins(needLoadUsers).filterNotNull()
                add(loadedUsers, submit = false, checkIfLessThen30 = false)
                temp = temp + loadedUsers.filter {
                    !temp.contains(it)
                }
            }
            tookNotes = tookNotes.filter {
                needLoadUsers.none { s ->
                    s == it.login
                }
            }
            temp = temp + defaultList.filter {
                it != null && tookNotes.any { n ->
                    it.login == n.login && !temp.contains(it)
                }
            }
        }
        // Етап 5
        if(temp.none {query == it?.login
                    || it?.login?.lowercase()?.startsWith(query) == true}) {
            val newUser = tryGetUserByProfile(query)
            if(newUser != null && !temp.contains(newUser)) temp = temp + newUser
        }
        return temp
    }

    /**
     * Метод намагається отримати користувача по рядку запиту, який ввів користувач.
     * @param query рядок-запит користувача.
     * @return [User], якщо такий користувач знайдений і null - якщо ні.
     */
    private suspend fun tryGetUserByProfile(query: String): User? {
        val user = viewModel.getUserProfile(query)
        return user?.toUser()
    }

    /**
     * Призначений для утримання необхідних елементів списку користувачів.
     */
    class MainViewHolder(view: View) : ViewHolder(view) {
        val username: TextView? = view.findViewById(R.id.userUsername)
        val avatar: ImageView? = view.findViewById(R.id.userAvatar)
        val notesIcon: ImageView? = view.findViewById(R.id.notesIcon)
    }
}