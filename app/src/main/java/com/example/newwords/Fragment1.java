package com.example.newwords;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;
import java.util.concurrent.Executors;

public class Fragment1 extends Fragment {

    private TextView daysTextView, wordsInProgressTextView, wordsLearnedTextView, goodJobTextView;
    private WordRepository wordRepository;
    private boolean isProcessingClick = false;
    private static final long BUTTON_COOLDOWN = 2000;
    private ProgressBar todayProgressBar;
    private TextView todayProgressText;
    private static final int DAILY_GOAL = 10;
    private String currentLanguage;
    private BroadcastReceiver librariesUpdateReceiver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment1, container, false);

        // Получаем текущий язык
        LanguageManager languageManager = new LanguageManager(getContext());
        currentLanguage = languageManager.getCurrentLanguage();
        Log.d(TAG, "onCreateView: currentLanguage = " + currentLanguage);

        wordRepository = new WordRepository(getContext());
        initViews(view);
        wordRepository.checkAndResetDailyProgress();
        // Загружаем статистику ИЗ КЕША (мгновенно!)
        loadStatsFromLocalCache();

        // Подписываемся на LiveData для реального времени
        wordRepository.getStatsLiveData().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                updateStatsUI(stats);
                saveStatsToLocalCache(stats);
            }
        });

        // Пересчитываем статистику из активных библиотек ДЛЯ ТЕКУЩЕГО ЯЗЫКА
        refreshStats();

        setupStartButton(view);
        setupBottomButtons(view);

        return view;
    }

    private void initViews(View view) {
        daysTextView = view.findViewById(R.id.daysTextView);
        wordsInProgressTextView = view.findViewById(R.id.wordsInProgressTextView);
        wordsLearnedTextView = view.findViewById(R.id.wordsLearnedTextView);
        goodJobTextView = view.findViewById(R.id.goodJobTextView);
        todayProgressBar = view.findViewById(R.id.todayProgressBar);
        todayProgressText = view.findViewById(R.id.todayProgressText);
    }

    /**
     * Обновляет статистику для текущего языка
     */
    private void refreshStats() {
        if (wordRepository == null) return;

        wordRepository.recalculateStatsFromCache(currentLanguage, new WordRepository.OnStatsLoadedListener() {
            @Override
            public void onStatsLoaded(UserStats stats) {
                Log.d(TAG, "✅ Статистика пересчитана: wordsInProgress=" + stats.getWordsInProgress() +
                        ", wordsLearned=" + stats.getWordsLearned() +
                        ", todayProgress=" + stats.getTodayProgress());
                updateStatsUI(stats);
                saveStatsToLocalCache(stats);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка пересчета статистики", e);
            }
        });
    }

    private void saveStatsToLocalCache(UserStats stats) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase.getInstance(getContext()).statsDao().insertStats(stats);
                Log.d(TAG, "💾 Статистика сохранена в Room");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка сохранения в Room", e);
            }
        });
    }

    private void loadStatsFromLocalCache() {
        Log.d(TAG, "📦 Загрузка статистики из локального кеша");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String userId = wordRepository.getUserId();
                if (userId == null || userId.equals("anonymous")) {
                    Log.d(TAG, "Пользователь не авторизован");
                    new Handler(Looper.getMainLooper()).post(this::showDefaultStats);
                    return;
                }

                UserStats localStats = AppDatabase.getInstance(getContext())
                        .statsDao()
                        .getStats(userId);

                if (localStats != null) {
                    Log.d(TAG, "✅ Статистика из Room: wordsInProgress=" + localStats.getWordsInProgress() +
                            ", wordsLearned=" + localStats.getWordsLearned() +
                            ", todayProgress=" + localStats.getTodayProgress());
                    new Handler(Looper.getMainLooper()).post(() -> {
                        updateStatsUI(localStats);
                    });
                } else {
                    Log.d(TAG, "Нет статистики в Room");
                    new Handler(Looper.getMainLooper()).post(this::showDefaultStats);
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка загрузки из Room", e);
                new Handler(Looper.getMainLooper()).post(this::showDefaultStats);
            }
        });
    }

    private void updateStatsUI(UserStats stats) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            daysTextView.setText(getString(R.string.stats_streak_days, stats.getStreakDays()));
            wordsInProgressTextView.setText(" " + stats.getWordsInProgress());
            wordsLearnedTextView.setText(" " + stats.getWordsLearned());

            // Обновляем прогресс-бар
            int todayProgress = stats.getTodayProgress();
            int progressPercent = Math.min(todayProgress, DAILY_GOAL);
            todayProgressBar.setProgress(progressPercent);
            todayProgressBar.setMax(DAILY_GOAL);

            if (todayProgress >= DAILY_GOAL) {
                todayProgressText.setText("🎉 Цель достигнута! " + todayProgress + "/" + DAILY_GOAL);
            } else if (todayProgress >= DAILY_GOAL / 2) {
                todayProgressText.setText("👍 Отлично! " + todayProgress + "/" + DAILY_GOAL);
            } else {
                todayProgressText.setText("📚 " + todayProgress + "/" + DAILY_GOAL + " слов сегодня");
            }

            updateMotivationalMessage(stats);
        });
    }

    private void showDefaultStats() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            daysTextView.setText(getString(R.string.stats_streak_days, 0));
            wordsInProgressTextView.setText(" 0");
            wordsLearnedTextView.setText(" 0");
            todayProgressBar.setProgress(0);
            todayProgressText.setText("0/" + DAILY_GOAL + " слов сегодня");
            goodJobTextView.setText("let's\nstart!");
        });
    }

    private void updateMotivationalMessage(UserStats stats) {
        int messageResId;
        int todayProgress = stats.getTodayProgress();

        if (stats.getStreakDays() >= 7) {
            messageResId = R.string.msg_great_work;
        } else if (todayProgress >= DAILY_GOAL) {
            messageResId = R.string.msg_good_job;
        } else if (todayProgress >= DAILY_GOAL / 2) {
            messageResId = R.string.msg_keep_going;
        } else if (stats.getWordsLearned() > 0) {
            messageResId = R.string.msg_keep_going;
        } else {
            messageResId = R.string.msg_lets_start;
        }
        goodJobTextView.setText(messageResId);
    }

    private void setupStartButton(View view) {
        Button startButton = view.findViewById(R.id.startButton);
        ProgressBar progressBar = view.findViewById(R.id.loadingProgressBar);
        final Animation clickAnim = AnimationUtils.loadAnimation(getContext(), R.anim.button_click);

        startButton.setOnClickListener(v -> {
            v.startAnimation(clickAnim);
            if (isProcessingClick) return;

            isProcessingClick = true;
            startButton.setEnabled(false);
            startButton.setAlpha(0.5f);
            progressBar.setVisibility(View.VISIBLE);

            startButton.postDelayed(() -> {
                checkIfWordsAvailable();

                startButton.postDelayed(() -> {
                    isProcessingClick = false;
                    startButton.setEnabled(true);
                    startButton.setAlpha(1f);
                    progressBar.setVisibility(View.GONE);
                }, BUTTON_COOLDOWN);
            }, 500);
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Обновляем текущий язык при возвращении
        LanguageManager languageManager = new LanguageManager(getContext());
        String newLanguage = languageManager.getCurrentLanguage();

        if (!newLanguage.equals(currentLanguage)) {
            Log.d(TAG, "Язык изменился с " + currentLanguage + " на " + newLanguage);
            currentLanguage = newLanguage;
        }

        // Всегда пересчитываем статистику при возвращении
        refreshStats();

        // Остальной код...
        if (getView() != null) {
            Button startButton = getView().findViewById(R.id.startButton);
            ProgressBar progressBar = getView().findViewById(R.id.loadingProgressBar);

            isProcessingClick = false;
            startButton.setEnabled(true);
            startButton.setAlpha(1f);
            progressBar.setVisibility(View.GONE);
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        // Регистрируем приемник уведомлений об изменении библиотек
        librariesUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String updatedLanguage = intent.getStringExtra("language");
                Log.d(TAG, "📡 Получено уведомление об обновлении библиотек для языка: " + updatedLanguage);

                LanguageManager languageManager = new LanguageManager(getContext());
                currentLanguage = languageManager.getCurrentLanguage();
                refreshStats();
            }
        };

        IntentFilter filter = new IntentFilter("LIBRARIES_UPDATED");
        // ✅ Используем LocalBroadcastManager - не нужны флаги!
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(librariesUpdateReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (librariesUpdateReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(librariesUpdateReceiver);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Receiver not registered: " + e.getMessage());
            }
        }
    }


    private void setupBottomButtons(View view) {
        LinearLayout searchButtonLayout = view.findViewById(R.id.searchButtonLayout);
        LinearLayout addButtonLayout = view.findViewById(R.id.addButtonLayout);
        final Animation clickAnim = AnimationUtils.loadAnimation(getContext(), R.anim.button_click);

        searchButtonLayout.setOnClickListener(v -> {
            v.startAnimation(clickAnim);
            openSearchFragment();
        });

        addButtonLayout.setOnClickListener(v -> {
            v.startAnimation(clickAnim);
            showAddWordDialog();
        });
    }

    private void openSearchFragment() {
        LanguageManager languageManager = new LanguageManager(getContext());
        String currentLanguage = languageManager.getCurrentLanguage();

        SearchWordsFragment fragment = SearchWordsFragment.newInstance(currentLanguage);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack("search_navigation")
                .commit();
    }

    private void showAddWordDialog() {
        AddWordWithLibraryDialog dialog = AddWordWithLibraryDialog.newInstance();
        dialog.setOnWordAddedListener((word, translation, note, libraryId) -> {
            WordItem newWord = new WordItem(word, translation, note);
            wordRepository.addWordToCustomLibrary(libraryId, newWord, new WordRepository.OnWordAddedListener() {
                @Override
                public void onWordAdded(WordItem addedWord) {
                    Toast.makeText(getContext(), R.string.stats_word_added, Toast.LENGTH_SHORT).show();
                    // Обновляем статистику после добавления слова
                    refreshStats();
                }

                @Override
                public void onError(Exception e) {
                    String errorMsg = getString(R.string.word_add_error_toast) + ": " + e.getMessage();
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.show(getParentFragmentManager(), "add_word_with_library_dialog");
    }

    private void checkIfWordsAvailable() {
        LanguageManager languageManager = new LanguageManager(getContext());
        String currentLanguage = languageManager.getCurrentLanguage();

        Log.d("Fragment1", "🔍 ПРОВЕРКА КЕША ДЛЯ ЯЗЫКА: " + currentLanguage);

        wordRepository.getWordsFromCacheOnly(currentLanguage, new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                hideLoading();
                Log.d("Fragment1", "📊 getWordsFromCacheOnly вернул: " + words.size() + " слов");

                if (!words.isEmpty()) {
                    WordsFragment startFragment = WordsFragment.newInstanceWithWords(words, currentLanguage);
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(android.R.id.content, startFragment)
                            .addToBackStack("fragment1_navigation")
                            .commit();
                } else {
                    Toast.makeText(getContext(), "Нет слов. Добавьте слова в библиотеках.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                hideLoading();
                Log.e("Fragment1", "Ошибка", e);
                Toast.makeText(getContext(), "Ошибка загрузки из кеша", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void hideLoading() {
        if (getView() != null) {
            ProgressBar progressBar = getView().findViewById(R.id.loadingProgressBar);
            Button startButton = getView().findViewById(R.id.startButton);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (startButton != null) {
                startButton.setEnabled(true);
                startButton.setAlpha(1f);
            }
            isProcessingClick = false;
        }
    }
}