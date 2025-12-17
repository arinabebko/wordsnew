package com.example.newwords;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;

public class Fragment3 extends Fragment {

    private TextView userNameTextView, userEmailTextView;
    private ImageView avatarImageView;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment3, container, false);

        mAuth = FirebaseAuth.getInstance();
        initViews(view);
        loadUserData();
        setupClickListeners(view);

        return view;
    }

    private void initViews(View view) {
        userNameTextView = view.findViewById(R.id.userNameTextView);
        userEmailTextView = view.findViewById(R.id.userEmailTextView);
        avatarImageView = view.findViewById(R.id.avatarImageView);
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                userNameTextView.setText(displayName);
            } else {
                String email = currentUser.getEmail();
                if (email != null && email.contains("@")) {
                    String username = email.substring(0, email.indexOf("@"));
                    userNameTextView.setText(username);
                } else {
                    userNameTextView.setText("Пользователь");
                }
            }

            String email = currentUser.getEmail();
            if (email != null) {
                userEmailTextView.setText(email);
            }

            String photoUrl = currentUser.getPhotoUrl() != null ?
                    currentUser.getPhotoUrl().toString() : null;

            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .circleCrop()
                        .into(avatarImageView);
            }
        } else {
            userNameTextView.setText("Гость");
            userEmailTextView.setText("Войдите в аккаунт");
        }
    }

    private void setupClickListeners(View view) {
        TextView settingsTextView = view.findViewById(R.id.settingsOption);

        // ⭐⭐⭐ ИСПРАВЛЕННЫЙ КОД - только один слушатель ⭐⭐⭐
        settingsTextView.setOnClickListener(v -> {
            FragmentSettingsOption settingsFragment = new FragmentSettingsOption();

            // Устанавливаем слушатель ПРАВИЛЬНО
            settingsFragment.setOnUsernameChangedListener(new FragmentSettingsOption.OnUsernameChangedListener() {
                @Override
                public void onUsernameChanged(String newUsername) {
                    // Обновляем имя сразу
                    userNameTextView.setText(newUsername);
                    // Также можно обновить email если нужно
                }
            });

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, settingsFragment)
                    .addToBackStack("fragment3_navigation")
                    .commit();
        });

        TextView notificationTextView = view.findViewById(R.id.notificationsOption);
        notificationTextView.setOnClickListener(v -> {
            FragmentNotificationsOption notificationsFragment = new FragmentNotificationsOption();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, notificationsFragment)
                    .addToBackStack("fragment3_navigation")
                    .commit();
        });

        TextView loginParamsTextView = view.findViewById(R.id.loginParamsOption);
        loginParamsTextView.setOnClickListener(v -> {
            FragmentLoginParamsOption loginParamsFragment = new FragmentLoginParamsOption();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, loginParamsFragment)
                    .addToBackStack("fragment3_navigation")
                    .commit();
        });

        avatarImageView.setOnClickListener(v -> refreshUserData());
        userNameTextView.setOnClickListener(v -> refreshUserData());
    }

    private void refreshUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    loadUserData();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // ⭐⭐⭐ ДОБАВЬТЕ ПРОВЕРКУ ОБНОВЛЕНИЙ ⭐⭐⭐
        checkForRecentUpdates();
    }

    // ⭐⭐⭐ НОВЫЙ МЕТОД: Проверка обновлений из SharedPreferences ⭐⭐⭐
    private void checkForRecentUpdates() {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("user_updates", Context.MODE_PRIVATE);
        boolean needsRefresh = prefs.getBoolean("needs_refresh", false);
        long lastUpdate = prefs.getLong("last_update", 0);

        // Если было обновление менее 30 секунд назад
        if (needsRefresh && (System.currentTimeMillis() - lastUpdate < 30000)) {
            // Принудительно обновляем данные
            refreshUserData();

            // Сбрасываем флаг
            prefs.edit().putBoolean("needs_refresh", false).apply();
        } else {
            // Обычная загрузка
            loadUserData();
        }
    }
}