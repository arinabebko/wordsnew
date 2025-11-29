package com.example.newwords;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

public class Fragment1 extends Fragment {

    private TextView daysTextView, wordsInProgressTextView, wordsLearnedTextView, goodJobTextView;
    private WordRepository wordRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment1, container, false);

        // Инициализируем репозиторий
        wordRepository = new WordRepository(getContext());

        // Находим все TextView
        initViews(view);

        // Загружаем статистику
        loadUserStats();

        // Настраиваем кнопку start
        setupStartButton(view);

        // Настраиваем кнопки поиска и добавления
        setupBottomButtons(view);

        return view;
    }

    private void initViews(View view) {
        daysTextView = view.findViewById(R.id.daysTextView);
        wordsInProgressTextView = view.findViewById(R.id.wordsInProgressTextView);
        wordsLearnedTextView = view.findViewById(R.id.wordsLearnedTextView);
        goodJobTextView = view.findViewById(R.id.goodJobTextView);
    }

    private void loadUserStats() {
        wordRepository.getUserStats(new WordRepository.OnStatsLoadedListener() {
            @Override
            public void onStatsLoaded(UserStats stats) {
                updateStatsUI(stats);
            }

            @Override
            public void onError(Exception e) {
                Log.e("Fragment1", "Ошибка загрузки статистики: " + e.getMessage());
                showDefaultStats();
            }
        });
    }

    private void updateStatsUI(UserStats stats) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            // Обновляем дни подряд
            String daysText = stats.getStreakDays() + " дней подряд";
            daysTextView.setText(daysText);

            // Слова в процессе
            String inProgressText = "слов в процессе: " + stats.getWordsInProgress();
            wordsInProgressTextView.setText(inProgressText);

            // Выучено слов
            String learnedText = "выучено слов: " + stats.getWordsLearned();
            wordsLearnedTextView.setText(learnedText);

            // Мотивационное сообщение
            updateMotivationalMessage(stats);
        });
    }

    private void updateMotivationalMessage(UserStats stats) {
        String message;
        if (stats.getStreakDays() >= 7) {
            message = "great\nwork!";
        } else if (stats.getTodayProgress() >= 10) {
            message = "good\njob!";
        } else if (stats.getWordsLearned() > 0) {
            message = "keep\ngoing!";
        } else {
            message = "let's\nstart!";
        }
        goodJobTextView.setText(message);
    }

    private void showDefaultStats() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            daysTextView.setText("0 дней подряд");
            wordsInProgressTextView.setText("слов в процессе: 0");
            wordsLearnedTextView.setText("выучено слов: 0");
            goodJobTextView.setText("let's\nstart!");
        });
    }

    private void setupStartButton(View view) {
        Button startButton = view.findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> {
            // Проверяем есть ли слова для изучения
            checkIfWordsAvailable();
        });
    }

    private void checkIfWordsAvailable() {
        wordRepository.getWordsFromActiveLibrariesFirebase(new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                if (words.isEmpty()) {
                    Toast.makeText(getContext(), "Нет слов для изучения. Добавьте слова в библиотеках.", Toast.LENGTH_LONG).show();
                } else {
                    // Создаем новый фрагмент
                    WordsFragment startFragment = new WordsFragment();

                    // Открываем его поверх текущей активности
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(android.R.id.content, startFragment)
                            .addToBackStack("fragment1_navigation")
                            .commit();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Ошибка загрузки слов", Toast.LENGTH_SHORT).show();
                // Все равно переходим, там будет своя обработка
                WordsFragment startFragment = new WordsFragment();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(android.R.id.content, startFragment)
                        .addToBackStack("fragment1_navigation")
                        .commit();
            }
        });
    }

    private void setupBottomButtons(View view) {
        EditText searchEditText = view.findViewById(R.id.searchEditText);
        ImageButton searchButton = view.findViewById(R.id.searchButton);
        ImageButton addButton = view.findViewById(R.id.addButton);

        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            } else {
                Toast.makeText(getContext(), "Введите поисковый запрос", Toast.LENGTH_SHORT).show();
            }
        });

        addButton.setOnClickListener(v -> {
            showAddWordDialog();
        });
    }

    private void performSearch(String query) {
        // TODO: Реализовать поиск по словам
        Toast.makeText(getContext(), "Поиск: " + query, Toast.LENGTH_SHORT).show();
    }

    private void showAddWordDialog() {
        // TODO: Реализовать диалог добавления слова
        Toast.makeText(getContext(), "Добавление слова", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем статистику при возвращении на фрагмент
        loadUserStats();
    }
}