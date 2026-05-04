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
    // НОВЫЕ ПЕРЕМЕННЫЕ ДЛЯ ЯЗЫКОВ
    private LanguageManager languageManager;
    private TextView languageEnglishText;
    private TextView languageBashkirText;
    private String currentLanguage = "en"; // текущий выбранный язык
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

        // Инициализируем менеджер языков
        languageManager = new LanguageManager(getContext());
        currentLanguage = languageManager.getCurrentLanguage();

        // Находим View элементы
        librariesRecyclerView = view.findViewById(R.id.librariesRecyclerView);
      //  progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        startLearningButton = view.findViewById(R.id.startLearningButton);
        searchEditText = view.findViewById(R.id.searchEditText);

        // НАХОДИМ ЭЛЕМЕНТЫ ВЫБОРА ЯЗЫКА
        languageEnglishText = view.findViewById(R.id.languageEnglish);
        languageBashkirText = view.findViewById(R.id.languageBashkir);

        // Настраиваем RecyclerView
        setupRecyclerView();

        // Настраиваем кнопку начала обучения
        setupStartLearningButton();

        // Настраиваем переключатель языков
        setupLanguageSelector();

        setupAddLibraryButton(view);
        setupRefreshButton(view);

        // Настраиваем поиск
        setupSearch();

        // Загружаем библиотеки (с учетом текущего языка)
        loadLibraries();

        return view;
    }
    /**
     * Настраивает переключатель языков
     */
    private void setupLanguageSelector() {
        updateLanguageUI();

        languageEnglishText.setOnClickListener(v -> {
            if (!currentLanguage.equals("en")) {
                // 1. СОХРАНЯЕМ активные библиотеки для ТЕКУЩЕГО языка
                saveActiveLibrariesForCurrentLanguage();

                // 2. Меняем язык с передачей текущих активных библиотек
                languageManager.setCurrentLanguage("en", activeLibrariesMap);
                currentLanguage = "en";

                // 3. Обновляем UI
                updateLanguageUI();

                // 4. Очищаем UI и загружаем библиотеки для нового языка
                clearUIForLanguageChange();
                loadLibraries();
            }
        });

        languageBashkirText.setOnClickListener(v -> {
            if (!currentLanguage.equals("ba")) {
                // 1. СОХРАНЯЕМ активные библиотеки для ТЕКУЩЕГО языка
                saveActiveLibrariesForCurrentLanguage();

                // 2. Меняем язык с передачей текущих активных библиотек
                languageManager.setCurrentLanguage("ba", activeLibrariesMap);
                currentLanguage = "ba";

                // 3. Обновляем UI
                updateLanguageUI();

                // 4. Очищаем UI и загружаем библиотеки для нового языка
                clearUIForLanguageChange();
                loadLibraries();
            }
        });
    }

    /**
     * Сохраняет активные библиотеки для текущего языка
     */
    private void saveActiveLibrariesForCurrentLanguage() {
        if (!activeLibrariesMap.isEmpty()) {
            languageManager.saveActiveLibrariesForCurrentLanguage(activeLibrariesMap);
            Log.d(TAG, "✅ Сохранено активных библиотек для языка " + currentLanguage +
                    ": " + activeLibrariesMap.size() + " шт.");
        }
    }

    /**
     * Очищает UI при смене языка
     */
    /**
     * Очищает UI при смене языка
     */
    private void clearUIForLanguageChange() {
        // Очищаем адаптер
        if (libraryAdapter != null) {
            libraryAdapter.updateLibraries(new ArrayList<>());
            libraryAdapter.updateActiveLibraries(new HashMap<>());
        }

        // Очищаем списки библиотек
        availableLibraries.clear();
        filteredLibraries.clear();

        // НЕ очищаем activeLibrariesMap!
        // Он будет загружен из сохраненного состояния для нового языка

        // Сбрасываем кнопку
        updateStartButtonState();

        // Показываем загрузку
        showLoading(true);
        showEmptyState(false);
    }

    /**
     * Обновляет UI переключателя языков
     */
    private void updateLanguageUI() {
        if (languageEnglishText == null || languageBashkirText == null) return;

        // Сбрасываем все стили
        languageEnglishText.setBackgroundResource(R.drawable.language_selector_background);
        languageBashkirText.setBackgroundResource(R.drawable.language_selector_background);
        languageEnglishText.setTextColor(0xFFBABBA9);
        languageBashkirText.setTextColor(0xFFBABBA9);

        // Выделяем выбранный язык
        if (currentLanguage.equals("en")) {
            languageEnglishText.setBackgroundResource(R.drawable.language_selector_selected);
            languageEnglishText.setTextColor(0xFFFFFFFF);
        } else if (currentLanguage.equals("ba")) {
            languageBashkirText.setBackgroundResource(R.drawable.language_selector_selected);
            languageBashkirText.setTextColor(0xFFFFFFFF);
        }
    }

    private void loadLibraries() {
        Log.d(TAG, "Загрузка библиотек для языка: " + currentLanguage);
        showLoading(true);

        // СНАЧАЛА ОЧИСТИТЬ ВИДИМЫЕ ЭЛЕМЕНТЫ
        if (libraryAdapter != null) {
            libraryAdapter.updateLibraries(new ArrayList<>()); // очистить список
        }
        showEmptyState(false); // временно скрыть empty state

        wordRepository.getAvailableLibraries(new WordRepository.OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> libraries) {
                Log.d(TAG, "Загружено всего библиотек: " + libraries.size());

                availableLibraries.clear();
                filteredLibraries.clear();
                // НЕ очищаем activeLibrariesMap здесь! // ← УБРАТЬ ЭТУ СТРОКУ

                // ФИЛЬТРУЕМ БИБЛИОТЕКИ ПО languageFrom (оригинальный язык)
                for (WordLibrary library : libraries) {
                    String libraryOriginalLanguage = library.getLanguageFrom();

                    if (libraryOriginalLanguage != null && libraryOriginalLanguage.equals(currentLanguage)) {
                        availableLibraries.add(library);
                        Log.d(TAG, "✅ Добавлена библиотека: " + library.getName());
                    }
                }

                Log.d(TAG, "Отфильтровано для языка " + currentLanguage + ": " +
                        availableLibraries.size() + " библиотек");

                // Всегда обновляем filteredLibraries
                filteredLibraries.clear();
                filteredLibraries.addAll(availableLibraries);

                // ОБНОВЛЯЕМ АДАПТЕР СРАЗУ
                if (libraryAdapter != null) {
                    libraryAdapter.updateLibraries(filteredLibraries);
                    libraryAdapter.updateActiveLibraries(new HashMap<>()); // очистить отображение
                }

                if (availableLibraries.isEmpty()) {
                    showEmptyState(true);
                    String languageName = languageManager.getLanguageDisplayName(currentLanguage);
                    emptyStateText.setText(getString(R.string.lib_select_error_no_libraries, languageName));
                    // Если нет библиотек для этого языка, очищаем activeLibrariesMap
                    activeLibrariesMap.clear();
                } else {
                    showEmptyState(false);
                    // Загружаем активные библиотеки пользователя ДЛЯ ТЕКУЩЕГО ЯЗЫКА
                    loadUserActiveLibrariesForCurrentLanguage();
                }

                // Обновляем состояние кнопки
                updateStartButtonState();

                showLoading(false);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка загрузки библиотек: " + e.getMessage()); // Логи оставляем на английском/русском для дебага [cite: 217]

                // Очистить адаптер и показать ошибку [cite: 218]
                if (libraryAdapter != null) {
                    libraryAdapter.updateLibraries(new ArrayList<>()); // [cite: 220]
                }

                // Используем строковый ресурс для Toast [cite: 222]
                Toast.makeText(getContext(), R.string.lib_load_error_toast, Toast.LENGTH_SHORT).show();

                showEmptyState(true); // [cite: 223]

                // Устанавливаем текст ошибки из ресурсов [cite: 224]
                emptyStateText.setText(R.string.lib_load_error_connection);

                showLoading(false); // [cite: 225]
            }
        });
    }
    /**
     * Загружает активные библиотеки пользователя для текущего языка
     */
    /**
     * Загружает активные библиотеки пользователя для текущего языка
     */
    private void loadUserActiveLibrariesForCurrentLanguage() {
        Log.d(TAG, "Загрузка активных библиотек для языка: " + currentLanguage);

        // СНАЧАЛА: загружаем из локального хранилища (SharedPreferences)
        Map<String, Boolean> savedActiveLibraries =
                languageManager.getActiveLibrariesMapForCurrentLanguage();

        // Очищаем текущую карту и загружаем сохраненные
        activeLibrariesMap.clear();
        if (!savedActiveLibraries.isEmpty()) {
            activeLibrariesMap.putAll(savedActiveLibraries);
            Log.d(TAG, "✅ Загружено из SharedPreferences: " +
                    savedActiveLibraries.size() + " активных библиотек для языка " + currentLanguage);
        }

        // Теперь загружаем из Firebase (для синхронизации)
        wordRepository.getUserActiveLibraries(new WordRepository.OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> firebaseActiveLibraries) {
                Log.d(TAG, "Загружено из Firebase: " + firebaseActiveLibraries.size() + " библиотек");

                // Обновляем из Firebase ТОЛЬКО библиотеки для текущего языка
                for (WordLibrary library : firebaseActiveLibraries) {
                    String libraryLanguage = library.getLanguageFrom(); // используем languageFrom
                    if (libraryLanguage != null && libraryLanguage.equals(currentLanguage)) {
                        activeLibrariesMap.put(library.getLibraryId(), true);
                        Log.d(TAG, "Firebase: активная библиотека " + library.getName());
                    }
                }

                // Обновляем состояние библиотек в списке
                updateLibrariesActiveState();

                // Сохраняем объединенный результат
                if (!activeLibrariesMap.isEmpty()) {
                    languageManager.saveActiveLibrariesForCurrentLanguage(activeLibrariesMap);
                }

                // Обновляем адаптер
                libraryAdapter.updateLibraries(filteredLibraries);
                libraryAdapter.updateActiveLibraries(activeLibrariesMap);

                updateStartButtonState();

                Log.d(TAG, "Итого активных библиотек для языка " + currentLanguage +
                        ": " + activeLibrariesMap.size());
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка загрузки из Firebase: " + e.getMessage());

                // Используем только локальные данные
                updateLibrariesActiveState();
                libraryAdapter.updateLibraries(filteredLibraries);
                libraryAdapter.updateActiveLibraries(activeLibrariesMap);
                updateStartButtonState();

                Log.d(TAG, "Используем локальные данные: " +
                        activeLibrariesMap.size() + " активных библиотек");
            }
        });
    }

    /**
     * Обновляет состояние активных библиотек в списке
     */
    private void updateLibrariesActiveState() {
        for (WordLibrary library : availableLibraries) {
            String libraryId = library.getLibraryId();
            boolean isActive = activeLibrariesMap.containsKey(libraryId) &&
                    activeLibrariesMap.get(libraryId);

            library.setActive(isActive);
            library.setActiveForLanguage(currentLanguage, isActive);
        }
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
                library.setActiveForLanguage(currentLanguage, isActive);
                break;
            }
        }

        // НЕМЕДЛЕННО сохраняем состояние для текущего языка
        if (!activeLibrariesMap.isEmpty()) {
            languageManager.saveActiveLibrariesForCurrentLanguage(activeLibrariesMap);
            Log.d(TAG, "✅ Сохранено активных библиотек: " + activeLibrariesMap.size());
        }

        updateStartButtonState();

        String message = isActive ? getString(R.string.lib_status_activated) : getString(R.string.lib_status_deactivated);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
    /**
     * Загружает активные библиотеки из локального хранилища
     */
    private void loadActiveLibrariesFromLocalStorage() {
        try {
            activeLibrariesMap.clear();

            // Получаем активные библиотеки из SharedPreferences для текущего языка
            String activeLibsString = languageManager.getActiveLibrariesForCurrentLanguage();
            if (activeLibsString != null && !activeLibsString.isEmpty()) {
                String[] activeLibs = activeLibsString.split(",");
                for (String libId : activeLibs) {
                    if (!libId.trim().isEmpty()) {
                        activeLibrariesMap.put(libId.trim(), true);
                    }
                }
            }

            // Обновляем состояние библиотек
            for (WordLibrary library : availableLibraries) {
                String libId = library.getLibraryId();
                boolean isActive = activeLibrariesMap.containsKey(libId);
                library.setActive(isActive);
                library.setActiveForLanguage(currentLanguage, isActive);
            }

            // Обновляем адаптер
            libraryAdapter.updateLibraries(filteredLibraries);
            libraryAdapter.updateActiveLibraries(activeLibrariesMap);
            updateStartButtonState();

            Log.d(TAG, "Загружено из локального хранилища для " + currentLanguage +
                    ": " + activeLibrariesMap.size() + " активных библиотек");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки из локального хранилища", e);
        }
    }

    /**
     * Сохраняет активные библиотеки для текущего языка
     */
    /**
     * Сохраняет активные библиотеки для текущего языка
     */

    private void saveActiveLibrariesToLocalDB() {
        new Thread(() -> {
            try {
                // Обновляем состояние библиотек в локальной БД
                for (WordLibrary library : availableLibraries) {
                    boolean isActive = activeLibrariesMap.containsKey(library.getLibraryId());
                    library.setActive(isActive);

                    // Обновляем в локальной БД
                    LocalWordLibrary localLib = localDb.libraryDao().getLibraryById(library.getLibraryId());
                    if (localLib != null) {
                        localLib.setActive(isActive);
                        localDb.libraryDao().updateLibrary(localLib);
                    }
                }
                Log.d(TAG, "✅ Локальная БД обновлена для языка " + currentLanguage);
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка обновления локальной БД", e);
            }
        }).start();
    }

    // Модифицируем метод сохранения активных библиотек
    private void saveActiveLibraries() {
        Log.d(TAG, "💾 Сохранение активных библиотек");

        // 1. Сохраняем для текущего языка
        saveActiveLibrariesForCurrentLanguage();

        // 2. Сохраняем в локальную БД
        new Thread(() -> {
            try {
                // Сначала деактивируем ВСЕ библиотеки для текущего языка
                List<LocalWordLibrary> allLibraries = localDb.libraryDao().getAllLibraries();
                for (LocalWordLibrary library : allLibraries) {
                    // Деактивируем только если библиотека для текущего языка
                    String libLanguage = library.getLanguageTo();
                    if (libLanguage != null && libLanguage.equals(currentLanguage)) {
                        localDb.libraryDao().updateLibraryActiveStatus(library.getLibraryId(), false);
                    }
                }

                // Затем активируем выбранные для текущего языка
                int activatedCount = 0;
                for (Map.Entry<String, Boolean> entry : activeLibrariesMap.entrySet()) {
                    if (entry.getValue()) {
                        localDb.libraryDao().updateLibraryActiveStatus(entry.getKey(), true);
                        activatedCount++;
                    }
                }

                Log.d(TAG, "✅ Локальное сохранение: активировано " +
                        activatedCount + " библиотек для языка " + currentLanguage);

                // Синхронизируем с Firebase
                syncWithFirebase();

            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка локального сохранения", e);
            }
        }).start();
    }
    @Override
    public void onPause() {
        super.onPause();
        // Сохраняем состояние при уходе с фрагмента
        saveActiveLibrariesForCurrentLanguage();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Сохраняем состояние при уничтожении view
        saveActiveLibrariesForCurrentLanguage();
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
            filteredLibraries.clear();
            filteredLibraries.addAll(availableLibraries);
        } else {
            filteredLibraries.clear();
            for (WordLibrary library : availableLibraries) {
                String name = library.getName() != null ? library.getLocalizedName().toLowerCase() : "";
                String description = library.getDescription() != null ? library.getLocalizedDescription().toLowerCase() : "";
                String category = library.getCategory() != null ? library.getCategory().toLowerCase() : "";

                String queryLower = query.toLowerCase();

                if (name.contains(queryLower) ||
                        description.contains(queryLower) ||
                        category.contains(queryLower)) {
                    filteredLibraries.add(library);
                }
            }
        }

        libraryAdapter.updateLibraries(filteredLibraries);

        // Показываем/скрываем состояние пустого списка
        if (filteredLibraries.isEmpty() && !query.isEmpty()) {
            showEmptyState(true);
            // ⭐ ИСПОЛЬЗУЕМ РЕСУРС С ПАРАМЕТРОМ:
            emptyStateText.setText(getString(R.string.lib_search_not_found, query));
        } else if (filteredLibraries.isEmpty()) {
            showEmptyState(true);
            // Тут можно использовать общий текст "Библиотеки не найдены"
            emptyStateText.setText(R.string.lib_select_tv_empty_state);
        } else {
            showEmptyState(false);
        }
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

                // ⭐ Используем ресурс для ошибки загрузки
                Toast.makeText(getContext(), R.string.lib_error_load_active, Toast.LENGTH_SHORT).show();

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
        Log.d(TAG, "Начало обучения для языка: " + currentLanguage +
                " с " + getActiveLibrariesCount() + " активными библиотеками");

        // Сохраняем активные библиотеки
        saveActiveLibraries();

        // Создаем фрагмент обучения с указанием языка
        WordsFragment wordsFragment = WordsFragment.newInstance(currentLanguage);

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, wordsFragment)
                    .addToBackStack(null)
                    .commit();
        }

        // Синхронизируем с Firebase в фоне
        syncWithFirebase();
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
                //todo
               // if (hasActive) {
                 //   int activeCount = getActiveLibrariesCount();
                    // ⭐ Используем ресурс с числовым параметром
                   // startLearningButton.setText(getString(R.string.lib_btn_start_with_count, activeCount));
                //} else {
                    // ⭐ Используем обычный ресурс кнопки
                    startLearningButton.setText(R.string.lib_select_btn_start);
                //}
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
     /**
     * Показывает/скрывает индикатор загрузки
     */
    private void showLoading(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (progressBar != null) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                    if (show) {
                        progressBar.bringToFront(); // поверх всего
                    }
                }
                if (librariesRecyclerView != null) {
                    librariesRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
                if (emptyStateText != null) {
                    emptyStateText.setVisibility(show ? View.GONE : emptyStateText.getVisibility());
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

                    if (show) {
                        // Поднимаем emptyStateText на верх, чтобы он был поверх RecyclerView
                        emptyStateText.bringToFront();
                    }
                }
                if (librariesRecyclerView != null) {
                    librariesRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
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
            public void onLibraryCreated(String name, String description, String category, String language) {
                // Передаем все 4 параметра
                createCustomLibrary(name, description, category, language);
            }
        });
        dialog.show(getParentFragmentManager(), "add_library_dialog");
    }
    private void createCustomLibrary(String name, String description, String category, String language) {
        wordRepository.createCustomLibrary(name, description, category, language,
                new WordRepository.OnLibraryCreatedListener() {
                    @Override
                    public void onLibraryCreated(WordLibrary library) {
                        // Если созданная библиотека для текущего языка
                        if (language.equals(currentLanguage)) {
                            Toast.makeText(getContext(), R.string.lib_create_success, Toast.LENGTH_SHORT).show();
                            loadLibraries();
                        } else {
                            // Если создали для другого языка (например, создаем англ. библиотеку, находясь в башк. интерфейсе)
                            String langName = languageManager.getLanguageDisplayName(language);
                            Toast.makeText(getContext(),
                                    getString(R.string.lib_create_success_other_lang, langName),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(),
                                getString(R.string.lib_create_error_params, e.getMessage()),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLibraryInfoDialog(WordLibrary library) {
        // Формируем красивую строку информации из ресурсов
        String info = getString(R.string.lib_info_name) + ": " + library.getName() + "\n" +
                getString(R.string.lib_info_desc) + ": " + library.getDescription() + "\n" +
                getString(R.string.lib_info_count) + ": " + library.getWordCount() + "\n" +
                getString(R.string.lib_info_category) + ": " + library.getCategory();

        Toast.makeText(getContext(), info, Toast.LENGTH_LONG).show();
    }

    private void showLibraryManagementDialog(WordLibrary library) {
        // Опции меню из ресурсов
        String[] options = {
                getString(R.string.lib_manage_add_word),
                getString(R.string.lib_manage_view_words),
                getString(R.string.lib_manage_delete)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.lib_manage_title, library.getName()))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: showAddWordDialog(library); break;
                        case 1: showLibraryWords(library); break;
                        case 2: deleteLibrary(library); break;
                    }
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    private void addWordToLibrary(String libraryId, String word, String translation, String note) {
        WordItem newWord = new WordItem(word, translation, note);
        wordRepository.addWordToCustomLibrary(libraryId, newWord,
                new WordRepository.OnWordAddedListener() {
                    @Override
                    public void onWordAdded(WordItem word) {
                        Toast.makeText(getContext(), R.string.word_add_success, Toast.LENGTH_SHORT).show();
                        loadLibraries();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), R.string.word_add_error_toast, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLibraryWords(WordLibrary library) {
        Log.d(TAG, "Просмотр слов библиотеки: " + library.getName());

        boolean isCustomLibrary = library.getCreatedBy() != null && !library.getCreatedBy().equals("system");

        LibraryWordsFragment wordsFragment = LibraryWordsFragment.newInstance(
                library.getLibraryId(),
                library.getLocalizedName(),
                isCustomLibrary
        );

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, wordsFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
    private void showAddWordDialog(WordLibrary library) {
        AddWordDialog dialog = AddWordDialog.newInstance(library.getLibraryId(), library.getLocalizedName());
        dialog.setOnWordAddedListener(new AddWordDialog.OnWordAddedListener() {
            @Override
            public void onWordAdded(String word, String translation, String note) {
                addWordToLibrary(library.getLibraryId(), word, translation, note);
            }
        });
        dialog.show(getParentFragmentManager(), "add_word_dialog");
    }

    /**
     * Удаляет библиотеку
     */
    private void deleteLibrary(WordLibrary library) {
        if (library.getCreatedBy() == null || library.getCreatedBy().equals("system")) {
            // Используем ресурс для запрета удаления системных библиотек
            Toast.makeText(getContext(), R.string.lib_delete_system_error, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.lib_delete_confirm_title)
                // Используем getString с параметром для названия библиотеки
                .setMessage(getString(R.string.lib_delete_confirm_msg, library.getName()))
                .setPositiveButton(R.string.lib_manage_delete, (dialog, which) -> {
                    performLibraryDelete(library);
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    private void performLibraryDelete(WordLibrary library) {
        wordRepository.deleteCustomLibrary(library.getLibraryId(),
                () -> {
                    // Ресурс для успешного удаления
                    Toast.makeText(getContext(), R.string.lib_delete_success, Toast.LENGTH_SHORT).show();
                    loadLibraries();
                },
                e -> {
                    // Ресурс для ошибки удаления
                    Toast.makeText(getContext(), R.string.lib_delete_error, Toast.LENGTH_SHORT).show();
                }
        );
    }
}