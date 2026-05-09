package com.example.newwords;

import android.util.Log;
import java.util.Calendar;
import java.util.Date;

public class SimpleRepetitionSystem {
    private static final String TAG = "RepetitionSystem";

    // Интервалы в днях для каждого этапа: [0, 1, 3, 7, 14, 30, 60]
    private static final int[] REVIEW_INTERVALS = {0, 1, 3, 7, 14, 30, 60};
    private static final int MAX_STAGE = REVIEW_INTERVALS.length - 1;

    /**
     * Обрабатывает ответ пользователя
     */
    public static void processAnswer(WordItem word, boolean isCorrect) {
        Log.d(TAG, "Обработка: " + word.getWord() + ", правильный: " + isCorrect);

        word.setReviewCount(word.getReviewCount() + 1);
        word.setLastReviewed(new Date());

        if (isCorrect) {
            word.setCorrectAnswers(word.getCorrectAnswers() + 1);
            handleCorrectAnswer(word);
        } else {
            handleIncorrectAnswer(word);
        }

        updateNextReviewDate(word);
        updateDifficulty(word); // Используем ЕДИНСТВЕННЫЙ метод
    }


    /**
     * Обрабатывает правильный ответ
     */
    private static void handleCorrectAnswer(WordItem word) {
        if (word.getReviewStage() == 0) {
            // Новое слово - увеличиваем счетчик показов
            word.setConsecutiveShows(word.getConsecutiveShows() + 1);

            // Если показали 3 раза (первый показ + еще два) - переходим к этапу 1
            if (word.getConsecutiveShows() >= 3) {
                word.setReviewStage(1);
                word.setConsecutiveShows(0);
                Log.d(TAG, "✅ Слово показано 3 раза, переходим к этапу 1");
            } else {
                Log.d(TAG, "✅ Новое слово показано " + word.getConsecutiveShows() + "/3 раз");
            }
        } else {
            // Уже не новое слово - переходим к следующему этапу
            if (word.getReviewStage() < MAX_STAGE) {
                word.setReviewStage(word.getReviewStage() + 1);
                word.setConsecutiveShows(0); // Сбрасываем счетчик показов для следующих этапов
                Log.d(TAG, "✅ Переход к этапу " + word.getReviewStage());
            } else {
                Log.d(TAG, "✅ Слово достигло максимального этапа");
            }
        }
    }
    /**
     * Обрабатывает неправильный ответ
     */
    private static void handleIncorrectAnswer(WordItem word) {
        // Сбрасываем на предыдущий этап, но не ниже 0
        int newStage = Math.max(0, word.getReviewStage() - 2);
        word.setReviewStage(newStage);
        word.setConsecutiveShows(0);
        Log.d(TAG, "❌ Сброс к этапу " + newStage);
    }

    /**
     * Обновляет дату следующего повторения
     */
    private static void updateNextReviewDate(WordItem word) {
        Calendar calendar = Calendar.getInstance();

        if (word.getReviewStage() == 0 && word.getConsecutiveShows() < 3) {
            // Новое слово - показать сразу (устанавливаем прошедшую дату)
            calendar.add(Calendar.MINUTE, -1);
            Log.d(TAG, "🔄 Новое слово - показать сразу в сессии");
        } else {
            int intervalDays = REVIEW_INTERVALS[word.getReviewStage()];
            calendar.add(Calendar.DAY_OF_YEAR, intervalDays);
            Log.d(TAG, "📅 Следующее повторение через " + intervalDays + " дней");
        }

        word.setNextReviewDate(calendar.getTime());
    }

    /**
     * Обновляет сложность слова на основе этапа
     */

    private static void updateDifficulty(WordItem word) {
        int stage = word.getReviewStage();

        if (stage >= 6) {
            word.setDifficulty(1); // Выучено (после 30 дней)
        } else if (stage >= 2) {
            word.setDifficulty(2); // Изучается (после 1 дня)
        } else {
            word.setDifficulty(3); // Новое
        }

        Log.d(TAG, "Сложность слова " + word.getWord() + " установлена: " + word.getDifficulty());
    }
    /**
     * Нужно ли показывать слово в текущей сессии

    public static boolean shouldShowInSession(WordItem word) {
        // Показываем если слово готово к повторению И не достигло максимального этапа
       boolean isDue = word.isDueForReview();
        boolean isNotFullyLearned = word.getReviewStage() < MAX_STAGE;
        boolean shouldShow = isDue && isNotFullyLearned;
       // boolean shouldShow = isNotFullyLearned;

        Log.d(TAG, "Проверка показа: " + word.getWord() +
                ", готово: " + isDue +
                ", не выучено: " + isNotFullyLearned +
                ", показывать: " + shouldShow);

        return shouldShow;
    }  ВРЕМЕННО*/
    public static boolean shouldShowInSession(WordItem word) {
        // ВРЕМЕННО: показываем ВСЕ слова, кроме выученных (stage >= 6)
        // Это нужно, чтобы проверить, что карточки вообще работают
        boolean isNotFullyLearned = word.getReviewStage() < MAX_STAGE;

        // Если слово выучено (stage >= 6) - не показываем
        if (!isNotFullyLearned) {
            Log.d(TAG, "❌ НЕ ПОКАЗЫВАЕМ (выучено): " + word.getWord());
            return false;
        }

        Log.d(TAG, "✅ ПОКАЗЫВАЕМ (временно все): " + word.getWord());
        return true;
    }
    /**
     * Получает текст для отображения следующего повторения
     */
    public static String getNextReviewText(android.content.Context context, WordItem word) {
        if (word.getReviewStage() == 0 && word.getConsecutiveShows() < 3) {
            int remainingShows = 3 - word.getConsecutiveShows();
            // Используем getString с параметром
            return context.getString(R.string.review_remaining_times, remainingShows);
        }

        if (word.getNextReviewDate() == null) return context.getString(R.string.review_now);

        if (word.getReviewStage() >= MAX_STAGE) {
            return context.getString(R.string.word_status_learned);
        }

        long diff = word.getNextReviewDate().getTime() - new Date().getTime();
        long days = diff / (1000 * 60 * 60 * 24);

        if (days <= 0) return context.getString(R.string.review_now);

        // Для дней используем getQuantityString (плюрализация)
        return context.getResources().getQuantityString(R.plurals.review_in_days, (int) days, (int) days);
    }
    /**
     * Проверяет, является ли слово новым
     */
    public static boolean isNewWord(WordItem word) {
        return word.getReviewStage() == 0 && word.getConsecutiveShows() == 0;
    }

    /**
     * Проверяет, является ли слово выученным
     */
    public static boolean isLearnedWord(WordItem word) {
        return word.getReviewStage() >= MAX_STAGE;
    }
}