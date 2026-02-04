package com.example.newwords;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout; // ДОБАВЬ ЭТОТ ИМПОРТ

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

public class Fragment1 extends Fragment {

    private TextView daysTextView, wordsInProgressTextView, wordsLearnedTextView, goodJobTextView;
    private WordRepository wordRepository;
    private boolean isProcessingClick = false;
    private static final long BUTTON_COOLDOWN = 2000; // 2 секунды
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
            String inProgressText = " " + stats.getWordsInProgress();
            wordsInProgressTextView.setText(inProgressText);

            // Выучено слов
            String learnedText = " " + stats.getWordsLearned();
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
        ProgressBar progressBar = view.findViewById(R.id.loadingProgressBar);

        startButton.setOnClickListener(v -> {
            // Если уже обрабатываем клик - игнорируем
            if (isProcessingClick) {
                return;
            }

            // Устанавливаем флаг
            isProcessingClick = true;

            // Визуальная обратная связь
            startButton.setEnabled(false);
            startButton.setAlpha(0.5f);
            progressBar.setVisibility(View.VISIBLE);

            // Показываем сообщение
            Toast.makeText(getContext(), "Начинаем обучение...", Toast.LENGTH_SHORT).show();

            // Запускаем с задержкой
            startButton.postDelayed(() -> {
                checkIfWordsAvailable();

                // Восстанавливаем кнопку через 2 секунды
                startButton.postDelayed(() -> {
                    isProcessingClick = false;
                    startButton.setEnabled(true);
                    startButton.setAlpha(1f);
                    progressBar.setVisibility(View.GONE);
                }, BUTTON_COOLDOWN);
            }, 500); // Небольшая задержка перед вызовом checkIfWordsAvailable
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Сбрасываем состояние при возвращении
        if (getView() != null) {
            Button startButton = getView().findViewById(R.id.startButton);
            ProgressBar progressBar = getView().findViewById(R.id.loadingProgressBar);

            isProcessingClick = false;
            startButton.setEnabled(true);
            startButton.setAlpha(1f);
            progressBar.setVisibility(View.GONE);


            loadUserStats();
        }
    }

    private void checkIfWordsAvailable() {
        // Сначала получаем текущий язык
        LanguageManager languageManager = new LanguageManager(getContext());
        String currentLanguage = languageManager.getCurrentLanguage();

        Log.d("Fragment1", "Проверка слов для языка: " + currentLanguage);

        // Используем новую версию метода с языком
        wordRepository.getWordsFromActiveLibrariesFirebase(currentLanguage, new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                Log.d("Fragment1", "Для языка " + currentLanguage + " найдено слов: " + words.size());

                if (words.isEmpty()) {
                    Toast.makeText(getContext(), "Нет слов для изучения. Добавьте слова в библиотеках.", Toast.LENGTH_LONG).show();
                } else {
                    // Создаем фрагмент с указанием языка
                    WordsFragment startFragment = WordsFragment.newInstance(currentLanguage);

                    // Открываем его
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(android.R.id.content, startFragment)
                            .addToBackStack("fragment1_navigation")
                            .commit();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("Fragment1", "Ошибка загрузки слов для языка " + currentLanguage, e);
                Toast.makeText(getContext(), "Ошибка загрузки слов", Toast.LENGTH_SHORT).show();


            }
        });
    }

    private void setupBottomButtons(View view) {
        // Находим новые кнопки по правильным ID
        LinearLayout searchButtonLayout = view.findViewById(R.id.searchButtonLayout);
        LinearLayout addButtonLayout = view.findViewById(R.id.addButtonLayout);

        // Обработчик кнопки поиска
        searchButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSearchFragment();
            }
        });

        // Обработчик кнопки добавления
        addButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddWordDialog();
            }
        });
    }

    private void openSearchFragment() {
        // Создаем фрагмент поиска
        SearchWordsFragment searchFragment = new SearchWordsFragment();

        // Открываем фрагмент поиска
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, searchFragment)
                .addToBackStack("search_navigation")
                .commit();
    }

    //@Override


    private void showAddWordDialog() {
        // Используем диалог с выбором библиотеки
        AddWordWithLibraryDialog dialog = AddWordWithLibraryDialog.newInstance();
        dialog.setOnWordAddedListener(new AddWordWithLibraryDialog.OnWordAddedListener() {
            @Override
            public void onWordAdded(String word, String translation, String note, String libraryId) {
                // Создаем новое слово
                WordItem newWord = new WordItem(word, translation, note);

                // Добавляем слово в выбранную библиотеку
                wordRepository.addWordToCustomLibrary(libraryId, newWord, new WordRepository.OnWordAddedListener() {
                    @Override
                    public void onWordAdded(WordItem addedWord) {
                        Toast.makeText(getContext(), "Слово добавлено в библиотеку!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "Ошибка добавления слова: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        dialog.show(getParentFragmentManager(), "add_word_with_library_dialog");
    }
}