package com.example.newwords;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "learning_reminders";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");

        if (title == null) title = "Пора учить слова!";
        if (message == null) message = "Не забудьте позаниматься сегодня ✨";

        showNotification(context, title, message);

        // Если это уведомление о бездействии, перепланируем его
        if ("INACTIVITY_REMINDER".equals(intent.getAction())) {
            NotificationScheduler.scheduleInactivityNotification(context);
        }
    }

    private void showNotification(Context context, String title, String message) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Создаем канал уведомлений (для Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Напоминания об обучении",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Напоминания о необходимости позаниматься словами");
            notificationManager.createNotificationChannel(channel);
        }

        // ВРЕМЕННО используем стандартную иконку Android
        int notificationIcon = android.R.drawable.ic_dialog_info; // временная иконка

        // Создаем уведомление
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(notificationIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // ПРОВЕРЯЕМ РАЗРЕШЕНИЕ ДЛЯ ANDROID 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            if (notificationManagerCompat.areNotificationsEnabled()) {
                notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
            }
            // Если разрешения нет - просто не показываем уведомление
        } else {
            // Для старых версий Android показываем без проверки
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}