package com.example.newwords;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView registerLinkTextView, forgotPasswordTextView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Получаем ссылки на UI-элементы
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerLinkTextView = findViewById(R.id.registerLinkTextView);

        // Находим TextView для восстановления пароля (если добавили в layout)
        // forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);

        // Инициализируем Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Проверяем, авторизован ли пользователь
        checkCurrentUser();

        // Обработка входа
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Обработчик для ссылки на регистрацию
        registerLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToRegisterActivity();
            }
        });

        // Если добавили восстановление пароля
        /*
        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
        */
    }

    private void checkCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Пользователь уже авторизован - переходим в MainActivity
           // if (currentUser.isEmailVerified()) {
                // Email подтвержден - сразу переходим
                goToMainActivity();
           // } else {
                // Email не подтвержден - предлагаем подтвердить
             //   showEmailNotVerifiedDialog(currentUser);
           // }
        }
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Валидация полей
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(LoginActivity.this, "Введите email", Toast.LENGTH_SHORT).show();
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Введите пароль", Toast.LENGTH_SHORT).show();
            passwordEditText.requestFocus();
            return;
        }

        // Показываем прогресс (можно добавить ProgressBar)
        loginButton.setEnabled(false);
        loginButton.setText("Вход...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Восстанавливаем кнопку
                        loginButton.setEnabled(true);
                        loginButton.setText("Вход");

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                // Проверяем подтвержден ли email
                              //  if (user.isEmailVerified()) {
                                    // Email подтвержден - вход успешен
                                    Toast.makeText(LoginActivity.this,
                                            "Вход выполнен",
                                            Toast.LENGTH_SHORT).show();
                                    goToMainActivity();
                             //   } else {
                                    // Email не подтвержден
                                 //   showEmailNotVerifiedDialog(user);
                                    // Выходим, чтобы пользователь подтвердил email
                                 //   mAuth.signOut();
                                //}
                            }
                        } else {
                            // Ошибка входа
                            String errorMessage = "Ошибка входа";
                            if (task.getException() != null) {
                                String exceptionMessage = task.getException().getMessage();

                                // Более понятные сообщения об ошибках
                                if (exceptionMessage.contains("invalid credential") ||
                                        exceptionMessage.contains("password is invalid")) {
                                    errorMessage = "Неверный email или пароль";
                                } else if (exceptionMessage.contains("no user record")) {
                                    errorMessage = "Аккаунт не найден";
                                } else if (exceptionMessage.contains("network error")) {
                                    errorMessage = "Проверьте подключение к интернету";
                                } else {
                                    errorMessage = exceptionMessage;
                                }
                            }

                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showEmailNotVerifiedDialog(FirebaseUser user) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Email не подтвержден")
                .setMessage("Подтвердите ваш email " + user.getEmail() + " для доступа к приложению.\n\n" +
                        "Отправить письмо подтверждения еще раз?")
                .setPositiveButton("Отправить", (dialog, which) -> {
                    sendVerificationEmail(user);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Письмо подтверждения отправлено на " + user.getEmail(),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Ошибка отправки: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToRegisterActivity() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
        // Анимация перехода (раскомментируйте когда создадите анимации)
        // overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Метод для восстановления пароля (если будете добавлять)
    /*
    private void showForgotPasswordDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Восстановление пароля");

        final EditText input = new EditText(this);
        input.setHint("Введите ваш email");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Отправить", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!TextUtils.isEmpty(email)) {
                sendPasswordResetEmail(email);
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this,
                        "Инструкции отправлены на " + email,
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this,
                        "Ошибка: " + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            });
    }


    @Override
    public void onBackPressed() {
        // При нажатии назад выходим из приложения
        finishAffinity();
    } */
}