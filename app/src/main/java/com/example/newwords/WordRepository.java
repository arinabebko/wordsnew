package com.example.newwords;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class WordRepository {
    private final FirebaseFirestore db;
    private final String userId;
    private final AppDatabase localDb;

    public WordRepository() {
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.userId = user != null ? user.getUid() : "anonymous";
        this.localDb = AppDatabase.getInstance(FirebaseApp.getInstance().getApplicationContext());
    }

    public WordRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // ПРОВЕРКА, что пользователь залогинен
        if (user != null) {
            this.userId = user.getUid();
            Log.d(TAG, "✅ Пользователь аутентифицирован: " + this.userId);
        } else {
            this.userId = "anonymous";
            Log.e(TAG, "❌ Пользователь НЕ аутентифицирован!");
        }

        this.localDb = AppDatabase.getInstance(context);
    }


    // === ИНТЕРФЕЙСЫ ДЛЯ КОЛБЭКОВ ===




    public interface StatUpdateListener {
        UserStats onUpdate(UserStats stats);
    }




    public interface OnStatsLoadedListener {
        void onStatsLoaded(UserStats stats);
        void onError(Exception e);
    }

    public interface StatsUpdater {
        UserStats update(UserStats stats);
    }

    public interface OnWordsLoadedListener {
        void onWordsLoaded(List<WordItem> words);

        void onError(Exception e);
    }

    public interface OnWordAddedListener {
        void onWordAdded(WordItem word);

        void onError(Exception e);
    }

    public interface OnLibrariesLoadedListener {
        void onLibrariesLoaded(List<WordLibrary> libraries);

        void onError(Exception e);
    }

    public interface OnSuccessListener {
        void onSuccess();
    }

    public interface OnErrorListener {
        void onError(Exception e);
    }

    public interface OnLibraryCreatedListener {
        void onLibraryCreated(WordLibrary library);

        void onError(Exception e);
    }

    // === ОСНОВНЫЕ МЕТОДЫ ===


    /**
     * Синхронизация слов из Firebase
     */
    /**
     * Синхронизация слов из Firebase
     */
    public void syncWordsFromFirebase(OnWordsLoadedListener listener) {
        Log.d(TAG, "🔄 Синхронизация с Firebase...");

        getUserActiveLibraries(new OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> activeLibraries) {
                Log.d(TAG, "📚 Активных библиотек для синхронизации: " + activeLibraries.size());

                List<WordItem> allWords = new ArrayList<>();
                List<Task<QuerySnapshot>> allTasks = new ArrayList<>();

                for (WordLibrary library : activeLibraries) {
                    if (!library.getIsActive()) {
                        Log.d(TAG, "❌ Пропускаем неактивную библиотеку: " + library.getName());
                        continue;
                    }

                    boolean isCustom = library.getCreatedBy() != null && !library.getCreatedBy().equals("system");
                    Task<QuerySnapshot> task = getWordsFromSingleLibrary(library.getLibraryId(), isCustom);
                    allTasks.add(task);
                    Log.d(TAG, "✅ Синхронизация библиотеки: " + library.getName());
                }

                if (allTasks.isEmpty()) {
                    Log.d(TAG, "ℹ️ Нет активных библиотек для синхронизации");
                    if (listener != null) {
                        listener.onWordsLoaded(new ArrayList<>());
                    }
                    return;
                }

                Tasks.whenAllSuccess(allTasks).addOnSuccessListener(results -> {
                    for (Object result : results) {
                        if (result instanceof QuerySnapshot) {
                            QuerySnapshot snapshot = (QuerySnapshot) result;
                            for (QueryDocumentSnapshot document : snapshot) {
                                WordItem word = document.toObject(WordItem.class);
                                word.setWordId(document.getId());
                                word.setLibraryId(document.getReference().getParent().getParent().getId());
                                allWords.add(word);
                            }
                        }
                    }

                    Log.d(TAG, "📥 Синхронизировано слов: " + allWords.size());
                    saveWordsToCache(allWords);
                    saveActiveLibrariesToCache(activeLibraries);

                    if (listener != null) {
                        listener.onWordsLoaded(allWords);
                    }

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка синхронизации", e);
                    if (listener != null) {
                        listener.onError(e);
                    }
                });
            }



            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Ошибка загрузки библиотек для синхронизации", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * Сохраняет слова в кеш
     */
    private void saveWordsToCache(List<WordItem> words) {
        new Thread(() -> {
            try {
                List<LocalWordItem> localWords = new ArrayList<>();
                for (WordItem word : words) {
                    localWords.add(new LocalWordItem(word));
                }
                localDb.wordDao().insertWords(localWords);
                Log.d(TAG, "💾 Сохранено в кеш: " + localWords.size() + " слов");
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка сохранения в кеш", e);
            }
        }).start();
    }

    /**
     * Сохраняет активные библиотеки в кеш
     */
    private void saveActiveLibrariesToCache(List<WordLibrary> libraries) {
        new Thread(() -> {
            try {
                List<LocalWordLibrary> localLibraries = new ArrayList<>();
                for (WordLibrary library : libraries) {
                    LocalWordLibrary localLib = new LocalWordLibrary(library);
                    localLib.setActive(library.getIsActive()); // Сохраняем статус активности
                    localLibraries.add(localLib);
                }
                localDb.libraryDao().insertLibraries(localLibraries);
                Log.d(TAG, "💾 Сохранено в кеш: " + localLibraries.size() + " библиотек");
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка сохранения библиотек в кеш", e);
            }
        }).start();
    }


    /**
     * Проверяет статус кеша
     */
    public void checkCacheStatus(OnCacheStatusListener listener) {
        new Thread(() -> {
            try {
                int libraryCount = localDb.libraryDao().getAllLibraries().size();
                int wordCount = localDb.wordDao().getAllWords().size();
                int activeLibraryCount = localDb.libraryDao().getActiveLibraries().size();
                int wordsFromActive = localDb.wordDao().getWordsFromActiveLibraries().size();

                Log.d(TAG, "📊 Статус кеша:");
                Log.d(TAG, "   Библиотеки: " + libraryCount);
                Log.d(TAG, "   Слова: " + wordCount);
                Log.d(TAG, "   Активные библиотеки: " + activeLibraryCount);
                Log.d(TAG, "   Слова из активных: " + wordsFromActive);

                if (listener != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        listener.onStatusChecked(libraryCount, wordCount, activeLibraryCount, wordsFromActive);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка проверки кеша", e);
            }
        }).start();
    }

    public interface OnCacheStatusListener {
        void onStatusChecked(int libraryCount, int wordCount, int activeLibraryCount, int wordsFromActiveLibraries);
    }

    /**
     * Конвертирует LocalWordItem в WordItem
     */
    private List<WordItem> convertToWordItems(List<LocalWordItem> localWords) {
        List<WordItem> words = new ArrayList<>();
        for (LocalWordItem localWord : localWords) {
            WordItem word = new WordItem();
            word.setWordId(localWord.getWordId());
            word.setWord(localWord.getWord());
            word.setTranslation(localWord.getTranslation());
            word.setNote(localWord.getNote());
            word.setIsFavorite(localWord.isFavorite());

            // ДОБАВЬ ЭТИ СТРОКИ:
            word.setReviewStage(localWord.getReviewStage());
            word.setNextReviewDate(localWord.getNextReviewDate());
            word.setConsecutiveShows(localWord.getConsecutiveShows());
            try {
                word.setDifficulty(Integer.parseInt(localWord.getDifficulty()));
            } catch (NumberFormatException e) {
                word.setDifficulty(1);
            }

            word.setReviewCount(localWord.getReviewCount());
            word.setCorrectAnswers(localWord.getCorrectAnswers());
            word.setIsCustomWord(localWord.isCustomWord());
            word.setLibraryId(localWord.getLibraryId());
            word.setUserId(localWord.getUserId());
            word.setCreatedAt(localWord.getCreatedAt());
            word.setLastReviewed(localWord.getLastReviewed());
            words.add(word);
        }
        return words;
    }

    public void getWordsFromActiveLibrariesFirebaseOld(OnWordsLoadedListener listener) {
        Log.d(TAG, "🔥 Загрузка слов из активных библиотек (FIREBASE)");

        getUserActiveLibraries(new OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> activeLibraries) {
                Log.d(TAG, "📚 Активных библиотек для загрузки слов: " + activeLibraries.size());

                if (activeLibraries.isEmpty()) {
                    listener.onWordsLoaded(new ArrayList<>());
                    return;
                }

                List<WordItem> allWords = new ArrayList<>();
                List<Task<QuerySnapshot>> tasks = new ArrayList<>();

                for (WordLibrary library : activeLibraries) {
                    boolean isCustom = library.getCreatedBy() != null &&
                            !library.getCreatedBy().equals("system");

                    Task<QuerySnapshot> task = getWordsFromSingleLibrary(library.getLibraryId(), isCustom);
                    tasks.add(task);
                }

                Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                    for (Object result : results) {
                        if (result instanceof QuerySnapshot) {
                            QuerySnapshot snapshot = (QuerySnapshot) result;
                            for (QueryDocumentSnapshot document : snapshot) {
                                WordItem word = document.toObject(WordItem.class);
                                word.setWordId(document.getId());
                                word.setLibraryId(document.getReference().getParent().getParent().getId());

                                // Загружаем поля системы повторений
                                loadRepetitionFields(word, document);
                                allWords.add(word);
                            }
                        }
                    }

                    Log.d(TAG, "✅ Загружено слов из Firebase: " + allWords.size());
                    listener.onWordsLoaded(allWords);

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка загрузки слов из Firebase", e);
                    listener.onError(e);
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Ошибка загрузки активных библиотек", e);
                listener.onError(e);
            }
        });
    }
    /**
     * Получить ВСЕ активные слова пользователя (из библиотек + кастомные)
     */

    /**
     * Загружает слова из активных библиотек напрямую из Firebase
     */
    public void getUserActiveLibrariesForLanguage(String language, OnLibrariesLoadedListener listener) {
        Log.d(TAG, "🔄 Загрузка активных библиотек для языка: " + language);

        db.collection("users")
                .document(userId)
                .collection("active_libraries")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(task -> {
                    List<String> activeLibraryIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task) {
                        String libraryId = document.getString("libraryId");
                        if (libraryId != null) activeLibraryIds.add(libraryId);
                    }

                    if (activeLibraryIds.isEmpty()) {
                        // Если в сети ничего нет, сбрасываем активность в Room для этого языка
                        updateRoomActiveStatus(new ArrayList<>(), language);
                        listener.onLibrariesLoaded(new ArrayList<>());
                        return;
                    }

                    loadLibrariesInfo(activeLibraryIds, new OnLibrariesLoadedListener() {
                        @Override
                        public void onLibrariesLoaded(List<WordLibrary> allLibraries) {
                            List<WordLibrary> filteredLibraries = new ArrayList<>();
                            for (WordLibrary library : allLibraries) {
                                if (language.equals(library.getLanguageFrom())) {
                                    library.setActive(true);
                                    filteredLibraries.add(library);
                                }
                            }

                            // ВАЖНО: Синхронизируем локальную базу
                            // Помечаем эти библиотеки в Room как активные
                            updateRoomActiveStatus(activeLibraryIds, language);

                            listener.onLibrariesLoaded(filteredLibraries);
                        }

                        @Override
                        public void onError(Exception e) { listener.onError(e); }
                    });
                })
                .addOnFailureListener(e -> {
                    // ОФЛАЙН: Если нет сети, берем данные ТОЛЬКО из Room
                    loadActiveFromRoomAsync(language, listener);
                });
    }

    // Метод для обновления статусов в Room
    private void updateRoomActiveStatus(List<String> activeIds, String lang) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // 1. Сначала сбрасываем isActive = false для всех библиотек этого языка
            localDb.libraryDao().deactivateAllForLanguage(lang, false);

            // 2. Теперь для пришедших ID ставим isActive = true
            for (String id : activeIds) {
                localDb.libraryDao().updateLibraryActiveStatus(id, true);
            }
            Log.d(TAG, "✅ Статусы активности синхронизированы с Room");
        });
    }

    // Метод для загрузки из Room, когда нет интернета
    private void loadActiveFromRoomAsync(String lang, OnLibrariesLoadedListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<LocalWordLibrary> locals = localDb.libraryDao().getActiveLibrariesByLanguage(lang);
            List<WordLibrary> webs = convertToWeb(locals);

            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                listener.onLibrariesLoaded(webs);
            });
        });
    }

    /**
     *
     *
     *
     * Загружает слова из активных библиотек для указанного языка
     */
    public void getWordsFromActiveLibrariesFirebase(String language, OnWordsLoadedListener listener) {
        Log.d(TAG, "🔥 Загрузка слов из активных библиотек для языка: " + language);

        getUserActiveLibraries(new OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> activeLibraries) {
                Log.d(TAG, "📚 Всего активных библиотек: " + activeLibraries.size());

                // ФИЛЬТРУЕМ библиотеки по языку
                List<WordLibrary> filteredLibraries = new ArrayList<>();
                for (WordLibrary library : activeLibraries) {
                    String libraryLanguage = library.getLanguageFrom();
                    if (libraryLanguage != null && libraryLanguage.equals(language)) {
                        filteredLibraries.add(library);
                        Log.d(TAG, "✅ Подходит для обучения: " + library.getName() +
                                " (язык: " + libraryLanguage + ")");
                    } else {
                        Log.d(TAG, "❌ Не подходит для обучения: " + library.getName() +
                                " (ожидали: " + language + ", получили: " + libraryLanguage + ")");
                    }
                }

                Log.d(TAG, "📚 Для обучения на языке " + language + ": " +
                        filteredLibraries.size() + " библиотек");

                if (filteredLibraries.isEmpty()) {
                    listener.onWordsLoaded(new ArrayList<>());
                    return;
                }

                List<WordItem> allWords = new ArrayList<>();
                List<Task<QuerySnapshot>> tasks = new ArrayList<>();

                for (WordLibrary library : filteredLibraries) {
                    boolean isCustom = library.getCreatedBy() != null &&
                            !library.getCreatedBy().equals("system");

                    Task<QuerySnapshot> task = getWordsFromSingleLibrary(library.getLibraryId(), isCustom);
                    tasks.add(task);
                }

                Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                    for (Object result : results) {
                        if (result instanceof QuerySnapshot) {
                            QuerySnapshot snapshot = (QuerySnapshot) result;
                            for (QueryDocumentSnapshot document : snapshot) {
                                WordItem word = document.toObject(WordItem.class);
                                word.setWordId(document.getId());
                                word.setLibraryId(document.getReference().getParent().getParent().getId());

                                // Загружаем поля системы повторений
                                loadRepetitionFields(word, document);
                                allWords.add(word);
                            }
                        }
                    }

                    Log.d(TAG, "✅ Загружено слов для языка " + language + ": " + allWords.size());
                    listener.onWordsLoaded(allWords);

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка загрузки слов для языка " + language, e);
                    listener.onError(e);
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Ошибка загрузки активных библиотек", e);
                listener.onError(e);
            }
        });
    }

    /**
     * Ручной способ получения слов из активных библиотек
     */
    private void manualGetWordsFromActiveLibraries(OnWordsLoadedListener listener) {
        new Thread(() -> {
            try {
                // Получаем активные библиотеки
                List<LocalWordLibrary> activeLibraries = localDb.libraryDao().getActiveLibraries();
                Log.d(TAG, "📚 Активных библиотек найдено: " + activeLibraries.size());

                List<WordItem> allWords = new ArrayList<>();

                // Для каждой активной библиотеки получаем слова
                for (LocalWordLibrary library : activeLibraries) {
                    List<LocalWordItem> libraryWords = localDb.wordDao().getWordsByLibrary(library.getLibraryId());
                    Log.d(TAG, "Библиотека " + library.getName() + ": " + libraryWords.size() + " слов");

                    for (LocalWordItem localWord : libraryWords) {
                        WordItem word = convertLocalWordToWordItem(localWord);
                        allWords.add(word);
                    }
                }

                Log.d(TAG, "✅ Ручной способ: всего слов " + allWords.size());

                if (listener != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        listener.onWordsLoaded(allWords);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка ручного способа: " + e.getMessage());
                if (listener != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        listener.onError(e);
                    });
                }
            }
        }).start();
    }

    private WordItem convertLocalWordToWordItem(LocalWordItem localWord) {
        WordItem word = new WordItem();
        word.setWordId(localWord.getWordId());
        word.setWord(localWord.getWord());
        word.setTranslation(localWord.getTranslation());
        word.setNote(localWord.getNote());
        word.setIsFavorite(localWord.isFavorite());

        try {
            word.setDifficulty(Integer.parseInt(localWord.getDifficulty()));
        } catch (NumberFormatException e) {
            word.setDifficulty(3);
        }

        word.setReviewStage(localWord.getReviewStage());
        word.setNextReviewDate(localWord.getNextReviewDate());
        word.setConsecutiveShows(localWord.getConsecutiveShows());
        word.setLibraryId(localWord.getLibraryId());

        return word;
    }

    public void getUserActiveWords(OnWordsLoadedListener listener) {
        List<WordItem> allWords = new ArrayList<>();

        // Сначала загружаем слова из активных библиотек
        getLibraryWords(new OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> libraryWords) {
                allWords.addAll(libraryWords);

                // Затем загружаем кастомные слова
                getCustomWords(new OnWordsLoadedListener() {
                    @Override
                    public void onWordsLoaded(List<WordItem> customWords) {
                        allWords.addAll(customWords);
                        listener.onWordsLoaded(allWords);
                    }

                    @Override
                    public void onError(Exception e) {
                        // Если ошибка с кастомными словами, возвращаем хотя бы библиотечные
                        listener.onWordsLoaded(allWords);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                // Если ошибка с библиотеками, пробуем загрузить только кастомные слова
                getCustomWords(listener);
            }
        });
    }

    /**
     * Получить слова из активных библиотек пользователя
     */
    private void getLibraryWords(OnWordsLoadedListener listener) {
        // Сначала получаем активные библиотеки пользователя
        getUserActiveLibraries(new OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> activeLibraries) {
                Log.d(TAG, "🔄 Загрузка слов из " + activeLibraries.size() + " активных библиотек");

                if (activeLibraries.isEmpty()) {
                    listener.onWordsLoaded(new ArrayList<>());
                    return;
                }

                List<WordItem> libraryWords = new ArrayList<>();
                List<Task<QuerySnapshot>> allTasks = new ArrayList<>();

                // Загружаем слова только из активных библиотек
                for (WordLibrary library : activeLibraries) {
                    if (!library.getIsActive()) {
                        Log.d(TAG, "❌ Пропускаем неактивную библиотеку: " + library.getName());
                        continue;
                    }

                    boolean isCustom = library.getCreatedBy() != null && !library.getCreatedBy().equals("system");
                    Task<QuerySnapshot> task = getWordsFromSingleLibrary(library.getLibraryId(), isCustom);
                    allTasks.add(task);
                    Log.d(TAG, "✅ Добавлена задача для библиотеки: " + library.getName());
                }

                if (allTasks.isEmpty()) {
                    Log.d(TAG, "ℹ️ Нет активных библиотек для загрузки слов");
                    listener.onWordsLoaded(new ArrayList<>());
                    return;
                }

                Tasks.whenAllSuccess(allTasks).addOnSuccessListener(results -> {
                    for (Object result : results) {
                        if (result instanceof QuerySnapshot) {
                            QuerySnapshot snapshot = (QuerySnapshot) result;
                            for (QueryDocumentSnapshot document : snapshot) {
                                //WordItem word = document.toObject(WordItem.class);

// НОВОЕ (ПРАВИЛЬНОЕ):
                                WordItem word = new WordItem(); // Создаем пустой объект
                                word.setWord(document.getString("word"));
                                word.setTranslation(document.getString("translation"));
                                word.setNote(document.getString("note"));
                                word.setWordId(document.getId());
                                word.setLibraryId(document.getReference().getParent().getParent().getId());
                                word.setCustomWord(false);


                                loadRepetitionFields(word, document);
                                libraryWords.add(word);
                            }
                        }
                    }

                    Log.d(TAG, "✅ Загружено " + libraryWords.size() + " слов из активных библиотек");
                    listener.onWordsLoaded(libraryWords);

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка загрузки слов из библиотек", e);
                    listener.onError(e);
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Ошибка загрузки активных библиотек", e);
                listener.onError(e);
            }
        });
    }

    // WordRepository.java
    private void loadUserProgressSynchronously(String userId, String wordId, LoadProgressCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ПРОВЕРКА userId
        if (userId == null || userId.equals("anonymous")) {
            Log.e(TAG, "❌ Нельзя загрузить прогресс: userId = " + userId);
            callback.onProgressNotFound();
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("word_progress") // ТА ЖЕ КОЛЛЕКЦИЯ!
                .document(wordId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            // Обработка данных
                            Map<String, Object> progressData = document.getData();
                            Log.d(TAG, "✅ Прогресс загружен для слова: " + wordId);
                            callback.onProgressLoaded(progressData);
                        } else {
                            Log.d(TAG, "ℹ️ Прогресс не найден для слова: " + wordId);
                            callback.onProgressNotFound();
                        }
                    } else {
                        Log.e(TAG, "❌ Ошибка загрузки прогресса: " + wordId, task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    // Интерфейс callback
    public interface LoadProgressCallback {
        void onProgressLoaded(Map<String, Object> progressData);

        void onProgressNotFound();

        void onError(Exception e);
    }

    /**
     * Получить кастомные слова пользователя
     */
    private void getCustomWords(OnWordsLoadedListener listener) {
        db.collection("users")
                .document(userId)
                .collection("custom_words")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<WordItem> customWords = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            WordItem word = document.toObject(WordItem.class);
                            word.setWordId(document.getId());
                            word.setCustomWord(true);
                            word.setUserId(userId);
                            // ДОБАВЬ ЗАГРУЗКУ ПОЛЕЙ СИСТЕМЫ ПОВТОРЕНИЙ
                            loadRepetitionFields(word, document);
                            customWords.add(word);
                        }

                        listener.onWordsLoaded(customWords);
                    } else {
                        listener.onError(task.getException());
                    }
                });
    }
    /**
     * Загружает поля системы повторений из документа Firebase

     private void loadRepetitionFields(WordItem word, QueryDocumentSnapshot document) {
     // difficulty
     if (document.contains("difficulty")) {
     Object difficulty = document.get("difficulty");
     if (difficulty instanceof Long) {
     word.setDifficulty(((Long) difficulty).intValue());
     } else if (difficulty instanceof Integer) {
     word.setDifficulty((Integer) difficulty);
     }
     } else {
     word.setDifficulty(3); // По умолчанию для новых слов
     }

     // reviewStage - УСТАНАВЛИВАЕМ ПО УМОЛЧАНИЮ 0
     if (document.contains("reviewStage")) {
     Object reviewStage = document.get("reviewStage");
     if (reviewStage instanceof Long) {
     word.setReviewStage(((Long) reviewStage).intValue());
     } else if (reviewStage instanceof Integer) {
     word.setReviewStage((Integer) reviewStage);
     }
     } else {
     word.setReviewStage(0); // По умолчанию для новых слов
     }

     // consecutiveShows - УСТАНАВЛИВАЕМ ПО УМОЛЧАНИЮ 0
     if (document.contains("consecutiveShows")) {
     Object consecutiveShows = document.get("consecutiveShows");
     if (consecutiveShows instanceof Long) {
     word.setConsecutiveShows(((Long) consecutiveShows).intValue());
     } else if (consecutiveShows instanceof Integer) {
     word.setConsecutiveShows((Integer) consecutiveShows);
     }
     } else {
     word.setConsecutiveShows(0); // По умолчанию для новых слов
     }

     // nextReviewDate - УСТАНАВЛИВАЕМ ПО УМОЛЧАНИЮ текущую дату
     if (document.contains("nextReviewDate")) {
     word.setNextReviewDate(document.getDate("nextReviewDate"));
     } else {
     word.setNextReviewDate(new Date()); // По умолчанию готово к изучению
     }

     Log.d(TAG, "Загружены поля повторений для " + word.getWord() +
     ": difficulty=" + word.getDifficulty() +
     ", reviewStage=" + word.getReviewStage() +
     ", consecutiveShows=" + word.getConsecutiveShows());
     }
     /**
     * Получить все доступные библиотеки (публичные + пользовательские)
     */

    /**
     * Применяет данные прогресса к объекту WordItem
     */
    private void applyProgressDataToWord(WordItem word, Map<String, Object> progressData) {
        if (progressData != null) {
            // difficulty
            if (progressData.containsKey("difficulty")) {
                Object difficulty = progressData.get("difficulty");
                if (difficulty instanceof Long) {
                    word.setDifficulty(((Long) difficulty).intValue());
                } else if (difficulty instanceof Integer) {
                    word.setDifficulty((Integer) difficulty);
                }
            }

            // reviewStage
            if (progressData.containsKey("reviewStage")) {
                Object reviewStage = progressData.get("reviewStage");
                if (reviewStage instanceof Long) {
                    word.setReviewStage(((Long) reviewStage).intValue());
                } else if (reviewStage instanceof Integer) {
                    word.setReviewStage((Integer) reviewStage);
                }
            }

            // consecutiveShows
            if (progressData.containsKey("consecutiveShows")) {
                Object consecutiveShows = progressData.get("consecutiveShows");
                if (consecutiveShows instanceof Long) {
                    word.setConsecutiveShows(((Long) consecutiveShows).intValue());
                } else if (consecutiveShows instanceof Integer) {
                    word.setConsecutiveShows((Integer) consecutiveShows);
                }
            }

            // nextReviewDate
            if (progressData.containsKey("nextReviewDate")) {
                Object nextReviewDate = progressData.get("nextReviewDate");
                if (nextReviewDate instanceof Date) {
                    word.setNextReviewDate((Date) nextReviewDate);
                }
            }

            // reviewCount
            if (progressData.containsKey("reviewCount")) {
                Object reviewCount = progressData.get("reviewCount");
                if (reviewCount instanceof Long) {
                    word.setReviewCount(((Long) reviewCount).intValue());
                } else if (reviewCount instanceof Integer) {
                    word.setReviewCount((Integer) reviewCount);
                }
            }

            // correctAnswers
            if (progressData.containsKey("correctAnswers")) {
                Object correctAnswers = progressData.get("correctAnswers");
                if (correctAnswers instanceof Long) {
                    word.setCorrectAnswers(((Long) correctAnswers).intValue());
                } else if (correctAnswers instanceof Integer) {
                    word.setCorrectAnswers((Integer) correctAnswers);
                }
            }

            Log.d(TAG, "✅ Загружен прогресс для " + word.getWord() +
                    ": stage=" + word.getReviewStage() +
                    ", difficulty=" + word.getDifficulty());
        }
    }
    /**
    private void loadRepetitionFields(WordItem word, QueryDocumentSnapshot document) {
        // Сначала загружаем базовые поля
        loadBasicRepetitionFields(word, document);

        // ДЛЯ ВСЕХ СЛОВ загружаем прогресс из word_progress АСИНХРОННО
        loadUserProgressSynchronously(userId, word.getWordId(), new LoadProgressCallback() {
            @Override
            public void onProgressLoaded(Map<String, Object> progressData) {
                // Применяем загруженные данные прогресса к слову
                applyProgressDataToWord(word, progressData);
            }

            @Override
            public void onProgressNotFound() {
                // Если прогресс не найден, устанавливаем значения по умолчанию
                initializeDefaultProgress(word);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Ошибка загрузки прогресса для: " + word.getWord(), e);
                initializeDefaultProgress(word);
            }
        });
    }
     */
    private void loadRepetitionFields(WordItem word, QueryDocumentSnapshot document) {
        Log.d(TAG, "=== ЗАГРУЗКА ПРОГРЕССА ДЛЯ: " + word.getWord() + " ===");

        loadBasicRepetitionFields(word, document);
        Log.d(TAG, "ДО прогресса - stage: " + word.getReviewStage() + ", shows: " + word.getConsecutiveShows());

        // Загружаем прогресс в фоновом потоке
        loadUserProgressInBackground(word);

        Log.d(TAG, "ПОСЛЕ прогресса - stage: " + word.getReviewStage() + ", shows: " + word.getConsecutiveShows());
    }
    /**
     * Упрощенная загрузка прогресса - синхронно но в фоновом потоке
     */
    private void loadUserProgressInBackground(WordItem word) {
        if (userId.equals("anonymous")) {
            return;
        }

        try {
            // Используем Executor для фонового потока
            Future<DocumentSnapshot> future = Executors.newSingleThreadExecutor().submit(() ->
                    Tasks.await(db.collection("users")
                            .document(userId)
                            .collection("word_progress")
                            .document(word.getWordId())
                            .get())
            );

            DocumentSnapshot progressDoc = future.get(5, TimeUnit.SECONDS); // таймаут 5 секунд

            if (progressDoc != null && progressDoc.exists()) {
                Log.d(TAG, "✅ Прогресс загружен для: " + word.getWord());

                if (progressDoc.contains("difficulty")) {
                    Long difficulty = progressDoc.getLong("difficulty");
                    if (difficulty != null) word.setDifficulty(difficulty.intValue());
                }
                if (progressDoc.contains("reviewStage")) {
                    Long reviewStage = progressDoc.getLong("reviewStage");
                    if (reviewStage != null) word.setReviewStage(reviewStage.intValue());
                }
                if (progressDoc.contains("consecutiveShows")) {
                    Long shows = progressDoc.getLong("consecutiveShows");
                    if (shows != null) word.setConsecutiveShows(shows.intValue());
                }
                if (progressDoc.contains("nextReviewDate")) {
                    word.setNextReviewDate(progressDoc.getDate("nextReviewDate"));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Ошибка загрузки прогресса: " + word.getWord(), e);
        }
    }
    /**
     * СИНХРОННАЯ загрузка прогресса

    private void loadUserProgressSync(WordItem word) {
        if (userId.equals("anonymous")) {
            initializeDefaultProgress(word);
            return;
        }

        try {
            // СИНХРОННАЯ загрузка с Tasks.await
            DocumentSnapshot progressDoc = Tasks.await(
                    db.collection("users")
                            .document(userId)
                            .collection("word_progress")
                            .document(word.getWordId())
                            .get()
            );

            if (progressDoc != null && progressDoc.exists()) {
                // Загружаем ВСЕ поля прогресса
                if (progressDoc.contains("difficulty")) {
                    Long difficulty = progressDoc.getLong("difficulty");
                    if (difficulty != null) word.setDifficulty(difficulty.intValue());
                }
                if (progressDoc.contains("reviewStage")) {
                    Long reviewStage = progressDoc.getLong("reviewStage");
                    if (reviewStage != null) word.setReviewStage(reviewStage.intValue());
                }
                if (progressDoc.contains("consecutiveShows")) {
                    Long consecutiveShows = progressDoc.getLong("consecutiveShows");
                    if (consecutiveShows != null) word.setConsecutiveShows(consecutiveShows.intValue());
                }
                if (progressDoc.contains("nextReviewDate")) {
                    word.setNextReviewDate(progressDoc.getDate("nextReviewDate"));
                }

                Log.d(TAG, "✅ СИНХРОННО загружен прогресс для: " + word.getWord() +
                        ", stage=" + word.getReviewStage());
            } else {
                initializeDefaultProgress(word);
                Log.d(TAG, "ℹ️ Прогресс не найден, установлены значения по умолчанию: " + word.getWord());
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Ошибка синхронной загрузки прогресса: " + word.getWord(), e);
            initializeDefaultProgress(word);
        }
    }

     */
    /**
     * АСИНХРОННАЯ загрузка прогресса пользователя
     */
    private void loadUserProgressAsync(WordItem word, OnProgressLoadedListener listener) {
        if (userId.equals("anonymous")) {
            Log.d(TAG, "❌ Анонимный пользователь - пропускаем загрузку прогресса");
            listener.onProgressLoaded(false);
            return;
        }

        Log.d(TAG, "🔄 Асинхронная загрузка прогресса для: " + word.getWord());

        db.collection("users")
                .document(userId)
                .collection("word_progress")
                .document(word.getWordId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot progressDoc = task.getResult();
                        Log.d(TAG, "✅ Документ прогресса найден для: " + word.getWord());

                        // ЗАГРУЖАЕМ ВСЕ ПОЛЯ ПРОГРЕССА
                        if (progressDoc.contains("difficulty")) {
                            Long difficulty = progressDoc.getLong("difficulty");
                            if (difficulty != null) {
                                word.setDifficulty(difficulty.intValue());
                                Log.d(TAG, "✅ Загружена difficulty: " + difficulty);
                            }
                        }

                        if (progressDoc.contains("reviewStage")) {
                            Long reviewStage = progressDoc.getLong("reviewStage");
                            if (reviewStage != null) {
                                word.setReviewStage(reviewStage.intValue());
                                Log.d(TAG, "✅ Загружен reviewStage: " + reviewStage);
                            }
                        }

                        if (progressDoc.contains("consecutiveShows")) {
                            Long consecutiveShows = progressDoc.getLong("consecutiveShows");
                            if (consecutiveShows != null) {
                                word.setConsecutiveShows(consecutiveShows.intValue());
                                Log.d(TAG, "✅ Загружены consecutiveShows: " + consecutiveShows);
                            }
                        }

                        if (progressDoc.contains("nextReviewDate")) {
                            Date nextReview = progressDoc.getDate("nextReviewDate");
                            word.setNextReviewDate(nextReview);
                            Log.d(TAG, "✅ Загружен nextReviewDate: " + nextReview);
                        }

                        listener.onProgressLoaded(true);
                    } else {
                        Log.d(TAG, "ℹ️ Прогресс не найден для: " + word.getWord());
                        initializeDefaultProgress(word);
                        listener.onProgressLoaded(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка загрузки прогресса для: " + word.getWord(), e);
                    initializeDefaultProgress(word);
                    listener.onProgressLoaded(false);
                });
    }

    // Интерфейс для колбэка
    private interface OnProgressLoadedListener {
        void onProgressLoaded(boolean success);
    }
    /**
     * СИНХРОННАЯ загрузка прогресса пользователя

     private void loadUserProgressSynchronously(WordItem word) {
     try {
     DocumentSnapshot progressDoc = Tasks.await(
     db.collection("users")
     .document(userId)
     .collection("word_progress")
     .document(word.getWordId())
     .get()
     );

     if (progressDoc != null && progressDoc.exists()) {
     // Загружаем ВСЕ поля прогресса
     word.setDifficulty(progressDoc.getLong("difficulty").intValue());
     word.setReviewStage(progressDoc.getLong("reviewStage").intValue());
     word.setConsecutiveShows(progressDoc.getLong("consecutiveShows").intValue());
     word.setNextReviewDate(progressDoc.getDate("nextReviewDate"));
     word.setReviewCount(progressDoc.getLong("reviewCount").intValue());
     word.setCorrectAnswers(progressDoc.getLong("correctAnswers").intValue());

     Log.d(TAG, "✅ Загружен прогресс для: " + word.getWord());
     } else {
     // Если прогресса нет - инициализируем дефолтными значениями
     initializeDefaultProgress(word);
     }
     } catch (Exception e) {
     Log.e(TAG, "❌ Ошибка загрузки прогресса: " + word.getWord(), e);
     initializeDefaultProgress(word);
     }
     }
     */
    /**
     * Обновляет основной документ кастомного слова
     */
    private void updateCustomWordDocument(WordItem word) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isFavorite", word.isFavorite());
        updates.put("difficulty", word.getDifficulty());
        updates.put("reviewStage", word.getReviewStage());
        updates.put("nextReviewDate", word.getNextReviewDate());
        updates.put("consecutiveShows", word.getConsecutiveShows());
        updates.put("reviewCount", word.getReviewCount());
        updates.put("correctAnswers", word.getCorrectAnswers());
        updates.put("lastReviewed", new Date());

        if (word.getLibraryId() != null && !word.getLibraryId().isEmpty()) {
            // Слово из пользовательской библиотеки
            db.collection("users")
                    .document(userId)
                    .collection("custom_libraries")
                    .document(word.getLibraryId())
                    .collection("words")
                    .document(word.getWordId())
                    .update(updates);
        } else {
            // Обычное кастомное слово
            db.collection("users")
                    .document(userId)
                    .collection("custom_words")
                    .document(word.getWordId())
                    .update(updates);
        }

        Log.d(TAG, "✅ Основной документ кастомного слова обновлен: " + word.getWord());
    }

    private void initializeDefaultProgress(WordItem word) {
        word.setDifficulty(3);
        word.setReviewStage(0);
        word.setConsecutiveShows(0);
        word.setNextReviewDate(new Date());
        word.setReviewCount(0);
        word.setCorrectAnswers(0);
    }

    private void loadUserProgressFromFirebase(WordItem word) {
        db.collection("users")
                .document(userId)
                .collection("word_progress")
                .document(word.getWordId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot progressDoc = task.getResult();

                        // Перезаписываем поля прогрессом пользователя
                        if (progressDoc.contains("difficulty")) {
                            Object difficulty = progressDoc.get("difficulty");
                            if (difficulty instanceof Long) {
                                word.setDifficulty(((Long) difficulty).intValue());
                            } else if (difficulty instanceof Integer) {
                                word.setDifficulty((Integer) difficulty);
                            }
                        }

                        if (progressDoc.contains("reviewStage")) {
                            Object reviewStage = progressDoc.get("reviewStage");
                            if (reviewStage instanceof Long) {
                                word.setReviewStage(((Long) reviewStage).intValue());
                            } else if (reviewStage instanceof Integer) {
                                word.setReviewStage((Integer) reviewStage);
                            }
                        }

                        if (progressDoc.contains("consecutiveShows")) {
                            Object consecutiveShows = progressDoc.get("consecutiveShows");
                            if (consecutiveShows instanceof Long) {
                                word.setConsecutiveShows(((Long) consecutiveShows).intValue());
                            } else if (consecutiveShows instanceof Integer) {
                                word.setConsecutiveShows((Integer) consecutiveShows);
                            }
                        }

                        if (progressDoc.contains("nextReviewDate")) {
                            word.setNextReviewDate(progressDoc.getDate("nextReviewDate"));
                        }

                        Log.d(TAG, "✅ Загружен прогресс для " + word.getWord() +
                                ": stage=" + word.getReviewStage() +
                                ", shows=" + word.getConsecutiveShows());
                    } else {
                        Log.d(TAG, "ℹ️ Прогресс для " + word.getWord() + " не найден, используем базовые значения");
                    }
                });
    }

    private void loadBasicRepetitionFields(WordItem word, QueryDocumentSnapshot document) {
        // difficulty
        if (document.contains("difficulty")) {
            Object difficulty = document.get("difficulty");
            if (difficulty instanceof Long) {
                word.setDifficulty(((Long) difficulty).intValue());
            } else if (difficulty instanceof Integer) {
                word.setDifficulty((Integer) difficulty);
            }
        } else {
            word.setDifficulty(3);
        }
    }



    public void getAvailableLibraries(OnLibrariesLoadedListener listener) {
        db.collection("word_libraries")
                .whereEqualTo("isPublic", true)
                .get()
                .addOnSuccessListener(publicSnapshots -> {
                    List<WordLibrary> allWebLibraries = new ArrayList<>();

                    // 1. Собираем публичные
                    for (DocumentSnapshot doc : publicSnapshots) {
                        WordLibrary lib = doc.toObject(WordLibrary.class);
                        if (lib != null) {
                            lib.setLibraryId(doc.getId());
                            lib.setCreatedBy("system");
                            allWebLibraries.add(lib);
                        }
                    }

                    // 2. Теперь подгружаем пользовательские (Custom)
                    db.collection("users").document(userId).collection("custom_libraries")
                            .get()
                            .addOnSuccessListener(customSnapshots -> {
                                for (DocumentSnapshot doc : customSnapshots) {
                                    WordLibrary lib = doc.toObject(WordLibrary.class);
                                    if (lib != null) {
                                        lib.setLibraryId(doc.getId());
                                        allWebLibraries.add(lib);
                                    }
                                }

                                // 3. Сохраняем ВСЁ в Room массово
                                saveToRoomAsync(allWebLibraries);

                                // Возвращаем результат в UI
                                listener.onLibrariesLoaded(allWebLibraries);
                            });
                })
                .addOnFailureListener(e -> {
                    // Если интернета нет — берем из кеша Room
                    loadFromRoomAsync(listener);
                });
    }
    // Вспомогательный метод для сохранения
    private void saveToRoomAsync(List<WordLibrary> webList) {
        List<LocalWordLibrary> localList = new ArrayList<>();
        for (WordLibrary lib : webList) {
            localList.add(convertToLocal(lib));
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            localDb.libraryDao().insertAll(localList);
            Log.d("DB_SYNC", "Кеш Room обновлен: " + localList.size() + " библиотек");
        });
    }

    // Вспомогательный метод для загрузки из кеша
    private void loadFromRoomAsync(OnLibrariesLoadedListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<LocalWordLibrary> cached = localDb.libraryDao().getAllLibraries();
            List<WordLibrary> webList = convertToWeb(cached);

            // Возвращаемся в главный поток для UI
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                listener.onLibrariesLoaded(webList);
            });
        });
    }
    // Из сетевой модели в локальную (для сохранения в БД)
    private LocalWordLibrary convertToLocal(WordLibrary web) {
        LocalWordLibrary local = new LocalWordLibrary();
        local.setLibraryId(web.getLibraryId());

        // Теперь это передача Map в Map — всё корректно
        local.setName(web.getName());
        local.setDescription(web.getDescription());
        local.setSubcategory(web.getSubcategory()); // Не забываем про новое поле

        local.setCategory(web.getCategory());
        local.setLanguageTo(web.getLanguageTo());
        local.setWordCount(web.getWordCount());
        local.setActive(web.isActive());
        return local;
    }

    // Из локальной модели в сетевую (для отображения в UI)
    private List<WordLibrary> convertToWeb(List<LocalWordLibrary> locals) {
        List<WordLibrary> webs = new ArrayList<>();
        for (LocalWordLibrary local : locals) {
            WordLibrary web = new WordLibrary();
            web.setLibraryId(local.getLibraryId());

            // Перекладываем Map
            web.setName(local.getName());
            web.setDescription(local.getDescription());
            web.setSubcategory(local.getSubcategory());

            web.setCategory(local.getCategory());
            web.setLanguageTo(local.getLanguageTo());
            web.setWordCount(local.getWordCount());
            web.setActive(local.isActive());
            webs.add(web);
        }
        return webs;
    }

    /**
     * Получить пользовательские библиотеки
     */
    public void getCustomLibraries(OnLibrariesLoadedListener listener) {
        db.collection("users")
                .document(userId)
                .collection("custom_libraries")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<WordLibrary> customLibraries = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            WordLibrary library = document.toObject(WordLibrary.class);
                            library.setLibraryId(document.getId());
                            library.setCreatedBy(userId);
                            customLibraries.add(library);
                        }

                        listener.onLibrariesLoaded(customLibraries);
                    } else {
                        listener.onError(task.getException());
                    }
                });
    }

    /**
     * Создать пользовательскую библиотеку
     */
    public void createCustomLibrary(String name, String description, String category, String language,
                                    OnLibraryCreatedListener listener) {
        // 1. Упаковываем введенные пользователем данные в Map
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put(language, name); // Название будет храниться под ключом выбранного языка

        Map<String, String> descMap = new HashMap<>();
        descMap.put(language, description);

        // 2. Формируем данные для Firestore (в папку пользователя)
        Map<String, Object> libraryData = new HashMap<>();
        libraryData.put("name", nameMap); // <-- Теперь это Map, а не String!
        libraryData.put("description", descMap); // <-- Тоже Map
        libraryData.put("category", category);
        libraryData.put("wordCount", 0);
        libraryData.put("languageFrom", language);
        libraryData.put("languageTo", "ru");
        libraryData.put("isPublic", false);
        libraryData.put("createdBy", userId);
        libraryData.put("createdAt", new Date());

        db.collection("users")
                .document(userId)
                .collection("custom_libraries")
                .add(libraryData)
                .addOnSuccessListener(documentReference -> {
                    // В коде приложения тоже обновляем объект
                    WordLibrary library = new WordLibrary();
                    library.setLibraryId(documentReference.getId());
                    library.setName(nameMap); // Передаем Map
                    library.setDescription(descMap); // Передаем Map
                    library.setCategory(category);
                    library.setWordCount(0);
                    library.setLanguageFrom(language);
                    library.setCreatedBy(userId);
                    listener.onLibraryCreated(library);
                })
                .addOnFailureListener(listener::onError);
    }


    public void toggleFavorite(WordItem word, OnSuccessListener successListener) {
        if (word == null || word.getWordId() == null) return;

        boolean newStatus = !word.isFavorite();
        word.setFavorite(newStatus); // Сразу меняем в объекте для UI

        // 1. Обновляем в Room (мгновенно)
        Executors.newSingleThreadExecutor().execute(() -> {
            localDb.wordDao().updateFavoriteStatus(word.getWordId(), newStatus);
        });

        // 2. Пытаемся обновить в Firebase, если есть libraryId
        if (word.getLibraryId() != null) {
            db.collection("users").document(userId)
                    .collection("custom_libraries").document(word.getLibraryId())
                    .collection("words").document(word.getWordId())
                    .update("isFavorite", newStatus)
                    .addOnSuccessListener(aVoid -> {
                        if (successListener != null) successListener.onSuccess();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Firebase favorite sync failed", e));
        }
    }

    /**
     * Добавить слово в пользовательскую библиотеку
     */
    public void addWordToCustomLibrary(String libraryId, WordItem word, OnWordAddedListener listener) {
        word.setUserId(userId);
        word.setCustomWord(true);
        word.setLibraryId(libraryId);
        word.setCreatedAt(new Date());

        // Готовим данные для Firebase (как у тебя и было)
        Map<String, Object> wordData = new HashMap<>();
        wordData.put("word", word.getWord());
        wordData.put("translation", word.getTranslation());
        wordData.put("note", word.getNote());
        wordData.put("userId", userId);
        wordData.put("libraryId", libraryId);
        wordData.put("isCustomWord", true);
        wordData.put("createdAt", word.getCreatedAt());

        db.collection("users")
                .document(userId)
                .collection("custom_libraries")
                .document(libraryId)
                .collection("words")
                .add(wordData)
                .addOnSuccessListener(documentReference -> {
                    String newId = documentReference.getId();
                    word.setWordId(newId);

                    // ВАЖНО: Синхронизируем с Room
                    saveWordToLocal(word, libraryId);

                    // Обновляем счетчик слов в Firebase (фоном)
                    updateLibraryWordCount(libraryId);

                    listener.onWordAdded(word);
                })
                .addOnFailureListener(listener::onError);
    }

    // Метод для локального сохранения
    private void saveWordToLocal(WordItem word, String libraryId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // 1. Конвертируем в локальную модель (нужно создать метод convertToLocalWord)
            LocalWordItem localWord = convertToLocalWord(word);

            // 2. Сохраняем слово в Room
            localDb.wordDao().insertWord(localWord);

            // 3. Мгновенно обновляем счетчик в локальной базе библиотек
            localDb.libraryDao().incrementWordCount(libraryId);

            Log.d(TAG, "💾 Слово сохранено в Room и счетчик обновлен");
        });
    }

    private LocalWordItem convertToLocalWord(WordItem web) {
        LocalWordItem local = new LocalWordItem();
        local.setWordId(web.getWordId());
        local.setWord(web.getWord());
        local.setTranslation(web.getTranslation());
        local.setNote(web.getNote());
        local.setLibraryId(web.getLibraryId());
        local.setFavorite(web.isFavorite());

        // ИСПРАВЛЕНИЕ: Передаем Date напрямую, а не getTime()
        local.setCreatedAt(web.getCreatedAt());

        return local;
    }
    /**
     * Обновить счетчик слов в библиотеке
     */
    private void updateLibraryWordCount(String libraryId) {
        db.collection("users")
                .document(userId)
                .collection("custom_libraries")
                .document(libraryId)
                .collection("words")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int wordCount = task.getResult().size();

                        // Обновляем счетчик в библиотеке
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("wordCount", wordCount);

                        db.collection("users")
                                .document(userId)
                                .collection("custom_libraries")
                                .document(libraryId)
                                .update(updates);
                    }
                });
    }

    /**
     * Получить слова из конкретной библиотеки
     */
    public void getWordsFromLibrary(String libraryId, boolean isCustomLibrary, OnWordsLoadedListener listener) {
        if (isCustomLibrary) {
            // Слова из пользовательской библиотеки
            db.collection("users")
                    .document(userId)
                    .collection("custom_libraries")
                    .document(libraryId)
                    .collection("words")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<WordItem> words = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                WordItem word = document.toObject(WordItem.class);
                                word.setWordId(document.getId());
                                word.setCustomWord(true);
                                word.setLibraryId(libraryId);
                                words.add(word);
                            }
                            listener.onWordsLoaded(words);
                        } else {
                            listener.onError(task.getException());
                        }
                    });
        } else {
            // Слова из публичной библиотеки
            db.collection("word_libraries")
                    .document(libraryId)
                    .collection("words")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<WordItem> words = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                WordItem word = document.toObject(WordItem.class);
                                word.setWordId(document.getId());
                                word.setCustomWord(false);
                                word.setLibraryId(libraryId);
                                words.add(word);
                            }
                            listener.onWordsLoaded(words);
                        } else {
                            listener.onError(task.getException());
                        }
                    });
        }
    }

    /**
     * Добавить кастомное слово (общее, не привязанное к библиотеке)
     */
    public void addCustomWord(WordItem word, OnWordAddedListener listener) {
        word.setUserId(userId);
        word.setCustomWord(true);
        word.setCreatedAt(new Date());

        Map<String, Object> wordData = new HashMap<>();
        wordData.put("word", word.getWord());
        wordData.put("translation", word.getTranslation());
        wordData.put("note", word.getNote());
        wordData.put("isFavorite", word.isFavorite());
        wordData.put("difficulty", word.getDifficulty());
        wordData.put("reviewCount", word.getReviewCount());
        wordData.put("correctAnswers", word.getCorrectAnswers());
        wordData.put("userId", userId);
        wordData.put("isCustomWord", true);
        wordData.put("createdAt", word.getCreatedAt());

        db.collection("users")
                .document(userId)
                .collection("custom_words")
                .add(wordData)
                .addOnSuccessListener(documentReference -> {
                    word.setWordId(documentReference.getId());
                    listener.onWordAdded(word);
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Обновить слово (например, при клике на звезду)
     */
    /**
     * Обновить слово (например, при клике на звезду)
     */
    public void updateWord(WordItem word) {
        if (word.getWordId() == null) return;

        Log.d(TAG, "🔄 Обновление слова: " + word.getWord() +
                ", этап: " + word.getReviewStage() +
                ", сложность: " + word.getDifficulty());

        // ДЛЯ ВСЕХ СЛОВ сохраняем прогресс в word_progress  ← ПЕРВЫЙ РАЗ
        updateUserWordProgress(word);

        // ТОЛЬКО для кастомных слов обновляем еще и основной документ ← ВТОРОЙ РАЗ для кастомных слов
        if (word.isCustomWord()) {
            updateCustomWordDocument(word);
        }
    }

    /**
     * Получить слова для текущей сессии обучения
     */
    public void getLearningSessionWords(OnWordsLoadedListener listener) {
        getUserActiveWords(listener);
    }

    /**
     * Пометить слово как выученное (убрать из текущей сессии)
     */
    public void markWordAsLearned(String wordId, OnSuccessListener success, OnErrorListener error) {
        success.onSuccess();
    }

    /**
     * Пометить слово для повторения (вернуть в колоду позже)
     */
    public void markWordForReview(String wordId, OnSuccessListener success, OnErrorListener error) {
        success.onSuccess();
    }

    /**
     * Получить прогресс пользователя по слову
     */
    public void getUserWordProgress(String wordId, OnWordsLoadedListener listener) {
        db.collection("users")
                .document(userId)
                .collection("word_progress")
                .document(wordId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        listener.onWordsLoaded(new ArrayList<>());
                    } else {
                        listener.onWordsLoaded(new ArrayList<>());
                    }
                });
    }


    /**
     * Удаляет слово из пользовательской библиотеки
     */
    public void deleteWordFromLibrary(String libraryId, String wordId, OnSuccessListener success, OnErrorListener error) {
        Log.d(TAG, "Удаление слова из библиотеки: " + libraryId + ", слово: " + wordId);

        db.collection("users")
                .document(userId)
                .collection("custom_libraries")
                .document(libraryId)
                .collection("words")
                .document(wordId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Обновляем счетчик слов в библиотеке
                    updateLibraryWordCount(libraryId);
                    success.onSuccess();
                })
                .addOnFailureListener(error::onError);
    }

    /**
     * Удаляет кастомное слово (не из библиотеки)
     */
    public void deleteCustomWord(String wordId, OnSuccessListener success, OnErrorListener error) {
        Log.d(TAG, "Удаление кастомного слова: " + wordId);

        db.collection("users")
                .document(userId)
                .collection("custom_words")
                .document(wordId)
                .delete()
                .addOnSuccessListener(aVoid -> success.onSuccess())
                .addOnFailureListener(error::onError);
    }

    /**
     * Удаляет пользовательскую библиотеку
     */
    public void deleteCustomLibrary(String libraryId, OnSuccessListener success, OnErrorListener error) {
        Log.d(TAG, "Удаление пользовательской библиотеки: " + libraryId);

        // Сначала удаляем все слова из библиотеки
        deleteAllWordsFromLibrary(libraryId,
                () -> {
                    // Затем удаляем саму библиотеку
                    db.collection("users")
                            .document(userId)
                            .collection("custom_libraries")
                            .document(libraryId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Также удаляем из активных библиотек
                                deactivateLibrary(libraryId, success, error);
                            })
                            .addOnFailureListener(error::onError);
                },
                error::onError
        );
    }

    /**
     * Удаляет все слова из библиотеки
     */
    private void deleteAllWordsFromLibrary(String libraryId, OnSuccessListener success, OnErrorListener error) {
        db.collection("users")
                .document(userId)
                .collection("custom_libraries")
                .document(libraryId)
                .collection("words")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Task<Void>> deleteTasks = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            deleteTasks.add(document.getReference().delete());
                        }

                        // Ждем завершения всех операций удаления
                        Tasks.whenAll(deleteTasks)
                                .addOnSuccessListener(aVoid -> success.onSuccess())
                                .addOnFailureListener(error::onError);
                    } else {
                        error.onError(task.getException());
                    }
                });
    }

    /**
     * Загружает информацию о библиотеках по их ID
     */
    private void loadLibrariesInfo(List<String> libraryIds, OnLibrariesLoadedListener listener) {
        if (libraryIds.isEmpty()) {
            listener.onLibrariesLoaded(new ArrayList<>());
            return;
        }

        List<WordLibrary> activeLibraries = new ArrayList<>();
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

        for (String libraryId : libraryIds) {
            // Пробуем загрузить из публичных библиотек
            Task<DocumentSnapshot> publicTask = db.collection("word_libraries")
                    .document(libraryId)
                    .get();

            tasks.add(publicTask);
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(combinedTask -> {
            for (Task<DocumentSnapshot> task : tasks) {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    WordLibrary library = task.getResult().toObject(WordLibrary.class);
                    if (library != null) {
                        library.setLibraryId(task.getResult().getId());
                        library.setCreatedBy("system");
                        activeLibraries.add(library);
                    }
                }
            }

            // Теперь загружаем пользовательские библиотеки
            loadCustomLibrariesInfo(libraryIds, activeLibraries, listener);
        });
    }

    /**
     * Загружает информацию о пользовательских библиотеках
     */
    private void loadCustomLibrariesInfo(List<String> libraryIds, List<WordLibrary> activeLibraries, OnLibrariesLoadedListener listener) {
        List<Task<DocumentSnapshot>> customTasks = new ArrayList<>();

        for (String libraryId : libraryIds) {
            Task<DocumentSnapshot> customTask = db.collection("users")
                    .document(userId)
                    .collection("custom_libraries")
                    .document(libraryId)
                    .get();
            customTasks.add(customTask);
        }

        Tasks.whenAllComplete(customTasks).addOnCompleteListener(combinedTask -> {
            for (Task<DocumentSnapshot> task : customTasks) {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    WordLibrary library = task.getResult().toObject(WordLibrary.class);
                    if (library != null) {
                        library.setLibraryId(task.getResult().getId());
                        library.setCreatedBy(userId);
                        activeLibraries.add(library);
                    }
                }
            }
            listener.onLibrariesLoaded(activeLibraries);
        });
    }

    /**
     * Получить слова ТОЛЬКО из активных библиотек пользователя
     */
    /**
     * Получить слова ТОЛЬКО из активных библиотек пользователя
     */

    /**
     * Активировать библиотеку для пользователя
     */
    /**
     * Активировать библиотеку для пользователя - УПРОЩЕННАЯ ВЕРСИЯ
     */
    public void activateLibrary(String libraryId, OnSuccessListener success, OnErrorListener error) {
        Log.d(TAG, "🔗 Активация библиотеки: " + libraryId);

        Map<String, Object> data = new HashMap<>();
        data.put("active", true);
        data.put("activatedAt", new Date());
        data.put("libraryId", libraryId);
        data.put("userId", userId);

        db.collection("users")
                .document(userId)
                .collection("active_libraries")
                .document(libraryId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Библиотека активирована в Firebase: " + libraryId);

                    // ВАЖНО: Синхронизируем с Room
                    updateLibraryLocalStatus(libraryId, true);

                    success.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка активации: " + libraryId, e);
                    error.onError(e);
                });
    }

    public void deactivateLibrary(String libraryId, OnSuccessListener success, OnErrorListener error) {
        db.collection("users")
                .document(userId)
                .collection("active_libraries")
                .document(libraryId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Библиотека деактивирована в Firebase");

                    // ВАЖНО: Синхронизируем с Room
                    updateLibraryLocalStatus(libraryId, false);

                    success.onSuccess();
                })
                .addOnFailureListener(error::onError);
    }

    // Вспомогательный метод для обновления Room
    private void updateLibraryLocalStatus(String libraryId, boolean isActive) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                localDb.libraryDao().updateLibraryActiveStatus(libraryId, isActive);
                Log.d(TAG, "💾 Room обновлен: библиотека " + libraryId + " теперь isActive=" + isActive);
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка обновления Room", e);
            }
        });
    }

    /**
     * Деактивировать библиотеку для пользователя
     */


    /**
     * Загружает информацию о библиотеке по ID
     */
    private void loadLibraryInfo(String libraryId, OnLibrariesLoadedListener listener) {
        List<String> libraryIds = new ArrayList<>();
        libraryIds.add(libraryId);
        loadLibrariesInfo(libraryIds, listener);
    }


    /**
     * Полностью очищает локальный кеш
     */
    public void clearLocalCache(OnSuccessListener success, OnErrorListener error) {
        Log.d(TAG, "🧹 === НАЧАЛО ОЧИСТКИ ЛОКАЛЬНОГО КЕША ===");

        new Thread(() -> {
            try {
                Log.d(TAG, "🧹 Получаем текущее состояние БД...");

                // Получаем текущие данные ДО очистки
                int libCountBefore = localDb.libraryDao().getAllLibraries().size();
                int wordCountBefore = localDb.wordDao().getAllWords().size();
                int activeLibCountBefore = localDb.libraryDao().getActiveLibraries().size();

                Log.d(TAG, "🧹 Состояние ДО очистки:");
                Log.d(TAG, "   Библиотеки: " + libCountBefore);
                Log.d(TAG, "   Слова: " + wordCountBefore);
                Log.d(TAG, "   Активные библиотеки: " + activeLibCountBefore);

                // Очищаем таблицы
                Log.d(TAG, "🧹 Очищаем таблицу библиотек...");
                localDb.libraryDao().clearAllLibraries();

                Log.d(TAG, "🧹 Очищаем таблицу слов...");
                localDb.wordDao().clearAllWords();

                // Получаем данные ПОСЛЕ очистки
                int libCountAfter = localDb.libraryDao().getAllLibraries().size();
                int wordCountAfter = localDb.wordDao().getAllWords().size();
                int activeLibCountAfter = localDb.libraryDao().getActiveLibraries().size();

                Log.d(TAG, "✅ Состояние ПОСЛЕ очистки:");
                Log.d(TAG, "   Библиотеки: " + libCountAfter + " (было: " + libCountBefore + ")");
                Log.d(TAG, "   Слова: " + wordCountAfter + " (было: " + wordCountBefore + ")");
                Log.d(TAG, "   Активные библиотеки: " + activeLibCountAfter + " (было: " + activeLibCountBefore + ")");

                if (libCountAfter == 0 && wordCountAfter == 0) {
                    Log.d(TAG, "✅ Локальный кеш полностью очищен!");
                } else {
                    Log.w(TAG, "⚠️ Кеш очищен не полностью!");
                }

                // Вызываем колбэк в UI потоке
                if (success != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        Log.d(TAG, "✅ Колбэк успеха вызван");
                        success.onSuccess();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка очистки кеша", e);
                if (error != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        Log.d(TAG, "❌ Колбэк ошибки вызван");
                        error.onError(e);
                    });
                }
            }
        }).start();
    }

    /**
     * Получить активные библиотеки пользователя
     */
    // Упрощенная версия:
    public void getUserActiveLibraries(OnLibrariesLoadedListener listener) {
        Log.d(TAG, "🔄 Загрузка активных библиотек...");

        db.collection("users")
                .document(userId)
                .collection("active_libraries")
                .whereEqualTo("active", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<String> activeLibraryIds = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String libraryId = document.getString("libraryId");
                            if (libraryId != null && !libraryId.isEmpty()) {
                                activeLibraryIds.add(libraryId);
                                Log.d(TAG, "✅ Найдена активная библиотека: " + libraryId);
                            }
                        }

                        Log.d(TAG, "📚 Всего активных библиотек: " + activeLibraryIds.size());

                        if (activeLibraryIds.isEmpty()) {
                            listener.onLibrariesLoaded(new ArrayList<>());
                            return;
                        }

                        // Загружаем информацию о библиотеках
                        loadLibrariesInfo(activeLibraryIds, listener);
                    } else {
                        Log.e(TAG, "❌ Ошибка загрузки активных библиотек", task.getException());
                        listener.onError(task.getException());
                    }
                });
    }

    /**
     * Загружает полную информацию о библиотеках
     */
    private void loadFullLibrariesInfo(List<WordLibrary> libraries, OnLibrariesLoadedListener listener) {
        List<String> libraryIds = new ArrayList<>();
        for (WordLibrary library : libraries) {
            libraryIds.add(library.getLibraryId());
        }

        loadLibrariesInfo(libraryIds, new OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> fullLibraries) {
                // Объединяем информацию об активности с полной информацией
                for (WordLibrary fullLibrary : fullLibraries) {
                    for (WordLibrary activeLibrary : libraries) {
                        if (activeLibrary.getLibraryId().equals(fullLibrary.getLibraryId())) {
                            fullLibrary.setActive(true);
                            break;
                        }
                    }
                }
                listener.onLibrariesLoaded(fullLibraries);
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e);
            }
        });
    }

    /**
     * Получить слова из одной библиотеки
     */
    private Task<QuerySnapshot> getWordsFromSingleLibrary(String libraryId, boolean isCustom) {
        if (isCustom) {
            return db.collection("users")
                    .document(userId)
                    .collection("custom_libraries")
                    .document(libraryId)
                    .collection("words")
                    .get();
        } else {
            return db.collection("word_libraries")
                    .document(libraryId)
                    .collection("words")
                    .get();
        }
    }
    /**
     * Сохраняет прогресс изучения слова - СОЗДАЕТ АВТОМАТИЧЕСКИ
     */
    private void updateUserWordProgress(WordItem word) {
        // ПРОВЕРКА 1: userId
        Log.d(TAG, "=== ОТЛАДКА СОХРАНЕНИЯ ПРОГРЕССА ===");
        Log.d(TAG, "userId: " + userId);

        if (userId == null || userId.equals("anonymous")) {
            Log.e(TAG, "❌ ОШИБКА: userId = " + userId + " - прогресс не сохранится!");
            return;
        }

        // ПРОВЕРКА 2: wordId
        Log.d(TAG, "wordId: " + word.getWordId());
        if (word.getWordId() == null) {
            Log.e(TAG, "❌ ОШИБКА: wordId = null - прогресс не сохранится!");
            return;
        }

        // ПРОВЕРКА 3: Данные прогресса
        Log.d(TAG, "Данные прогресса:");
        Log.d(TAG, " - difficulty: " + word.getDifficulty());
        Log.d(TAG, " - reviewStage: " + word.getReviewStage());
        Log.d(TAG, " - consecutiveShows: " + word.getConsecutiveShows());

        Map<String, Object> progress = new HashMap<>();
        progress.put("difficulty", word.getDifficulty());
        progress.put("reviewStage", word.getReviewStage());
        progress.put("consecutiveShows", word.getConsecutiveShows());
        progress.put("nextReviewDate", word.getNextReviewDate());
        progress.put("reviewCount", word.getReviewCount());
        progress.put("correctAnswers", word.getCorrectAnswers());
        progress.put("libraryId", word.getLibraryId());
        progress.put("word", word.getWord());
        progress.put("translation", word.getTranslation());
        progress.put("lastReviewed", new Date());
        progress.put("updatedAt", new Date());
        progress.put("createdAt", new Date());

        Log.d(TAG, "💾 Попытка сохранения в: users/" + userId + "/word_progress/" + word.getWordId());

        db.collection("users")
                .document(userId)
                .collection("word_progress")
                .document(word.getWordId())
                .set(progress, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ УСПЕХ: Прогресс сохранен для: " + word.getWord());
                    Log.d(TAG, "✅ Коллекция word_progress должна быть создана!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ ОШИБКА сохранения прогресса: " + word.getWord(), e);
                    Log.e(TAG, "❌ Коллекция word_progress НЕ создана из-за ошибки!");
                });
    }

    /**
     * Гарантирует создание структуры word_progress
     */
    public void ensureWordProgressStructure(OnSuccessListener listener) {
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("_createdAt", new Date());
        initialData.put("_initialized", true);

        db.collection("users")
                .document(userId)
                .collection("word_progress")
                .document("_initialization")
                .set(initialData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Коллекция word_progress создана!");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка создания word_progress", e);
                });
    }



    // === МЕТОДЫ СТАТИСТИКИ ===

    /**
     * Получить статистику пользователя
     */
    public void getUserStats(OnStatsLoadedListener listener) {
        if (userId.equals("anonymous")) return;

        // 1. Сначала берем из Room (Фоновый поток)
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                UserStats localStats = localDb.statsDao().getStats(userId);

                // Если в базе что-то есть, сразу отправляем в UI
                if (localStats != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        listener.onStatsLoaded(localStats);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка чтения Room stats", e);
            }
        });

        // 2. Параллельно идем в Firebase
        db.collection("users").document(userId)
                .collection("stats").document("main")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserStats remoteStats = documentSnapshot.toObject(UserStats.class);
                        if (remoteStats != null) {
                            // Обновляем локальную базу свежими данными
                            Executors.newSingleThreadExecutor().execute(() -> {
                                localDb.statsDao().insertStats(remoteStats);
                            });
                            listener.onStatsLoaded(remoteStats);
                        }
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public void updateStatsAsync(StatUpdateListener updateListener) {
        if (userId.equals("anonymous")) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Получаем текущую статистику из Room
                UserStats stats = localDb.statsDao().getStats(userId);
                if (stats == null) {
                    stats = new UserStats(userId);
                }

                // Применяем изменения через интерфейс
                UserStats updatedStats = updateListener.onUpdate(stats);

                // 1. Сохраняем в Room
                localDb.statsDao().insertStats(updatedStats);

                // 2. Отправляем в Firebase
                db.collection("users").document(userId)
                        .collection("stats").document("main")
                        .set(updatedStats, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Статистика синхронизирована с облаком"))
                        .addOnFailureListener(e -> Log.e(TAG, "Ошибка синхронизации облака", e));

            } catch (Exception e) {
                Log.e(TAG, "Критическая ошибка обновления статистики", e);
            }
        });
    }
    /**
     * Вызывается когда новое слово добавлено в изучение
     */
    public void onWordAddedToLearning() {
        updateStatsAsync(stats -> {
            stats.setWordsInProgress(stats.getWordsInProgress() + 1);
            Log.d(TAG, "➕ Новое слово добавлено! В процессе: " + stats.getWordsInProgress());
            return stats;
        });
    }

    /**
     * Обновляет статистику асинхронно
     */
    /**
     * Обновляет статистику асинхронно
     */
    public void updateStats(UserStats stats) {
        if (userId == null || userId.equals("anonymous")) return;

        // 1. Сохраняем в Firebase
        db.collection("users").document(userId)
                .collection("stats").document("main")
                .set(stats, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Статистика сохранена в облако"));

        // 2. СИНХРОНИЗАЦИЯ: Сохраняем в Room
        // Мы передаем напрямую объект stats, потому что твой UserStatsDao
        // ожидает именно этот класс.
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Исправление: используем сразу stats, а не LocalUserStats
                localDb.statsDao().insertStats(stats);
                Log.d(TAG, "Статистика синхронизирована с Room");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка сохранения статистики в Room", e);
            }
        });
    }
    /**
     * Синхронизирует статистику с Firebase
     */
    private void syncStatsToFirebase(UserStats stats) {
        db.collection("userStats")
                .document(stats.getUserId())
                .set(stats)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Статистика синхронизирована с Firebase");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка синхронизации статистики", e);
                });
    }

    /**
     * Синхронизирует статистику из Firebase
     */
    private void syncStatsFromFirebase(String userId, OnStatsLoadedListener listener) {
        db.collection("userStats")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserStats firebaseStats = documentSnapshot.toObject(UserStats.class);
                        if (firebaseStats != null) {
                            saveStatsToLocal(firebaseStats);
                            if (listener != null) {
                                listener.onStatsLoaded(firebaseStats);
                            }
                        } else {
                            createDefaultStats(userId, listener);
                        }
                    } else {
                        createDefaultStats(userId, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка синхронизации из Firebase", e);
                    UserStats localStats = localDb.getOrCreateStats(userId);
                    if (listener != null) {
                        listener.onStatsLoaded(localStats);
                    }
                });
    }

    /**
     * Создает статистику по умолчанию
     */
    private void createDefaultStats(String userId, OnStatsLoadedListener listener) {
        UserStats defaultStats = new UserStats(userId);
        saveStatsToLocal(defaultStats);
        syncStatsToFirebase(defaultStats);
        if (listener != null) {
            listener.onStatsLoaded(defaultStats);
        }
    }

    /**
     * Сохраняет статистику в локальную БД
     */
    private void saveStatsToLocal(UserStats stats) {
        new Thread(() -> {
            localDb.statsDao().insertStats(stats);
        }).start();
    }

    /**
     * Проверяет нужно ли обновить streak (новый день)
     */
    private boolean needsDailyReset(UserStats stats) {
        if (stats.getLastSessionDate() == null) return false;

        Date today = new Date();
        long diff = today.getTime() - stats.getLastSessionDate().getTime();
        long daysDiff = diff / (24 * 60 * 60 * 1000);

        return daysDiff >= 1;
    }

    /**
     * Обновляет streak для нового дня
     */
    private void updateStreakForNewDay(UserStats stats, OnStatsLoadedListener listener) {
        new Thread(() -> {
            try {
                Date today = new Date();

                if (stats.getTodayProgress() > 0) {
                    stats.setStreakDays(stats.getStreakDays() + 1);
                    Log.d(TAG, "🔥 Streak увеличен: " + stats.getStreakDays() + " дней");
                } else {
                    stats.setStreakDays(0);
                    Log.d(TAG, "💔 Streak сброшен");
                }

                stats.setTodayProgress(0);
                stats.setLastSessionDate(today);
                stats.setLastUpdated(today);

                localDb.statsDao().insertStats(stats);
                syncStatsToFirebase(stats);

                if (listener != null) {
                    listener.onStatsLoaded(stats);
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка обновления streak", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        }).start();
    }

    /**
     * Проверяет устарели ли данные
     */
    private boolean isStatsOutdated(UserStats stats) {
        if (stats.getLastUpdated() == null) return true;

        long now = System.currentTimeMillis();
        long lastUpdate = stats.getLastUpdated().getTime();
        long oneHour = 60 * 60 * 1000;

        return (now - lastUpdate) > oneHour;
    }

    /**
     * Получает текущий userId
     */
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }
// === МЕТОДЫ СТАТИСТИКИ ===

    /**
     * Вызывается когда слово успешно изучено
     */
    public void onWordLearned(String wordId) {
        updateStatsAsync(stats -> {
            stats.setWordsLearned(stats.getWordsLearned() + 1);
            stats.setTodayProgress(stats.getTodayProgress() + 1);
            stats.setLastSessionDate(new Date());

            Log.d(TAG, "✅ Слово изучено! Выучено: " + stats.getWordsLearned() +
                    ", сегодня: " + stats.getTodayProgress());
            return stats;
        });
    }

// ← ВСТАВЬТЕ ЗДЕСЬ ШАГ 3
    /**
     * Полностью пересчитывает статистику на основе всех слов
     */
    /**
     * Полностью пересчитывает статистику на основе всех слов
     */
    public void recalculateAllStats(List<WordItem> allWords, OnSuccessListener listener) {
        new Thread(() -> {
            try {
                // Создаем final переменные для использования в лямбде
                final int[] counts = {0, 0}; // [wordsInProgress, wordsLearned]

                for (WordItem word : allWords) {
                    if (SimpleRepetitionSystem.isLearnedWord(word)) {
                        counts[1]++; // wordsLearned
                    } else if (SimpleRepetitionSystem.shouldShowInSession(word)) {
                        counts[0]++; // wordsInProgress
                    }
                }

                updateStatsAsync(stats -> {
                    stats.setWordsInProgress(counts[0]);
                    stats.setWordsLearned(counts[1]);
                    stats.setLastUpdated(new Date());
                    return stats;
                });

                if (listener != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        listener.onSuccess();
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка пересчета статистики", e);
            }
        }).start();
    }
    /**
     * Вызывается при простом повторении слова
     */
    public void onWordReviewed() {
        updateStatsAsync(stats -> {
            stats.setTodayProgress(stats.getTodayProgress() + 1);
            stats.setLastSessionDate(new Date());

            Log.d(TAG, "📖 Слово повторено! Сегодня: " + stats.getTodayProgress());
            return stats;
        });
    }

}