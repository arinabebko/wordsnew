package com.example.newwords;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);
        loadCurrentUserData();
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
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                currentUsernameTextView.setText(displayName);
                newUsernameEditText.setHint("Текущее: " + displayName);
            } else {
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

        loadOtherSettings();
    }

    private void loadOtherSettings() {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("settings", 0);

        String language = prefs.getString("language", "Русский");
        if (languageTextView != null) {
            languageTextView.setText(language);
        }

        String theme = prefs.getString("theme", "Темная");
        if (themeTextView != null) {
            themeTextView.setText(theme);
        }
    }

    private void setupClickListeners() {
        saveUsernameButton.setOnClickListener(v -> saveNewUsername());
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        if (languageTextView != null) {
            languageTextView.setOnClickListener(v -> showLanguageDialog());
        }

        if (themeTextView != null) {
            themeTextView.setOnClickListener(v -> showThemeDialog());
        }
    }

    private void saveNewUsername() {
        String newUsername = newUsernameEditText.getText().toString().trim();

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

        saveUsernameButton.setEnabled(false);
        saveUsernameButton.setText("Сохранение...");

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateUsernameInFirestore(user.getUid(), newUsername);
                        currentUsernameTextView.setText(newUsername);
                        newUsernameEditText.setText("");
                        newUsernameEditText.setHint("Текущее: " + newUsername);
                        sendUsernameUpdateSignal(newUsername);
                        Toast.makeText(getContext(), "Имя успешно изменено!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                "Ошибка: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }

                    saveUsernameButton.setEnabled(true);
                    saveUsernameButton.setText("Сохранить имя");
                });
    }

    private void sendUsernameUpdateSignal(String newUsername) {
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("user_updates", Context.MODE_PRIVATE);
            prefs.edit()
                    .putString("updated_username", newUsername)
                    .putBoolean("needs_refresh", true)
                    .putLong("last_update", System.currentTimeMillis())
                    .apply();
        }

        if (usernameChangedListener != null) {
            usernameChangedListener.onUsernameChanged(newUsername);
        }
    }

    private void updateUsernameInFirestore(String userId, String newUsername) {
        db.collection("users").document(userId)
                .update("username", newUsername)
                .addOnSuccessListener(aVoid -> {})
                .addOnFailureListener(e -> {});
    }

    private void saveSetting(String key, String value) {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("settings", 0);
        prefs.edit()
                .putString(key, value)
                .apply();
    }

    private void showLanguageDialog() {
        // Создаем кастомный диалог вместо стандартного
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_language_theme, null);

        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        titleTextView.setText(R.string.settings_label_language);

        TextView option1 = dialogView.findViewById(R.id.option1);
        TextView option2 = dialogView.findViewById(R.id.option2);
        TextView option3 = dialogView.findViewById(R.id.option3);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        option1.setText("Русский");
        option2.setText("English");
        option3.setText("Башҡорт");

        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        option1.setOnClickListener(v -> {
            saveSetting("language", "Русский");
            if (languageTextView != null) {
                languageTextView.setText("Русский");
            }
            setAppLocale("ru");
            Toast.makeText(getContext(), R.string.settings_toast_language_changed, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        option2.setOnClickListener(v -> {
            saveSetting("language", "English");
            if (languageTextView != null) {
                languageTextView.setText("English");
            }
            setAppLocale("en");
            Toast.makeText(getContext(), R.string.settings_toast_language_changed, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        option3.setOnClickListener(v -> {
            saveSetting("language", "Башҡорт");
            if (languageTextView != null) {
                languageTextView.setText("Башҡорт");
            }
            setAppLocale("ba");
            Toast.makeText(getContext(), R.string.settings_toast_language_changed, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showThemeDialog() {
        // Создаем кастомный диалог вместо стандартного
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_language_theme, null);

        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        titleTextView.setText("Выберите тему");

        TextView option1 = dialogView.findViewById(R.id.option1);
        TextView option2 = dialogView.findViewById(R.id.option2);
        TextView option3 = dialogView.findViewById(R.id.option3);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        option1.setText("Темная");
        option2.setText("Светлая");
        option3.setText("Авто");

        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        option1.setOnClickListener(v -> {
            String selectedTheme = "Темная";
            if (themeTextView != null) {
                themeTextView.setText(selectedTheme);
            }
            saveSetting("theme", selectedTheme);
            Toast.makeText(getContext(), "Тема изменена на " + selectedTheme, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        option2.setOnClickListener(v -> {
            String selectedTheme = "Светлая";
            if (themeTextView != null) {
                themeTextView.setText(selectedTheme);
            }
            saveSetting("theme", selectedTheme);
            Toast.makeText(getContext(), "Тема изменена на " + selectedTheme, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        option3.setOnClickListener(v -> {
            String selectedTheme = "Авто";
            if (themeTextView != null) {
                themeTextView.setText(selectedTheme);
            }
            saveSetting("theme", selectedTheme);
            Toast.makeText(getContext(), "Тема изменена на " + selectedTheme, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAuth != null && getView() != null) {
            loadCurrentUserData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        usernameChangedListener = null;
    }

    private void setAppLocale(String languageCode) {
        androidx.core.os.LocaleListCompat appLocale =
                androidx.core.os.LocaleListCompat.forLanguageTags(languageCode);
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale);

        if (getActivity() != null) {
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }
}