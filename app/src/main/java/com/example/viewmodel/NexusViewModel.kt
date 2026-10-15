package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AfricanCountries
import com.example.data.model.ApprovalStatus
import com.example.data.model.CommunicationChannel
import com.example.data.model.ConnectionStats
import com.example.data.model.Country
import com.example.data.model.DeviceInfo
import com.example.data.model.LogEntry
import com.example.data.model.LogLevel
import com.example.data.model.RegistrationRequest
import com.example.data.model.UserSession
import com.example.data.model.VpnConfig
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
    val config: VpnConfig = VpnConfig(),
    val deviceInfo: DeviceInfo,
    val isSubmitting: Boolean = false,
    val isCheckingStatus: Boolean = false,
    val feedbackMessage: String? = null,
    val activeTab: MainTab = MainTab.DASHBOARD
)

class NexusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NexusRepository(application)

    private val _uiState = MutableStateFlow(
        NexusUiState(
            userSession = repository.getUserSession(),
            savedRegistration = repository.getSavedRegistration(),
            approvalStatus = repository.getApprovalStatus(),
            config = repository.getVpnConfig(),
            deviceInfo = repository.getDeviceInfo()
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        // Observe VPN status from service
        viewModelScope.launch {
            NexusVpnService.vpnStatus.collect { status ->
                _uiState.update { it.copy(vpnStatus = status) }
            }
        }

        // Observe VPN live metrics
        viewModelScope.launch {
            NexusVpnService.stats.collect { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }

        // Observe VPN logs
        viewModelScope.launch {
            NexusVpnService.logsFlow.collect { entry ->
                _uiState.update { current ->
                    current.copy(logs = (listOf(entry) + current.logs).take(150))
                }
            }
        }

        // Real-time quota consumption from VPN traffic
        viewModelScope.launch {
            NexusVpnService.consumedBytesFlow.collect { addedBytes ->
                repository.updateQuotaConsumed(addedBytes)
                val updatedSession = repository.getUserSession()
                if (updatedSession != null) {
                    _uiState.update { it.copy(userSession = updatedSession) }
                }
            }
        }

        // Add initial system log
        NexusVpnService.log(LogLevel.INFO, "[SYSTEM] Nexus Tunnel Client démarré. Prêt.")
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
            val result = repository.submitMembershipRequest(req)
            val updatedStatus = repository.getApprovalStatus()
            val updatedSession = repository.getUserSession()

            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    savedRegistration = req,
                    approvalStatus = updatedStatus,
                    userSession = updatedSession,
                    feedbackMessage = result.getOrNull() ?: "Demande envoyée."
                )
            }
            NexusVpnService.log(LogLevel.SUCCESS, "[REGISTER] Demande soumise pour $trimmedName ($trimmedContact)")
        }
    }

    fun checkApprovalStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingStatus = true) }
            val result = repository.checkMembershipStatus()
            val status = result.getOrDefault(repository.getApprovalStatus())
            val session = repository.getUserSession()

            _uiState.update {
                it.copy(
                    isCheckingStatus = false,
                    approvalStatus = status,
                    userSession = session,
                    feedbackMessage = when (status) {
                        ApprovalStatus.APPROVED -> "Félicitations ! Votre demande a été approuvée par l'administrateur !"
                        ApprovalStatus.REJECTED -> "Votre demande a été refusée par l'administrateur."
                        ApprovalStatus.PENDING -> "Demande toujours en attente de validation."
                        ApprovalStatus.NOT_SUBMITTED -> "Aucune demande en cours."
                    }
                )
            }
        }
    }

    fun activateToken(rawToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = repository.verifyAndActivateToken(rawToken)
            result.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        userSession = session,
                        approvalStatus = ApprovalStatus.APPROVED,
                        feedbackMessage = "Token activé avec succès ! Quota et UID attribués."
                    )
                }
                NexusVpnService.log(LogLevel.SUCCESS, "[AUTH] Token activé : UID ${session.uid}")
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

    fun simulateAdminApproval() {
        val session = repository.simulateAdminApprovalForTesting()
        _uiState.update {
            it.copy(
                userSession = session,
                approvalStatus = ApprovalStatus.APPROVED,
                feedbackMessage = "Simulation : Adhésion validée par l'administrateur !"
            )
        }
        NexusVpnService.log(LogLevel.SUCCESS, "[ADMIN SIMULATION] Adhésion approuvée avec UID: ${session.uid}")
    }

    fun toggleVpnConnection(context: Context) {
        val current = _uiState.value.vpnStatus
        if (current == VpnStatus.CONNECTED || current == VpnStatus.CONNECTING) {
            NexusVpnService.stop(context)
        } else {
            val session = _uiState.value.userSession
            val config = _uiState.value.config
            val uid = session?.uid ?: "default_uid"
            val host = config.customSni.ifBlank { "nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio" }

            NexusVpnService.start(
                context = context,
                host = host,
                uid = uid,
                transport = config.transport
            )
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun updateConfig(newConfig: VpnConfig) {
        repository.saveVpnConfig(newConfig)
        if (newConfig.webPanelUrl != repository.getBaseUrl()) {
            repository.updateBaseUrl(newConfig.webPanelUrl)
        }
        _uiState.update { it.copy(config = newConfig, feedbackMessage = "Configuration enregistrée.") }
        NexusVpnService.log(LogLevel.INFO, "[CONFIG] Paramètres VLESS mis à jour.")
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun logout() {
        repository.clearSessionAndLogout()
        _uiState.update {
            it.copy(
                userSession = null,
                approvalStatus = ApprovalStatus.NOT_SUBMITTED,
                feedbackMessage = "Déconnexion réussie."
            )
        }
        NexusVpnService.log(LogLevel.WARN, "[LOGOUT] Profil réinitialisé.")
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
