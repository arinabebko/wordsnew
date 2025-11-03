package com.example.newwords;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class FragmentLoginParamsOption extends Fragment {

    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_params_option, container, false);

        mAuth = FirebaseAuth.getInstance();

        // Отображаем email текущего пользователя
        TextView loginLabel = view.findViewById(R.id.loginLabel);
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            loginLabel.setText("    логин: " + email);
        }

        TextView logoutLabel = view.findViewById(R.id.logoutLabel);
        logoutLabel.setOnClickListener(v -> {
            logoutUser();
        });

        TextView deleteAccountLabel = view.findViewById(R.id.deleteAccountLabel);
        deleteAccountLabel.setOnClickListener(v -> {
            // Функционал удаления аккаунта
            showDeleteAccountConfirmation();
        });

        return view;
    }

    // Добавьте этот метод для выхода из аккаунта
    private void logoutUser() {
        mAuth.signOut();

        // Показываем сообщение об успешном выходе
        Toast.makeText(getContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();

        // Переходим на экран логина
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Завершаем текущую активность
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void showDeleteAccountConfirmation() {
        if (getActivity() != null) {
            new androidx.appcompat.app.AlertDialog.Builder(getActivity())
                    .setTitle("Удаление аккаунта")
                    .setMessage("Вы уверены, что хотите удалить аккаунт? Это действие нельзя отменить.")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        deleteUserAccount();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        }
    }

    private void deleteUserAccount() {
        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Аккаунт удален", Toast.LENGTH_SHORT).show();
                            // Переходим на экран логина
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            if (getActivity() != null) {
                                getActivity().finish();
                            }
                        } else {
                            Toast.makeText(getContext(), "Ошибка при удалении аккаунта", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}