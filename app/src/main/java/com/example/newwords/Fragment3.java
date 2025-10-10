package com.example.newwords;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.newwords.R;
import com.google.firebase.auth.FirebaseUser;

public class Fragment3 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment3, container, false);

        // Находим ваш TextView (замените R.id.your_textview на реальный ID)
        TextView SettingsTextView = view.findViewById(R.id.settingsOption);
        SettingsTextView.setOnClickListener(v -> {
            // Создаем новый фрагмент
            FragmentSettingsOption FragmentSettingsOption = new FragmentSettingsOption();

            // Открываем его поверх текущей активности
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, FragmentSettingsOption)
                    .addToBackStack("fragment3_navigation")
                    .commit();
        });



        // Находим ваш TextView (замените R.id.your_textview на реальный ID)
        TextView notificationTextView = view.findViewById(R.id.notificationsOption);
        notificationTextView.setOnClickListener(v -> {
            // Создаем новый фрагмент
            FragmentNotificationsOption FragmentNotificationsOption = new FragmentNotificationsOption();

            // Открываем его поверх текущей активности
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, FragmentNotificationsOption)
                    .addToBackStack("fragment3_navigation")
                    .commit();
        });

        // Находим ваш TextView (замените R.id.your_textview на реальный ID)
        TextView loginParamsTextView = view.findViewById(R.id.loginParamsOption);
        loginParamsTextView.setOnClickListener(v -> {
            // Создаем новый фрагмент
            FragmentLoginParamsOption FragmentLoginParamsOption = new FragmentLoginParamsOption();

            // Открываем его поверх текущей активности
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, FragmentLoginParamsOption)
                    .addToBackStack("fragment3_navigation")
                    .commit();
        });

        // Находим ваш TextView (замените R.id.your_textview на реальный ID)
        TextView helpTextView = view.findViewById(R.id.helpOption);
        helpTextView.setOnClickListener(v -> {
            // Создаем новый фрагмент
            FragmentHelpOption FragmentHelpOption = new FragmentHelpOption();

            // Открываем его поверх текущей активности
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, FragmentHelpOption)
                    .addToBackStack("fragment3_navigation")
                    .commit();
        });

        return view;
    }
}