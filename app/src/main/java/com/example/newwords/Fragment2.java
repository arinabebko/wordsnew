package com.example.newwords;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fragment2 extends Fragment implements LibraryAdapter.OnLibraryActionListener {

    private RecyclerView librariesRecyclerView;
    private LibraryAdapter libraryAdapter;
    private WordRepository wordRepository;
    private List<WordLibrary> availableLibraries = new ArrayList<>();
    private List<WordLibrary> filteredLibraries = new ArrayList<>(); // ← ДОБАВЬТЕ ЭТУ СТРОКУ
    private Map<String, Boolean> activeLibrariesMap = new HashMap<>();
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private Button startLearningButton;
    private AppDatabase localDb;

    private EditText searchEditText;
    private static final String TAG = "Fragment2";
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment2, container, false);

        // Инициализируем репозиторий
        wordRepository = new WordRepository(getContext());
        localDb = AppDatabase.getInstance(getContext());

        // Находим View элементы
        librariesRecyclerView = view.findViewById(R.id.librariesRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        startLearningButton = view.findViewById(R.id.startLearningButton);

        // ДОБАВИТЬ: находим поисковую строку
        searchEditText = view.findViewById(R.id.searchEditText);

        // Настраиваем RecyclerView
        setupRecyclerView();

        // Настраиваем кнопку начала обучения
        setupStartLearningButton();
        setupAddLibraryButton(view);
        setupRefreshButton(view);

        // ДОБАВИТЬ: настраиваем поиск
        setupSearch();

        // Загружаем библиотеки
        loadLibraries();

        return view;
    }



    /**
     * Настраивает поисковую строку
     */
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLibraries(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Обработчик кнопки поиска на клавиатуре
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Скрываем клавиатуру
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });
    }

    /**
     * Фильтрует библиотеки по поисковому запросу
     */

    private void filterLibraries(String query) {
        if (query.isEmpty()) {
            // Если запрос пустой, показываем все библиотеки
            filteredLibraries.clear();
            filteredLibraries.addAll(availableLibraries);
        } else {
            // Фильтруем библиотеки по названию и описанию
            filteredLibraries.clear();
            for (WordLibrary library : availableLibraries) {
                // ЗАЩИТА ОТ NULL: проверяем каждое поле перед вызовом toLowerCase()
                String name = library.getName() != null ? library.getName().toLowerCase() : "";
                String description = library.getDescription() != null ? library.getDescription().toLowerCase() : "";
                String category = library.getCategory() != null ? library.getCategory().toLowerCase() : "";

                String queryLower = query.toLowerCase();

                if (name.contains(queryLower) ||
                        description.contains(queryLower) ||
                        category.contains(queryLower)) {
                    filteredLibraries.add(library);
                }
            }
        }

        // Обновляем адаптер
        libraryAdapter.updateLibraries(filteredLibraries);

        // Показываем/скрываем состояние пустого списка
        if (filteredLibraries.isEmpty() && !query.isEmpty()) {
            showEmptyState(true);
            emptyStateText.setText("Библиотеки по запросу \"" + query + "\" не найдены");
        } else if (filteredLibraries.isEmpty()) {
            showEmptyState(true);
            emptyStateText.setText("Библиотеки не найдены\nПопробуйте позже");
        } else {
            showEmptyState(false);
        }
    }

    /**
     * Загружает доступные библиотеки
     */
    private void loadLibraries() {
        Log.d(TAG, "Загрузка библиотек...");
        showLoading(true);

        wordRepository.getAvailableLibraries(new WordRepository.OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> libraries) {
                Log.d(TAG, "Успешно загружено библиотек: " + libraries.size());

                availableLibraries.clear();
                availableLibraries.addAll(libraries);

                // Инициализируем filteredLibraries
                filteredLibraries.clear();
                filteredLibraries.addAll(availableLibraries);

                if (availableLibraries.isEmpty()) {
                    showEmptyState(true);
                } else {
                    showEmptyState(false);
                    // Загружаем активные библиотеки пользователя
                    loadUserActiveLibraries();
                }

                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка загрузки библиотек: " + e.getMessage());
                Toast.makeText(getContext(), "Ошибка загрузки библиотек", Toast.LENGTH_SHORT).show();
                showEmptyState(true);
                showLoading(false);
            }
        });
    }



    /**
     * Настраивает RecyclerView для списка библиотек
     */
    private void setupRecyclerView() {
        libraryAdapter = new LibraryAdapter(new ArrayList<>(), this);
        librariesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        librariesRecyclerView.setAdapter(libraryAdapter);
    }

    private void setupRefreshButton(View view) {
        ImageButton refreshButton = view.findViewById(R.id.refreshButton);
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> {
                Log.d(TAG, "Принудительное обновление библиотек");
                loadLibraries();
            });
        }
    }


    /**
     * Загружает активные библиотеки пользователя
     */
    private void loadUserActiveLibraries() {
        Log.d(TAG, "Загрузка активных библиотек пользователя...");

        wordRepository.getUserActiveLibraries(new WordRepository.OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> activeLibraries) {
                Log.d(TAG, "Успешно загружено активных библиотек: " + activeLibraries.size());

                // Очищаем карту активных библиотек
                activeLibrariesMap.clear();

                // Заполняем карту активных библиотек
                for (WordLibrary library : activeLibraries) {
                    activeLibrariesMap.put(library.getLibraryId(), true);
                    Log.d(TAG, "Активная библиотека: " + library.getName() + " (ID: " + library.getLibraryId() + ")");
                }

                // Обновляем состояние isActive в availableLibraries
                for (WordLibrary library : availableLibraries) {
                    boolean isActive = activeLibrariesMap.containsKey(library.getLibraryId());
                    library.setActive(isActive);
                    Log.d(TAG, "Библиотека " + library.getName() + " активна: " + isActive);
                }

                // Обновляем адаптер
               // libraryAdapter.updateLibraries(availableLibraries);
                //libraryAdapter.updateActiveLibraries(activeLibrariesMap);


                libraryAdapter.updateLibraries(filteredLibraries); // ← ИЗМЕНИТЬ
                libraryAdapter.updateActiveLibraries(activeLibrariesMap);

                // Обновляем состояние кнопки
                updateStartButtonState();

                Log.d(TAG, "Всего активных библиотек: " + activeLibrariesMap.size());
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка загрузки активных библиотек: " + e.getMessage());
                Toast.makeText(getContext(), "Ошибка загрузки активных библиотек", Toast.LENGTH_SHORT).show();

                // Если ошибка, устанавливаем все библиотеки как неактивные
                for (WordLibrary library : availableLibraries) {
                    library.setActive(false);
                }
                libraryAdapter.updateLibraries(availableLibraries);
                updateStartButtonState();
            }
        });
    }

    /**
     * Настраивает кнопку начала обучения
     */
    private void setupStartLearningButton() {
        startLearningButton.setOnClickListener(v -> {
            if (hasActiveLibraries()) {
                // Сохраняем активные библиотеки и переходим к обучению
                saveActiveLibraries();
                startLearning();
            } else {
                Toast.makeText(getContext(), "Выберите хотя бы одну библиотеку", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Начинает обучение с выбранными библиотеками
     */


    private void saveActiveLibraries() {
        Log.d(TAG, "💾 Сохранение активных библиотек в ЛОКАЛЬНОЕ хранилище");

        // Сохраняем в локальную БД
        new Thread(() -> {
            try {
                // Сначала деактивируем ВСЕ библиотеки
                List<LocalWordLibrary> allLibraries = localDb.libraryDao().getAllLibraries();
                for (LocalWordLibrary library : allLibraries) {
                    localDb.libraryDao().updateLibraryActiveStatus(library.getLibraryId(), false);
                }

                // Затем активируем выбранные
                int activatedCount = 0;
                for (Map.Entry<String, Boolean> entry : activeLibrariesMap.entrySet()) {
                    if (entry.getValue()) {
                        localDb.libraryDao().updateLibraryActiveStatus(entry.getKey(), true);
                        activatedCount++;
                    }
                }

                Log.d(TAG, "✅ Локальное сохранение: активировано " + activatedCount + " библиотек");

                // Теперь синхронизируем с Firebase (в фоне, не блокируем пользователя)
                syncWithFirebase();

            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка локального сохранения: " + e.getMessage());
            }
        }).start();
    }

    private void syncWithFirebase() {
        // Это можно делать асинхронно, не блокируя пользователя
        for (Map.Entry<String, Boolean> entry : activeLibrariesMap.entrySet()) {
            if (entry.getValue()) {
                wordRepository.activateLibrary(entry.getKey(),
                        () -> Log.d(TAG, "Firebase: библиотека активирована: " + entry.getKey()),
                        e -> Log.e(TAG, "Firebase: ошибка активации: " + entry.getKey())
                );
            } else {
                wordRepository.deactivateLibrary(entry.getKey(),
                        () -> Log.d(TAG, "Firebase: библиотека деактивирована: " + entry.getKey()),
                        e -> Log.e(TAG, "Firebase: ошибка деактивации: " + entry.getKey())
                );
            }
        }
    }

    private void startLearning() {
        Log.d(TAG, "Начало обучения с " + getActiveLibrariesCount() + " активными библиотеками");

        NotificationScheduler.resetInactivityTimer(getActivity());

        // ПРИНУДИТЕЛЬНО ОБНОВЛЯЕМ КЕШ перед переходом
        wordRepository.syncWordsFromFirebase(new WordRepository.OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                Log.d(TAG, "Кеш обновлен, слов: " + words.size());

                // Переходим к обучению
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(android.R.id.content, new WordsFragment())
                            .addToBackStack(null)
                            .commit();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка обновления кеша", e);
                // Все равно переходим, но с устаревшими данными
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(android.R.id.content, new WordsFragment())
                            .addToBackStack(null)
                            .commit();
                }
            }
        });
    }



    /**
     * Проверяет есть ли активные библиотеки
     */
    private boolean hasActiveLibraries() {
        return getActiveLibrariesCount() > 0;
    }

    /**
     * Возвращает количество активных библиотек
     */
    private int getActiveLibrariesCount() {
        return (int) activeLibrariesMap.values().stream().filter(active -> active).count();
    }

    /**
     * Обновляет состояние кнопки начала обучения
     */
    private void updateStartButtonState() {
        if (startLearningButton != null && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                boolean hasActive = hasActiveLibraries();
                startLearningButton.setEnabled(hasActive);
                startLearningButton.setAlpha(hasActive ? 1.0f : 0.5f);

                if (hasActive) {
                    int activeCount = getActiveLibrariesCount();
                    startLearningButton.setText("Начать обучение (" + activeCount + ")");
                } else {
                    startLearningButton.setText("Начать обучение");
                }
            });
        }
    }

    /**
     * Настраивает кнопку назад

    private void setupBackButton(View view) {
        ImageButton backButton = view.findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }
    }
 */
    /**
     * Показывает/скрывает индикатор загрузки
     */
    private void showLoading(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
                if (librariesRecyclerView != null) {
                    librariesRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
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
                if (librariesRecyclerView != null) {
                    librariesRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
        }
    }

    // === РЕАЛИЗАЦИЯ ИНТЕРФЕЙСА LibraryAdapter.OnLibraryActionListener ===

    @Override
    public WordRepository getWordRepository() {
        return wordRepository;
    }

    @Override
    public void onLibraryToggleSuccess(String libraryId, boolean isActive) {
        Log.d(TAG, "Успешное переключение библиотеки: " + libraryId + " = " + isActive);

        // Обновляем локальное состояние
        activeLibrariesMap.put(libraryId, isActive);

        // Обновляем состояние в availableLibraries
        for (WordLibrary library : availableLibraries) {
            if (library.getLibraryId().equals(libraryId)) {
                library.setActive(isActive);
                break;
            }
        }

        updateStartButtonState();

        String message = isActive ? "Библиотека активирована" : "Библиотека деактивирована";
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLibraryToggleError(String libraryId, boolean originalState) {
        Log.e(TAG, "Ошибка переключения библиотеки: " + libraryId);

        // Возвращаем в исходное состояние
        activeLibrariesMap.put(libraryId, originalState);

        // Обновляем адаптер
        libraryAdapter.updateActiveLibraries(activeLibrariesMap);

        Toast.makeText(getContext(), "Ошибка изменения состояния библиотеки", Toast.LENGTH_SHORT).show();
    }



    @Override
    public void onLibraryInfoClicked(WordLibrary library) {
        Log.d(TAG, "Информация о библиотеке: " + library.getName());
        showLibraryInfoDialog(library);
    }

    @Override
    public void onLibraryManageClicked(WordLibrary library) {
        Log.d(TAG, "Управление библиотекой: " + library.getName());
        showLibraryManagementDialog(library);
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private void setupAddLibraryButton(View view) {
        ImageButton addLibraryButton = view.findViewById(R.id.addLibraryButton);
        if (addLibraryButton != null) {
            addLibraryButton.setOnClickListener(v -> showAddLibraryDialog());
        }
    }

    private void showAddLibraryDialog() {
        AddLibraryDialog dialog = new AddLibraryDialog();
        dialog.setOnLibraryCreatedListener(new AddLibraryDialog.OnLibraryCreatedListener() {
            @Override
            public void onLibraryCreated(String name, String description, String category) {
                createCustomLibrary(name, description, category);
            }
        });
        dialog.show(getParentFragmentManager(), "add_library_dialog");
    }

    private void createCustomLibrary(String name, String description, String category) {
        Log.d(TAG, "Создание библиотеки: " + name);

        wordRepository.createCustomLibrary(name, description, category,
                new WordRepository.OnLibraryCreatedListener() {
                    @Override
                    public void onLibraryCreated(WordLibrary library) {
                        Log.d(TAG, "Библиотека успешно создана: " + library.getName() + ", ID: " + library.getLibraryId());
                        Toast.makeText(getContext(), "Библиотека создана!", Toast.LENGTH_SHORT).show();
                        loadLibraries();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Ошибка создания библиотеки: " + e.getMessage());
                        Toast.makeText(getContext(), "Ошибка создания библиотеки: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLibraryInfoDialog(WordLibrary library) {
        String info = "Название: " + library.getName() + "\n" +
                "Описание: " + library.getDescription() + "\n" +
                "Слов: " + library.getWordCount() + "\n" +
                "Категория: " + library.getCategory();

        Toast.makeText(getContext(), info, Toast.LENGTH_LONG).show();
    }

    private void showLibraryManagementDialog(WordLibrary library) {
        String[] options = {"Добавить слово", "Просмотреть слова", "Удалить библиотеку"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Управление: " + library.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showAddWordDialog(library);
                            break;
                        case 1:
                            showLibraryWords(library);
                            break;
                        case 2:
                            deleteLibrary(library);
                            break;
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showAddWordDialog(WordLibrary library) {
        AddWordDialog dialog = AddWordDialog.newInstance(library.getLibraryId(), library.getName());
        dialog.setOnWordAddedListener(new AddWordDialog.OnWordAddedListener() {
            @Override
            public void onWordAdded(String word, String translation, String note) {
                addWordToLibrary(library.getLibraryId(), word, translation, note);
            }
        });
        dialog.show(getParentFragmentManager(), "add_word_dialog");
    }

    private void addWordToLibrary(String libraryId, String word, String translation, String note) {
        WordItem newWord = new WordItem(word, translation, note);

        wordRepository.addWordToCustomLibrary(libraryId, newWord,
                new WordRepository.OnWordAddedListener() {
                    @Override
                    public void onWordAdded(WordItem word) {
                        Toast.makeText(getContext(), "Слово добавлено!", Toast.LENGTH_SHORT).show();
                        loadLibraries();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "Ошибка добавления слова", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLibraryWords(WordLibrary library) {
        Log.d(TAG, "Просмотр слов библиотеки: " + library.getName());

        boolean isCustomLibrary = library.getCreatedBy() != null && !library.getCreatedBy().equals("system");

        LibraryWordsFragment wordsFragment = LibraryWordsFragment.newInstance(
                library.getLibraryId(),
                library.getName(),
                isCustomLibrary
        );

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, wordsFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     * Удаляет библиотеку
     */
    private void deleteLibrary(WordLibrary library) {
        if (library.getCreatedBy() == null || library.getCreatedBy().equals("system")) {
            Toast.makeText(getContext(), "Нельзя удалять системные библиотеки", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Удаление библиотеки")
                .setMessage("Вы уверены, что хотите удалить библиотеку \"" + library.getName() + "\"? Все слова в ней будут удалены.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    performLibraryDelete(library);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void performLibraryDelete(WordLibrary library) {
        wordRepository.deleteCustomLibrary(library.getLibraryId(),
                () -> {
                    Toast.makeText(getContext(), "Библиотека удалена", Toast.LENGTH_SHORT).show();
                    // Обновляем список библиотек
                    loadLibraries();
                },
                e -> {
                    Toast.makeText(getContext(), "Ошибка удаления библиотеки", Toast.LENGTH_SHORT).show();
                }
        );
    }
}