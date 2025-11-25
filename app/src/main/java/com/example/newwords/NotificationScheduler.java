package com.example.newwords;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class NotificationScheduler {

    private static final int DAILY_NOTIFICATION_ID = 1001;
    private static final int INACTIVITY_NOTIFICATION_ID = 1002;

    // ОСТАВЬТЕ этот метод, но он больше не используется напрямую
    public static void scheduleDailyNotification(Context context, String time) {
        try {
            String[] timeParts = time.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

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
                Log.d("NotificationScheduler", "Ежедневное уведомление установлено: " + calendar.getTime());
            }

        } catch (Exception e) {
            Log.e("NotificationScheduler", "Ошибка установки ежедневного уведомления", e);
        }
    }

    public static void scheduleInactivityNotification(Context context) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

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
                Log.d("NotificationScheduler", "Уведомление о бездействии установлено: " + calendar.getTime());
            }
        } catch (Exception e) {
            Log.e("NotificationScheduler", "Ошибка установки уведомления о бездействии", e);
        }
    }

    public static void cancelAllNotifications(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // Отменяем ежедневное уведомление
            Intent dailyIntent = new Intent(context, NotificationReceiver.class);
            PendingIntent dailyPending = PendingIntent.getBroadcast(
                    context,
                    DAILY_NOTIFICATION_ID,
                    dailyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            if (alarmManager != null && dailyPending != null) {
                alarmManager.cancel(dailyPending);
                dailyPending.cancel();
            }

            // Отменяем уведомление о бездействии
            Intent inactivityIntent = new Intent(context, NotificationReceiver.class);
            PendingIntent inactivityPending = PendingIntent.getBroadcast(
                    context,
                    INACTIVITY_NOTIFICATION_ID,
                    inactivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            if (alarmManager != null && inactivityPending != null) {
                alarmManager.cancel(inactivityPending);
                inactivityPending.cancel();
            }

            Log.d("NotificationScheduler", "Все уведомления отменены");
        } catch (Exception e) {
            Log.e("NotificationScheduler", "Ошибка отмены уведомлений", e);
        }
    }

    public static void resetInactivityTimer(Context context) {
        try {
            SharedPreferences preferences = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
            String type = preferences.getString("notification_type", "none");

            if ("after_inactivity".equals(type)) {
                cancelInactivityNotification(context);
                scheduleInactivityNotification(context);
                Log.d("NotificationScheduler", "Таймер бездействия сброшен");
            }
        } catch (Exception e) {
            Log.e("NotificationScheduler", "Ошибка сброса таймера бездействия", e);
        }
    }

    private static void cancelInactivityNotification(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent inactivityIntent = new Intent(context, NotificationReceiver.class);
            PendingIntent inactivityPending = PendingIntent.getBroadcast(
                    context,
                    INACTIVITY_NOTIFICATION_ID,
                    inactivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            if (alarmManager != null && inactivityPending != null) {
                alarmManager.cancel(inactivityPending);
                inactivityPending.cancel();
            }
        } catch (Exception e) {
            Log.e("NotificationScheduler", "Ошибка отмены уведомления о бездействии", e);
        }
    }
}