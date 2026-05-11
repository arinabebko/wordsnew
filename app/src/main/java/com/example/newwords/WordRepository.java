package com.example.newwords;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

    // ========== КОНСТРУКТОРЫ ==========

    public WordRepository() {
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.userId = user != null ? user.getUid() : "anonymous";
        this.localDb = AppDatabase.getInstance(FirebaseApp.getInstance().getApplicationContext());
    }

    public WordRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            this.userId = user.getUid();
            Log.d(TAG, "✅ Пользователь аутентифицирован: " + this.userId);
        } else {
            this.userId = "anonymous";
            Log.e(TAG, "❌ Пользователь НЕ аутентифицирован!");
        }

        this.localDb = AppDatabase.getInstance(context);
    }

    // ========== ИНТЕРФЕЙСЫ ==========

    public interface StatUpdateListener {
        UserStats onUpdate(UserStats stats);
    }

    public interface OnStatsLoadedListener {
        void onStatsLoaded(UserStats stats);

        void onError(Exception e);
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

    public interface OnWordsWithProgressListener {
        void onWordsLoaded(List<WordItem> words);

        void onError(Exception e);
    }

    public interface OnCacheStatusListener {
        void onStatusChecked(int libraryCount, int wordCount, int activeLibraryCount, int wordsFromActiveLibraries);
    }

    public interface LoadProgressCallback {
        void onProgressLoaded(Map<String, Object> progressData);

        void onProgressNotFound();

        void onError(Exception e);
    }

    private interface OnProgressLoadedListener {
        void onProgressLoaded(boolean success);
    }

    // ========== КОНВЕРТАЦИЯ ==========

    private WordItem convertLocalWordToWordItem(LocalWordItem localWord) {
        WordItem word = new WordItem();
        word.setWordId(localWord.getWordId());
        word.setWord(localWord.getWord());
        word.setTranslation(localWord.getTranslation());
        word.setNote(localWord.getNote());
        word.setIsFavorite(localWord.isFavorite());
        word.setIsCustomWord(localWord.isCustomWord());
        word.setLibraryId(localWord.getLibraryId());
        word.setUserId(localWord.getUserId());
        word.setCreatedAt(localWord.getCreatedAt());
        word.setLastReviewed(localWord.getLastReviewed());

        word.setReviewStage(localWord.getReviewStage());
        word.setNextReviewDate(localWord.getNextReviewDate());
        word.setConsecutiveShows(localWord.getConsecutiveShows());

        try {
            word.setDifficulty(Integer.parseInt(localWord.getDifficulty()));
        } catch (NumberFormatException e) {
            word.setDifficulty(3);
        }

        word.setReviewCount(localWord.getReviewCount());
        word.setCorrectAnswers(localWord.getCorrectAnswers());

        return word;
    }

    private LocalWordItem convertToLocalWord(WordItem web) {
        LocalWordItem local = new LocalWordItem();
        local.setWordId(web.getWordId());
        local.setWord(web.getWord());
        local.setTranslation(web.getTranslation());
        local.setNote(web.getNote());
        local.setLibraryId(web.getLibraryId());
        local.setFavorite(web.isFavorite());
        local.setCreatedAt(web.getCreatedAt());
        local.setReviewStage(web.getReviewStage());
        local.setNextReviewDate(web.getNextReviewDate());
        local.setConsecutiveShows(web.getConsecutiveShows());
        local.setDifficulty(String.valueOf(web.getDifficulty()));
        local.setReviewCount(web.getReviewCount());
        local.setCorrectAnswers(web.getCorrectAnswers());
        local.setCustomWord(web.isCustomWord());
        local.setUserId(web.getUserId());
        local.setLastReviewed(web.getLastReviewed());

        return local;
    }

    private List<WordLibrary> convertToWeb(List<LocalWordLibrary> locals) {
        List<WordLibrary> webs = new ArrayList<>();
        for (LocalWordLibrary local : locals) {
            WordLibrary web = new WordLibrary();
            web.setLibraryId(local.getLibraryId());
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

    private LocalWordLibrary convertToLocal(WordLibrary web) {
        LocalWordLibrary local = new LocalWordLibrary();
        local.setLibraryId(web.getLibraryId());
        local.setName(web.getName());
        local.setDescription(web.getDescription());
        local.setSubcategory(web.getSubcategory());
        local.setCategory(web.getCategory());
        local.setLanguageTo(web.getLanguageTo());
        local.setWordCount(web.getWordCount());
        local.setActive(web.isActive());
        return local;
    }

    // ========== ПРОГРЕСС (LOCAL) ==========

    private void initializeDefaultProgress(WordItem word) {
        word.setDifficulty(3);
        word.setReviewStage(0);
        word.setConsecutiveShows(0);
        word.setNextReviewDate(new Date());
        word.setReviewCount(0);
        word.setCorrectAnswers(0);
    }

    private void saveWordProgressToLocal(WordItem word) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                localDb.wordDao().updateWordProgress(
                        word.getWordId(),
                        word.getReviewStage(),
                        word.getConsecutiveShows(),
                        word.getNextReviewDate(),
                        new Date()
                );
                Log.d(TAG, "💾 Прогресс сохранен в Room для: " + word.getWord());
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка сохранения прогресса в Room", e);
            }
        });
    }

    // ========== ЗАГРУЗКА ИЗ КЕША (OFFLINE-FIRST) ==========

    /**
     * Мгновенная загрузка слов из кеша (без интернета)
     */
    public void getWordsWithProgressFromCache(String language, OnWordsLoadedListener listener) {
        Log.d(TAG, "📦 Загрузка слов из кеша для языка: " + language);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<LocalWordLibrary> libraries = localDb.libraryDao().getActiveLibrariesByLanguage(language);

                if (libraries.isEmpty()) {
                    Log.d(TAG, "⚠️ Нет активных библиотек в кеше");
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            listener.onWordsLoaded(new ArrayList<>()));
                    return;
                }

                List<WordItem> allWords = new ArrayList<>();
                for (LocalWordLibrary library : libraries) {
                    List<LocalWordItem> libraryWords = localDb.wordDao().getWordsByLibrary(library.getLibraryId());
                    for (LocalWordItem localWord : libraryWords) {
                        allWords.add(convertLocalWordToWordItem(localWord));
                    }
                }

                Log.d(TAG, "📦 Найдено слов в кеше: " + allWords.size());
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                        listener.onWordsLoaded(allWords));

            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка загрузки из кеша", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                        listener.onError(e));
            }
        });
    }

    /**
     * Offline-first загрузка активных библиотек
     */
    public void getUserActiveLibrariesOfflineFirst(OnLibrariesLoadedListener listener) {
        Log.d(TAG, "🚀 Offline-first загрузка активных библиотек");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<LocalWordLibrary> localLibraries = localDb.libraryDao().getActiveLibraries();

                if (!localLibraries.isEmpty()) {
                    List<WordLibrary> libraries = convertToWeb(localLibraries);
                    Log.d(TAG, "✅ INSTANT: Загружено " + libraries.size() + " библиотек из Room");
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            listener.onLibrariesLoaded(libraries));
                } else {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            listener.onLibrariesLoaded(new ArrayList<>()));
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка чтения Room", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                        listener.onError(e));
            }
        });
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    FirebaseApp.getInstance().getApplicationContext()
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        } catch (Exception e) {
            return false;
        }
    }

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

    private void saveWordsToCache(List<WordItem> words) {
        new Thread(() -> {
            try {
                List<LocalWordItem> localWords = new ArrayList<>();
                for (WordItem word : words) {
                    localWords.add(convertToLocalWord(word));
                }
                localDb.wordDao().insertWords(localWords);
                Log.d(TAG, "💾 Сохранено в кеш: " + localWords.size() + " слов");
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка сохранения в кеш", e);
            }
        }).start();
    }

    private void saveActiveLibrariesToCache(List<WordLibrary> libraries) {
        new Thread(() -> {
            try {
                List<LocalWordLibrary> localLibraries = new ArrayList<>();
                for (WordLibrary library : libraries) {
                    LocalWordLibrary localLib = convertToLocal(library);
                    localLib.setActive(library.getIsActive());
                    localLibraries.add(localLib);
                }
                localDb.libraryDao().insertLibraries(localLibraries);
                Log.d(TAG, "💾 Сохранено в кеш: " + localLibraries.size() + " библиотек");
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка сохранения библиотек в кеш", e);
            }
        }).start();
    }

    private void loadBasicRepetitionFields(WordItem word, QueryDocumentSnapshot document) {
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

    private void loadRepetitionFields(WordItem word, QueryDocumentSnapshot document) {
        loadBasicRepetitionFields(word, document);
        word.setReviewStage(0);
        word.setConsecutiveShows(0);
        word.setNextReviewDate(new Date());
    }

    // ========== ОБНОВЛЕНИЕ ПРОГРЕССА ==========

    private void updateUserWordProgress(WordItem word) {
        Log.d(TAG, "=== СОХРАНЕНИЕ ПРОГРЕССА ===");
        Log.d(TAG, "Слово: " + word.getWord());
        Log.d(TAG, "Stage: " + word.getReviewStage());

        if (userId == null || userId.equals("anonymous")) {
            saveWordProgressToLocal(word);
            return;
        }

        if (word.getWordId() == null) {
            Log.e(TAG, "❌ wordId = null");
            return;
        }

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

        db.collection("users")
                .document(userId)
                .collection("word_progress")
                .document(word.getWordId())
                .set(progress, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Прогресс сохранен в Firebase"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Ошибка сохранения в Firebase", e));

        saveWordProgressToLocal(word);
    }

    public void updateWord(WordItem word) {
        if (word.getWordId() == null) return;
        updateUserWordProgress(word);
    }

    // ========== МЕТОДЫ ДЛЯ FRAGMENT1 ==========

    /**
     * Главный метод для Fragment1 - загружает слова с прогрессом
     */
    public void getWordsWithProgress(String language, OnWordsWithProgressListener listener) {
        Log.d(TAG, "🚀 Загрузка слов с прогрессом для языка: " + language);

        getWordsFromActiveLibrariesFirebase(language, new OnWordsLoadedListener() {
            @Override
            public void onWordsLoaded(List<WordItem> words) {
                if (words.isEmpty()) {
                    listener.onWordsLoaded(words);
                    return;
                }
                loadAllWordsProgress(words, listener);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка загрузки слов, пробуем кеш", e);
                getWordsWithProgressFromCache(language, new OnWordsLoadedListener() {
                    @Override
                    public void onWordsLoaded(List<WordItem> cachedWords) {
                        listener.onWordsLoaded(cachedWords);
                    }

                    @Override
                    public void onError(Exception err) {
                        listener.onError(err);
                    }
                });
            }
        });
    }



    public void getWordsFromActiveLibrariesFirebase(String language, OnWordsLoadedListener listener) {
        Log.d(TAG, "🔥 Загрузка слов из Firebase для языка: " + language);

        getUserActiveLibraries(new OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> activeLibraries) {
                List<WordLibrary> filteredLibraries = new ArrayList<>();
                for (WordLibrary library : activeLibraries) {
                    if (language.equals(library.getLanguageFrom())) {
                        filteredLibraries.add(library);
                    }
                }

                if (filteredLibraries.isEmpty()) {
                    listener.onWordsLoaded(new ArrayList<>());
                    return;
                }

                List<WordItem> allWords = new ArrayList<>();
                List<Task<QuerySnapshot>> tasks = new ArrayList<>();

                for (WordLibrary library : filteredLibraries) {
                    boolean isCustom = library.getCreatedBy() != null && !library.getCreatedBy().equals("system");
                    tasks.add(getWordsFromSingleLibrary(library.getLibraryId(), isCustom));
                }

                Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
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
                    listener.onWordsLoaded(allWords);
                }).addOnFailureListener(listener::onError);
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e);
            }
        });
    }

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
                            }
                        }

                        if (activeLibraryIds.isEmpty()) {
                            listener.onLibrariesLoaded(new ArrayList<>());
                            return;
                        }

                        loadLibrariesInfo(activeLibraryIds, listener);
                    } else {
                        Log.e(TAG, "Ошибка загрузки, пробуем кеш", task.getException());
                        getUserActiveLibrariesOfflineFirst(listener);
                    }
                });
    }

    private void loadLibrariesInfo(List<String> libraryIds, OnLibrariesLoadedListener listener) {
        if (libraryIds.isEmpty()) {
            listener.onLibrariesLoaded(new ArrayList<>());
            return;
        }

        List<WordLibrary> activeLibraries = new ArrayList<>();
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

        for (String libraryId : libraryIds) {
            tasks.add(db.collection("word_libraries").document(libraryId).get());
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
            loadCustomLibrariesInfo(libraryIds, activeLibraries, listener);
        });
    }

    private void loadCustomLibrariesInfo(List<String> libraryIds, List<WordLibrary> activeLibraries, OnLibrariesLoadedListener listener) {
        List<Task<DocumentSnapshot>> customTasks = new ArrayList<>();

        for (String libraryId : libraryIds) {
            customTasks.add(db.collection("users")
                    .document(userId)
                    .collection("custom_libraries")
                    .document(libraryId)
                    .get());
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

    // ========== СТАТИСТИКА ==========

    public void getUserStats(OnStatsLoadedListener listener) {
        if (userId.equals("anonymous")) return;

        db.collection("users").document(userId)
                .collection("stats").document("main")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserStats remoteStats = documentSnapshot.toObject(UserStats.class);
                        if (remoteStats != null) {
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
                UserStats stats = new UserStats(userId);
                UserStats updatedStats = updateListener.onUpdate(stats);

                db.collection("users").document(userId)
                        .collection("stats").document("main")
                        .set(updatedStats, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Статистика синхронизирована"))
                        .addOnFailureListener(e -> Log.e(TAG, "Ошибка синхронизации", e));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка обновления статистики", e);
            }
        });
    }

    public void onWordLearned(String wordId) {
        updateStatsAsync(stats -> {
            stats.setWordsLearned(stats.getWordsLearned() + 1);
            stats.setTodayProgress(stats.getTodayProgress() + 1);
            stats.setLastSessionDate(new Date());
            return stats;
        });
    }

    public void onWordReviewed() {
        updateStatsAsync(stats -> {
            stats.setTodayProgress(stats.getTodayProgress() + 1);
            stats.setLastSessionDate(new Date());
            return stats;
        });
    }


    /**
     * Добавить слово в пользовательскую библиотеку
     */
    public void addWordToCustomLibrary(String libraryId, WordItem word, OnWordAddedListener listener) {
        word.setUserId(userId);
        word.setCustomWord(true);
        word.setLibraryId(libraryId);
        word.setCreatedAt(new Date());
        word.setReviewStage(0);
        word.setConsecutiveShows(0);
        word.setNextReviewDate(new Date());
        word.setDifficulty(3);
        word.setReviewCount(0);
        word.setCorrectAnswers(0);

        // Готовим данные для Firebase
        Map<String, Object> wordData = new HashMap<>();
        wordData.put("word", word.getWord());
        wordData.put("translation", word.getTranslation());
        wordData.put("note", word.getNote());
        wordData.put("userId", userId);
        wordData.put("libraryId", libraryId);
        wordData.put("isCustomWord", true);
        wordData.put("createdAt", word.getCreatedAt());
        wordData.put("reviewStage", word.getReviewStage());
        wordData.put("consecutiveShows", word.getConsecutiveShows());
        wordData.put("nextReviewDate", word.getNextReviewDate());
        wordData.put("difficulty", word.getDifficulty());
        wordData.put("reviewCount", word.getReviewCount());
        wordData.put("correctAnswers", word.getCorrectAnswers());
        wordData.put("isFavorite", false);

        db.collection("users")
                .document(userId)
                .collection("custom_libraries")
                .document(libraryId)
                .collection("words")
                .add(wordData)
                .addOnSuccessListener(documentReference -> {
                    String newId = documentReference.getId();
                    word.setWordId(newId);

                    // Сохраняем в локальную БД (Room)
                    saveWordToLocal(word, libraryId);

                    // Обновляем счетчик слов в Firebase
                    updateLibraryWordCount(libraryId);

                    listener.onWordAdded(word);
                })
                .addOnFailureListener(listener::onError);
    }

    // Вспомогательный метод для сохранения в Room
    private void saveWordToLocal(WordItem word, String libraryId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            LocalWordItem localWord = convertToLocalWord(word);
            localDb.wordDao().insertWord(localWord);
            localDb.libraryDao().incrementWordCount(libraryId);
            Log.d(TAG, "💾 Слово сохранено в Room: " + word.getWord());
        });
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
                    if (task.isSuccessful() && task.getResult() != null) {
                        int wordCount = task.getResult().size();

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("wordCount", wordCount);

                        db.collection("users")
                                .document(userId)
                                .collection("custom_libraries")
                                .document(libraryId)
                                .update(updates)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "✅ Счетчик слов обновлен: " + wordCount))
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "❌ Ошибка обновления счетчика", e));
                    }
                });
    }

    /**
     * Получить пользовательские библиотеки
     */
    public void getCustomLibraries(OnLibrariesLoadedListener listener) {
        Log.d(TAG, "📚 Загрузка пользовательских библиотек");

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

                        Log.d(TAG, "✅ Загружено пользовательских библиотек: " + customLibraries.size());
                        listener.onLibrariesLoaded(customLibraries);
                    } else {
                        Log.e(TAG, "❌ Ошибка загрузки пользовательских библиотек", task.getException());
                        listener.onError(task.getException());
                    }
                });
    }
    // ========== МЕТОДЫ ДЛЯ РАБОТЫ С БИБЛИОТЕКАМИ ==========

    /**
     * Получить все доступные библиотеки (публичные + пользовательские)
     */
    public void getAvailableLibraries(OnLibrariesLoadedListener listener) {
        Log.d(TAG, "📚 Загрузка всех доступных библиотек");

        List<WordLibrary> allLibraries = new ArrayList<>();

        // 1. Загружаем публичные библиотеки
        db.collection("word_libraries")
                .whereEqualTo("isPublic", true)
                .get()
                .addOnSuccessListener(publicSnapshots -> {
                    for (DocumentSnapshot doc : publicSnapshots) {
                        WordLibrary lib = doc.toObject(WordLibrary.class);
                        if (lib != null) {
                            lib.setLibraryId(doc.getId());
                            lib.setCreatedBy("system");
                            allLibraries.add(lib);
                        }
                    }

                    // 2. Загружаем пользовательские библиотеки
                    db.collection("users")
                            .document(userId)
                            .collection("custom_libraries")
                            .get()
                            .addOnSuccessListener(customSnapshots -> {
                                for (DocumentSnapshot doc : customSnapshots) {
                                    WordLibrary lib = doc.toObject(WordLibrary.class);
                                    if (lib != null) {
                                        lib.setLibraryId(doc.getId());
                                        lib.setCreatedBy(userId);
                                        allLibraries.add(lib);
                                    }
                                }

                                Log.d(TAG, "✅ Загружено библиотек: " + allLibraries.size());
                                listener.onLibrariesLoaded(allLibraries);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Ошибка загрузки пользовательских библиотек", e);
                                listener.onLibrariesLoaded(allLibraries);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка загрузки публичных библиотек", e);
                    listener.onError(e);
                });
    }

    /**
     * Активировать библиотеку для пользователя
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
                    Log.d(TAG, "✅ Библиотека активирована: " + libraryId);
                    updateLibraryLocalStatus(libraryId, true);
                    success.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка активации", e);
                    error.onError(e);
                });
    }

    /**
     * Деактивировать библиотеку для пользователя
     */
    public void deactivateLibrary(String libraryId, OnSuccessListener success, OnErrorListener error) {
        Log.d(TAG, "🔗 Деактивация библиотеки: " + libraryId);

        db.collection("users")
                .document(userId)
                .collection("active_libraries")
                .document(libraryId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Библиотека деактивирована: " + libraryId);
                    updateLibraryLocalStatus(libraryId, false);
                    success.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка деактивации", e);
                    error.onError(e);
                });
    }

    /**
     * Создать пользовательскую библиотеку
     */
    public void createCustomLibrary(String name, String description, String category, String language,
                                    OnLibraryCreatedListener listener) {
        Log.d(TAG, "📚 Создание библиотеки: " + name);

        Map<String, String> nameMap = new HashMap<>();
        nameMap.put(language, name);

        Map<String, String> descMap = new HashMap<>();
        descMap.put(language, description);

        Map<String, Object> libraryData = new HashMap<>();
        libraryData.put("name", nameMap);
        libraryData.put("description", descMap);
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
                    WordLibrary library = new WordLibrary();
                    library.setLibraryId(documentReference.getId());
                    library.setName(nameMap);
                    library.setDescription(descMap);
                    library.setCategory(category);
                    library.setWordCount(0);
                    library.setLanguageFrom(language);
                    library.setCreatedBy(userId);

                    Log.d(TAG, "✅ Библиотека создана: " + documentReference.getId());
                    listener.onLibraryCreated(library);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка создания библиотеки", e);
                    listener.onError(e);
                });
    }

    /**
     * Удалить пользовательскую библиотеку
     */
    public void deleteCustomLibrary(String libraryId, OnSuccessListener success, OnErrorListener error) {
        Log.d(TAG, "🗑️ Удаление библиотеки: " + libraryId);

        // Сначала удаляем все слова из библиотеки
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

                        Tasks.whenAll(deleteTasks)
                                .addOnSuccessListener(aVoid -> {
                                    // Затем удаляем саму библиотеку
                                    db.collection("users")
                                            .document(userId)
                                            .collection("custom_libraries")
                                            .document(libraryId)
                                            .delete()
                                            .addOnSuccessListener(aVoid2 -> {
                                                // Удаляем из активных библиотек
                                                deactivateLibrary(libraryId, success, error);
                                            })
                                            .addOnFailureListener(error::onError);
                                })
                                .addOnFailureListener(error::onError);
                    } else {
                        error.onError(task.getException());
                    }
                });
    }

    /**
     * Обновить статус библиотеки в локальной БД
     */
    private void updateLibraryLocalStatus(String libraryId, boolean isActive) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                localDb.libraryDao().updateLibraryActiveStatus(libraryId, isActive);
                Log.d(TAG, "💾 Room обновлен: библиотека " + libraryId + " isActive=" + isActive);
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка обновления Room", e);
            }
        });
    }

    /**
     * Полностью очищает локальный кеш (Room)
     */
    public void clearLocalCache(OnSuccessListener success, OnErrorListener error) {
        Log.d(TAG, "🧹 === НАЧАЛО ОЧИСТКИ ЛОКАЛЬНОГО КЕША ===");

        new Thread(() -> {
            try {
                // Очищаем таблицы
                localDb.libraryDao().clearAllLibraries();
                localDb.wordDao().clearAllWords();

                Log.d(TAG, "✅ Локальный кеш полностью очищен!");

                // Вызываем колбэк в UI потоке
                if (success != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        success.onSuccess();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка очистки кеша", e);
                if (error != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        error.onError(e);
                    });
                }
            }
        }).start();
    }

    /**
     * Получить слова из конкретной библиотеки
     */
    public void getWordsFromLibrary(String libraryId, boolean isCustomLibrary, OnWordsLoadedListener listener) {
        Log.d(TAG, "📚 Загрузка слов из библиотеки: " + libraryId + ", isCustom: " + isCustomLibrary);

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
                            Log.d(TAG, "✅ Загружено слов из пользовательской библиотеки: " + words.size());
                            listener.onWordsLoaded(words);
                        } else {
                            Log.e(TAG, "❌ Ошибка загрузки слов из пользовательской библиотеки", task.getException());
                            listener.onError(task.getException());
                        }
                    });
        } else {
            // Слова из публичной (системной) библиотеки
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
                            Log.d(TAG, "✅ Загружено слов из публичной библиотеки: " + words.size());
                            listener.onWordsLoaded(words);
                        } else {
                            Log.e(TAG, "❌ Ошибка загрузки слов из публичной библиотеки", task.getException());
                            listener.onError(task.getException());
                        }
                    });
        }
    }

    /**
     * Синхронизация Firebase -> Room для конкретного языка
     * Этот метод загружает слова из Firebase и сохраняет их в локальный кеш
     */
    public void syncWordsFromFirebaseForLanguage(String language, OnWordsLoadedListener listener) {
        Log.d(TAG, "🔄 СИНХРОНИЗАЦИЯ для языка: " + language);

        getUserActiveLibraries(new OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> allLibraries) {
                Log.d(TAG, "📚 Всего библиотек: " + allLibraries.size());

                // Фильтруем библиотеки по нужному языку
                List<WordLibrary> filteredLibraries = new ArrayList<>();
                for (WordLibrary lib : allLibraries) {
                    if (language.equals(lib.getLanguageFrom())) {
                        filteredLibraries.add(lib);
                        Log.d(TAG, "✅ Подходит библиотека: " + lib.getName());
                    }
                }

                if (filteredLibraries.isEmpty()) {
                    Log.d(TAG, "⚠️ Нет библиотек для языка: " + language);
                    if (listener != null) {
                        listener.onWordsLoaded(new ArrayList<>());
                    }
                    return;
                }

                // Загружаем слова из каждой библиотеки
                List<WordItem> allWords = new ArrayList<>();
                List<Task<QuerySnapshot>> tasks = new ArrayList<>();

                for (WordLibrary lib : filteredLibraries) {
                    boolean isCustom = lib.getCreatedBy() != null && !lib.getCreatedBy().equals("system");
                    tasks.add(getWordsFromSingleLibrary(lib.getLibraryId(), isCustom));
                }

                Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
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

                    Log.d(TAG, "🌐 Загружено из Firebase: " + allWords.size() + " слов для языка " + language);

                    // СОХРАНЯЕМ В КЕШ (это самое важное!)
                    saveWordsToCache(allWords);

                    // Сохраняем активные библиотеки
                    saveActiveLibrariesToCache(filteredLibraries);

                    if (listener != null) {
                        listener.onWordsLoaded(allWords);
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка синхронизации для языка " + language, e);
                    if (listener != null) {
                        listener.onError(e);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Ошибка загрузки библиотек для языка " + language, e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * Удаляет слово из пользовательской библиотеки
     */
    public void deleteWordFromLibrary(String libraryId, String wordId, OnSuccessListener success, OnErrorListener error) {
        Log.d(TAG, "🗑️ Удаление слова из библиотеки: " + libraryId + ", слово: " + wordId);

        db.collection("users")
                .document(userId)
                .collection("custom_libraries")
                .document(libraryId)
                .collection("words")
                .document(wordId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Слово удалено из Firebase: " + wordId);

                    // Обновляем счетчик слов в библиотеке
                    updateLibraryWordCount(libraryId);

                    // Удаляем из локальной БД (Room)
                    deleteWordFromLocal(wordId);

                    if (success != null) {
                        success.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка удаления слова из библиотеки", e);
                    if (error != null) {
                        error.onError(e);
                    }
                });
    }

    /**
     * Удаляет кастомное слово (не из библиотеки)
     */
    public void deleteCustomWord(String wordId, OnSuccessListener success, OnErrorListener error) {
        Log.d(TAG, "🗑️ Удаление кастомного слова: " + wordId);

        db.collection("users")
                .document(userId)
                .collection("custom_words")
                .document(wordId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Кастомное слово удалено из Firebase: " + wordId);

                    // Удаляем из локальной БД (Room)
                    deleteWordFromLocal(wordId);

                    if (success != null) {
                        success.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка удаления кастомного слова", e);
                    if (error != null) {
                        error.onError(e);
                    }
                });
    }

    /**
     * Удаляет слово из локальной БД (Room)
     */
    private void deleteWordFromLocal(String wordId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                localDb.wordDao().deleteWord(wordId);
                Log.d(TAG, "💾 Слово удалено из Room: " + wordId);
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка удаления слова из Room", e);
            }
        });
    }

    // ========== МЕТОДЫ ДЛЯ WordsFragment ==========

    /**
     * Гарантирует создание структуры word_progress в Firestore
     */
    public void ensureWordProgressStructure(OnSuccessListener listener) {
        Log.d(TAG, "🔧 Проверка структуры word_progress");

        Map<String, Object> initialData = new HashMap<>();
        initialData.put("_createdAt", new Date());
        initialData.put("_initialized", true);

        db.collection("users")
                .document(userId)
                .collection("word_progress")
                .document("_initialization")
                .set(initialData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Коллекция word_progress создана/проверена");
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка создания word_progress", e);
                    if (listener != null) {
                        // Всё равно вызываем onSuccess, так как структура может уже существовать
                        listener.onSuccess();
                    }
                });
    }

    /**
     * Умная загрузка слов (сначала из кеша, потом фоном из Firebase)
     */
    public void loadWordsSmart(String language, OnWordsLoadedListener uiListener) {
        Log.d(TAG, "🧠 SMART LOAD старт для языка: " + language);

        // ШАГ 1: Мгновенно отдаем из кеша (если есть)
        Executors.newSingleThreadExecutor().execute(() -> {
            List<WordItem> cachedWords = new ArrayList<>();
            try {
                List<LocalWordLibrary> activeLibraries = localDb.libraryDao()
                        .getActiveLibrariesByLanguage(language);

                for (LocalWordLibrary lib : activeLibraries) {
                    List<LocalWordItem> libWords = localDb.wordDao().getWordsByLibrary(lib.getLibraryId());
                    for (LocalWordItem localWord : libWords) {
                        cachedWords.add(convertLocalWordToWordItem(localWord));
                    }
                }

                final List<WordItem> finalCached = new ArrayList<>(cachedWords);
                Log.d(TAG, "📦 КЕШ: " + finalCached.size() + " слов");

                // Отдаем в UI потоке
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (uiListener != null && !finalCached.isEmpty()) {
                        uiListener.onWordsLoaded(finalCached);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка чтения кеша", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (uiListener != null) {
                        uiListener.onWordsLoaded(new ArrayList<>());
                    }
                });
            }

            // ШАГ 2: ФОНОМ качаем из Firebase и обновляем кеш
            syncWordsFromFirebaseForLanguage(language, new OnWordsLoadedListener() {
                @Override
                public void onWordsLoaded(List<WordItem> freshWords) {
                    Log.d(TAG, "🌐 FIREBASE: загружено " + freshWords.size() + " свежих слов");
                    // Обновляем UI если данные изменились и кеш был пуст
                    if (cachedWords.isEmpty() && !freshWords.isEmpty() && uiListener != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            uiListener.onWordsLoaded(freshWords);
                        });
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "⚠️ Ошибка синхронизации с Firebase", e);
                    if (cachedWords.isEmpty() && uiListener != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            uiListener.onError(e);
                        });
                    }
                }
            });
        });
    }



















    //аааааа
    // ========== МЕТОДЫ ДЛЯ WordsFragment (ПРОДОЛЖЕНИЕ) ==========

    /**
     * Загружает активные библиотеки для указанного языка
     */
    public void getUserActiveLibrariesForLanguage(String language, OnLibrariesLoadedListener listener) {
        Log.d(TAG, "🔄 Загрузка активных библиотек для языка: " + language);

        getUserActiveLibraries(new OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> allLibraries) {
                List<WordLibrary> filteredLibraries = new ArrayList<>();
                for (WordLibrary library : allLibraries) {
                    if (language.equals(library.getLanguageFrom())) {
                        library.setActive(true);
                        filteredLibraries.add(library);
                    }
                }
                Log.d(TAG, "📚 Найдено библиотек для языка " + language + ": " + filteredLibraries.size());
                listener.onLibrariesLoaded(filteredLibraries);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Ошибка загрузки библиотек для языка " + language, e);
                listener.onError(e);
            }
        });
    }

    /**
     * Проверяет статус кеша (для дебага)
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

    // ========== ЗАГРУЗКА ПРОГРЕССА (АДАПТИВНАЯ) ==========

    /**
     * Загружает прогресс для всех слов (адаптивная стратегия)
     */
    private void loadAllWordsProgress(List<WordItem> words, OnWordsWithProgressListener listener) {
        if (userId.equals("anonymous")) {
            for (WordItem word : words) initializeDefaultProgress(word);
            listener.onWordsLoaded(words);
            return;
        }

        List<String> wordIds = new ArrayList<>();
        Map<String, WordItem> wordMap = new HashMap<>();
        for (WordItem word : words) {
            if (word.getWordId() != null && !word.getWordId().isEmpty()) {
                wordIds.add(word.getWordId());
                wordMap.put(word.getWordId(), word);
            }
        }

        if (wordIds.isEmpty()) {
            listener.onWordsLoaded(words);
            return;
        }

        // Проверяем интернет
        boolean hasInternet = isNetworkAvailable();

        if (!hasInternet) {
            // Нет интернета - используем локальные данные
            Log.d(TAG, "📴 Нет интернета, используем локальный прогресс");
            loadProgressFromLocalDB(words, listener);
            return;
        }

        // Firestore лимит: максимум 30 элементов в whereIn
        final int BATCH_SIZE = 25;

        // Разбиваем ID на пачки
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < wordIds.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, wordIds.size());
            batches.add(new ArrayList<>(wordIds.subList(i, end)));
        }

        Log.d(TAG, "📦 Разбиваем " + wordIds.size() + " слов на " + batches.size() + " пачек");

        // Адаптивная загрузка
        if (batches.size() <= 3 && hasInternet) {
            loadBatchesInParallel(batches, wordMap, words, listener);
        } else {
            loadBatchesSequentially(batches, wordMap, words, listener);
        }
    }

    /**
     * Параллельная загрузка пачек (для быстрого интернета)
     */
    private void loadBatchesInParallel(List<List<String>> batches,
                                       Map<String, WordItem> wordMap,
                                       List<WordItem> words,
                                       OnWordsWithProgressListener listener) {
        Log.d(TAG, "⚡ Параллельная загрузка " + batches.size() + " пачек");

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (List<String> batch : batches) {
            tasks.add(db.collection("users")
                    .document(userId)
                    .collection("word_progress")
                    .whereIn("__name__", batch)
                    .get());
        }

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    for (Object result : results) {
                        if (result instanceof QuerySnapshot) {
                            QuerySnapshot snapshot = (QuerySnapshot) result;
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                WordItem word = wordMap.get(doc.getId());
                                if (word != null) {
                                    applyProgressToWord(word, doc);
                                }
                            }
                        }
                    }
                    finalizeProgress(words, listener);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка параллельной загрузки, пробуем последовательно", e);
                    loadBatchesSequentially(batches, wordMap, words, listener);
                });
    }

    /**
     * Последовательная загрузка пачек (для плохого интернета)
     */
    private void loadBatchesSequentially(List<List<String>> batches,
                                         Map<String, WordItem> wordMap,
                                         List<WordItem> words,
                                         OnWordsWithProgressListener listener) {
        Log.d(TAG, "🐢 Последовательная загрузка " + batches.size() + " пачек");
        loadBatchSequentially(batches, 0, wordMap, words, listener);
    }

    private void loadBatchSequentially(List<List<String>> batches,
                                       int index,
                                       Map<String, WordItem> wordMap,
                                       List<WordItem> words,
                                       OnWordsWithProgressListener listener) {
        if (index >= batches.size()) {
            finalizeProgress(words, listener);
            return;
        }

        List<String> batch = batches.get(index);

        db.collection("users")
                .document(userId)
                .collection("word_progress")
                .whereIn("__name__", batch)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        WordItem word = wordMap.get(doc.getId());
                        if (word != null) {
                            applyProgressToWord(word, doc);
                        }
                    }
                    loadBatchSequentially(batches, index + 1, wordMap, words, listener);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Ошибка загрузки пачки " + (index + 1) + ", пропускаем", e);
                    loadBatchSequentially(batches, index + 1, wordMap, words, listener);
                });
    }

    /**
     * Загрузка прогресса из локальной БД (офлайн)
     */
    private void loadProgressFromLocalDB(List<WordItem> words, OnWordsWithProgressListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<LocalWordItem> localWords = localDb.wordDao().getAllWords();
                Map<String, LocalWordItem> localMap = new HashMap<>();
                for (LocalWordItem localWord : localWords) {
                    localMap.put(localWord.getWordId(), localWord);
                }

                int loadedCount = 0;
                for (WordItem word : words) {
                    LocalWordItem localWord = localMap.get(word.getWordId());
                    if (localWord != null) {
                        word.setReviewStage(localWord.getReviewStage());
                        word.setConsecutiveShows(localWord.getConsecutiveShows());
                        word.setNextReviewDate(localWord.getNextReviewDate());
                        loadedCount++;
                    } else {
                        initializeDefaultProgress(word);
                    }
                }

                Log.d(TAG, "📦 Загружено из локального кеша: " + loadedCount + "/" + words.size());

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    listener.onWordsLoaded(words);
                });

            } catch (Exception e) {
                Log.e(TAG, "Ошибка загрузки из локальной БД", e);
                for (WordItem word : words) initializeDefaultProgress(word);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    listener.onWordsLoaded(words);
                });
            }
        });
    }

    /**
     * Применяет прогресс из документа к слову
     */
    private void applyProgressToWord(WordItem word, DocumentSnapshot doc) {
        if (doc.contains("difficulty")) {
            Long val = doc.getLong("difficulty");
            if (val != null) word.setDifficulty(val.intValue());
        }
        if (doc.contains("reviewStage")) {
            Long val = doc.getLong("reviewStage");
            if (val != null) word.setReviewStage(val.intValue());
        }
        if (doc.contains("consecutiveShows")) {
            Long val = doc.getLong("consecutiveShows");
            if (val != null) word.setConsecutiveShows(val.intValue());
        }
        if (doc.contains("nextReviewDate")) {
            word.setNextReviewDate(doc.getDate("nextReviewDate"));
        }
        if (doc.contains("reviewCount")) {
            Long val = doc.getLong("reviewCount");
            if (val != null) word.setReviewCount(val.intValue());
        }
        if (doc.contains("correctAnswers")) {
            Long val = doc.getLong("correctAnswers");
            if (val != null) word.setCorrectAnswers(val.intValue());
        }
    }

    /**
     * Финализирует загрузку прогресса (дефолтные значения для неподгруженных)
     */
    private void finalizeProgress(List<WordItem> words, OnWordsWithProgressListener listener) {
        for (WordItem word : words) {
            if (word.getReviewStage() == 0 && word.getConsecutiveShows() == 0) {
                initializeDefaultProgress(word);
            }
        }
        listener.onWordsLoaded(words);
    }

    public interface OnWordUpdatedListener {
        void onWordUpdated();
        void onError(Exception e);
    }
    // В класс WordRepository добавьте этот метод
    public void updateWord(WordItem word, OnWordUpdatedListener listener) {
        if (word.getWordId() == null) {
            listener.onError(new Exception("ID слова не может быть null"));
            return;
        }

        if (userId == null || userId.equals("anonymous")) {
            listener.onError(new Exception("Пользователь не авторизован"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("word", word.getWord());
        updates.put("translation", word.getTranslation());
        updates.put("note", word.getNote());
        updates.put("isFavorite", word.isFavorite());

        // ОПРЕДЕЛЯЕМ ГДЕ ХРАНИТСЯ СЛОВО
        Task<Void> updateTask;

        if (word.getLibraryId() != null && !word.getLibraryId().isEmpty()) {
            // Слово находится в пользовательской библиотеке
            Log.d(TAG, "Обновление слова в библиотеке: " + word.getLibraryId());
            updateTask = db.collection("users")
                    .document(userId)
                    .collection("custom_libraries")
                    .document(word.getLibraryId())
                    .collection("words")
                    .document(word.getWordId())
                    .update(updates);
        } else if (word.isCustomWord()) {
            // Кастомное слово не в библиотеке
            Log.d(TAG, "Обновление кастомного слова");
            updateTask = db.collection("users")
                    .document(userId)
                    .collection("custom_words")
                    .document(word.getWordId())
                    .update(updates);
        } else {
            // Публичное слово - нельзя редактировать
            listener.onError(new Exception("Нельзя редактировать публичные слова"));
            return;
        }

        updateTask.addOnSuccessListener(aVoid -> {
            Log.d(TAG, "✅ Слово обновлено в Firebase: " + word.getWord());
            updateWordInLocal(word);
            listener.onWordUpdated();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "❌ Ошибка обновления слова", e);

            // Если документ не найден, пробуем найти в другом месте
            if (e.getMessage().contains("NOT_FOUND")) {
                Log.d(TAG, "Пробуем найти слово в альтернативном месте...");
                tryToFindAndUpdateWord(word, updates, listener);
            } else {
                listener.onError(e);
            }
        });
    }

    // Вспомогательный метод для поиска слова в разных коллекциях
    private void tryToFindAndUpdateWord(WordItem word, Map<String, Object> updates, OnWordUpdatedListener listener) {
        // Пробуем обновить в custom_words
        db.collection("users")
                .document(userId)
                .collection("custom_words")
                .document(word.getWordId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Слово найдено и обновлено в custom_words");
                    word.setLibraryId(null);
                    updateWordInLocal(word);
                    listener.onWordUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Слово не найдено нигде", e);
                    listener.onError(new Exception("Слово не найдено в базе данных"));
                });
    }
    // Добавьте этот метод в класс WordRepository
    private void updateWordInLocal(WordItem word) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                LocalWordItem localWord = convertToLocalWord(word);
                localDb.wordDao().updateWord(localWord);
                Log.d(TAG, "💾 Слово обновлено в Room: " + word.getWord());
            } catch (Exception e) {
                Log.e(TAG, "❌ Ошибка обновления слова в Room", e);
            }
        });
    }
}

