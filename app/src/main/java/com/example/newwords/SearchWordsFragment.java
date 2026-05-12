package com.example.newwords;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SearchWordsFragment extends Fragment implements
        WordListAdapter.OnWordDeleteListener,
        WordListAdapter.OnWordClickListener {

    private static final String TAG = "SearchWordsFragment";
    private static final String ARG_LANGUAGE = "current_language";

    private RecyclerView wordsRecyclerView;
    private WordListAdapter wordAdapter;
    private WordRepository wordRepository;
    private List<WordItem> allWords = new ArrayList<>();
    private List<WordItem> filteredWords = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private EditText searchEditText;
    private ImageButton backButton;
    private ImageButton clearButton;
  //  private ImageButton favoriteFilterButton; // ✅ НОВЫЙ ЭЛЕМЕНТ
    private TextToSpeechManager ttsManager;
    private String currentLanguage = "en";
    private ImageView favoriteFilterButton;

    // ✅ Флаг для отслеживания режима фильтрации
    private boolean isShowingFavoritesOnly = false;

    public static SearchWordsFragment newInstance(String language) {
        SearchWordsFragment fragment = new SearchWordsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LANGUAGE, language);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_words, container, false);

        if (getArguments() != null) {
            currentLanguage = getArguments().getString(ARG_LANGUAGE, "en");
        }
        Log.d(TAG, "SearchWordsFragment создан для языка: " + currentLanguage);

        wordRepository = new WordRepository(getContext());
        ttsManager = TextToSpeechManager.getInstance(getContext());

        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupFavoriteFilter(); // ✅ НОВЫЙ МЕТОД
        loadAllUserWords();

        return view;
    }

    private void initViews(View view) {
        wordsRecyclerView = view.findViewById(R.id.wordsRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        searchEditText = view.findViewById(R.id.searchEditText);
        backButton = view.findViewById(R.id.backButton);
        clearButton = view.findViewById(R.id.clearButton);
        favoriteFilterButton = view.findViewById(R.id.favoriteFilterButton);

        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        clearButton.setOnClickListener(v -> {
            searchEditText.setText("");
        });

        Bundle args = getArguments();
        if (args != null && args.containsKey("initial_query")) {
            String initialQuery = args.getString("initial_query");
            searchEditText.setText(initialQuery);
            // ✅ ЗАМЕНИТЕ filterWords на applyFilter
            applyFilter();
        }
    }

    // ✅ НОВЫЙ МЕТОД: настройка кнопки фильтрации избранного
    private void setupFavoriteFilter() {
        favoriteFilterButton.setOnClickListener(v -> {
            // Переключаем режим фильтрации
            isShowingFavoritesOnly = !isShowingFavoritesOnly;

            // Обновляем иконку кнопки
            updateFavoriteFilterIcon();

            // Применяем фильтрацию
            applyFilter();

            // Показываем уведомление
            String message = isShowingFavoritesOnly ?
                    "Показаны только избранные слова" :
                    "Показаны все слова";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    // ✅ Обновление иконки кнопки избранного
    private void updateFavoriteFilterIcon() {
        // ОЧИЩАЕМ ЛЮБЫЕ ФИЛЬТРЫ ЦВЕТА
        favoriteFilterButton.clearColorFilter();

        if (isShowingFavoritesOnly) {
            Log.d(TAG, "Устанавливаю иконку ic_full_heart_icon");
            favoriteFilterButton.setImageResource(R.drawable.ic_full_heart_icon);
            // НЕ применяем colorFilter, используем цвет из самой иконки
        } else {
            Log.d(TAG, "Устанавливаю иконку ic_empty_heart_icon");
            favoriteFilterButton.setImageResource(R.drawable.ic_empty_heart_icon);
        }

        // Убираем фон (если есть какая-то заливка)
        favoriteFilterButton.setBackgroundResource(android.R.color.transparent);
    }

    // ✅ Применение текущих фильтров (поиск + избранное)
    private void applyFilter() {
        String query = searchEditText.getText().toString();

        if (query.isEmpty() && !isShowingFavoritesOnly) {
            // Без фильтров - показываем все
            filteredWords.clear();
            filteredWords.addAll(allWords);
        } else {
            filteredWords.clear();
            String queryLower = query.toLowerCase();

            for (WordItem word : allWords) {
                // Проверяем фильтр избранного
                boolean matchesFavorite = !isShowingFavoritesOnly || word.isFavorite();

                // Проверяем поисковый запрос
                boolean matchesSearch = query.isEmpty() ||
                        (word.getWord() != null && word.getWord().toLowerCase().contains(queryLower)) ||
                        (word.getTranslation() != null && word.getTranslation().toLowerCase().contains(queryLower)) ||
                        (word.getNote() != null && word.getNote().toLowerCase().contains(queryLower));

                if (matchesFavorite && matchesSearch) {
                    filteredWords.add(word);
                }
            }
        }

        // Обновляем адаптер
        wordAdapter.updateWords(filteredWords);

        // Показываем/скрываем пустое состояние
        if (filteredWords.isEmpty()) {
            String emptyMessage;
            if (isShowingFavoritesOnly && query.isEmpty()) {
                emptyMessage = "Нет избранных слов\nДобавьте слова в избранное, нажав на сердечко";
            } else if (isShowingFavoritesOnly && !query.isEmpty()) {
                emptyMessage = "Нет избранных слов по запросу \"" + query + "\"";
            } else if (!query.isEmpty()) {
                emptyMessage = "Слова по запросу \"" + query + "\" не найдены";
            } else {
                emptyMessage = "У вас пока нет слов для изучения на этом языке";
            }
            showEmptyState(true, emptyMessage);
        } else {
            showEmptyState(false, "");
        }
    }

    private void setupRecyclerView() {
        wordAdapter = new WordListAdapter(new ArrayList<>(), wordRepository, true);
        wordAdapter.setOnWordDeleteListener(this);
        wordAdapter.setOnWordClickListener(this);
        wordsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        wordsRecyclerView.setAdapter(wordAdapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateClearButtonVisibility(s.toString());
                applyFilter(); // ✅ Используем общий метод applyFilter
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateClearButtonVisibility(String text) {
        if (clearButton != null) {
            clearButton.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void loadAllUserWords() {
        Log.d(TAG, "Загрузка всех слов пользователя для языка: " + currentLanguage);
        showLoading(true);

        wordRepository.getWordsWithProgressFromFirebase(currentLanguage, new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                Log.d(TAG, "Успешно загружено слов: " + words.size() + " для языка " + currentLanguage);

                allWords.clear();
                allWords.addAll(words);

                // ✅ Применяем фильтры после загрузки
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (allWords.isEmpty()) {
                            showEmptyState(true, "У вас пока нет слов для изучения на этом языке");
                        } else {
                            applyFilter(); // ✅ Применяем фильтры (поиск + избранное)

                            // Восстанавливаем текст поиска если был
                            Bundle args = getArguments();
                            if (args != null && args.containsKey("initial_query")) {
                                String initialQuery = args.getString("initial_query");
                                searchEditText.setText(initialQuery);
                            }
                        }
                        showLoading(false);
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка загрузки слов: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showEmptyState(true, "Ошибка загрузки слов");
                        showLoading(false);
                    });
                }
            }
        });
    }

    // ✅ УДАЛЯЕМ старый метод filterWords, заменяем на applyFilter

    private void showLoading(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
                if (wordsRecyclerView != null) {
                    wordsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
        }
    }

    private void showEmptyState(boolean show, String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (emptyStateText != null) {
                    emptyStateText.setVisibility(show ? View.VISIBLE : View.GONE);
                    if (show) {
                        emptyStateText.setText(message);
                    }
                }
                if (wordsRecyclerView != null) {
                    wordsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
        }
    }

    @Override
    public void onWordDeleted(WordItem word) {
        allWords.remove(word);        // ← ЭТО НУЖНО (удаляем из главного списка)
        applyFilter();                 // ← ЭТО ПЕРЕСОЗДАСТ filteredWords заново
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ttsManager != null) {
            ttsManager.restart();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onWordClick(WordItem word) {
        WordDetailDialogFragment dialog = WordDetailDialogFragment.newInstance(word);

        dialog.setOnWordActionListener(new WordDetailDialogFragment.OnWordActionListener() {
            @Override
            public void onEditWord(WordItem word) {
                if (word.isCustomWord()) {
                    openEditWordDialog(word);
                } else {
                    Toast.makeText(getContext(), "Нельзя редактировать слова из публичных библиотек", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDeleteWord(WordItem word) {
                if (word.isCustomWord()) {
                    confirmAndDeleteWord(word);
                } else {
                    Toast.makeText(getContext(), "Нельзя удалять слова из публичных библиотек", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onPlayPronunciation(String wordText) {
                if (ttsManager != null) {
                    if (!ttsManager.isInitialized()) {
                        ttsManager.restart();
                        new android.os.Handler(android.os.Looper.getMainLooper())
                                .postDelayed(() -> ttsManager.speak(wordText), 300);
                    } else {
                        ttsManager.speak(wordText);
                    }
                }
            }
        });

        dialog.show(getChildFragmentManager(), "word_detail_dialog");
    }

    private void openEditWordDialog(WordItem word) {
        if (!word.isCustomWord()) {
            Toast.makeText(getContext(), "Нельзя редактировать слова из публичных библиотек", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Редактировать слово: " + word.getWord());

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_word, null);
        EditText editWord = dialogView.findViewById(R.id.editWordText);
        EditText editTranslation = dialogView.findViewById(R.id.editTranslationText);
        EditText editNote = dialogView.findViewById(R.id.editNoteText);

        editWord.setText(word.getWord());
        editTranslation.setText(word.getTranslation());
        editNote.setText(word.getNote());

        builder.setView(dialogView);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newWord = editWord.getText().toString().trim();
            String newTranslation = editTranslation.getText().toString().trim();
            String newNote = editNote.getText().toString().trim();

            if (!newWord.isEmpty() && !newTranslation.isEmpty()) {
                word.setWord(newWord);
                word.setTranslation(newTranslation);
                word.setNote(newNote);

                wordRepository.updateWord(word, new WordRepository.OnWordUpdatedListener() {
                    @Override
                    public void onWordUpdated() {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                int index = allWords.indexOf(word);
                                if (index != -1) {
                                    allWords.set(index, word);
                                    applyFilter(); // ← ЗАМЕНИТЕ filterWords на applyFilter
                                }
                                Toast.makeText(getContext(), "Слово обновлено", Toast.LENGTH_SHORT).show();

                            });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });
            } else {
                Toast.makeText(getContext(), "Заполните слово и перевод", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void confirmAndDeleteWord(WordItem word) {
        if (!word.isCustomWord()) {
            Toast.makeText(getContext(), "Нельзя удалять слова из публичных библиотек", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить слово")
                .setMessage("Вы уверены, что хотите удалить слово \"" + word.getWord() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    if (word.getWordId() != null) {
                        if (word.getLibraryId() != null && !word.getLibraryId().isEmpty()) {
                            wordRepository.deleteWordFromLibrary(
                                    word.getLibraryId(),
                                    word.getWordId(),
                                    () -> {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                allWords.remove(word);
                                                applyFilter(); // ✅ Обновляем фильтры
                                                Toast.makeText(getContext(), "Слово удалено", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    },
                                    e -> {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() ->
                                                    Toast.makeText(getContext(), "Ошибка удаления: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                            );
                                        }
                                    }
                            );
                        } else {
                            wordRepository.deleteCustomWord(
                                    word.getWordId(),
                                    () -> {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                allWords.remove(word);
                                                applyFilter(); // ✅ Обновляем фильтры
                                                Toast.makeText(getContext(), "Слово удалено", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    },
                                    e -> {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() ->
                                                    Toast.makeText(getContext(), "Ошибка удаления: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                            );
                                        }
                                    }
                            );
                        }
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}