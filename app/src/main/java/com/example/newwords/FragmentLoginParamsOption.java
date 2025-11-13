package com.example.newwords;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class FragmentLoginParamsOption extends Fragment {

    private FirebaseAuth mAuth;
    private WordRepository wordRepository;
    private TextView loginLabel;
    private TextView passwordLabel;
    private Button changePasswordButton;
    private TextView logoutLabel;
    private TextView deleteAccountLabel;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_params_option, container, false);

        mAuth = FirebaseAuth.getInstance();
        wordRepository = new WordRepository(getContext());

        // Инициализация View элементов
        initViews(view);

        // Настройка данных пользователя
        setupUserData();

        // Настройка обработчиков кликов
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        loginLabel = view.findViewById(R.id.loginLabel);
        passwordLabel = view.findViewById(R.id.passwordLabel);
        changePasswordButton = view.findViewById(R.id.changePasswordButton);
        logoutLabel = view.findViewById(R.id.logoutLabel);
        deleteAccountLabel = view.findViewById(R.id.deleteAccountLabel);
    }

    private void setupUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Email пользователя
            String email = currentUser.getEmail();
            if (email != null) {
                loginLabel.setText("логин: " + email);
            }

            // Пароль (замаскированный) - Firebase не хранит пароль, поэтому показываем заглушку
            passwordLabel.setText("пароль: ••••••••••");
        }
    }

    private void setupClickListeners() {
        // Кнопка смены пароля
        changePasswordButton.setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        // Выход из аккаунта
        logoutLabel.setOnClickListener(v -> {
            logoutUser();
        });

        // Удаление аккаунта
        deleteAccountLabel.setOnClickListener(v -> {
            showDeleteAccountConfirmation();
        });
    }

    /**
     * Диалог смены пароля
     */
    /**
     * Диалог смены пароля
     */
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Смена пароля");

        // Создаем layout для диалога
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        EditText currentPasswordEditText = dialogView.findViewById(R.id.currentPasswordEditText);
        EditText newPasswordEditText = dialogView.findViewById(R.id.newPasswordEditText);
        EditText confirmPasswordEditText = dialogView.findViewById(R.id.confirmPasswordEditText);

        // Создаем диалог
        AlertDialog dialog = builder.create();

        // Устанавливаем кастомные обработчики для кнопок
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Сменить", (message, which) -> {
            // Пустая реализация - обрабатываем вручную ниже
        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Отмена", (message, which) -> {
            dialog.dismiss();
        });

        dialog.show();

        // Кастомная обработка кнопки "Сменить"
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPassword = currentPasswordEditText.getText().toString().trim();
            String newPassword = newPasswordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            // Валидация (диалог НЕ закрывается при ошибках)
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                return; // Не закрываем диалог
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Новые пароли не совпадают", Toast.LENGTH_SHORT).show();
                return; // Не закрываем диалог
            }

            if (newPassword.length() < 6) {
                Toast.makeText(getContext(), "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show();
                return; // Не закрываем диалог
            }

            // Если валидация прошла - закрываем диалог и меняем пароль
            dialog.dismiss();
            changePassword(currentPassword, newPassword, confirmPassword);
        });
    }

    /**
     * Смена пароля
     */
    private void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        // Валидация
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Новые пароли не совпадают", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(getContext(), "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        // Показываем прогресс
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Смена пароля...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Для смены пароля в Firebase нужно переаутентифицировать пользователя
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        // Переаутентификация успешна - меняем пароль
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    progressDialog.dismiss();

                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(getContext(), "Пароль успешно изменен", Toast.LENGTH_SHORT).show();
                                        // Обновляем отображение пароля
                                        passwordLabel.setText("пароль: ••••••••••");
                                    } else {
                                        Toast.makeText(getContext(), "Ошибка смены пароля: " +
                                                updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Неверный текущий пароль", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Остальные методы (logoutUser, deleteUserAccount и т.д.) остаются без изменений
    private void logoutUser() {
        Log.d("Logout", "🚪 Начало выхода из аккаунта с очисткой кеша");
        Toast.makeText(getContext(), "Выход...", Toast.LENGTH_SHORT).show();

        wordRepository.clearLocalCache(
                () -> {
                    mAuth.signOut();
                    Log.d("Logout", "✅ Кеш очищен, выход выполнен");
                    goToLoginActivity();
                },
                e -> {
                    Log.e("Logout", "❌ Ошибка очистки кеша, но выходим", e);
                    mAuth.signOut();
                    goToLoginActivity();
                }
        );
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (getActivity() != null) {
            getActivity().finish();
        }

        Toast.makeText(getContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteAccountConfirmation() {
        if (getActivity() != null) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Удаление аккаунта")
                    .setMessage("Вы уверены, что хотите удалить аккаунт? Все данные будут потеряны.")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        deleteUserAccount();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        }
    }

    private void deleteUserAccount() {
        if (mAuth.getCurrentUser() != null) {
            wordRepository.clearLocalCache(
                    () -> performAccountDeletion(),
                    e -> performAccountDeletion()
            );
        }
    }

    private void performAccountDeletion() {
        mAuth.getCurrentUser().delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Аккаунт удален", Toast.LENGTH_SHORT).show();
                        goToLoginActivity();
                    } else {
                        Toast.makeText(getContext(), "Ошибка при удалении аккаунта", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}