package com.example.newwords;

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class Fragment1 extends Fragment {

    private TextView daysTextView, wordsInProgressTextView, wordsLearnedTextView, goodJobTextView;
    private WordRepository wordRepository;
    private boolean isProcessingClick = false;
    private static final long BUTTON_COOLDOWN = 2000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment1, container, false);

        wordRepository = new WordRepository(getContext());
        initViews(view);
        loadUserStats();
        setupStartButton(view);
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
            daysTextView.setText(getString(R.string.stats_streak_days, stats.getStreakDays()));
            wordsInProgressTextView.setText(" " + stats.getWordsInProgress());
            wordsLearnedTextView.setText(" " + stats.getWordsLearned());
            updateMotivationalMessage(stats);
        });
    }

    private void showDefaultStats() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            daysTextView.setText(getString(R.string.stats_streak_days, 0));
            wordsInProgressTextView.setText(" 0");
            wordsLearnedTextView.setText(" 0");
            goodJobTextView.setText("let's\nstart!");
        });
    }

    private void updateMotivationalMessage(UserStats stats) {
        int messageResId;
        if (stats.getStreakDays() >= 7) {
            messageResId = R.string.msg_great_work;
        } else if (stats.getTodayProgress() >= 10) {
            messageResId = R.string.msg_good_job;
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

    // ИСПРАВЛЕНО: получаем текущий язык и передаем его в SearchWordsFragment
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



    private void loadFromFirebase(String currentLanguage) {
        wordRepository.getWordsWithProgress(currentLanguage,
                new WordRepository.OnWordsWithProgressListener() {
                    @Override
                    public void onWordsLoaded(List<WordItem> words) {
                        hideLoading();

                        if (words.isEmpty()) {
                            Toast.makeText(getContext(), R.string.stats_no_words, Toast.LENGTH_LONG).show();
                        } else {
                            WordsFragment startFragment = WordsFragment.newInstanceWithWords(words, currentLanguage);
                            requireActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(android.R.id.content, startFragment)
                                    .addToBackStack("fragment1_navigation")
                                    .commit();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        hideLoading();
                        Log.e("Fragment1", "Ошибка загрузки", e);
                        Toast.makeText(getContext(), R.string.lib_load_error_toast, Toast.LENGTH_SHORT).show();
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

    private void checkIfWordsAvailable() {
        LanguageManager languageManager = new LanguageManager(getContext());
        String currentLanguage = languageManager.getCurrentLanguage();

        Log.d("Fragment1", "🔍 ПРОВЕРКА КЕША ДЛЯ ЯЗЫКА: " + currentLanguage);

        // ДИАГНОСТИКА: прямой запрос в Room
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getContext());

                // 1. Все библиотеки
                List<LocalWordLibrary> allLibs = db.libraryDao().getAllLibraries();
                Log.d("Fragment1", "=== ВСЕ БИБЛИОТЕКИ ===");
                for (LocalWordLibrary lib : allLibs) {
                    Log.d("Fragment1", "📚 " + lib.getName() + " | languageFrom: " + lib.getLanguageFrom() + " | isActive: " + lib.isActive());
                }

                // 2. Активные библиотеки для текущего языка
                List<LocalWordLibrary> activeLibs = db.libraryDao().getActiveLibrariesByLanguage(currentLanguage);
                Log.d("Fragment1", "=== АКТИВНЫЕ БИБЛИОТЕКИ ДЛЯ " + currentLanguage + " ===");
                Log.d("Fragment1", "Найдено: " + activeLibs.size());
                for (LocalWordLibrary lib : activeLibs) {
                    Log.d("Fragment1", "📚 " + lib.getName());

                    // Слова в этой библиотеке
                    List<LocalWordItem> words = db.wordDao().getWordsByLibrary(lib.getLibraryId());
                    Log.d("Fragment1", "   Слов: " + words.size());
                    for (LocalWordItem w : words) {
                        Log.d("Fragment1", "      - " + w.getWord() + " -> " + w.getTranslation());
                    }
                }

                // 3. Все слова из активных библиотек (через JOIN)
                List<LocalWordItem> wordsFromActive = db.wordDao().getWordsFromActiveLibraries();
                Log.d("Fragment1", "=== СЛОВА ИЗ АКТИВНЫХ БИБЛИОТЕК (JOIN) ===");
                Log.d("Fragment1", "Всего: " + wordsFromActive.size());
                for (LocalWordItem w : wordsFromActive) {
                    Log.d("Fragment1", "📖 " + w.getWord());
                }

            } catch (Exception e) {
                Log.e("Fragment1", "Ошибка диагностики", e);
            }
        }).start();

        // Обычная проверка
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
                    // Если через метод ничего не вернулось, но в Room есть слова - значит проблема в методе
                    Log.e("Fragment1", "❌ getWordsFromCacheOnly вернул 0, хотя в Room могут быть слова!");
                    Toast.makeText(getContext(), "Нет слов. Проверьте интернет и перезапустите.", Toast.LENGTH_LONG).show();
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
}