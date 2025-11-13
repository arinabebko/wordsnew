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
        this.userId = user != null ? user.getUid() : "anonymous";
        this.localDb = AppDatabase.getInstance(context);
    }


    // === ИНТЕРФЕЙСЫ ДЛЯ КОЛБЭКОВ ===

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
    public void syncWordsFromFirebase(OnWordsLoadedListener listener) {
        Log.d(TAG, "🔄 Синхронизация с Firebase...");

        getUserActiveLibraries(new OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> activeLibraries) {
                Log.d(TAG, "📚 Активных библиотек для синхронизации: " + activeLibraries.size());

                List<WordItem> allWords = new ArrayList<>();
                List<Task<QuerySnapshot>> allTasks = new ArrayList<>();

                // Загружаем слова ТОЛЬКО из активных библиотек
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

                    // Сохраняем в кеш
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







































    /**
     * Получить ВСЕ активные слова пользователя (из библиотек + кастомные)
     */


    public void getWordsFromActiveLibraries(OnWordsLoadedListener listener) {
        Log.d(TAG, "🔍 Получение слов из АКТИВНЫХ библиотек (ЛОКАЛЬНЫЙ СПОСОБ)");

        new Thread(() -> {
            try {
                // Способ 1: Прямой запрос к БД с JOIN (самый надежный)
                List<LocalWordItem> wordsFromActive = localDb.wordDao().getWordsFromActiveLibraries();
                Log.d(TAG, "✅ Найдено слов через JOIN: " + wordsFromActive.size());

                if (!wordsFromActive.isEmpty()) {
                    List<WordItem> words = convertToWordItems(wordsFromActive);
                    if (listener != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            listener.onWordsLoaded(words);
                        });
                    }
                    return;
                }

                // Способ 2: Если JOIN не сработал, делаем вручную
                Log.d(TAG, "🔄 JOIN не дал результатов, пробуем ручной способ...");
                manualGetWordsFromActiveLibraries(listener);

            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка локального получения слов: " + e.getMessage());
                // Пробуем ручной способ при ошибке
                manualGetWordsFromActiveLibraries(listener);
            }
        }).start();
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
                                WordItem word = document.toObject(WordItem.class);
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
     */
    private void loadRepetitionFields(WordItem word, QueryDocumentSnapshot document) {
        if (document.contains("difficulty")) {
            Object difficulty = document.get("difficulty");
            if (difficulty instanceof Long) {
                word.setDifficulty(((Long) difficulty).intValue());
            } else if (difficulty instanceof Integer) {
                word.setDifficulty((Integer) difficulty);
            }
        } else {
            word.setDifficulty(3); // Значение по умолчанию для новых слов
        }

        if (document.contains("reviewStage")) {
            Object reviewStage = document.get("reviewStage");
            if (reviewStage instanceof Long) {
                word.setReviewStage(((Long) reviewStage).intValue());
            } else if (reviewStage instanceof Integer) {
                word.setReviewStage((Integer) reviewStage);
            }
        }

        if (document.contains("consecutiveShows")) {
            Object consecutiveShows = document.get("consecutiveShows");
            if (consecutiveShows instanceof Long) {
                word.setConsecutiveShows(((Long) consecutiveShows).intValue());
            } else if (consecutiveShows instanceof Integer) {
                word.setConsecutiveShows((Integer) consecutiveShows);
            }
        }

        if (document.contains("nextReviewDate")) {
            word.setNextReviewDate(document.getDate("nextReviewDate"));
        }

        Log.d(TAG, "Загружены поля повторений для " + word.getWord() +
                ": difficulty=" + word.getDifficulty() +
                ", reviewStage=" + word.getReviewStage() +
                ", consecutiveShows=" + word.getConsecutiveShows());
    }
    /**
     * Получить все доступные библиотеки (публичные + пользовательские)
     */
    public void getAvailableLibraries(OnLibrariesLoadedListener listener) {
        List<WordLibrary> allLibraries = new ArrayList<>();

        // Загружаем публичные библиотеки
        db.collection("word_libraries")
                .whereEqualTo("isPublic", true)
                .get()
                .addOnCompleteListener(publicTask -> {
                    if (publicTask.isSuccessful() && publicTask.getResult() != null) {
                        for (QueryDocumentSnapshot document : publicTask.getResult()) {
                            WordLibrary library = document.toObject(WordLibrary.class);
                            library.setLibraryId(document.getId());
                            library.setCreatedBy("system");
                            allLibraries.add(library);
                        }

                        // Теперь загружаем пользовательские библиотеки
                        getCustomLibraries(new OnLibrariesLoadedListener() {
                            @Override
                            public void onLibrariesLoaded(List<WordLibrary> customLibraries) {
                                allLibraries.addAll(customLibraries);
                                listener.onLibrariesLoaded(allLibraries);
                            }

                            @Override
                            public void onError(Exception e) {
                                // Если ошибка с пользовательскими, возвращаем хотя бы публичные
                                listener.onLibrariesLoaded(allLibraries);
                            }
                        });

                    } else {
                        // Если ошибка с публичными, пробуем загрузить только пользовательские
                        getCustomLibraries(listener);
                    }
                });
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
    public void createCustomLibrary(String name, String description, String category,
                                    OnLibraryCreatedListener listener) {
        Map<String, Object> libraryData = new HashMap<>();
        libraryData.put("name", name);
        libraryData.put("description", description);
        libraryData.put("category", category);
        libraryData.put("wordCount", 0);
        libraryData.put("languageFrom", "en");
        libraryData.put("languageTo", "ru");
        libraryData.put("isPublic", false);
        libraryData.put("createdBy", userId);
        libraryData.put("createdAt", new Date());

        db.collection("users")
                .document(userId)
                .collection("custom_libraries")
                .add(libraryData)
                .addOnSuccessListener(documentReference -> {
                    WordLibrary library = new WordLibrary();
                    library.setLibraryId(documentReference.getId());
                    library.setName(name);
                    library.setDescription(description);
                    library.setCategory(category);
                    library.setWordCount(0);
                    library.setCreatedBy(userId);
                    listener.onLibraryCreated(library);
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Добавить слово в пользовательскую библиотеку
     */
    public void addWordToCustomLibrary(String libraryId, WordItem word, OnWordAddedListener listener) {
        word.setUserId(userId);
        word.setCustomWord(true);
        word.setLibraryId(libraryId);
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
        wordData.put("libraryId", libraryId);
        wordData.put("createdAt", word.getCreatedAt());

        // Добавляем слово в подколлекцию библиотеки
        db.collection("users")
                .document(userId)
                .collection("custom_libraries")
                .document(libraryId)
                .collection("words")
                .add(wordData)
                .addOnSuccessListener(documentReference -> {
                    word.setWordId(documentReference.getId());

                    // Обновляем счетчик слов в библиотеке
                    updateLibraryWordCount(libraryId);

                    listener.onWordAdded(word);
                })
                .addOnFailureListener(listener::onError);
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

        Map<String, Object> updates = new HashMap<>();
        updates.put("isFavorite", word.isFavorite());
        updates.put("lastReviewed", new Date());
        updates.put("reviewCount", word.getReviewCount() + 1);

        // ДОБАВЬ ЭТИ ПОЛЯ ДЛЯ СИСТЕМЫ ПОВТОРЕНИЙ:
        updates.put("difficulty", word.getDifficulty());
        updates.put("reviewStage", word.getReviewStage());
        updates.put("nextReviewDate", word.getNextReviewDate());
        updates.put("consecutiveShows", word.getConsecutiveShows());

        if (word.isCustomWord()) {
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
        } else {
            // Для слов из библиотек сохраняем прогресс отдельно
            updateUserWordProgress(word, updates);
        }

        Log.d(TAG, "Слово обновлено: " + word.getWord() +
                ", этап=" + word.getReviewStage() +
                ", показы=" + word.getConsecutiveShows());
    }

    /**
     * Сохранить прогресс изучения слова из библиотеки
     */
    private void updateUserWordProgress(WordItem word, Map<String, Object> updates) {
        db.collection("users")
                .document(userId)
                .collection("word_progress")
                .document(word.getWordId())
                .set(updates, SetOptions.merge());
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
    public void activateLibrary(String libraryId, OnSuccessListener success, OnErrorListener error) {
        Log.d(TAG, "🔗 Активация библиотеки: " + libraryId);

        // Сначала загружаем информацию о библиотеке
        loadLibraryInfo(libraryId, new OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> libraries) {
                if (libraries.isEmpty()) {
                    error.onError(new Exception("Библиотека не найдена"));
                    return;
                }

                WordLibrary library = libraries.get(0);
                library.setActive(true);

                // Сохраняем в активные библиотеки пользователя
                Map<String, Object> data = new HashMap<>();
                data.put("active", true);
                data.put("activatedAt", new Date());
                data.put("libraryInfo", library); // Сохраняем полную информацию
                data.put("libraryId", libraryId); // ← ДОБАВИТЬ ЭТУ СТРОКУ!

                db.collection("users")
                        .document(userId)
                        .collection("active_libraries")
                        .document(libraryId)
                        .set(data)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "✅ Библиотека активирована: " + libraryId);
                            success.onSuccess();
                        })
                        .addOnFailureListener(error::onError);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Ошибка загрузки информации о библиотеке: " + libraryId, e);
                error.onError(e);
            }
        });
    }
    /**
     * Деактивировать библиотеку для пользователя
     */
    public void deactivateLibrary(String libraryId, OnSuccessListener success, OnErrorListener error) {
        db.collection("users")
                .document(userId)
                .collection("active_libraries")
                .document(libraryId)
                .delete()
                .addOnSuccessListener(aVoid -> success.onSuccess())
                .addOnFailureListener(error::onError);
    }

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
    public void getUserActiveLibraries(OnLibrariesLoadedListener listener) {
        Log.d(TAG, "🔄 Загрузка активных библиотек для пользователя: " + userId);

        db.collection("users")
                .document(userId)
                .collection("active_libraries")
                .whereEqualTo("active", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<String> activeLibraryIds = new ArrayList<>();

                        Log.d(TAG, "📄 Найдено документов в active_libraries: " + task.getResult().size());

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // ПРОБУЕМ ОБА ВАРИАНТА:
                            String libraryId = document.getString("libraryId");

                            // Если libraryId нет в корне, пробуем взять из libraryInfo
                            if (libraryId == null || libraryId.isEmpty()) {
                                Map<String, Object> libraryInfo = (Map<String, Object>) document.get("libraryInfo");
                                if (libraryInfo != null) {
                                    libraryId = (String) libraryInfo.get("libraryId");
                                }
                            }

                            // Если все еще нет, используем ID документа как запасной вариант
                            if (libraryId == null || libraryId.isEmpty()) {
                                libraryId = document.getId();
                                Log.w(TAG, "⚠️ Используем ID документа как libraryId: " + libraryId);
                            }

                            if (libraryId != null && !libraryId.isEmpty()) {
                                activeLibraryIds.add(libraryId);
                                Log.d(TAG, "✅ Активная библиотека найдена: " + libraryId);
                            }
                        }

                        Log.d(TAG, "📚 Всего активных библиотек: " + activeLibraryIds.size());

                        if (activeLibraryIds.isEmpty()) {
                            Log.d(TAG, "ℹ️ Нет активных библиотек для загрузки");
                            listener.onLibrariesLoaded(new ArrayList<>());
                            return;
                        }

                        // Загружаем полную информацию о библиотеках
                        loadLibrariesInfo(activeLibraryIds, new OnLibrariesLoadedListener() {
                            @Override
                            public void onLibrariesLoaded(List<WordLibrary> fullLibraries) {
                                Log.d(TAG, "✅ Загружено полной информации о библиотеках: " + fullLibraries.size());

                                for (WordLibrary library : fullLibraries) {
                                    library.setActive(true);
                                    library.setIsActive(true);
                                }
                                listener.onLibrariesLoaded(fullLibraries);
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "❌ Ошибка загрузки полной информации", e);
                                listener.onError(e);
                            }
                        });
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
}