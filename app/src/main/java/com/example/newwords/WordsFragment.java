package com.example.newwords;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
    private static final String ARG_WORDS = "words_list";  // ← ДОБАВЬ ЭТУ СТРОКУ
    private static final String TAG = "WordsFragment";

    private boolean hasPassedWords = false;

    private boolean isLoading = false;


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

    private void processLoadedWords(List<WordItem> words) {
        Log.d(TAG, "!!! processLoadedWords ВЫЗВАН с " + words.size() + " словами !!!");

        if (words == null || words.isEmpty()) {
            Log.e(TAG, "processLoadedWords: слова пустые!");
            return;
        }

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

    private void processLoadedWords(List<WordItem> words) {
        Log.d(TAG, "=== processLoadedWords ВЫЗВАН ===");
        Log.d(TAG, "Загружено слов всего: " + words.size());

        // ВРЕМЕННО: берем первые 20 слов без всякой фильтрации
        List<WordItem> sessionWords = new ArrayList<>();
        int maxWords = 90;

        for (int i = 0; i < Math.min(maxWords, words.size()); i++) {
            WordItem w = words.get(i);
            sessionWords.add(w);
            Log.d(TAG, "Добавляю слово " + i + ": " + w.getWord() + ", libraryId=" + w.getLibraryId());
        }

        Log.d(TAG, "Слов для отображения: " + sessionWords.size());

        if (sessionWords.isEmpty()) {
            Log.d(TAG, "Нет слов для изучения");
            showNoWordsState();
        } else {
            setupViewPagerWithWords(sessionWords);
        }

        showLoading(false);
    }  }ВРЕМЕННО*/

    private void processLoadedWords(List<WordItem> words) {
        Log.d(TAG, "=== processLoadedWords ВЫЗВАН ===");
        Log.d(TAG, "Загружено слов всего: " + words.size());

        // ✅ ПРАВИЛЬНАЯ ФИЛЬТРАЦИЯ через getWordsForSession
        List<WordItem> sessionWords = getWordsForSession(words);

        Log.d(TAG, "Слов для отображения после фильтрации: " + sessionWords.size());

        // Логируем КАЖДОЕ слово, которое попало в сессию
        for (int i = 0; i < sessionWords.size(); i++) {
            WordItem w = sessionWords.get(i);
            Log.d(TAG, "✅ Слово " + i + " в сессии: " + w.getWord() +
                    ", stage=" + w.getReviewStage() +
                    ", due=" + w.isDueForReview());
        }

        if (sessionWords.isEmpty()) {
            Log.d(TAG, "Нет слов для изучения (после фильтрации)");
            showNoWordsState();
        } else {
            setupViewPagerWithWords(sessionWords);
        }

        showLoading(false);
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
        final int maxWords = 90;

        Log.d(TAG, "=== ВЫБОР СЛОВ ДЛЯ СЕССИИ ==================================");
        Log.d(TAG, "Всего слов доступно: " + allWords.size());

        for (WordItem word : allWords) {
            // Логируем КАЖДОЕ слово и все его параметры
            boolean isDue = word.isDueForReview();
            boolean isNotFullyLearned = word.getReviewStage() < 6;
            boolean isNew = word.isNew();
            boolean shouldShow = SimpleRepetitionSystem.shouldShowInSession(word);

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            Log.d(TAG, "Слово: " + word.getWord());
            Log.d(TAG, "  reviewStage: " + word.getReviewStage());
            Log.d(TAG, "  consecutiveShows: " + word.getConsecutiveShows());
            Log.d(TAG, "  nextReviewDate: " + word.getNextReviewDate());
            Log.d(TAG, "  isDue (дата прошла?): " + isDue);
            Log.d(TAG, "  isNotFullyLearned (stage<6?): " + isNotFullyLearned);
            Log.d(TAG, "  isNew: " + isNew);
            Log.d(TAG, "  shouldShow ИТОГ: " + shouldShow);

            if (shouldShow) {
                sessionWords.add(word);
                Log.d(TAG, "  ✅ ДОБАВЛЕНО В СЕССИЮ");
                if (sessionWords.size() >= maxWords) break;
            } else {
                Log.d(TAG, "  ❌ НЕ ДОБАВЛЕНО");
            }
        }

        Log.d(TAG, "============================================================");
        Log.d(TAG, "Всего для сессии: " + sessionWords.size());
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
        isLoading = show;  // ✅ ДОБАВЬТЕ ЭТУ СТРОКУ
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






    // Создать новый экземпляр с указанием языка
    public static WordsFragment newInstance(String currentLanguage) {
        WordsFragment fragment = new WordsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LANGUAGE, currentLanguage);
        fragment.setArguments(args);
        return fragment;
    }

    // НОВЫЙ МЕТОД - создает фрагмент с готовыми словами
    public static WordsFragment newInstanceWithWords(List<WordItem> words, String currentLanguage) {
        WordsFragment fragment = new WordsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LANGUAGE, currentLanguage);
        args.putParcelableArrayList(ARG_WORDS, new ArrayList<>(words));  // Теперь работает
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            currentLanguage = getArguments().getString(ARG_LANGUAGE, "en");
            Log.d(TAG, "📱 WordsFragment создан для языка: " + currentLanguage);

            // Получаем переданные слова
            ArrayList<WordItem> passedWords = getArguments().getParcelableArrayList(ARG_WORDS);
            if (passedWords != null && !passedWords.isEmpty()) {
                Log.d(TAG, "📦 Использую переданные слова: " + passedWords.size() + " шт.");
                // Сохраняем слова для использования в onCreateView
                wordList.clear();
                wordList.addAll(passedWords);
                hasPassedWords = true;
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_words, container, false);

        wordRepository = new WordRepository(getContext());
        debugRoomDatabase();  // ← ДОБАВЬТЕ ЭТУ СТРОКУ
        viewPager2 = view.findViewById(R.id.viewPager2);
        progressBar = view.findViewById(R.id.progressBar);
        progressText = view.findViewById(R.id.progressText);

        setupBackButton(view);
        setupSwipeGestures(view);
        debugCacheContentsForLanguage("ba");
        // Если есть переданные слова - используем их
        if (hasPassedWords && !wordList.isEmpty()) {
            Log.d(TAG, "📦 Использую переданные слова в onCreateView: " + wordList.size());
            processLoadedWords(wordList);
            showLoading(false);
        } else {
            loadWordsFromFirebase();
        }

        return view;
    }

    //надо будет потом испрваить ноооо не сейяас бох с ним

    private void debugRoomDatabase() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getContext());

                // 1. Все библиотеки
                List<LocalWordLibrary> allLibraries = db.libraryDao().getAllLibraries();
                Log.d("DEBUG_ROOM", "=== ВСЕ БИБЛИОТЕКИ ===");
                Log.d("DEBUG_ROOM", "Всего библиотек: " + allLibraries.size());
                for (LocalWordLibrary lib : allLibraries) {
                    Log.d("DEBUG_ROOM", "📚 " + lib.getName() +
                            " | ID: " + lib.getLibraryId() +
                            " | languageFrom: " + lib.getLanguageFrom() +
                            " | isActive: " + lib.isActive() +
                            " | wordCount: " + lib.getWordCount());
                }

                // 2. Все слова
                List<LocalWordItem> allWords = db.wordDao().getAllWords();
                Log.d("DEBUG_ROOM", "=== ВСЕ СЛОВА ===");
                Log.d("DEBUG_ROOM", "Всего слов: " + allWords.size());
                for (int i = 0; i < Math.min(10, allWords.size()); i++) {
                    LocalWordItem w = allWords.get(i);
                    Log.d("DEBUG_ROOM", "📖 " + w.getWord() + " -> " + w.getTranslation() +
                            " | libraryId: " + w.getLibraryId());
                }

                // 3. Слова для конкретной библиотеки
                List<LocalWordLibrary> baLibraries = db.libraryDao().getLibrariesByLanguage("ba");
                Log.d("DEBUG_ROOM", "=== БИБЛИОТЕКИ ДЛЯ ЯЗЫКА ba ===");
                Log.d("DEBUG_ROOM", "Найдено: " + baLibraries.size());
                for (LocalWordLibrary lib : baLibraries) {
                    Log.d("DEBUG_ROOM", "📚 " + lib.getName() + " | ID: " + lib.getLibraryId() + " | isActive: " + lib.isActive());

                    // Слова в этой библиотеке
                    List<LocalWordItem> words = db.wordDao().getWordsByLibrary(lib.getLibraryId());
                    Log.d("DEBUG_ROOM", "   Слов в этой библиотеке: " + words.size());
                    for (LocalWordItem w : words) {
                        Log.d("DEBUG_ROOM", "      - " + w.getWord());
                    }
                }

                // 4. Активные библиотеки для ba
                List<LocalWordLibrary> activeBaLibs = db.libraryDao().getActiveLibrariesByLanguage("ba");
                Log.d("DEBUG_ROOM", "=== АКТИВНЫЕ БИБЛИОТЕКИ ДЛЯ ba ===");
                Log.d("DEBUG_ROOM", "Найдено активных: " + activeBaLibs.size());
                for (LocalWordLibrary lib : activeBaLibs) {
                    Log.d("DEBUG_ROOM", "📚 " + lib.getName());
                    List<LocalWordItem> words = db.wordDao().getWordsByLibrary(lib.getLibraryId());
                    Log.d("DEBUG_ROOM", "   Слов: " + words.size());
                }

            } catch (Exception e) {
                Log.e("DEBUG_ROOM", "Ошибка", e);
            }
        }).start();
    }
    private void checkForUpdatesInBackground() {
        // Только если есть интернет
        if (isNetworkAvailable()) {
            wordRepository.smartSyncForLanguage(currentLanguage, new WordRepository.OnWordsLoadedListener() {
                @Override
                public void onWordsLoaded(List<WordItem> freshWords) {
                    if (!freshWords.isEmpty()) {
                        Log.d(TAG, "🔄 Фоновое обновление: " + freshWords.size() + " слов");
                        // Обновляем UI если есть изменения
                        processLoadedWords(freshWords);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.d(TAG, "Фоновое обновление не удалось, используем кеш");
                }
            });
        }
    }



    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
    private void loadFromFirebaseWithTimeout() {
        // Показываем загрузку
        showLoading(true);

        // Устанавливаем таймаут 5 секунд
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (isLoading) {  // ✅ ТЕПЕРЬ РАБОТАЕТ
                Log.d(TAG, "⏰ Таймаут загрузки, показываем пустое состояние");
                showNoWordsState();
                showLoading(false);
            }
        };
        handler.postDelayed(timeoutRunnable, 5000);

        wordRepository.smartSyncForLanguage(currentLanguage, new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                handler.removeCallbacks(timeoutRunnable);
                if (!words.isEmpty()) {
                    processLoadedWords(words);
                } else {
                    showNoWordsState();
                }
                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                handler.removeCallbacks(timeoutRunnable);
                Log.e(TAG, "❌ Ошибка загрузки", e);
                showNoWordsState();
                showLoading(false);
            }
        });
    }

    private void loadWords() {
        Log.d(TAG, "📖 Загрузка слов для языка: " + currentLanguage);

        // ✅ ТОЛЬКО КЕШ!
        wordRepository.getWordsFromCacheOnly(currentLanguage, new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                if (!words.isEmpty()) {
                    processLoadedWords(words);
                } else {
                    showNoWordsState();
                }
                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка", e);
                showNoWordsState();
                showLoading(false);
            }
        });
    }


    private void loadWordsFromFirebase() {
        Log.d(TAG, "🚀 [OFFLINE-FIRST] Загрузка для: " + currentLanguage);
        showLoading(true);

        // ✅ ТОЛЬКО КЕШ! (никаких сетевых запросов)
       wordRepository.getWordsFromCacheOnlyForActiveLibraries(currentLanguage,
        // wordRepository.getWordsFromCacheIgnoreActive(currentLanguage,


                new WordRepository.OnWordsLoadedListener() {
                    @Override
                    public void onWordsLoaded(List<WordItem> cachedWords) {
                        if (!cachedWords.isEmpty()) {
                            Log.d(TAG, "⚡ Из кеша: " + cachedWords.size() + " слов");
                            processLoadedWords(cachedWords);
                            showLoading(false);

                            // ✅ ФОНОМ проверяем обновления (если есть интернет)
                            if (isNetworkAvailable()) {
                                refreshCacheInBackground();
                            }
                        } else {
                            // Кеша нет — идём в сеть
                            Log.d(TAG, "📭 Кеш пуст, загружаем из сети...");
                            loadFromNetworkAndCache();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "❌ Ошибка кеша", e);
                        loadFromNetworkAndCache();
                    }
                });
    }
    private void loadFromNetworkAndCache() {
        Log.d(TAG, "🌐 Загрузка из сети и сохранение в кеш...");

        wordRepository.forceSyncForLanguage(currentLanguage, new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                if (!words.isEmpty()) {
                    Log.d(TAG, "✅ Загружено из сети: " + words.size() + " слов, сохранено в кеш");
                    processLoadedWords(words);
                } else {
                    Log.d(TAG, "📭 Нет слов для языка: " + currentLanguage);
                    showNoWordsState();
                }
                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Ошибка загрузки из сети: " + e.getMessage());
                showNoWordsState();
                showLoading(false);
            }
        });
    }
    private void debugCacheContentsForLanguage(String language) {
        wordRepository.getWordsFromCacheOnly(language, new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                Log.d(TAG, "=== ДЕБАГ КЕША ДЛЯ " + language + " ===");
                Log.d(TAG, "Всего слов в кеше: " + words.size());

                if (words.isEmpty()) {
                    Log.d(TAG, "❌ КЕШ ПУСТ!");
                    return;
                }

                int dueCount = 0;
                int newCount = 0;
                int learnedCount = 0;

                for (WordItem word : words) {
                    boolean isDue = word.isDueForReview();
                    boolean isNew = word.isNew();
                    boolean isLearned = word.isLearned();
                    boolean shouldShow = SimpleRepetitionSystem.shouldShowInSession(word);

                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    Log.d(TAG, "Слово: " + word.getWord());
                    Log.d(TAG, "  reviewStage: " + word.getReviewStage());
                    Log.d(TAG, "  nextReviewDate: " + word.getNextReviewDate());
                    Log.d(TAG, "  isDue: " + isDue);
                    Log.d(TAG, "  isNew: " + isNew);
                    Log.d(TAG, "  isLearned: " + isLearned);
                    Log.d(TAG, "  shouldShowInSession: " + shouldShow);

                    if (shouldShow) dueCount++;
                    if (isNew) newCount++;
                    if (isLearned) learnedCount++;
                }

                Log.d(TAG, "=== ИТОГО ===");
                Log.d(TAG, "Готовых к показу: " + dueCount);
                Log.d(TAG, "Новых слов: " + newCount);
                Log.d(TAG, "Выученных слов: " + learnedCount);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка дебага кеша", e);
            }
        });
    }
    private void refreshCacheInBackground() {
        // Фоновое обновление, НЕ трогает UI
        wordRepository.smartSyncForLanguage(currentLanguage, new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> freshWords) {
                if (!freshWords.isEmpty()) {
                    Log.d(TAG, "🔄 Фоновое обновление: " + freshWords.size() + " слов");
                    // Только если текущий список пуст — обновляем
                    if (adapter == null || adapter.getItemCount() == 0) {
                        processLoadedWords(freshWords);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "Фоновое обновление не удалось");
            }
        });
    }

}