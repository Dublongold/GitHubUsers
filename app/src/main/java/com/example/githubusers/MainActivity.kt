package com.example.githubusers

import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.githubusers.interfaces.CanLoadData
import com.example.githubusers.viewModels.MainViewModel
import com.example.githubusers.views.MainFragment
import com.example.githubusers.views.ProfileFragment

class MainActivity : AppCompatActivity() {

    val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.lifecycleScope = lifecycleScope
        viewModel.fragmentManager = supportFragmentManager
        viewModel.database = (application as MainApplication).database

        val uiModeNightMask = resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)
        viewModel.setDarkTheme(uiModeNightMask == Configuration.UI_MODE_NIGHT_YES)
        supportFragmentManager.beginTransaction()
            .add(R.id.mainContainer, MainFragment())
            .commit()

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val networkCallback = object: ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                Log.i("Network", "Start available")
                super.onAvailable(network)
                viewModel.setNetworkState(false)
                if(supportFragmentManager.fragments.size > 0) {
                    val firstFragment: CanLoadData =
                        supportFragmentManager.fragments.first() as CanLoadData
                    if (firstFragment.failedLoad) {
                        Log.i("Network", "Fragment say what load is failed.")
                        firstFragment.loadData()
                    }
                }
                viewModel.loadMoreUsersCallback?.invoke()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                viewModel.setNetworkState(true)
            }
        }

        getSystemService(ConnectivityManager::class.java).registerNetworkCallback(
            networkRequest, networkCallback
        )

        onBackPressedDispatcher.addCallback {
            if(supportFragmentManager.fragments.first() is ProfileFragment) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.mainContainer, MainFragment())
                    .commit()
            }
            else {
                finish()
            }
        }
    }
}