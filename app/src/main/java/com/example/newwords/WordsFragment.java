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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WordsFragment extends Fragment implements StackCardAdapter.OnCardActionListener {
    private static final String ARG_LANGUAGE = "current_language"; // ← ДОБАВЬТЕ ЭТУ СТРОКУ
    private ViewPager2 viewPager2;
     private StackCardAdapter adapter;
   // private SimpleStackCardAdapter adapter; // Изменяем на новый адаптер

    private WordRepository wordRepository;
    private List<WordItem> wordList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView progressText;
    private String currentLanguage = "en"; // по умолчанию

    private static final String TAG = "WordsFragment";

    // Создать новый экземпляр с указанием языка
    public static WordsFragment newInstance(String currentLanguage) {
        WordsFragment fragment = new WordsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LANGUAGE, currentLanguage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Получаем переданный язык
        if (getArguments() != null) {
            currentLanguage = getArguments().getString(ARG_LANGUAGE, "en");
            Log.d(TAG, "📱 WordsFragment создан для языка: " + currentLanguage);
        }
    }
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
                    //  updateProgress();
                }
            });
        }

        if (reviewButton != null) {
            reviewButton.setOnClickListener(v -> {
                if (adapter != null) {
                    adapter.swipeLeft();
                    //  updateProgress();
                }
            });
        }
    }

    /**
     * Загружает слова из Firebase Firestore
     */
    /**
     * Загружает слова из АКТИВНЫХ библиотек Firebase

    private void loadWordsFromFirebase() {
        Log.d(TAG, "Начинаем загрузку слов из Firebase...");
        showLoading(true);

        // ИСПОЛЬЗУЙТЕ НОВЫЙ МЕТОД:
        wordRepository.getWordsFromActiveLibrariesFirebase(new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                Log.d(TAG, "Успешно загружено слов: " + words.size());
                Log.d(TAG, "Загружено слов: " + words.size());

                for (WordItem word : words) {
                    Log.d(TAG, "=== СЛОВО ИЗ " + (word.isCustomWord() ? "КАСТОМНОЙ" : "БИБЛИОТЕКИ") + " ===");
                    Log.d(TAG, "Слово: " + word.getWord());
                    Log.d(TAG, "Stage: " + word.getReviewStage());
                    Log.d(TAG, "Shows: " + word.getConsecutiveShows());
                    Log.d(TAG, "Difficulty: " + word.getDifficulty());
                    Log.d(TAG, "Next Review: " + word.getNextReviewDate());
                }
                wordList.clear();
                wordList.addAll(words);

                if (wordList.isEmpty()) {
                    Log.d(TAG, "Нет слов для изучения");
                    showNoWordsState();
                } else {
                    List<WordItem> sessionWords = getWordsForSession(wordList);
                    Log.d(TAG, "Слов для сессии: " + sessionWords.size());

                    if (sessionWords.isEmpty()) {
                        showNoWordsState();
                    } else {
                        setupViewPagerWithWords(sessionWords);
                    }
                }
                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка загрузки слов: " + e.getMessage());
                // Пробуем локальный способ как запасной вариант
                loadWordsFromLocalCache();
                showLoading(false);
            }
        });
    }
     */
    /**
     * Обрабатывает загруженные слова (вынес в отдельный метод для чистоты)
     */
    private void processLoadedWords(List<WordItem> words) {
        Log.d(TAG, "Загружено слов: " + words.size());

        wordList.clear();
        wordList.addAll(words);

        // ✅ ПРАВИЛЬНЫЙ ПЕРЕСЧЕТ статистики
        wordRepository.recalculateAllStats(wordList, () -> {
            Log.d(TAG, "✅ Статистика полностью пересчитана");
        });


        for (WordItem word : words) {
            Log.d(TAG, "=== СЛОВО ИЗ " + (word.isCustomWord() ? "КАСТОМНОЙ" : "БИБЛИОТЕКИ") + " ===");
            Log.d(TAG, "Слово: " + word.getWord());
            Log.d(TAG, "Stage: " + word.getReviewStage());
            Log.d(TAG, "Shows: " + word.getConsecutiveShows());
            Log.d(TAG, "Difficulty: " + word.getDifficulty());
            Log.d(TAG, "Next Review: " + word.getNextReviewDate());
        }

        wordList.clear();
        wordList.addAll(words);

        // ✅ ДОБАВИТЬ: Обновляем статистику "слов в процессе"
        updateWordsInProgressStats(wordList);

        if (wordList.isEmpty()) {
            Log.d(TAG, "Нет слов для изучения");
            showNoWordsState();
        } else {
            List<WordItem> sessionWords = getWordsForSession(wordList);
            Log.d(TAG, "Слов для сессии: " + sessionWords.size());

            if (sessionWords.isEmpty()) {
                showNoWordsState();
            } else {
                setupViewPagerWithWords(sessionWords);
            }
        }
    }
    /**
     * Обновляет статистику "слов в процессе изучения"
     */
    /**
     * Обновляет статистику "слов в процессе изучения"
     */
    private void updateWordsInProgressStats(List<WordItem> allWords) {
        // Создаем final переменную для использования в лямбде
        final int wordsInProgress = countWordsInProgress(allWords);

        Log.d(TAG, "📊 Слов в процессе изучения: " + wordsInProgress);

        // Используем публичный метод
        wordRepository.updateStatsAsync(stats -> {
            stats.setWordsInProgress(wordsInProgress);
            Log.d(TAG, "✅ Статистика обновлена: " + wordsInProgress + " слов в процессе");
            return stats;
        });
    }

    /**
     * Считает слова в процессе изучения
     */
    /**
     * Правильно считает слова в процессе изучения
     */
    private int countWordsInProgress(List<WordItem> allWords) {
        int count = 0;
        for (WordItem word : allWords) {
            // Слово в процессе если оно не выучено И готово к повторению
            if (!SimpleRepetitionSystem.isLearnedWord(word) &&
                    SimpleRepetitionSystem.shouldShowInSession(word)) {
                count++;
            }
        }
        Log.d(TAG, "📊 Слов в процессе (правильный подсчет): " + count);
        return count;
    }
    private void loadWordsFromLocalCache() {
        Log.d(TAG, "Пробуем загрузить слова из локального кеша для языка: " + currentLanguage);

        wordRepository.ensureWordProgressStructure(new WordRepository.OnSuccessListener() {
            @Override
            public void onSuccess() {
                // Используем версию с языком
                wordRepository.getWordsFromActiveLibrariesFirebase(currentLanguage, new WordRepository.OnWordsLoadedListener() {
                    @Override
                    public void onWordsLoaded(List<WordItem> words) {
                        if (words.isEmpty()) {
                         //   showNoWordsForLanguage();
                        } else {
                            List<WordItem> sessionWords = getWordsForSession(words);
                            setupViewPagerWithWords(sessionWords);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Ошибка локальной загрузки: " + e.getMessage());
                        //  showNoWordsForLanguage();
                    }
                });
            }
        });
    }

    /**
     * Загружает слова из Firebase с учетом текущего языка
     */
    private void loadWordsFromFirebase() {
        Log.d(TAG, "Начинаем загрузку слов из Firebase для языка: " + currentLanguage);
        showLoading(true);

        // СНАЧАЛА проверяем/создаем структуру word_progress
        wordRepository.ensureWordProgressStructure(new WordRepository.OnSuccessListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ Структура word_progress готова, загружаем слова для языка: " + currentLanguage);

                // ТЕПЕРЬ загружаем слова для конкретного языка
                wordRepository.getWordsFromActiveLibrariesFirebase(currentLanguage, new WordRepository.OnWordsLoadedListener() {
                    @Override
                    public void onWordsLoaded(List<WordItem> words) {
                        Log.d(TAG, "✅ Для языка " + currentLanguage + " загружено: " + words.size() + " слов");
                        processLoadedWords(words);
                        showLoading(false);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "❌ Ошибка загрузки слов для языка " + currentLanguage, e);


                        showLoading(false);
                    }
                });
            }
        });
    }
    /**
     * Фильтрует слова по языку библиотеки
     */
    private List<WordItem> filterWordsByLanguage(List<WordItem> allWords) {
        List<WordItem> filteredWords = new ArrayList<>();

        // К сожалению, WordItem не содержит информацию о языке библиотеки
        // Нужно загружать информацию о библиотеках

        // Временное решение: загрузим библиотеки и отфильтруем
        wordRepository.getUserActiveLibrariesForLanguage(currentLanguage, new WordRepository.OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> libraries) {
                Log.d(TAG, "📚 Найдено библиотек для языка " + currentLanguage + ": " + libraries.size());

                // Собираем ID библиотек для этого языка
                List<String> libraryIds = new ArrayList<>();
                for (WordLibrary library : libraries) {
                    libraryIds.add(library.getLibraryId());
                    Log.d(TAG, "   Библиотека: " + library.getName() + " (ID: " + library.getLibraryId() + ")");
                }

                // Фильтруем слова по ID библиотек
                for (WordItem word : allWords) {
                    if (word.getLibraryId() != null && libraryIds.contains(word.getLibraryId())) {
                        filteredWords.add(word);
                    }
                }

                Log.d(TAG, "✅ После фильтрации: " + filteredWords.size() + " слов");

                // Обрабатываем отфильтрованные слова
                if (filteredWords.isEmpty()) {
                   // showNoWordsForLanguage();
                } else {
                    processLoadedWords(filteredWords);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Ошибка загрузки библиотек для фильтрации", e);
                // Если не можем отфильтровать, показываем все
                processLoadedWords(allWords);
            }
        });

        return filteredWords;
    }



    /**
     * Выбирает слова для текущей сессии изучения
     */


    /**
     * Выбирает слова для текущей сессии изучения
     */
    private List<WordItem> getWordsForSession(List<WordItem> allWords) {
        List<WordItem> sessionWords = new ArrayList<>();
        final int maxWords = 20; // ← СДЕЛАТЬ final

        Log.d(TAG, "=== ВЫБОР СЛОВ ДЛЯ СЕССИИ ===");
        Log.d(TAG, "Всего слов доступно: " + allWords.size());

        // Используем final переменные для счетчиков
        final int[] counters = {0, 0, 0, 0}; // [new, due, learned, inProgress]

        // 1. Собираем слова которые нужно показать СЕЙЧАС
        for (WordItem word : allWords) {
            if (SimpleRepetitionSystem.shouldShowInSession(word)) {
                sessionWords.add(word);

                // Логируем тип слова
                if (word.isNew()) {
                    counters[0]++;
                } else {
                    counters[1]++;
                }

                if (sessionWords.size() >= maxWords) break;
            } else if (word.isLearned()) {
                counters[2]++;
            }

            // Считаем слова в процессе
            if (!word.isLearned()) {
                counters[3]++;
            }
        }

        Log.d(TAG, "Статистика сессии:");
        Log.d(TAG, " - Новые слова: " + counters[0]);
        Log.d(TAG, " - Для повторения: " + counters[1]);
        Log.d(TAG, " - Выученные (не показываем): " + counters[2]);
        Log.d(TAG, " - Слов в процессе: " + counters[3]);
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
       // wordList.addAll(createDemoWordList());

        // Используем ту же логику выбора слов для сессии
        List<WordItem> sessionWords = getWordsForSession(wordList);
        setupViewPagerWithWords(sessionWords);
    }


    /**
     * Настраивает кнопки управления для карточек
     */
    private void setupControlButtons() {
        View view = getView();
        if (view == null) return;

        ImageButton learnedButton = view.findViewById(R.id.learnedButton);
        ImageButton reviewButton = view.findViewById(R.id.reviewButton);

        // 1. Загружаем анимацию "пружинки"
        // Используй getContext() или v.getContext()
        final Animation clickAnim = AnimationUtils.loadAnimation(getContext(), R.anim.button_click);

        if (learnedButton != null && reviewButton != null) {

            learnedButton.setOnClickListener(v -> {
                // 2. Запускаем анимацию сразу при клике
                v.startAnimation(clickAnim);

                if (adapter != null) {
                    adapter.swipeRight();
                    updateProgress();
                }
            });

            reviewButton.setOnClickListener(v -> {
                // 3. И здесь тоже запускаем анимацию
                v.startAnimation(clickAnim);

                if (adapter != null) {
                    adapter.swipeLeft();
                    updateProgress();
                }
            });

            Log.d(TAG, "Кнопки управления настроены с анимацией");
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
                noWordsLayout.setBackgroundColor(0xFF211B20);

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

    private void setupWithLocalWords() {
        wordList.clear();
        wordList.addAll(createDemoWordList());
        setupCardStack();
    }

    /**
     * Добавляет демо-слова если база пустая

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
     */
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
        Log.d(TAG, "=== ОБРАБОТКА: onCardLearned ===");

        // ТОЛЬКО ОДИН вызов - либо onWordLearned, либо onWordReviewed
        if (SimpleRepetitionSystem.isLearnedWord(word)) {
            wordRepository.onWordLearned(word.getWordId());
        } else {
            wordRepository.onWordReviewed();
        }

        // Обновляем прогресс
        updateProgress();
        Toast.makeText(getContext(), "✅ " + word.getWord() + " - выучено!", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onCardNotLearned(WordItem word) {
        Log.d(TAG, "Слово не выучено: " + word.getWord());

        // УБЕРИТЕ ЭТИ СТРОКИ - они уже вызваны в адаптере:
        // SimpleRepetitionSystem.processAnswer(word, false);
        // wordRepository.updateWord(word);

        // Оставляем только статистику:
        wordRepository.onWordReviewed();

        // Обновляем прогресс
        updateProgress();

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

        // ✅ ДОБАВИТЬ: Обновляем статистику после завершения сессии
        updateSessionCompletionStats();

        // Минимальная задержка для плавности
        if (getActivity() != null) {
            showSessionCompletedState();
        }
    }
    /**
     * Обновляет статистику после завершения сессии обучения
     */
    private void updateSessionCompletionStats() {
        // Можно добавить дополнительную логику, например:
        // - Увеличить счетчик завершенных сессий
        // - Обновить время последней сессии
        // - Проверить достижение дневной цели

        Log.d(TAG, "📊 Сессия обучения завершена - статистика обновлена");

        // Просто обновляем общую статистику
        wordRepository.getUserStats(new WordRepository.OnStatsLoadedListener() {
            @Override
            public void onStatsLoaded(UserStats stats) {
                Log.d(TAG, "✅ Статистика после сессии: " +
                        "Streak: " + stats.getStreakDays() + " дней, " +
                        "Сегодня: " + stats.getTodayProgress() + " слов, " +
                        "Всего выучено: " + stats.getWordsLearned());
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Ошибка загрузки статистики после сессии", e);
            }
        });
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
                completedLayout.setBackgroundColor(0xFF211B20);

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
                noWordsLayout.setBackgroundColor(0xFF211B20);

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