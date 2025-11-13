package com.example.newwords;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WordsFragment extends Fragment implements StackCardAdapter.OnCardActionListener {

    private ViewPager2 viewPager2;
     private StackCardAdapter adapter;
   // private SimpleStackCardAdapter adapter; // Изменяем на новый адаптер

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
        wordRepository = new WordRepository(getContext());

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
    /**
     * Загружает слова из АКТИВНЫХ библиотек Firebase
     */
    private void loadWordsFromFirebase() {
        Log.d(TAG, "Начинаем загрузку слов из АКТИВНЫХ библиотек...");
        showLoading(true);
        debugActiveLibraries();
        wordRepository.getWordsFromActiveLibraries(new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                Log.d(TAG, "Успешно загружено слов из активных библиотек: " + words.size());

                wordList.clear();
                wordList.addAll(words);

                logWordDetails(wordList);
                // Фильтруем слова для сессии
                List<WordItem> sessionWords = getWordsForSession(wordList);

                if (sessionWords.isEmpty()) {
                    Log.d(TAG, "Нет слов для изучения в данный момент");
                    // ЗАМЕНИТЕ ЭТУ СТРОКУ:
                    showNoWordsState(); // было: showNoWordsForStudyState()
                } else {
                    Log.d(TAG, "Настройка ViewPager с " + sessionWords.size() + " словами для сессии");
                    setupViewPagerWithWords(sessionWords);
                }

                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка загрузки слов из активных библиотек: " + e.getMessage());
                Toast.makeText(getContext(), "Ошибка загрузки. Используем локальные слова", Toast.LENGTH_SHORT).show();
                setupViewPagerWithLocalWords();
                showLoading(false);
            }
        });
    }

    /**
     * Выбирает слова для текущей сессии изучения
     */


    /**
     * Выбирает слова для текущей сессии изучения
     */
    private List<WordItem> getWordsForSession(List<WordItem> allWords) {
        List<WordItem> sessionWords = new ArrayList<>();
        int maxWords = 20;

        Log.d(TAG, "=== ВЫБОР СЛОВ ДЛЯ СЕССИИ ===");
        Log.d(TAG, "Всего слов доступно: " + allWords.size());

        int newWordsCount = 0;
        int dueWordsCount = 0;
        int learnedWordsCount = 0;

        // 1. Собираем слова которые нужно показать СЕЙЧАС
        for (WordItem word : allWords) {
            if (SimpleRepetitionSystem.shouldShowInSession(word)) {
                sessionWords.add(word);

                // Логируем тип слова
                if (word.isNew() && word.needsMoreShows()) {
                    newWordsCount++;
                } else {
                    dueWordsCount++;
                }

                if (sessionWords.size() >= maxWords) break;
            } else if (word.isLearned()) {
                learnedWordsCount++;
            }
        }

        Log.d(TAG, "Статистика сессии:");
        Log.d(TAG, " - Новые слова: " + newWordsCount);
        Log.d(TAG, " - Для повторения: " + dueWordsCount);
        Log.d(TAG, " - Выученные (не показываем): " + learnedWordsCount);
        Log.d(TAG, " - Всего для сессии: " + sessionWords.size());

        return sessionWords;
    }

    /**
     * Получает слова, готовые к повторению
     */
    private List<WordItem> getDueWords(List<WordItem> allWords) {
        List<WordItem> dueWords = new ArrayList<>();
        for (WordItem word : allWords) {
            if (word.isDueForReview() && !word.isLearned()) {
                dueWords.add(word);
            }
        }

        // Сортируем по приоритету (самые старые первыми)
        Collections.sort(dueWords, (w1, w2) -> {
            if (w1.getNextReviewDate() == null) return -1;
            if (w2.getNextReviewDate() == null) return 1;
            return w1.getNextReviewDate().compareTo(w2.getNextReviewDate());
        });

        return dueWords;
    }

    /**
     * Получает новые слова
     */
    private List<WordItem> getNewWords(List<WordItem> allWords) {
        List<WordItem> newWords = new ArrayList<>();
        for (WordItem word : allWords) {
            if (word.isNew()) {
                newWords.add(word);
            }
        }
        return newWords;
    }

    /**
     * Получает слова в процессе изучения
     */
    private List<WordItem> getLearningWords(List<WordItem> allWords) {
        List<WordItem> learningWords = new ArrayList<>();
        for (WordItem word : allWords) {
            if (word.getDifficulty() == 2) { // Средняя сложность
                learningWords.add(word);
            }
        }
        return learningWords;
    }

    /**
     * Настраивает ViewPager с загруженными словами
     */
    /**
     * Настраивает ViewPager с загруженными словами
     */
    private void setupViewPagerWithWords(List<WordItem> sessionWords) {
        Log.d(TAG, "Настройка ViewPager с " + sessionWords.size() + " словами");

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // ИСПРАВЬ ЭТУ СТРОКУ: передавай sessionWords вместо wordList
                adapter = new StackCardAdapter(sessionWords, this, wordRepository); // ← sessionWords вместо wordList
                viewPager2.setAdapter(adapter);

                // Отключаем стандартные свайпы ViewPager2 (управляем кнопками)
                viewPager2.setUserInputEnabled(false);

                // Настраиваем кнопки управления
                setupControlButtons();

                // Обновляем прогресс
                updateProgress();

                Toast.makeText(getContext(), "Готово! Карточек: " + sessionWords.size(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Логирует детальную информацию о словах
     */
    private void logWordDetails(List<WordItem> words) {
        Log.d(TAG, "=== ДЕТАЛЬНАЯ ИНФОРМАЦИЯ О СЛОВАХ ===");
        for (WordItem word : words) {
            Log.d(TAG, "Слово: " + word.getWord() +
                    " | сложность: " + word.getDifficulty() +
                    " | этап: " + word.getReviewStage() +
                    " | показов: " + word.getConsecutiveShows() +
                    " | след. дата: " + word.getNextReviewDate() +
                    " | готово к повторению: " + word.isDueForReview() +
                    " | выучено: " + word.isLearned() +
                    " | нужно показывать: " + SimpleRepetitionSystem.shouldShowInSession(word));
        }
    }
    /**
     * Настраивает ViewPager с локальными словами (при ошибке загрузки)
     */
    private void setupViewPagerWithLocalWords() {
        Log.d(TAG, "Используем локальные слова");

        wordList.clear();
        wordList.addAll(createDemoWordList());

        // Используем ту же логику выбора слов для сессии
        List<WordItem> sessionWords = getWordsForSession(wordList);
        setupViewPagerWithWords(sessionWords);
    }


    /**
     * Настраивает кнопки управления для карточек
     */
    private void setupControlButtons() {
        // Находим кнопки в макете
        View view = getView();
        if (view == null) return;

        ImageButton learnedButton = view.findViewById(R.id.learnedButton);
        ImageButton reviewButton = view.findViewById(R.id.reviewButton);

        if (learnedButton != null && reviewButton != null) {
            learnedButton.setOnClickListener(v -> {
                if (adapter != null) {
                    adapter.swipeRight();
                    updateProgress();
                }
            });

            reviewButton.setOnClickListener(v -> {
                if (adapter != null) {
                    adapter.swipeLeft();
                    updateProgress();
                }
            });

            Log.d(TAG, "Кнопки управления настроены");
        } else {
            Log.w(TAG, "Кнопки управления не найдены в макете");
        }
    }
    /**
     * Показывает состояние когда нет слов для изучения
     */
    private void showNoWordsForStudyState() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                ConstraintLayout noWordsLayout = new ConstraintLayout(getContext());
                noWordsLayout.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                noWordsLayout.setBackgroundColor(0xFF322b36);

                // Текст сообщения
                TextView messageText = new TextView(getContext());
                messageText.setId(View.generateViewId());
                messageText.setText("На сегодня все слова изучены! 🎉\n\nНовые слова появятся завтра.");
                messageText.setTextColor(Color.WHITE);
                messageText.setTextSize(16f);
                messageText.setGravity(Gravity.CENTER);
                messageText.setLineSpacing(1.5f, 1.5f);

                // Кнопка возврата
                Button backButton = new Button(getContext());
                backButton.setId(View.generateViewId());
                backButton.setText("Вернуться назад");
                backButton.setBackgroundResource(R.drawable.button_primary_bg);
                backButton.setTextColor(Color.WHITE);

                // Добавляем элементы в layout
                noWordsLayout.addView(messageText);
                noWordsLayout.addView(backButton);

                // Настраиваем constraints
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(noWordsLayout);

                // Message text constraints
                constraintSet.connect(messageText.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 150);
                constraintSet.connect(messageText.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 32);
                constraintSet.connect(messageText.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 32);
                constraintSet.constrainHeight(messageText.getId(), ConstraintSet.WRAP_CONTENT);

                // Button constraints
                constraintSet.connect(backButton.getId(), ConstraintSet.TOP, messageText.getId(), ConstraintSet.BOTTOM, 32);
                constraintSet.connect(backButton.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 50);
                constraintSet.connect(backButton.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 50);
                constraintSet.constrainHeight(backButton.getId(), ConstraintSet.WRAP_CONTENT);

                constraintSet.applyTo(noWordsLayout);

                // Обработчик кнопки
                backButton.setOnClickListener(v -> {
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                });

                // Заменяем текущий view
                ViewGroup rootView = (ViewGroup) getView();
                if (rootView != null) {
                    rootView.removeAllViews();
                    rootView.addView(noWordsLayout);
                }
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

            // Если все карточки пройдены
           // if (current >= total && total > 0) {
          //      showSessionCompleted();
           // }
        }
    }

    /**
     * Показывает сообщение о завершении сессии
     */
    private void showSessionCompleted() {
       // Toast.makeText(getContext(), "🎉 Сессия завершена! Отлично поработали!", Toast.LENGTH_LONG).show();

        /**
        // Можно добавить автоматический возврат через 3 секунды
        if (getActivity() != null) {
            getActivity().getWindow().getDecorView().postDelayed(() -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }, 3000);
        }
         */
    }


    /**
     * Настраивает колоду карточек
     */

    private void debugActiveLibraries() {
        wordRepository.getUserActiveLibraries(new WordRepository.OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> activeLibraries) {
                Log.d(TAG, "=== ДЕБАГ АКТИВНЫХ БИБЛИОТЕК ===");
                Log.d(TAG, "Всего активных библиотек: " + activeLibraries.size());

                for (WordLibrary library : activeLibraries) {
                    Log.d(TAG, "Библиотека: " + library.getName() +
                            " | ID: " + library.getLibraryId() +
                            " | Активна: " + library.getIsActive());
                }

                // Проверим кеш
                wordRepository.checkCacheStatus(new WordRepository.OnCacheStatusListener() {
                    @Override
                    public void onStatusChecked(int libraryCount, int wordCount, int activeLibraryCount, int wordsFromActive) {
                        Log.d(TAG, "=== СТАТУС КЕША ===");
                        Log.d(TAG, "Библиотеки в кеше: " + libraryCount);
                        Log.d(TAG, "Слова в кеше: " + wordCount);
                        Log.d(TAG, "Активные библиотеки в кеше: " + activeLibraryCount);
                        Log.d(TAG, "Слов из активных библиотек: " + wordsFromActive);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка дебага библиотек: " + e.getMessage());
            }
        });
    }
    private void setupCardStack() {
        Log.d(TAG, "Настройка колоды с " + wordList.size() + " словами");

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // ДОБАВЬ wordRepository КАК ТРЕТИЙ ПАРАМЕТР:
                adapter = new StackCardAdapter(wordList, this, wordRepository);
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

        // Минимальная задержка для плавности (100ms вместо 1000ms)
        if (getActivity() != null) {
          //  getActivity().getWindow().getDecorView().postDelayed(() -> {
            //    showSessionCompletedState();
          //  }, 100); // 0.1 секунды вместо 1 секунды
            showSessionCompletedState();
        }
    }


    /**
     * Показывает экран завершения обучения (когда все карточки пройдены)
     */
    private void showSessionCompletedState() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                ConstraintLayout completedLayout = new ConstraintLayout(getContext());
                completedLayout.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                completedLayout.setBackgroundColor(0xFF322b36);

                // Иконка праздника
                TextView emojiIcon = new TextView(getContext());
                emojiIcon.setId(View.generateViewId());
                emojiIcon.setText("🎉");
                emojiIcon.setTextSize(64f);
                emojiIcon.setGravity(Gravity.CENTER);

                // Заголовок
                TextView titleText = new TextView(getContext());
                titleText.setId(View.generateViewId());
                titleText.setText("Молодец!");
                titleText.setTextColor(Color.WHITE);
                titleText.setTextSize(32f);
                titleText.setTypeface(titleText.getTypeface(), Typeface.BOLD);
                titleText.setGravity(Gravity.CENTER);

                // Сообщение
                TextView messageText = new TextView(getContext());
                messageText.setId(View.generateViewId());
                messageText.setText("Ты отлично поработал!\n\nПерейди в библиотеки и добавь новые слова для изучения.");
                messageText.setTextColor(0xFFCCCCCC);
                messageText.setTextSize(18f);
                messageText.setGravity(Gravity.CENTER);
                messageText.setLineSpacing(1.5f, 1.5f);

                // Кнопка "В библиотеки"
                Button librariesButton = new Button(getContext());
                librariesButton.setId(View.generateViewId());
                librariesButton.setText("Перейти в библиотеки");
                librariesButton.setBackgroundResource(R.drawable.button_primary_bg);
                librariesButton.setTextColor(Color.WHITE);
                librariesButton.setTextSize(16f);
                librariesButton.setPadding(32, 16, 32, 16);

                // Добавляем элементы в layout
                completedLayout.addView(emojiIcon);
                completedLayout.addView(titleText);
                completedLayout.addView(messageText);
                completedLayout.addView(librariesButton);

                // Настраиваем constraints
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(completedLayout);

                // Emoji constraints
                constraintSet.connect(emojiIcon.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 150);
                constraintSet.connect(emojiIcon.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                constraintSet.connect(emojiIcon.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                constraintSet.constrainHeight(emojiIcon.getId(), ConstraintSet.WRAP_CONTENT);

                // Title constraints
                constraintSet.connect(titleText.getId(), ConstraintSet.TOP, emojiIcon.getId(), ConstraintSet.BOTTOM, 16);
                constraintSet.connect(titleText.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                constraintSet.connect(titleText.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                constraintSet.constrainHeight(titleText.getId(), ConstraintSet.WRAP_CONTENT);

                // Message constraints
                constraintSet.connect(messageText.getId(), ConstraintSet.TOP, titleText.getId(), ConstraintSet.BOTTOM, 32);
                constraintSet.connect(messageText.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 48);
                constraintSet.connect(messageText.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 48);
                constraintSet.constrainHeight(messageText.getId(), ConstraintSet.WRAP_CONTENT);

                // Button constraints
                constraintSet.connect(librariesButton.getId(), ConstraintSet.TOP, messageText.getId(), ConstraintSet.BOTTOM, 48);
                constraintSet.connect(librariesButton.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 50);
                constraintSet.connect(librariesButton.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 50);
                constraintSet.constrainHeight(librariesButton.getId(), ConstraintSet.WRAP_CONTENT);

                constraintSet.applyTo(completedLayout);

                librariesButton.setOnClickListener(v -> {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();

                        // Сначала переключаем на библиотеки
                        mainActivity.switchToLibraryTab();

                        // Затем закрываем WordsFragment
                        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                            getParentFragmentManager().popBackStack();
                        }
                    }
                });

                // Заменяем текущий view
                ViewGroup rootView = (ViewGroup) getView();
                if (rootView != null) {
                    rootView.removeAllViews();
                    rootView.addView(completedLayout);
                }
            });
        }
    }
    /**
     * Показывает состояние когда нет слов для изучения (в начале)
     */
    private void showNoWordsState() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                ConstraintLayout noWordsLayout = new ConstraintLayout(getContext());
                noWordsLayout.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                noWordsLayout.setBackgroundColor(0xFF322b36);

                // Иконка
                TextView emojiIcon = new TextView(getContext());
                emojiIcon.setId(View.generateViewId());
                emojiIcon.setText("📚");
                emojiIcon.setTextSize(64f);
                emojiIcon.setGravity(Gravity.CENTER);

                // Заголовок
                TextView titleText = new TextView(getContext());
                titleText.setId(View.generateViewId());
                titleText.setText("Нет слов для изучения");
                titleText.setTextColor(Color.WHITE);
                titleText.setTextSize(28f);
                titleText.setTypeface(titleText.getTypeface(), Typeface.BOLD);
                titleText.setGravity(Gravity.CENTER);

                // Сообщение
                TextView messageText = new TextView(getContext());
                messageText.setId(View.generateViewId());
                messageText.setText("Все доступные слова изучены!\n\nДобавь новые слова в библиотеках или подожди до завтра.");
                messageText.setTextColor(0xFFCCCCCC);
                messageText.setTextSize(16f);
                messageText.setGravity(Gravity.CENTER);
                messageText.setLineSpacing(1.5f, 1.5f);

                // Кнопка "В библиотеки"
                Button librariesButton = new Button(getContext());
                librariesButton.setId(View.generateViewId());
                librariesButton.setText("Перейти в библиотеки");
                librariesButton.setBackgroundResource(R.drawable.button_primary_bg);
                librariesButton.setTextColor(Color.WHITE);

                // Добавляем элементы в layout
                noWordsLayout.addView(emojiIcon);
                noWordsLayout.addView(titleText);
                noWordsLayout.addView(messageText);
                noWordsLayout.addView(librariesButton);

                // Настраиваем constraints
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(noWordsLayout);

                // Emoji constraints
                constraintSet.connect(emojiIcon.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 150);
                constraintSet.connect(emojiIcon.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                constraintSet.connect(emojiIcon.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                constraintSet.constrainHeight(emojiIcon.getId(), ConstraintSet.WRAP_CONTENT);

                // Title constraints
                constraintSet.connect(titleText.getId(), ConstraintSet.TOP, emojiIcon.getId(), ConstraintSet.BOTTOM, 16);
                constraintSet.connect(titleText.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                constraintSet.connect(titleText.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                constraintSet.constrainHeight(titleText.getId(), ConstraintSet.WRAP_CONTENT);

                // Message constraints
                constraintSet.connect(messageText.getId(), ConstraintSet.TOP, titleText.getId(), ConstraintSet.BOTTOM, 32);
                constraintSet.connect(messageText.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 48);
                constraintSet.connect(messageText.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 48);
                constraintSet.constrainHeight(messageText.getId(), ConstraintSet.WRAP_CONTENT);

                // Button constraints
                constraintSet.connect(librariesButton.getId(), ConstraintSet.TOP, messageText.getId(), ConstraintSet.BOTTOM, 48);
                constraintSet.connect(librariesButton.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 50);
                constraintSet.connect(librariesButton.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 50);
                constraintSet.constrainHeight(librariesButton.getId(), ConstraintSet.WRAP_CONTENT);

                constraintSet.applyTo(noWordsLayout);

                // Обработчик кнопки
                librariesButton.setOnClickListener(v -> {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();

                        // Сначала переключаем на библиотеки
                        mainActivity.switchToLibraryTab();

                        // Затем закрываем WordsFragment
                        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                            getParentFragmentManager().popBackStack();
                        }
                    }
                });
                // Заменяем текущий view
                ViewGroup rootView = (ViewGroup) getView();
                if (rootView != null) {
                    rootView.removeAllViews();
                    rootView.addView(noWordsLayout);
                }
            });
        }
    }
}