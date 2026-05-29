package ch.rhosys.lyra.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import ch.rhosys.lyra.data.notification.NotificationEventBus
import ch.rhosys.lyra.domain.model.NotificationEvent
import ch.rhosys.lyra.domain.usecase.ProcessNotificationUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class KineticNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var processNotification: ProcessNotificationUseCase
    @Inject lateinit var eventBus: NotificationEventBus

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory dedup: sbnKey → timestamp of last seen
    private val recentKeys = mutableMapOf<String, Long>()
    private val dedupWindowMs = 2_000L

    override fun onListenerConnected() {
        super.onListenerConnected()
        eventBus.setListenerConnected(true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        eventBus.setListenerConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return

        // Noise filter: skip summaries, ongoing, and blanks
        if (notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0) return
        if (sbn.isOngoing) return

        // Dedup: same key within 2 s
        val now = System.currentTimeMillis()
        val lastSeen = recentKeys[sbn.key]
        if (lastSeen != null && now - lastSeen < dedupWindowMs) return
        recentKeys[sbn.key] = now

        val (groupName, contactName) = extractSender(sbn)
        if (contactName.isNullOrBlank()) return

        eventBus.emitEvent(
            NotificationEvent(
                packageName = sbn.packageName,
                senderName = contactName,
                text = notification.extras.getString(android.app.Notification.EXTRA_TEXT),
                category = notification.category,
                postedAt = sbn.postTime,
            )
        )

        scope.launch {
            processNotification.execute(
                packageName = sbn.packageName,
                groupName = groupName,
                contactName = contactName,
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun extractSender(sbn: StatusBarNotification): Pair<String, String?> {
        val notification = sbn.notification
        val style = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification)

        if (style != null) {
            val messages = style.messages
            val lastMessage = messages.lastOrNull()
            return if (style.isGroupConversation) {
                val groupName = style.conversationTitle?.toString() ?: ""
                val contactName = lastMessage?.person?.name?.toString()
                groupName to contactName
            } else {
                val contactName = lastMessage?.person?.name?.toString()
                    ?: notification.extras.getString(android.app.Notification.EXTRA_TITLE)
                "" to contactName
            }
        }

        val title = notification.extras.getString(android.app.Notification.EXTRA_TITLE)
        return "" to title
    }
}
