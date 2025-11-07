package com.example.newwords;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class WordsFragment extends Fragment implements StackCardAdapter.OnCardActionListener {

    private ViewPager2 viewPager2;
    private StackCardAdapter adapter;
    private WordRepository wordRepository;
    private List<WordItem> wordList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView progressText;

    private static final String TAG = "WordsFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_words, container, false);

        // Инициализируем репозиторий
        wordRepository = new WordRepository();

        // Находим View элементы
        viewPager2 = view.findViewById(R.id.viewPager2);
        progressBar = view.findViewById(R.id.progressBar);

        // Добавим TextView для прогресса (нужно добавить в макет)
        progressText = view.findViewById(R.id.progressText);

        // Настраиваем кнопку назад
        setupBackButton(view);

        // Настраиваем свайпы
        setupSwipeGestures(view);

        // Загружаем слова из Firebase
        loadWordsFromFirebase();

        return view;
    }

    /**
     * Настраивает жесты свайпа
     */
    private void setupSwipeGestures(View view) {
        // Кнопка "Выучено" (свайп вправо)
        ImageButton learnedButton = view.findViewById(R.id.learnedButton);
        // Кнопка "Повторить" (свайп влево)
        ImageButton reviewButton = view.findViewById(R.id.reviewButton);

        if (learnedButton != null) {
            learnedButton.setOnClickListener(v -> {
                if (adapter != null) {
                    adapter.swipeRight();
                    updateProgress();
                }
            });
        }

        if (reviewButton != null) {
            reviewButton.setOnClickListener(v -> {
                if (adapter != null) {
                    adapter.swipeLeft();
                    updateProgress();
                }
            });
        }
    }

    /**
     * Загружает слова из Firebase Firestore
     */
    private void loadWordsFromFirebase() {
        Log.d(TAG, "Начинаем загрузку слов из Firebase...");
        showLoading(true);

        wordRepository.getLearningSessionWords(new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                Log.d(TAG, "Успешно загружено слов для сессии: " + words.size());

                wordList.clear();
                wordList.addAll(words);

                if (wordList.isEmpty()) {
                    Log.d(TAG, "Слов нет, добавляем демо-слова...");
                    addDemoWords();
                } else {
                    Log.d(TAG, "Настройка колоды карточек...");
                    setupCardStack();
                }

                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка загрузки слов: " + e.getMessage());
                Toast.makeText(getContext(), "Ошибка загрузки. Используем локальные слова", Toast.LENGTH_SHORT).show();
                setupWithLocalWords();
                showLoading(false);
            }
        });
    }

    /**
     * Настраивает колоду карточек
     */
    private void setupCardStack() {
        Log.d(TAG, "Настройка колоды с " + wordList.size() + " словами");

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                adapter = new StackCardAdapter(wordList, this);
                viewPager2.setAdapter(adapter);

                // Отключаем стандартные свайпы ViewPager2
                viewPager2.setUserInputEnabled(false);

                updateProgress();

                Toast.makeText(getContext(), "Готово! Карточек: " + wordList.size(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Обновляет отображение прогресса
     */
    private void updateProgress() {
        if (adapter != null && progressText != null) {
            int current = adapter.getCurrentProgress() + 1;
            int total = adapter.getTotalCards();

            String progress = current + "/" + total;
            progressText.setText(progress);

            // Обновляем ProgressBar
            if (progressBar != null) {
                int progressPercent = total > 0 ? (current * 100) / total : 0;
                progressBar.setProgress(progressPercent);
            }
        }
    }

    /**
     * Использует локальные слова при ошибке
     */
    private void setupWithLocalWords() {
        wordList.clear();
        wordList.addAll(createDemoWordList());
        setupCardStack();
    }

    /**
     * Добавляет демо-слова если база пустая
     */
    private void addDemoWords() {
        Log.d(TAG, "Добавляем демо-слова...");

        List<WordItem> demoWords = createDemoWordList();
        final int[] wordsAdded = {0};

        for (WordItem word : demoWords) {
            wordRepository.addCustomWord(word, new WordRepository.OnWordAddedListener() {
                @Override
                public void onWordAdded(WordItem addedWord) {
                    wordList.add(addedWord);
                    wordsAdded[0]++;

                    if (wordsAdded[0] == demoWords.size()) {
                        setupCardStack();
                    }
                }

                @Override
                public void onError(Exception e) {
                    wordsAdded[0]++;
                    wordList.add(word);

                    if (wordsAdded[0] == demoWords.size()) {
                        setupCardStack();
                    }
                }
            });
        }
    }

    /**
     * Создает список демо-слов
     */
    private List<WordItem> createDemoWordList() {
        List<WordItem> demoWords = new ArrayList<>();
        demoWords.add(new WordItem("hello", "привет", "Основное приветствие"));
        demoWords.add(new WordItem("world", "мир", "Планета Земля"));
        demoWords.add(new WordItem("spring", "весна", "Сезон года"));
        demoWords.add(new WordItem("start", "начало", "Противоположность концу"));
        demoWords.add(new WordItem("note", "примечание", "Дополнительная информация"));
        demoWords.add(new WordItem("apple", "яблоко", "Фрукт"));
        demoWords.add(new WordItem("book", "книга", "Для чтения"));
        return demoWords;
    }

    /**
     * Настраивает кнопку назад
     */
    private void setupBackButton(View view) {
        ImageButton backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    /**
     * Показывает/скрывает индикатор загрузки
     */
    private void showLoading(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
                if (viewPager2 != null) {
                    viewPager2.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
        }
    }

    // === РЕАЛИЗАЦИЯ ИНТЕРФЕЙСА StackCardAdapter.OnCardActionListener ===

    @Override
    public void onCardLearned(WordItem word) {
        Log.d(TAG, "Слово выучено: " + word.getWord());

        // Сохраняем в базу
        wordRepository.markWordAsLearned(word.getWordId(),
                () -> Log.d(TAG, "Слово помечено как выучено"),
                e -> Log.e(TAG, "Ошибка сохранения: " + e.getMessage())
        );

        Toast.makeText(getContext(), "✅ " + word.getWord() + " - выучено!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCardNotLearned(WordItem word) {
        Log.d(TAG, "Слово не выучено: " + word.getWord());

        // Сохраняем в базу для повторения
        wordRepository.markWordForReview(word.getWordId(),
                () -> Log.d(TAG, "Слово отложено для повторения"),
                e -> Log.e(TAG, "Ошибка сохранения: " + e.getMessage())
        );

        Toast.makeText(getContext(), "🔄 " + word.getWord() + " - повторим позже", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCardFavoriteToggled(WordItem word, boolean isFavorite) {
        Log.d(TAG, "Избранное изменено: " + word.getWord() + " = " + isFavorite);
        wordRepository.updateWord(word);

        String message = isFavorite ? "★ Добавлено в избранное" : "☆ Убрано из избранного";
        Toast.makeText(getContext(), message + ": " + word.getWord(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAllCardsCompleted() {
        Log.d(TAG, "Все карточки пройдены!");
        Toast.makeText(getContext(), "🎉 Все карточки пройдены! Молодец!", Toast.LENGTH_LONG).show();

        // Можно добавить переход к результатам или повторение
    }
}