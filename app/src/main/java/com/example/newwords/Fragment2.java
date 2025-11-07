package com.example.newwords;

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
        wordRepository = new WordRepository();

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
                    libraryAdapter.updateLibraries(availableLibraries);

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
        // Временно активируем первую библиотеку по умолчанию
        // В реальном приложении здесь будет загрузка из Firebase
        if (!availableLibraries.isEmpty()) {
            activeLibrariesMap.put(availableLibraries.get(0).getLibraryId(), true);
            libraryAdapter.updateActiveLibraries(activeLibrariesMap);
            updateStartButtonState();
        }
    }

    /**
     * Настраивает кнопку начала обучения
     */
    private void setupStartLearningButton() {
        startLearningButton.setOnClickListener(v -> {
            if (hasActiveLibraries()) {
                // Переходим к обучению
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
        // Сохраняем выбранные библиотеки
        saveActiveLibraries();

        // Переходим к экрану обучения
        if (getActivity() != null) {
            // Используем тот же контейнер, что и в MainActivity
            // Обычно это R.id.fragment_container или android.R.id.content
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new WordsFragment()) // или R.id.fragment_container если у вас есть
                    .addToBackStack(null)
                    .commit();
        }
    }
    /**
     * Сохраняет активные библиотеки в Firebase
     */
    private void saveActiveLibraries() {
        for (Map.Entry<String, Boolean> entry : activeLibrariesMap.entrySet()) {
            if (entry.getValue()) {
                wordRepository.activateLibrary(entry.getKey(),
                        () -> Log.d(TAG, "Библиотека активирована: " + entry.getKey()),
                        e -> Log.e(TAG, "Ошибка активации библиотеки: " + e.getMessage())
                );
            }
        }
    }

    /**
     * Проверяет есть ли активные библиотеки
     */
    private boolean hasActiveLibraries() {
        return activeLibrariesMap.values().stream().anyMatch(active -> active);
    }

    /**
     * Обновляет состояние кнопки начала обучения
     */
    private void updateStartButtonState() {
        if (startLearningButton != null) {
            startLearningButton.setEnabled(hasActiveLibraries());
            startLearningButton.setAlpha(hasActiveLibraries() ? 1.0f : 0.5f);

            if (hasActiveLibraries()) {
                int activeCount = (int) activeLibrariesMap.values().stream().filter(active -> active).count();
                startLearningButton.setText("Начать обучение (" + activeCount + ")");
            } else {
                startLearningButton.setText("Начать обучение");
            }
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
    public void onLibraryToggled(String libraryId, boolean isActive) {
        Log.d(TAG, "Библиотека переключена: " + libraryId + " = " + isActive);

        activeLibrariesMap.put(libraryId, isActive);
        updateStartButtonState();

        String message = isActive ? "Библиотека активирована" : "Библиотека деактивирована";
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLibraryInfoClicked(WordLibrary library) {
        Log.d(TAG, "Информация о библиотеке: " + library.getName());

        // Показываем диалог с информацией о библиотеке
        showLibraryInfoDialog(library);
    }

    /**
     * Показывает диалог с информацией о библиотеке
     */






    // Добавьте метод:
    private void setupAddLibraryButton(View view) {
        ImageButton addLibraryButton = view.findViewById(R.id.addLibraryButton);
        if (addLibraryButton != null) {
            addLibraryButton.setOnClickListener(v -> showAddLibraryDialog());
        }
    }

    // Добавьте метод показа диалога:
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

    // Добавьте метод создания библиотеки:
    private void createCustomLibrary(String name, String description, String category) {
        wordRepository.createCustomLibrary(name, description, category,
                new WordRepository.OnLibraryCreatedListener() {
                    @Override
                    public void onLibraryCreated(WordLibrary library) {
                        Toast.makeText(getContext(), "Библиотека создана!", Toast.LENGTH_SHORT).show();
                        // Обновляем список библиотек
                        loadLibraries();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "Ошибка создания библиотеки", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void showLibraryInfoDialog(WordLibrary library) {
        // Создаем простой диалог с информацией
        String info = "Название: " + library.getName() + "\n" +
                "Описание: " + library.getDescription() + "\n" +
                "Слов: " + library.getWordCount() + "\n" +
                "Категория: " + library.getCategory();

        Toast.makeText(getContext(), info, Toast.LENGTH_LONG).show();

        // В будущем можно заменить на красивый DialogFragment
    }
}