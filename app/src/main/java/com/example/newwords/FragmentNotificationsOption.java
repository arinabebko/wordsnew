package com.example.newwords;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;

public class FragmentNotificationsOption extends Fragment {

    private RadioGroup notificationOptionsGroup;
    private RadioButton rbNone, rbOnceADay, rbAfterInactivity;
    private EditText reminderTimeEditText;

    private SharedPreferences preferences;
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String KEY_NOTIFICATION_TYPE = "notification_type";
    private static final String KEY_NOTIFICATION_TIME = "notification_time";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications_option, container, false);

        initViews(view);
        loadSavedPreferences();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        notificationOptionsGroup = view.findViewById(R.id.notificationOptionsGroup);
        rbNone = view.findViewById(R.id.rb_none);
        rbOnceADay = view.findViewById(R.id.rb_onceADay);
        rbAfterInactivity = view.findViewById(R.id.rb_afterInactivity);
        reminderTimeEditText = view.findViewById(R.id.reminderTimeEditText);

        preferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void loadSavedPreferences() {
        String type = preferences.getString(KEY_NOTIFICATION_TYPE, "none");
        String time = preferences.getString(KEY_NOTIFICATION_TIME, "19:00");

        reminderTimeEditText.setText(time);

        switch (type) {
            case "none":
                rbNone.setChecked(true);
                break;
            case "once_a_day":
                rbOnceADay.setChecked(true);
                break;
            case "after_inactivity":
                rbAfterInactivity.setChecked(true);
                break;
        }

        Log.d("Notifications", "Загружены настройки: type=" + type + ", time=" + time);
    }

    private void setupListeners() {
        notificationOptionsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            savePreferences();
            scheduleNotifications();
        });


        setupTimeInputListeners();
        // reminderTimeEditText.setOnFocusChangeListener((v, hasFocus) -> {
          //  if (!hasFocus) {
          //      savePreferences();
         //       scheduleNotifications();
         //   }
       // });
    }


    private void setupTimeInputListeners() {
        // Сохраняем при изменении текста (когда введено полное время)
        reminderTimeEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.length() == 5 && isValidTimeFormat(s.toString())) {
                    // Автоматически сохраняем когда введено полное время "HH:MM"
                    saveTimeImmediately(s.toString());
                }
            }
        });

        // Сохраняем при потере фокуса (когда уходим с поля)
        reminderTimeEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String time = reminderTimeEditText.getText().toString().trim();
                if (isValidTimeFormat(time)) {
                    saveTimeImmediately(time);
                }
            }
        });

        // Сохраняем при нажатии "Готово" на клавиатуре
        reminderTimeEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                String time = reminderTimeEditText.getText().toString().trim();
                if (isValidTimeFormat(time)) {
                    saveTimeImmediately(time);
                }
                // Скрываем клавиатуру
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) requireContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(reminderTimeEditText.getWindowToken(), 0);
                reminderTimeEditText.clearFocus();
                return true;
            }
            return false;
        });
    }

    // ⬇️⬇️⬇️ ДОБАВЬТЕ ЭТОТ МЕТОД ДЛЯ МГНОВЕННОГО СОХРАНЕНИЯ ⬇️⬇️⬇️
    private void saveTimeImmediately(String time) {
        if (isValidTimeFormat(time)) {
            // Сохраняем в SharedPreferences
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_NOTIFICATION_TIME, time);
            editor.apply();

            // Если активен режим "раз в день", сразу перепланируем уведомления
            String currentType = preferences.getString(KEY_NOTIFICATION_TYPE, "none");
            if ("once_a_day".equals(currentType)) {
                cancelAllNotifications();
                scheduleDailyNotification(time);

                // Показываем сообщение пользователю
                Toast.makeText(requireContext(),
                        "Время напоминания изменено на " + time,
                        Toast.LENGTH_SHORT).show();
            }

            Log.d("Notifications", "Время сразу сохранено: " + time);
        }
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = preferences.edit();

        String time = reminderTimeEditText.getText().toString().trim();
        if (TextUtils.isEmpty(time) || !isValidTimeFormat(time)) {
            time = "19:00";
            reminderTimeEditText.setText(time);
        }
        editor.putString(KEY_NOTIFICATION_TIME, time);

        if (rbNone.isChecked()) {
            editor.putString(KEY_NOTIFICATION_TYPE, "none");
        } else if (rbOnceADay.isChecked()) {
            editor.putString(KEY_NOTIFICATION_TYPE, "once_a_day");
        } else if (rbAfterInactivity.isChecked()) {
            editor.putString(KEY_NOTIFICATION_TYPE, "after_inactivity");
        }

        editor.apply();

        Log.d("Notifications", "Сохранены настройки: type=" +
                preferences.getString(KEY_NOTIFICATION_TYPE, "none") +
                ", time=" + time);
    }

    private boolean isValidTimeFormat(String time) {
        return time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");
    }

    private void scheduleNotifications() {
        // Сначала отменяем все уведомления
        cancelAllNotifications();

        String type = preferences.getString(KEY_NOTIFICATION_TYPE, "none");
        String time = preferences.getString(KEY_NOTIFICATION_TIME, "19:00");

        Log.d("Notifications", "Планируем уведомления: type=" + type + ", time=" + time);

        switch (type) {
            case "once_a_day":
                scheduleDailyNotification(time);
                Toast.makeText(requireContext(), "Ежедневное напоминание установлено на " + time, Toast.LENGTH_SHORT).show();
                break;
            case "after_inactivity":
                scheduleInactivityNotification();
                Toast.makeText(requireContext(), "Напоминание после бездействия установлено", Toast.LENGTH_SHORT).show();
                break;
            case "none":
                Toast.makeText(requireContext(), "Уведомления отключены", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void scheduleDailyNotification(String time) {
        try {
            String[] timeParts = time.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            Log.d("Notifications", "Устанавливаем уведомление на: " + calendar.getTime());

            // Если время уже прошло сегодня, установим на завтра
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                Log.d("Notifications", "Время прошло, устанавливаем на завтра: " + calendar.getTime());
            }

            Intent notificationIntent = new Intent(requireContext(), NotificationReceiver.class);
            notificationIntent.setAction("DAILY_REMINDER");
            notificationIntent.putExtra("title", "Пора учить слова!");
            notificationIntent.putExtra("message", "Не забудьте позаниматься сегодня ✨");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    1001,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

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

                Log.d("Notifications", "Ежедневное уведомление установлено на: " + calendar.getTime());
            }

        } catch (Exception e) {
            Log.e("Notifications", "Ошибка установки напоминания", e);
            Toast.makeText(requireContext(), "Ошибка установки напоминания: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private void scheduleInactivityNotification() {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 1); // Через 1 день
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            Intent notificationIntent = new Intent(requireContext(), NotificationReceiver.class);
            notificationIntent.setAction("INACTIVITY_REMINDER");
            notificationIntent.putExtra("title", "Мы по вам скучаем!");
            notificationIntent.putExtra("message", "Вы давно не занимались. Возвращайтесь! 📚");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    1002,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

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
                Log.d("Notifications", "Уведомление о бездействии установлено на: " + calendar.getTime());
            }
        } catch (Exception e) {
            Log.e("Notifications", "Ошибка установки уведомления о бездействии", e);
            Toast.makeText(requireContext(), "Ошибка установки напоминания: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void cancelAllNotifications() {
        try {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

            // Отменяем ежедневное уведомление
            Intent dailyIntent = new Intent(requireContext(), NotificationReceiver.class);
            PendingIntent dailyPending = PendingIntent.getBroadcast(
                    requireContext(),
                    1001,
                    dailyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            if (alarmManager != null && dailyPending != null) {
                alarmManager.cancel(dailyPending);
                dailyPending.cancel();
            }

            // Отменяем уведомление о бездействии
            Intent inactivityIntent = new Intent(requireContext(), NotificationReceiver.class);
            PendingIntent inactivityPending = PendingIntent.getBroadcast(
                    requireContext(),
                    1002,
                    inactivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            if (alarmManager != null && inactivityPending != null) {
                alarmManager.cancel(inactivityPending);
                inactivityPending.cancel();
            }

            Log.d("Notifications", "Все уведомления отменены");
        } catch (Exception e) {
            Log.e("Notifications", "Ошибка отмены уведомлений", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Сохраняем настройки при закрытии фрагмента
        savePreferences();
    }
}