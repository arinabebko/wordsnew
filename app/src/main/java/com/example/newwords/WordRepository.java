package com.example.newwords;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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
     * Активировать библиотеку для пользователя
     */
    public void activateLibrary(String libraryId, OnSuccessListener success, OnErrorListener error) {
        Map<String, Object> data = new HashMap<>();
        data.put("active", true);
        data.put("activatedAt", new Date());

        db.collection("users")
                .document(userId)
                .collection("active_libraries")
                .document(libraryId)
                .set(data)
                .addOnSuccessListener(aVoid -> success.onSuccess())
                .addOnFailureListener(error::onError);
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
}