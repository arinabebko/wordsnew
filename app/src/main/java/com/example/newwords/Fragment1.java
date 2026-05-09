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
import android.widget.LinearLayout; // ДОБАВЬ ЭТОТ ИМПОРТ

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;


// ... импорты те же ...

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

    private void openSearchFragment() {
        SearchWordsFragment searchFragment = new SearchWordsFragment();
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, searchFragment)
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
/*
    // ✅ ИСПРАВЛЕННЫЙ МЕТОД — берем ТОЛЬКО активные слова
    private void checkIfWordsAvailable() {
        LanguageManager languageManager = new LanguageManager(getContext());
        String currentLanguage = languageManager.getCurrentLanguage();

        // ✅ ИСПРАВЛЕНО: Используем правильный метод загрузки ТОЛЬКО активных слов
        wordRepository.getWordsFromActiveLibrariesFirebase(currentLanguage, new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                Log.d("Fragment1", "Загружено слов для изучения: " + words.size() + ", язык: " + currentLanguage);

                if (words.isEmpty()) {
                    // Показываем сообщение, что нет слов
                    Toast.makeText(getContext(), R.string.stats_no_words, Toast.LENGTH_LONG).show();
                } else {
                    // Передаем слова во фрагмент
                    WordsFragment startFragment = WordsFragment.newInstanceWithWords(words, currentLanguage);
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(android.R.id.content, startFragment)
                            .addToBackStack("fragment1_navigation")
                            .commit();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("Fragment1", "Ошибка загрузки слов", e);
                Toast.makeText(getContext(), R.string.lib_load_error_toast, Toast.LENGTH_SHORT).show();
            }
        });
    }
*/

    //todo
    // ❌ УДАЛИТЬ ЭТОТ МЕТОД — он больше не нужен и сверхум тож

    // private void loadDirectlyFromFirebase(String currentLanguage) { ... }



    private void checkIfWordsAvailable() {
        LanguageManager languageManager = new LanguageManager(getContext());
        String currentLanguage = languageManager.getCurrentLanguage();

        // ✅ СНАЧАЛА пытаемся загрузить из кеша (мгновенно)
        wordRepository.getWordsWithProgressFromCache(currentLanguage,
                new WordRepository.OnWordsLoadedListener() {
                    @Override
                    public void onWordsLoaded(List<WordItem> words) {
                        if (!words.isEmpty()) {
                            Log.d("Fragment1", "✅ МГНОВЕННО: загружено " + words.size() + " слов из кеша");

                            // Скрываем спиннер
                            hideLoading();

                            // Открываем фрагмент с карточками
                            WordsFragment startFragment = WordsFragment.newInstanceWithWords(words, currentLanguage);
                            requireActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(android.R.id.content, startFragment)
                                    .addToBackStack("fragment1_navigation")
                                    .commit();
                        } else {
                            // Кеш пуст - пробуем загрузить из Firebase
                            Log.d("Fragment1", "⚠️ Кеш пуст, загружаем из Firebase...");
                            loadFromFirebase(currentLanguage);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("Fragment1", "❌ Ошибка загрузки из кеша", e);
                        loadFromFirebase(currentLanguage);
                    }
                });
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
            progressBar.setVisibility(View.GONE);
            startButton.setEnabled(true);
            startButton.setAlpha(1f);
            isProcessingClick = false;
        }
    }
}