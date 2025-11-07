package com.example.newwords;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class WordsFragment extends Fragment implements WordCardAdapter.OnWordActionListener {

    private ViewPager2 viewPager2;
    private WordCardAdapter adapter;
    private WordRepository wordRepository;
    private List<WordItem> wordList = new ArrayList<>();
    private ProgressBar progressBar;
    private int previousPosition = 0;

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
        progressBar = view.findViewById(R.id.progressBar); // Добавим ProgressBar позже

        // Настраиваем кнопку назад
        setupBackButton(view);

        // Загружаем слова из Firebase
        loadWordsFromFirebase();

        return view;
    }

    /**
     * Загружает слова из Firebase Firestore
     */
    private void loadWordsFromFirebase() {
        Log.d(TAG, "Начинаем загрузку слов из Firebase...");

        // Показываем прогресс-бар (если он есть)
        showLoading(true);

        wordRepository.getUserActiveWords(new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                Log.d(TAG, "Успешно загружено слов: " + words.size());

                wordList.clear();
                wordList.addAll(words);

                // Если слов нет, добавляем демо-слова
                if (wordList.isEmpty()) {
                    Log.d(TAG, "Слов нет, добавляем демо-слова...");
                    addDemoWords();
                } else {
                    Log.d(TAG, "Настройка ViewPager с загруженными словами...");
                    setupViewPagerWithWords();
                }

                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка загрузки слов: " + e.getMessage());

                // Если ошибка, используем локальные слова
                Toast.makeText(getContext(), "Ошибка загрузки. Используем локальные слова", Toast.LENGTH_SHORT).show();
                setupViewPagerWithLocalWords();
                showLoading(false);
            }
        });
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

                    Log.d(TAG, "Добавлено слово: " + addedWord.getWord() + " (" + wordsAdded[0] + "/" + demoWords.size() + ")");

                    // Когда все слова добавлены, настраиваем ViewPager
                    if (wordsAdded[0] == demoWords.size()) {
                        Log.d(TAG, "Все демо-слова добавлены, настраиваем ViewPager...");
                        setupViewPagerWithWords();
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Ошибка добавления слова: " + e.getMessage());
                    wordsAdded[0]++;

                    // Все равно добавляем слово локально
                    wordList.add(word);

                    if (wordsAdded[0] == demoWords.size()) {
                        setupViewPagerWithWords();
                    }
                }
            });
        }
    }

    /**
     * Настраивает ViewPager с загруженными словами
     */
    private void setupViewPagerWithWords() {
        Log.d(TAG, "Настройка ViewPager с " + wordList.size() + " словами");

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                adapter = new WordCardAdapter(wordList, WordsFragment.this);
                viewPager2.setAdapter(adapter);

                // Настраиваем обработчик свайпов
                setupSwipeListener();

                Toast.makeText(getContext(), "Загружено слов: " + wordList.size(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Настраивает ViewPager с локальными словами (при ошибке)
     */
    private void setupViewPagerWithLocalWords() {
        Log.d(TAG, "Используем локальные слова");

        wordList.clear();
        wordList.addAll(createDemoWordList());
        setupViewPagerWithWords();
    }

    /**
     * Настраивает обработчик свайпов для отслеживания прогресса
     */
    private void setupSwipeListener() {
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Логируем свайпы для отладки
                if (position > previousPosition) {
                    Log.d(TAG, "Свайп влево → " + wordList.get(previousPosition).getWord());
                    onWordNotLearned(wordList.get(previousPosition));
                } else if (position < previousPosition) {
                    Log.d(TAG, "Свайп вправо → " + wordList.get(previousPosition).getWord());
                    onWordLearned(wordList.get(previousPosition));
                }
                previousPosition = position;
            }
        });
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
        if (getActivity() != null && progressBar != null) {
            getActivity().runOnUiThread(() -> {
                if (show) {
                    progressBar.setVisibility(View.VISIBLE);
                    viewPager2.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.GONE);
                    viewPager2.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    // === РЕАЛИЗАЦИЯ ИНТЕРФЕЙСА WordCardAdapter.OnWordActionListener ===

    @Override
    public void onWordLearned(WordItem word) {
        Log.d(TAG, "Слово выучено: " + word.getWord());

        // Обновляем слово в базе
        wordRepository.updateWord(word);

        // Показываем уведомление
        if (getContext() != null) {
            Toast.makeText(getContext(), "✅ " + word.getWord() + " - выучено!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWordNotLearned(WordItem word) {
        Log.d(TAG, "Слово не выучено: " + word.getWord());

        // Обновляем слово в базе
        wordRepository.updateWord(word);

        // Показываем уведомление
        if (getContext() != null) {
            Toast.makeText(getContext(), "❌ " + word.getWord() + " - нужно повторить", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWordFavoriteToggled(WordItem word, boolean isFavorite) {
        Log.d(TAG, "Избранное изменено: " + word.getWord() + " = " + isFavorite);

        // Обновляем слово в базе
        wordRepository.updateWord(word);

        // Показываем уведомление
        if (getContext() != null) {
            String message = isFavorite ? "★ Добавлено в избранное" : "☆ Убрано из избранного";
            Toast.makeText(getContext(), message + ": " + word.getWord(), Toast.LENGTH_SHORT).show();
        }
    }
}