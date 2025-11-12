package com.example.newwords;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
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
    private Map<String, Boolean> activeLibrariesMap = new HashMap<>();
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private Button startLearningButton;

    private static final String TAG = "Fragment2";

    @Nullable
    @Override



    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment2, container, false);

        // Инициализируем репозиторий
        wordRepository = new WordRepository(getContext());

        // Находим View элементы
        librariesRecyclerView = view.findViewById(R.id.librariesRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        startLearningButton = view.findViewById(R.id.startLearningButton);

        // Настраиваем кнопку назад
        setupBackButton(view);

        // Настраиваем RecyclerView
        setupRecyclerView();

        // Настраиваем кнопку начала обучения
        setupStartLearningButton();
        setupAddLibraryButton(view);
        setupRefreshButton(view);

        // Загружаем библиотеки
        loadLibraries();

        return view;
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
                libraryAdapter.updateLibraries(availableLibraries);
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
    private void startLearning() {
        Log.d(TAG, "Начало обучения с " + getActiveLibrariesCount() + " активными библиотеками");

        // Переходим к экрану обучения
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new WordsFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     * Сохраняет активные библиотеки в Firebase
     */
    private void saveActiveLibraries() {
        Log.d(TAG, "Сохранение активных библиотек: " + activeLibrariesMap.size());

        final int[] savedCount = {0};
        final int totalToSave = getActiveLibrariesCount();

        if (totalToSave == 0) {
            Log.d(TAG, "Нет библиотек для сохранения");
            return;
        }

        // Активируем выбранные библиотеки
        for (Map.Entry<String, Boolean> entry : activeLibrariesMap.entrySet()) {
            if (entry.getValue()) {
                wordRepository.activateLibrary(entry.getKey(),
                        () -> {
                            savedCount[0]++;
                            Log.d(TAG, "Библиотека активирована: " + entry.getKey() + " (" + savedCount[0] + "/" + totalToSave + ")");

                            if (savedCount[0] == totalToSave) {
                                Log.d(TAG, "Все библиотеки успешно активированы!");
                            }
                        },
                        e -> {
                            Log.e(TAG, "Ошибка активации библиотеки " + entry.getKey() + ": " + e.getMessage());
                            savedCount[0]++;
                        }
                );
            }
        }

        // Деактивируем невыбранные библиотеки (из списка доступных)
        for (WordLibrary library : availableLibraries) {
            if (!activeLibrariesMap.containsKey(library.getLibraryId()) ||
                    !activeLibrariesMap.get(library.getLibraryId())) {

                wordRepository.deactivateLibrary(library.getLibraryId(),
                        () -> Log.d(TAG, "Библиотека деактивирована: " + library.getLibraryId()),
                        e -> Log.e(TAG, "Ошибка деактивации библиотеки: " + library.getLibraryId())
                );
            }
        }
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
     */
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