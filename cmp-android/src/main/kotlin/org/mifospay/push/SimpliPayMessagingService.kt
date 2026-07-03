/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.push

import android.Manifest
import android.app.PendingIntent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.mifospay.R
import org.mifospay.shared.push.PushBridge

const val NOTIFICATION_CHANNEL_ID = "simplipay_default"
const val PUSH_TYPE_EXTRA = "type"

/**
 * Android half of the push bridge (mirror of the iOS AppDelegate wiring):
 * token rotations go to [PushBridge] for the shared coordinator to upload,
 * and foreground messages are rendered here (background notification-messages
 * are auto-displayed by the FCM SDK; their data lands in the launcher intent,
 * which MainActivity forwards to [PushBridge.onNotificationTapped]).
 */
class SimpliPayMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        PushBridge.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Only reached in foreground (or for data-only messages) — show it ourselves.
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val tapIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            putExtra(PUSH_TYPE_EXTRA, message.data[PUSH_TYPE_EXTRA])
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            message.messageId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(message.messageId.hashCode(), notification)
    }
}
