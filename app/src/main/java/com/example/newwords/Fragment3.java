package com.example.newwords;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;

public class Fragment3 extends Fragment {

    private TextView userNameTextView, userEmailTextView;
    private ImageView avatarImageView;
    private FirebaseAuth mAuth;
    private Typeface juraTypeface; // Добавляем переменную для шрифта

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment3, container, false);

        // Загружаем шрифт
        juraTypeface = ResourcesCompat.getFont(requireContext(), R.font.jura_font_wght);

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

        // Применяем шрифт программно
        if (juraTypeface != null) {
            userNameTextView.setTypeface(juraTypeface);
            userEmailTextView.setTypeface(juraTypeface);
        }
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

        // Повторно применяем шрифт после установки текста (на всякий случай)
        if (juraTypeface != null) {
            userNameTextView.setTypeface(juraTypeface);
            userEmailTextView.setTypeface(juraTypeface);
        }
    }

    private void setupClickListeners(View view) {
        TextView settingsTextView = view.findViewById(R.id.settingsOption);

        settingsTextView.setOnClickListener(v -> {
            FragmentSettingsOption settingsFragment = new FragmentSettingsOption();

            settingsFragment.setOnUsernameChangedListener(new FragmentSettingsOption.OnUsernameChangedListener() {
                @Override
                public void onUsernameChanged(String newUsername) {
                    // Обновляем имя сразу
                    userNameTextView.setText(newUsername);
                    // Применяем шрифт снова
                    if (juraTypeface != null) {
                        userNameTextView.setTypeface(juraTypeface);
                    }
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
        checkForRecentUpdates();
    }

    private void checkForRecentUpdates() {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("user_updates", Context.MODE_PRIVATE);
        boolean needsRefresh = prefs.getBoolean("needs_refresh", false);
        long lastUpdate = prefs.getLong("last_update", 0);

        if (needsRefresh && (System.currentTimeMillis() - lastUpdate < 30000)) {
            refreshUserData();
            prefs.edit().putBoolean("needs_refresh", false).apply();
        } else {
            loadUserData();
        }
    }
}