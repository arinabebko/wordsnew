package com.example.newwords;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

public class NotificationScheduler {

    private static final int DAILY_NOTIFICATION_ID = 1001;
    private static final int INACTIVITY_NOTIFICATION_ID = 1002;

    public static void scheduleDailyNotification(Context context, String time) {
        try {
            String[] timeParts = time.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            // Если время уже прошло сегодня, установим на завтра
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            Intent notificationIntent = new Intent(context, NotificationReceiver.class);
            notificationIntent.setAction("DAILY_REMINDER");
            notificationIntent.putExtra("title", "Пора учить слова!");
            notificationIntent.putExtra("message", "Не забудьте позаниматься сегодня ✨");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    DAILY_NOTIFICATION_ID,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }
            }

            // Устанавливаем повторение каждый день
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void scheduleInactivityNotification(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1); // Через 1 день

        Intent notificationIntent = new Intent(context, NotificationReceiver.class);
        notificationIntent.setAction("INACTIVITY_REMINDER");
        notificationIntent.putExtra("title", "Мы по вам скучаем!");
        notificationIntent.putExtra("message", "Вы давно не занимались. Возвращайтесь! 📚");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                INACTIVITY_NOTIFICATION_ID,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }

    public static void cancelAllNotifications(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Отменяем ежедневное уведомление
        Intent dailyIntent = new Intent(context, NotificationReceiver.class);
        PendingIntent dailyPending = PendingIntent.getBroadcast(
                context,
                DAILY_NOTIFICATION_ID,
                dailyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(dailyPending);
        }

        // Отменяем уведомление о бездействии
        Intent inactivityIntent = new Intent(context, NotificationReceiver.class);
        PendingIntent inactivityPending = PendingIntent.getBroadcast(
                context,
                INACTIVITY_NOTIFICATION_ID,
                inactivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(inactivityPending);
        }
    }

    public static void resetInactivityTimer(Context context) {
        // Этот метод нужно вызывать когда пользователь занимается
        SharedPreferences preferences = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        String type = preferences.getString("notification_type", "none");

        if ("after_inactivity".equals(type)) {
            cancelAllNotifications(context);
            scheduleInactivityNotification(context);
        }
    }
}