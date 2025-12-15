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
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
public class SearchWordsFragment extends Fragment implements WordListAdapter.OnWordDeleteListener {

    private static final String TAG = "SearchWordsFragment";

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_words, container, false);

        wordRepository = new WordRepository(getContext());
        initViews(view);
        setupRecyclerView();
        setupSearch();
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

        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        clearButton.setOnClickListener(v -> {
            searchEditText.setText("");
        });

        // Устанавливаем начальный запрос из аргументов
        Bundle args = getArguments();
        if (args != null && args.containsKey("initial_query")) {
            String initialQuery = args.getString("initial_query");
            searchEditText.setText(initialQuery);
            filterWords(initialQuery);
        }
    }

    private void setupRecyclerView() {
        // ИСПРАВЛЕННАЯ СТРОКА - используем конструктор с 3 параметрами
        wordAdapter = new WordListAdapter(new ArrayList<>(), wordRepository, true);
        wordAdapter.setOnWordDeleteListener(this);
        wordsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        wordsRecyclerView.setAdapter(wordAdapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterWords(s.toString());
                updateClearButtonVisibility(s.toString());
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
        Log.d(TAG, "Загрузка всех слов пользователя...");
        showLoading(true);

        wordRepository.getWordsFromActiveLibrariesFirebaseOld( new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                Log.d(TAG, "Успешно загружено слов: " + words.size());

                allWords.clear();
                allWords.addAll(words);
                filteredWords.clear();
                filteredWords.addAll(allWords);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (allWords.isEmpty()) {
                            showEmptyState(true, "У вас пока нет слов для изучения");
                        } else {
                            showEmptyState(false, "");
                            wordAdapter.updateWords(filteredWords);

                            // Применяем фильтр если есть начальный запрос
                            Bundle args = getArguments();
                            if (args != null && args.containsKey("initial_query")) {
                                String initialQuery = args.getString("initial_query");
                                filterWords(initialQuery);
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

    private void filterWords(String query) {
        if (query.isEmpty()) {
            filteredWords.clear();
            filteredWords.addAll(allWords);
        } else {
            filteredWords.clear();
            String queryLower = query.toLowerCase();

            for (WordItem word : allWords) {
                String wordText = word.getWord() != null ? word.getWord().toLowerCase() : "";
                String translation = word.getTranslation() != null ? word.getTranslation().toLowerCase() : "";
                String note = word.getNote() != null ? word.getNote().toLowerCase() : "";

                if (wordText.contains(queryLower) ||
                        translation.contains(queryLower) ||
                        note.contains(queryLower)) {
                    filteredWords.add(word);
                }
            }
        }

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                wordAdapter.updateWords(filteredWords);

                if (filteredWords.isEmpty() && !query.isEmpty()) {
                    showEmptyState(true, "Слова по запросу \"" + query + "\" не найдены");
                } else if (filteredWords.isEmpty()) {
                    showEmptyState(true, "У вас пока нет слов для изучения");
                } else {
                    showEmptyState(false, "");
                }
            });
        }
    }

    private void showLoading(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
                if (wordsRecyclerView != null) {
                    wordsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
                if (emptyStateText != null) {
                    emptyStateText.setVisibility(show ? View.GONE : View.GONE);
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

    // === РЕАЛИЗАЦИЯ ИНТЕРФЕЙСА OnWordDeleteListener ===

    @Override
    public void onWordDeleted(WordItem word) {
        // Удаляем слово из наших списков
        allWords.remove(word);
        filteredWords.remove(word);

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Обновляем адаптер
                wordAdapter.updateWords(filteredWords);

                // Проверяем не пустой ли теперь список
                if (filteredWords.isEmpty()) {
                    String currentQuery = searchEditText.getText().toString();
                    if (!currentQuery.isEmpty()) {
                        showEmptyState(true, "Слова по запросу \"" + currentQuery + "\" не найдены");
                    } else {
                        showEmptyState(true, "У вас пока нет слов для изучения");
                    }
                }
            });
        }
    }
}