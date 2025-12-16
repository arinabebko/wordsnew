package com.example.newwords;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private TextView loginLinkTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Инициализация Firebase
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Инициализация Firestore

        // Инициализация UI элементов
        initViews();

        // Обработчики кликов
        setupClickListeners();
    }

    private void initViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginLinkTextView = findViewById(R.id.loginLinkTextView);
    }

    private void setupClickListeners() {
        // Кнопка регистрации
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Ссылка "Уже есть аккаунт"
        loginLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToLoginActivity();
            }
        });
    }

    private void registerUser() {
        // Получаем данные из полей
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Валидация
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Введите имя пользователя", Toast.LENGTH_SHORT).show();
            usernameEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show();
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show();
            passwordEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Подтвердите пароль", Toast.LENGTH_SHORT).show();
            confirmPasswordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            confirmPasswordEditText.setText("");
            confirmPasswordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        // Показываем прогресс
        registerButton.setEnabled(false);
        registerButton.setText("Регистрация...");

        // Создаем пользователя в Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Регистрация успешна
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();

                            if (firebaseUser != null) {
                                // 1. Устанавливаем имя пользователя в профиль Firebase
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(username)
                                        .build();

                                firebaseUser.updateProfile(profileUpdates)
                                        .addOnCompleteListener(updateTask -> {
                                            if (updateTask.isSuccessful()) {
                                                // 2. Сохраняем дополнительную информацию в Firestore
                                                saveUserToFirestore(firebaseUser.getUid(), username, email);

                                                // 3. Отправляем email для подтверждения
                                                sendEmailVerification(firebaseUser);

                                                // 4. Показываем сообщение и переходим на главный экран
                                                Toast.makeText(RegisterActivity.this,
                                                        "Регистрация успешна! Проверьте email для подтверждения.",
                                                        Toast.LENGTH_LONG).show();

                                                // Сразу переходим в MainActivity (не блокируем вход)
                                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(RegisterActivity.this,
                                                        "Ошибка установки имени: " + updateTask.getException().getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                                resetButton();
                                            }
                                        });
                            }
                        } else {
                            // Ошибка регистрации
                            String errorMessage = "Ошибка регистрации";
                            if (task.getException() != null) {
                                String exceptionMessage = task.getException().getMessage();

                                if (exceptionMessage.contains("email address is already in use")) {
                                    errorMessage = "Этот email уже используется";
                                } else if (exceptionMessage.contains("invalid email")) {
                                    errorMessage = "Неверный формат email";
                                } else if (exceptionMessage.contains("network error")) {
                                    errorMessage = "Проверьте подключение к интернету";
                                } else {
                                    errorMessage = exceptionMessage;
                                }
                            }

                            Toast.makeText(RegisterActivity.this,
                                    errorMessage,
                                    Toast.LENGTH_LONG).show();
                            resetButton();
                        }
                    }
                });
    }

    private void saveUserToFirestore(String userId, String username, String email) {
        // Создаем объект пользователя
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());

        // Статистика пользователя
        Map<String, Object> stats = new HashMap<>();
        stats.put("wordsLearned", 0);
        stats.put("streakDays", 0);
        stats.put("totalTime", 0);
        user.put("stats", stats);

        // Сохраняем в Firestore
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(documentReference -> {
                    Log.d("RegisterActivity", "Пользователь сохранен в Firestore: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e("RegisterActivity", "Ошибка сохранения в Firestore: ", e);
                });
    }

    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("RegisterActivity", "Email подтверждения отправлен: " + user.getEmail());
                    } else {
                        Log.e("RegisterActivity", "Ошибка отправки email: ", task.getException());
                    }
                });
    }

    private void resetButton() {
        registerButton.setEnabled(true);
        registerButton.setText("Зарегистрироваться");
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
        // Анимация перехода
        // overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

   // @Override
    //public void onBackPressed() {
        // При нажатии назад переходим на экран входа
      //  goToLoginActivity();
    //}
}