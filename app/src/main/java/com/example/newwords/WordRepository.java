package com.example.newwords;

import static android.content.ContentValues.TAG;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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

    public WordRepository() {
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.userId = user != null ? user.getUid() : "anonymous";
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
     * Получить ВСЕ активные слова пользователя (из библиотек + кастомные)
     */
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
        // Временно: загружаем все слова из всех публичных библиотек
        db.collection("word_libraries")
                .whereEqualTo("isPublic", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<WordItem> libraryWords = new ArrayList<>();
                        List<String> libraryIds = new ArrayList<>();

                        // Сначала собираем ID всех библиотек
                        for (QueryDocumentSnapshot libraryDoc : task.getResult()) {
                            libraryIds.add(libraryDoc.getId());
                        }

                        // Если нет библиотек, возвращаем пустой список
                        if (libraryIds.isEmpty()) {
                            listener.onWordsLoaded(libraryWords);
                            return;
                        }

                        // Загружаем слова из каждой библиотеки
                        final int[] librariesProcessed = {0};
                        for (String libraryId : libraryIds) {
                            db.collection("word_libraries")
                                    .document(libraryId)
                                    .collection("words")
                                    .get()
                                    .addOnCompleteListener(wordTask -> {
                                        if (wordTask.isSuccessful() && wordTask.getResult() != null) {
                                            for (QueryDocumentSnapshot wordDoc : wordTask.getResult()) {
                                                WordItem word = wordDoc.toObject(WordItem.class);
                                                word.setWordId(wordDoc.getId());
                                                word.setLibraryId(libraryId);
                                                word.setCustomWord(false);
                                                libraryWords.add(word);
                                            }
                                        }

                                        librariesProcessed[0]++;
                                        // Когда все библиотеки обработаны
                                        if (librariesProcessed[0] == libraryIds.size()) {
                                            listener.onWordsLoaded(libraryWords);
                                        }
                                    });
                        }
                    } else {
                        listener.onError(task.getException());
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
                            customWords.add(word);
                        }

                        listener.onWordsLoaded(customWords);
                    } else {
                        listener.onError(task.getException());
                    }
                });
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
    public void updateWord(WordItem word) {
        if (word.getWordId() == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("isFavorite", word.isFavorite());
        updates.put("lastReviewed", new Date());
        updates.put("reviewCount", word.getReviewCount() + 1);

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
    public void getWordsFromActiveLibraries(OnWordsLoadedListener listener) {
        Log.d(TAG, "Получение слов из активных библиотек");

        getUserActiveLibraries(new OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> activeLibraries) {
                Log.d(TAG, "Активные библиотеки загружены: " + activeLibraries.size());

                // Фильтруем только активные библиотеки
                List<WordLibrary> filteredLibraries = new ArrayList<>();
                for (WordLibrary library : activeLibraries) {
                    if (library.isActive()) {
                        filteredLibraries.add(library);
                        Log.d(TAG, "Активная библиотека: " + library.getName());
                    }
                }

                Log.d(TAG, "После фильтрации активных библиотек: " + filteredLibraries.size());

                if (filteredLibraries.isEmpty()) {
                    Log.d(TAG, "Нет активных библиотек, возвращаем пустой список");
                    listener.onWordsLoaded(new ArrayList<>());
                    return;
                }

                List<WordItem> allWords = new ArrayList<>();
                List<Task<QuerySnapshot>> allTasks = new ArrayList<>();

                for (WordLibrary library : filteredLibraries) {
                    boolean isCustom = library.getCreatedBy() != null &&
                            !library.getCreatedBy().equals("system");

                    Task<QuerySnapshot> task = getWordsFromSingleLibrary(library.getLibraryId(), isCustom);
                    allTasks.add(task);
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
                    Log.d(TAG, "Всего слов из активных библиотек: " + allWords.size());
                    listener.onWordsLoaded(allWords);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка загрузки слов из активных библиотек", e);
                    listener.onError(e);
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Ошибка загрузки активных библиотек", e);
                listener.onError(e);
            }
        });
    }
    /**
     * Активировать библиотеку для пользователя
     */
    public void activateLibrary(String libraryId, OnSuccessListener success, OnErrorListener error) {
        // Сначала загружаем информацию о библиотеке
        loadLibraryInfo(libraryId, new OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> libraries) {
                if (libraries.isEmpty()) {
                    error.onError(new Exception("Библиотека не найдена"));
                    return;
                }

                WordLibrary library = libraries.get(0);
                library.setActive(true); // Устанавливаем флаг активности

                // Сохраняем в активные библиотеки пользователя
                Map<String, Object> data = new HashMap<>();
                data.put("active", true);
                data.put("activatedAt", new Date());
                data.put("libraryInfo", library); // Сохраняем полную информацию

                db.collection("users")
                        .document(userId)
                        .collection("active_libraries")
                        .document(libraryId)
                        .set(data)
                        .addOnSuccessListener(aVoid -> success.onSuccess())
                        .addOnFailureListener(error::onError);
            }

            @Override
            public void onError(Exception e) {
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
     * Получить активные библиотеки пользователя
     */
    public void getUserActiveLibraries(OnLibrariesLoadedListener listener) {
        db.collection("users")
                .document(userId)
                .collection("active_libraries")
                .whereEqualTo("active", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<WordLibrary> activeLibraries = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Пробуем получить информацию о библиотеке из документа
                            if (document.contains("libraryInfo")) {
                                WordLibrary library = document.get("libraryInfo", WordLibrary.class);
                                if (library != null) {
                                    library.setActive(true); // Устанавливаем флаг активности
                                    activeLibraries.add(library);
                                    continue;
                                }
                            }

                            // Если нет полной информации, загружаем по ID
                            String libraryId = document.getId();
                            WordLibrary library = new WordLibrary();
                            library.setLibraryId(libraryId);
                            library.setActive(true);
                            activeLibraries.add(library);
                        }

                        Log.d(TAG, "Найдено активных библиотек: " + activeLibraries.size());

                        // Если есть библиотеки без полной информации, загружаем её
                        loadFullLibrariesInfo(activeLibraries, listener);
                    } else {
                        Log.e(TAG, "Ошибка загрузки активных библиотек", task.getException());
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