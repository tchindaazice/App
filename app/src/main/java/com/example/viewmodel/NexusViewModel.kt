package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ApprovalStatus
import com.example.data.model.CommunicationChannel
import com.example.data.model.ConnectionStats
import com.example.data.model.Country
import com.example.data.model.DeviceInfo
import com.example.data.model.LogEntry
import com.example.data.model.LogLevel
import com.example.data.model.RegistrationRequest
import com.example.data.model.UserSession
import com.example.data.model.VpnPreferences
import com.example.data.model.VpnStatus
import com.example.data.repository.NexusRepository
import com.example.vpn.NexusVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab {
    DASHBOARD,
    LOGS,
    SETTINGS
}

data class NexusUiState(
    val userSession: UserSession? = null,
    val savedRegistration: RegistrationRequest? = null,
    val approvalStatus: ApprovalStatus = ApprovalStatus.NOT_SUBMITTED,
    val vpnStatus: VpnStatus = VpnStatus.DISCONNECTED,
    val stats: ConnectionStats = ConnectionStats(),
    val logs: List<LogEntry> = emptyList(),
    val preferences: VpnPreferences = VpnPreferences(),
    val serverConfig: com.example.data.model.DynamicServerConfig? = null,
    val serverUrl: String = "https://ais-dev-kbhcngbmjxsdibapd4euvg-354186393206.us-west1.run.app/",
    val deviceInfo: DeviceInfo,
    val isSubmitting: Boolean = false,
    val isCheckingStatus: Boolean = false,
    val isSyncingAccount: Boolean = false,
    val feedbackMessage: String? = null,
    val serverDisconnectAlert: String? = null,
    val activeTab: MainTab = MainTab.DASHBOARD
)

class NexusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NexusRepository(application)

    private val _uiState = MutableStateFlow(
        NexusUiState(
            userSession = repository.getUserSession(),
            savedRegistration = repository.getSavedRegistration(),
            approvalStatus = repository.getApprovalStatus(),
            preferences = repository.getPreferences(),
            serverConfig = repository.getDynamicServerConfig(),
            serverUrl = repository.getServerUrl(),
            deviceInfo = repository.getDeviceInfo()
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        // Observe VPN connection state
        viewModelScope.launch {
            NexusVpnService.vpnStatus.collect { status ->
                _uiState.update { it.copy(vpnStatus = status) }
                // Refresh quota whenever vpn state transitions
                val currentSession = repository.getUserSession()
                if (currentSession != null) {
                    _uiState.update { it.copy(userSession = currentSession) }
                }
            }
        }

        // Observe live real connection metrics
        viewModelScope.launch {
            NexusVpnService.stats.collect { stats ->
                val currentSession = repository.getUserSession()
                _uiState.update { it.copy(stats = stats, userSession = currentSession ?: it.userSession) }
            }
        }

        // Observe connection logs
        viewModelScope.launch {
            NexusVpnService.logsFlow.collect { entry ->
                _uiState.update { current ->
                    current.copy(logs = (listOf(entry) + current.logs).take(150))
                }
            }
        }

        // Observe remote disconnect events from Web Panel
        viewModelScope.launch {
            NexusVpnService.disconnectEvent.collect { reason ->
                _uiState.update {
                    it.copy(
                        serverDisconnectAlert = reason,
                        feedbackMessage = reason
                    )
                }
            }
        }

        NexusVpnService.log(LogLevel.INFO, "[CLIENT] Moteur Nexus Tunnel prêt. Protection active.")
    }

    fun submitRegistration(
        fullName: String,
        country: Country,
        channel: CommunicationChannel,
        contactDetail: String
    ) {
        val trimmedName = fullName.trim()
        val trimmedContact = contactDetail.trim()

        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Veuillez entrer votre nom complet.") }
            return
        }
        if (trimmedContact.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Veuillez renseigner votre moyen de communication.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val req = RegistrationRequest(
                fullName = trimmedName,
                country = country,
                channel = channel,
                contactDetail = trimmedContact,
                deviceInfo = repository.getDeviceInfo()
            )
            val result = repository.submitAccessRequest(req)
            val updatedStatus = repository.getApprovalStatus()
            val updatedSession = repository.getUserSession()

            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    savedRegistration = req,
                    approvalStatus = updatedStatus,
                    userSession = updatedSession,
                    feedbackMessage = result.getOrNull() ?: "Demande envoyée au Web Panel."
                )
            }
            NexusVpnService.log(LogLevel.SUCCESS, "[DEMANDE] Transmise pour $trimmedName")
        }
    }

    fun checkApprovalStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingStatus = true) }
            val result = repository.checkApprovalStatus()
            val status = result.getOrDefault(repository.getApprovalStatus())
            val session = repository.getUserSession()

            _uiState.update {
                it.copy(
                    isCheckingStatus = false,
                    approvalStatus = status,
                    userSession = session,
                    feedbackMessage = when (status) {
                        ApprovalStatus.APPROVED -> "Accès validé par l'administrateur ! Profil activé."
                        ApprovalStatus.REJECTED -> "Demande refusée par l'administrateur."
                        ApprovalStatus.PENDING -> "Demande en cours d'examen par le Web Panel."
                        ApprovalStatus.NOT_SUBMITTED -> "Aucune demande en cours."
                    }
                )
            }
        }
    }

    fun activateToken(rawToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = repository.activateToken(rawToken)
            result.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        userSession = session,
                        approvalStatus = ApprovalStatus.APPROVED,
                        feedbackMessage = "Token vérifié avec succès ! Quota et accès activés."
                    )
                }
                NexusVpnService.log(LogLevel.SUCCESS, "[AUTH] Token activé pour ${session.fullName}")
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        feedbackMessage = err.message ?: "Échec de l'activation du Token."
                    )
                }
            }
        }
    }

    // Three dots menu: "Actualiser le compte" (Sync quota, expiration & dynamic config from Web Panel)
    fun syncAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingAccount = true) }
            val res = repository.syncAccountWithServer()
            res.onSuccess { updated ->
                _uiState.update {
                    it.copy(
                        isSyncingAccount = false,
                        userSession = updated,
                        feedbackMessage = "Compte synchronisé avec le Web Panel !"
                    )
                }
                NexusVpnService.log(LogLevel.SUCCESS, "[SYNC] Quota et configuration mis à jour depuis le serveur.")
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isSyncingAccount = false,
                        feedbackMessage = "Impossible de contacter le Web Panel."
                    )
                }
            }
        }
    }

    fun toggleVpnConnection(context: Context) {
        val current = _uiState.value.vpnStatus
        if (current == VpnStatus.CONNECTED || current == VpnStatus.CONNECTING) {
            NexusVpnService.stop(context)
        } else {
            NexusVpnService.start(context)
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun updatePreferences(newPrefs: VpnPreferences) {
        repository.savePreferences(newPrefs)
        _uiState.update { it.copy(preferences = newPrefs, feedbackMessage = "Préférences enregistrées.") }
        NexusVpnService.log(LogLevel.INFO, "[CONFIG] Paramètres réseau mis à jour.")
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun logout() {
        NexusVpnService.stop(getApplication())
        repository.clearSessionAndLogout()
        _uiState.update {
            it.copy(
                userSession = null,
                savedRegistration = null,
                approvalStatus = ApprovalStatus.NOT_SUBMITTED,
                feedbackMessage = "Session réinitialisée."
            )
        }
        NexusVpnService.log(LogLevel.WARN, "[LOGOUT] Profil déconnecté.")
    }

    fun importVlessConfig(rawInput: String) {
        val result = repository.importVlessUri(rawInput)
        result.onSuccess { config ->
            val updatedSession = repository.getUserSession()
            _uiState.update {
                it.copy(
                    serverConfig = config,
                    userSession = updatedSession,
                    approvalStatus = ApprovalStatus.APPROVED,
                    feedbackMessage = "Configuration VLESS importée avec succès !"
                )
            }
            NexusVpnService.log(LogLevel.SUCCESS, "[CONFIG] Configuration VLESS active : ${config.serverAddress}:${config.serverPort} (SNI: ${config.sni})")
        }.onFailure { err ->
            _uiState.update {
                it.copy(feedbackMessage = err.message ?: "Format VLESS invalide.")
            }
        }
    }

    fun saveServerConfig(newConfig: com.example.data.model.DynamicServerConfig) {
        repository.saveDynamicServerConfig(newConfig)
        _uiState.update {
            it.copy(
                serverConfig = newConfig,
                feedbackMessage = "Configuration serveur VLESS enregistrée."
            )
        }
        NexusVpnService.log(LogLevel.INFO, "[CONFIG] Serveur VLESS mis à jour : ${newConfig.serverAddress}:${newConfig.serverPort}")
    }

    fun updateServerUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank() || (!trimmed.startsWith("http://") && !trimmed.startsWith("https://"))) {
            _uiState.update { it.copy(feedbackMessage = "URL invalide (doit commencer par http:// ou https://)") }
            return
        }
        repository.updateServerUrl(trimmed)
        _uiState.update {
            it.copy(
                serverUrl = repository.getServerUrl(),
                feedbackMessage = "URL du serveur Web Panel enregistrée avec succès."
            )
        }
        NexusVpnService.log(LogLevel.SUCCESS, "[CONFIG] URL Web Panel mise à jour : ${repository.getServerUrl()}")
    }

    fun bypassToDashboard() {
        val currentSession = repository.getUserSession()
        val devInfo = repository.getDeviceInfo()
        val session = currentSession ?: com.example.data.model.UserSession(
            fullName = "Membre Nexus VLESS",
            countryName = "Afrique",
            countryCode = "CM",
            countryFlag = "🇨🇲",
            channel = com.example.data.model.CommunicationChannel.WHATSAPP,
            contactDetail = "",
            uid = "nx_uid_${devInfo.deviceId.takeLast(6).lowercase()}",
            token = "direct_access_token",
            approvalStatus = ApprovalStatus.APPROVED,
            quotaTotalBytes = 10L * 1024 * 1024 * 1024,
            quotaUsedBytes = 0L,
            expirationTimestamp = System.currentTimeMillis() + (30L * 24 * 3600 * 1000),
            registeredDeviceId = devInfo.deviceId,
            registeredAt = System.currentTimeMillis()
        )
        repository.saveUserSession(session)
        val serverConfig = repository.getDynamicServerConfig()
        _uiState.update {
            it.copy(
                userSession = session,
                serverConfig = serverConfig,
                approvalStatus = ApprovalStatus.APPROVED,
                feedbackMessage = "Mode VLESS actif. Vous pouvez vous connecter !"
            )
        }
        NexusVpnService.log(LogLevel.SUCCESS, "[MODE] Accès direct au tableau de bord VPN activé.")
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun dismissDisconnectAlert() {
        _uiState.update { it.copy(serverDisconnectAlert = null) }
    }
}
