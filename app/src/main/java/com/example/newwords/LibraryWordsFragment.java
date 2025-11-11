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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LibraryWordsFragment extends Fragment {

    private RecyclerView wordsRecyclerView;
    private WordCardAdapter wordsAdapter;
    private WordRepository wordRepository;
    private ProgressBar progressBar;
    private TextView titleText;
    private TextView emptyStateText;

    private String libraryId;
    private String libraryName;
    private boolean isCustomLibrary;
    private List<WordItem> wordList = new ArrayList<>();

    private static final String TAG = "LibraryWordsFragment";

    public static LibraryWordsFragment newInstance(String libraryId, String libraryName, boolean isCustomLibrary) {
        LibraryWordsFragment fragment = new LibraryWordsFragment();
        Bundle args = new Bundle();
        args.putString("libraryId", libraryId);
        args.putString("libraryName", libraryName);
        args.putBoolean("isCustomLibrary", isCustomLibrary);
        fragment.setArguments(args);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: начал создание view");

        View view = inflater.inflate(R.layout.fragment_library_words, container, false);
        Log.d(TAG, "onCreateView: view создан");

        // Получаем аргументы
        if (getArguments() != null) {
            libraryId = getArguments().getString("libraryId");
            libraryName = getArguments().getString("libraryName");
            isCustomLibrary = getArguments().getBoolean("isCustomLibrary");
            Log.d(TAG, "onCreateView: аргументы получены - " + libraryName + ", custom: " + isCustomLibrary);
        } else {
            Log.e(TAG, "onCreateView: аргументы не получены!");
        }

        // Инициализируем репозиторий
        wordRepository = new WordRepository();
        Log.d(TAG, "onCreateView: репозиторий создан");

        // Находим View элементы
        try {
            wordsRecyclerView = view.findViewById(R.id.wordsRecyclerView);
            progressBar = view.findViewById(R.id.progressBar);
            titleText = view.findViewById(R.id.titleText);
            emptyStateText = view.findViewById(R.id.emptyStateText);
            Log.d(TAG, "onCreateView: все View элементы найдены");
        } catch (Exception e) {
            Log.e(TAG, "onCreateView: ошибка поиска View элементов", e);
        }

        // Устанавливаем заголовок
        if (titleText != null && libraryName != null) {
            titleText.setText(libraryName);
        }

        // Настраиваем кнопку назад
        setupBackButton(view);

        // Настраиваем RecyclerView
        setupRecyclerView();

        // Загружаем слова библиотеки
        loadLibraryWords();

        Log.d(TAG, "onCreateView: завершен успешно");
        return view;
    }

    /**
     * Настраивает RecyclerView для списка слов
     */
    /**
     * Настраивает RecyclerView для списка слов
     */
    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: начало настройки");
        try {
            // Создаем пустой адаптер сначала
            WordListAdapter adapter = new WordListAdapter(new ArrayList<>(), wordRepository, isCustomLibrary);
            wordsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            wordsRecyclerView.setAdapter(adapter);
            Log.d(TAG, "setupRecyclerView: RecyclerView настроен успешно");
        } catch (Exception e) {
            Log.e(TAG, "setupRecyclerView: ошибка настройки RecyclerView", e);
            Toast.makeText(getContext(), "Ошибка настройки списка", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Загружает слова из библиотеки
     */
    private void loadLibraryWords() {
        Log.d(TAG, "loadLibraryWords: начало загрузки слов");

        if (libraryId == null) {
            Log.e(TAG, "loadLibraryWords: libraryId is null!");
            Toast.makeText(getContext(), "Ошибка: ID библиотеки не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        wordRepository.getWordsFromLibrary(libraryId, isCustomLibrary, new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                Log.d(TAG, "onWordsLoaded: успешно загружено " + words.size() + " слов");

                if (getActivity() == null) {
                    Log.e(TAG, "onWordsLoaded: Activity is null, фрагмент отключен");
                    return;
                }

                getActivity().runOnUiThread(() -> {
                    try {
                        wordList.clear();
                        wordList.addAll(words);

                        if (wordList.isEmpty()) {
                            Log.d(TAG, "onWordsLoaded: список слов пуст");
                            showEmptyState(true);
                        } else {
                            Log.d(TAG, "onWordsLoaded: обновляем адаптер с " + wordList.size() + " словами");
                            showEmptyState(false);

                            // Безопасно обновляем адаптер
                            if (wordsRecyclerView != null && wordsRecyclerView.getAdapter() != null) {
                                WordListAdapter adapter = (WordListAdapter) wordsRecyclerView.getAdapter();
                                adapter.updateWords(wordList);
                                Log.d(TAG, "onWordsLoaded: адаптер обновлен");
                            } else {
                                Log.e(TAG, "onWordsLoaded: адаптер не найден, создаем новый");
                                WordListAdapter adapter = new WordListAdapter(wordList, wordRepository, isCustomLibrary);
                                wordsRecyclerView.setAdapter(adapter);
                            }
                        }

                        showLoading(false);
                        Log.d(TAG, "onWordsLoaded: завершено успешно");
                    } catch (Exception e) {
                        Log.e(TAG, "onWordsLoaded: ошибка в UI потоке", e);
                        showLoading(false);
                        Toast.makeText(getContext(), "Ошибка отображения слов", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "onError: ошибка загрузки слов: " + e.getMessage(), e);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Ошибка загрузки слов: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        showEmptyState(true);
                        showLoading(false);
                    });
                }
            }
        });
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
                if (wordsRecyclerView != null) {
                    wordsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
        }
    }

    /**
     * Показывает/скрывает состояние пустого списка
     */
    private void showEmptyState(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (emptyStateText != null) {
                    emptyStateText.setVisibility(show ? View.VISIBLE : View.GONE);
                }
                if (wordsRecyclerView != null) {
                    wordsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
                }

                if (show) {
                    emptyStateText.setText("В этой библиотеке пока нет слов");
                }
            });
        }
    }
}