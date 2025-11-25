package com.example.newwords;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "learning_reminders";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NotificationReceiver", "Получено уведомление: " + intent.getAction());

        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");

        if (title == null) title = "Пора учить слова!";
        if (message == null) message = "Не забудьте позаниматься сегодня ✨";

        Log.d("NotificationReceiver", "Показываем уведомление: " + title);

        showNotification(context, title, message);

        // Перепланируем уведомления в зависимости от типа
        if ("DAILY_REMINDER".equals(intent.getAction())) {
            Log.d("NotificationReceiver", "Перепланируем ежедневное уведомление");
            rescheduleDailyNotification(context);
        } else if ("INACTIVITY_REMINDER".equals(intent.getAction())) {
            Log.d("NotificationReceiver", "Перепланируем уведомление о бездействии");
            NotificationScheduler.scheduleInactivityNotification(context);
        }
    }

    private void rescheduleDailyNotification(Context context) {
        try {
            SharedPreferences preferences = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
            String type = preferences.getString("notification_type", "none");
            String time = preferences.getString("notification_time", "19:00");

            // Перепланируем только если тип всё еще "once_a_day"
            if ("once_a_day".equals(type)) {
                String[] timeParts = time.split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);

                // Устанавливаем на завтра в то же время
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1);
                calendar.set(java.util.Calendar.HOUR_OF_DAY, hour);
                calendar.set(java.util.Calendar.MINUTE, minute);
                calendar.set(java.util.Calendar.SECOND, 0);
                calendar.set(java.util.Calendar.MILLISECOND, 0);

                Intent notificationIntent = new Intent(context, NotificationReceiver.class);
                notificationIntent.setAction("DAILY_REMINDER");
                notificationIntent.putExtra("title", "Пора учить слова!");
                notificationIntent.putExtra("message", "Не забудьте позаниматься сегодня ✨");

                android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        1001,
                        notificationIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
                );

                android.app.AlarmManager alarmManager =
                        (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                if (alarmManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                                android.app.AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                        );
                    } else {
                        alarmManager.setExact(
                                android.app.AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                        );
                    }
                    Log.d("NotificationReceiver", "Ежедневное уведомление перепланировано на: " + calendar.getTime());
                }
            } else {
                Log.d("NotificationReceiver", "Тип уведомления изменился, перепланирование отменено");
            }
        } catch (Exception e) {
            Log.e("NotificationReceiver", "Ошибка перепланирования ежедневного уведомления", e);
        }
    }

    private void showNotification(Context context, String title, String message) {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            // Создаем канал уведомлений (для Android 8.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Напоминания об обучении",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Напоминания о необходимости позаниматься словами");
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 500, 200, 500});
                channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(channel);
            }

            // Используем кастомную иконку или системную
            int notificationIcon = context.getResources().getIdentifier(
                    "ic_notification", "drawable", context.getPackageName());

            if (notificationIcon == 0) {
                // Если кастомной иконки нет, используем системную
                notificationIcon = android.R.drawable.ic_dialog_info;
            }

            // Создаем уведомление
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(notificationIcon)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 500, 200, 500})
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

            // Показываем уведомление
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);

            if (notificationManagerCompat.areNotificationsEnabled()) {
                notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
                Log.d("NotificationReceiver", "Уведомление показано: " + title);
            } else {
                Log.d("NotificationReceiver", "Нет разрешения на уведомления");
            }

        } catch (Exception e) {
            Log.e("NotificationReceiver", "Ошибка показа уведомления", e);
        }
    }
}