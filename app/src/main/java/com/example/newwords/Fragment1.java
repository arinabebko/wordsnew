package com.example.newwords;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Fragment1 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Подключаем layout для этого фрагмента
        View view = inflater.inflate(R.layout.fragment1, container, false);

        // Находим кнопку start по ID
        Button startButton = view.findViewById(R.id.startButton);

        // Устанавливаем обработчик нажатия
        startButton.setOnClickListener(v -> {
            // Создаем новый фрагмент
            WordsFragment startFragment = new WordsFragment();

            // Открываем его поверх текущей активности
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, startFragment)
                    .addToBackStack("fragment1_navigation")
                    .commit();
        });

        return view;
    }
}