package com.example.newwords;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

public class FragmentSettingsOption extends Fragment {

    // Интерфейс для обратного вызова
    public interface OnUsernameChangedListener {
        void onUsernameChanged(String newUsername);
    }

    private OnUsernameChangedListener usernameChangedListener;

    private TextView currentUsernameTextView;
    private EditText newUsernameEditText;
    private Button saveUsernameButton, backButton;
    private TextView languageTextView, themeTextView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Метод для установки слушателя
    public void setOnUsernameChangedListener(OnUsernameChangedListener listener) {
        this.usernameChangedListener = listener;
    }

    // Убедитесь, что SharedPreferences инициализированы при первом запуске
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация настроек по умолчанию при первом запуске
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("settings", 0);

            if (!prefs.contains("language")) {
                prefs.edit()
                        .putString("language", "Русский")
                        .putString("theme", "Темная")
                        .apply();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_option, container, false);

        // Инициализация Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Инициализация View элементов
        initViews(view);

        // Загрузка текущих данных пользователя
        loadCurrentUserData();

        // Настройка обработчиков кликов
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        currentUsernameTextView = view.findViewById(R.id.currentUsernameTextView);
        newUsernameEditText = view.findViewById(R.id.newUsernameEditText);
        saveUsernameButton = view.findViewById(R.id.saveUsernameButton);
        backButton = view.findViewById(R.id.backButton);
        languageTextView = view.findViewById(R.id.languageTextView);
        themeTextView = view.findViewById(R.id.themeTextView);
    }

    private void loadCurrentUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Имя пользователя
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                currentUsernameTextView.setText(displayName);
                newUsernameEditText.setHint("Текущее: " + displayName);
            } else {
                // Если имя не установлено
                String email = currentUser.getEmail();
                if (email != null && email.contains("@")) {
                    String username = email.substring(0, email.indexOf("@"));
                    currentUsernameTextView.setText(username);
                    newUsernameEditText.setHint("Текущее: " + username);
                } else {
                    currentUsernameTextView.setText("Не установлено");
                }
            }
        } else {
            currentUsernameTextView.setText("Гость");
            saveUsernameButton.setEnabled(false);
            saveUsernameButton.setAlpha(0.5f);
        }

        // Загрузить другие настройки из SharedPreferences
        loadOtherSettings();
    }

    private void loadOtherSettings() {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("settings", 0);

        // Язык
        String language = prefs.getString("language", "Русский");
        if (languageTextView != null) {
            languageTextView.setText(language);
        }

        // Тема
        String theme = prefs.getString("theme", "Темная");
        if (themeTextView != null) {
            themeTextView.setText(theme);
        }
    }

    private void setupClickListeners() {
        // Кнопка сохранения имени
        saveUsernameButton.setOnClickListener(v -> {
            saveNewUsername();
        });

        // Кнопка назад
        backButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // Смена языка
        if (languageTextView != null) {
            languageTextView.setOnClickListener(v -> {
                showLanguageDialog();
            });
        }

        // Смена темы
        if (themeTextView != null) {
            themeTextView.setOnClickListener(v -> {
                showThemeDialog();
            });
        }
    }

    private void saveNewUsername() {
        String newUsername = newUsernameEditText.getText().toString().trim();

        // Валидация
        if (TextUtils.isEmpty(newUsername)) {
            Toast.makeText(getContext(), "Введите новое имя", Toast.LENGTH_SHORT).show();
            newUsernameEditText.requestFocus();
            return;
        }

        if (newUsername.length() < 3) {
            Toast.makeText(getContext(), "Имя должно содержать минимум 3 символа", Toast.LENGTH_SHORT).show();
            newUsernameEditText.requestFocus();
            return;
        }

        if (newUsername.length() > 20) {
            Toast.makeText(getContext(), "Имя не должно превышать 20 символов", Toast.LENGTH_SHORT).show();
            newUsernameEditText.requestFocus();
            return;
        }

        if (!newUsername.matches("^[a-zA-Zа-яА-Я0-9_]+$")) {
            Toast.makeText(getContext(), "Имя может содержать только буквы, цифры и нижнее подчеркивание", Toast.LENGTH_SHORT).show();
            newUsernameEditText.requestFocus();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        // Блокируем кнопку на время выполнения
        saveUsernameButton.setEnabled(false);
        saveUsernameButton.setText("Сохранение...");

        // Обновляем имя в Firebase Auth
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Обновляем в Firestore
                        updateUsernameInFirestore(user.getUid(), newUsername);

                        // Обновляем UI
                        currentUsernameTextView.setText(newUsername);
                        newUsernameEditText.setText("");
                        newUsernameEditText.setHint("Текущее: " + newUsername);

                        // ⭐⭐⭐ ВАЖНОЕ ИЗМЕНЕНИЕ: Отправляем сигнал об обновлении ⭐⭐⭐
                        sendUsernameUpdateSignal(newUsername);

                        // ⭐⭐⭐ ВАРИАНТ: Показываем сообщение и возвращаемся ⭐⭐⭐
                      //  Toast.makeText(getContext(), "Имя успешно изменено! Возвращаемся...", Toast.LENGTH_SHORT).show();



                        // ⭐⭐⭐ ВАРИАНТ Б: Оставляем пользователя в настройках ⭐⭐⭐
                        // Просто показываем сообщение
                         Toast.makeText(getContext(), "Имя успешно изменено!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                "Ошибка: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }

                    // Восстанавливаем кнопку
                    saveUsernameButton.setEnabled(true);
                    saveUsernameButton.setText("Сохранить имя");
                });
    }
    // Метод для отправки сигнала об обновлении
    private void sendUsernameUpdateSignal(String newUsername) {
        // Вариант 1: Через SharedPreferences
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("user_updates", Context.MODE_PRIVATE);
            prefs.edit()
                    .putString("updated_username", newUsername)
                    .putBoolean("needs_refresh", true)
                    .putLong("last_update", System.currentTimeMillis())
                    .apply();
        }

        // Вариант 2: Через интерфейс (если фрагмент 3 активен)
        if (usernameChangedListener != null) {
            usernameChangedListener.onUsernameChanged(newUsername);
        }
    }
    private void updateUsernameInFirestore(String userId, String newUsername) {
        db.collection("users").document(userId)
                .update("username", newUsername)
                .addOnSuccessListener(aVoid -> {
                    // Успешно обновлено в Firestore
                })
                .addOnFailureListener(e -> {
                    // Ошибка обновления в Firestore
                    // Можно залогировать, но не показывать пользователю
                });
    }

    private void saveSetting(String key, String value) {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("settings", 0);
        prefs.edit()
                .putString(key, value)
                .apply();
    }

    private void showLanguageDialog() {
        String[] languages = {"Русский", "English"};
        // Коды языков для системы
        String[] languageCodes = {"ru", "en"};

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(getContext());

        builder.setTitle(R.string.settings_label_language) // Используем строку из ресурсов!
                .setItems(languages, (dialog, which) -> {
                    String selectedLanguage = languages[which];
                    String selectedCode = languageCodes[which];

                    // 1. Сохраняем в настройки
                    saveSetting("language", selectedLanguage);

                    // 2. МЕНЯЕМ ЯЗЫК В ПРИЛОЖЕНИИ
                    setAppLocale(selectedCode);

                    // После setApplicationLocales активити сама перезагрузится,
                    // и всё приложение станет на новом языке!
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    private void showThemeDialog() {
        // Диалог выбора темы
        String[] themes = {"Темная", "Светлая", "Авто"};

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(getContext());

        builder.setTitle("Выберите тему")
                .setItems(themes, (dialog, which) -> {
                    String selectedTheme = themes[which];
                    if (themeTextView != null) {
                        themeTextView.setText(selectedTheme);
                    }
                    saveSetting("theme", selectedTheme);
                    Toast.makeText(getContext(),
                            "Тема изменена на " + selectedTheme,
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем данные при возвращении на фрагмент
        if (mAuth != null && getView() != null) {
            loadCurrentUserData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очищаем слушатель при уничтожении вью
        usernameChangedListener = null;
    }



    private void setAppLocale(String languageCode) {
        androidx.core.os.LocaleListCompat appLocale =
                androidx.core.os.LocaleListCompat.forLanguageTags(languageCode);
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale);

        // Добавляем плавный переход для текущего экрана
        if (getActivity() != null) {
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }
}