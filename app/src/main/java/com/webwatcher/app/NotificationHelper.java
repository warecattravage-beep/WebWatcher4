package com.webwatcher.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID = "new_message";
    private static final int NOTIF_ID = 42;
    private static long lastNotifTime = 0;
    private static final long COOLDOWN_MS = 3000; // 3초 쿨다운 (중복 방지)

    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        createChannel();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "새 메시지 알림",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("새 메시지가 도착했을 때 알림");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 300, 150, 300});
            nm.createNotificationChannel(channel);
        }
    }

    public void sendMessageNotification() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastNotifTime < COOLDOWN_MS) return; // 중복 알림 방지
        lastNotifTime = now;

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("💬 새 메시지 도착!")
                .setContentText("새로운 메시지가 있습니다. 탭하여 확인하세요.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 300, 150, 300});

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID, builder.build());
        }
    }
}
