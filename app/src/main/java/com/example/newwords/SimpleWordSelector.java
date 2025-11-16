package com.example.newwords;

import static android.content.ContentValues.TAG;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleWordSelector {

    /**
     * Получить слова для текущей сессии
     */
    /**
     * Выбирает слова для текущей сессии изучения

    private List<WordItem> getWordsForSession(List<WordItem> allWords) {
        List<WordItem> sessionWords = new ArrayList<>();
        int maxWords = 20;

        Log.d(TAG, "=== ВЫБОР СЛОВ ДЛЯ СЕССИИ ===");
        Log.d(TAG, "Всего слов доступно: " + allWords.size());

        // Сначала логируем все слова
        for (WordItem word : allWords) {
            Log.d(TAG, "Слово: " + word.getWord() +
                    ", сложность: " + word.getDifficulty() +
                    ", этап: " + word.getReviewStage() +
                    ", показов: " + word.getConsecutiveShows() +
                    ", след. дата: " + word.getNextReviewDate());
        }

        // 1. Собираем слова которые нужно показать СЕЙЧАС
        for (WordItem word : allWords) {
            boolean shouldShow = SimpleRepetitionSystem.shouldShowInSession(word);

            if (shouldShow) {
                sessionWords.add(word);
                Log.d(TAG, "✅ ДОБАВЛЕНО В СЕССИЮ: " + word.getWord());

                if (sessionWords.size() >= maxWords) break;
            } else {
                Log.d(TAG, "❌ НЕ ДОБАВЛЕНО: " + word.getWord());
            }
        }

        Log.d(TAG, "Итог сессии: " + sessionWords.size() + " слов");
        return sessionWords;
    }
     */
    private static List<WordItem> getDueWords(List<WordItem> allWords) {
        List<WordItem> dueWords = new ArrayList<>();
        for (WordItem word : allWords) {
            if (word.isDueForReview() && !word.isLearned()) {
                dueWords.add(word);
            }
        }
        return dueWords;
    }

    private static List<WordItem> getNewWords(List<WordItem> allWords) {
        List<WordItem> newWords = new ArrayList<>();
        for (WordItem word : allWords) {
            if (word.isNew()) {
                newWords.add(word);
            }
        }
        return newWords;
    }

    private static void addWordsToList(List<WordItem> target, List<WordItem> source, int max) {
        int count = Math.min(source.size(), max);
        for (int i = 0; i < count; i++) {
            target.add(source.get(i));
        }
    }
}