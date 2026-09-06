package com.example.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.core.model.NetworkType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Continuous, event-driven network type monitor using ConnectivityManager.NetworkCallback (Section 1).
 * Avoids periodic manual polling and guarantees seamless transport switches during active transfers.
 */
class NetworkTracker(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _activeNetworkType = MutableStateFlow(determineCurrentNetwork())
    val activeNetworkType: StateFlow<NetworkType> = _activeNetworkType.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateNetworkType(networkCapabilities)
        }

        override fun onLost(network: Network) {
            _activeNetworkType.value = determineCurrentNetwork()
        }

        override fun onAvailable(network: Network) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            updateNetworkType(capabilities)
        }
    }

    private var isRegistered = false

    fun startListening() {
        if (isRegistered) return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isRegistered = true
        } catch (_: Exception) {
            // Graceful fallback to default query
        }
        _activeNetworkType.value = determineCurrentNetwork()
    }

    fun stopListening() {
        if (!isRegistered) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        } finally {
            isRegistered = false
        }
    }

    private fun updateNetworkType(capabilities: NetworkCapabilities?) {
        val newType = when {
            capabilities == null -> NetworkType.NONE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.WIFI
            else -> NetworkType.NONE
        }
        _activeNetworkType.value = newType
    }

    private fun determineCurrentNetwork(): NetworkType {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.WIFI
            else -> NetworkType.NONE
        }
    }
}
