package com.example.newwords;

import android.content.Context;
import android.util.Log;
import java.util.Calendar;
import java.util.Date;

public class SimpleRepetitionSystem {
    private static final String TAG = "RepetitionSystem";

    // Интервалы в днях для каждого этапа
    private static final int[] REVIEW_INTERVALS = {0, 1, 3, 7, 14, 30, 60};
    private static final int MAX_STAGE = REVIEW_INTERVALS.length - 1; // = 6

    /**
     * Обрабатывает ответ пользователя (ОСНОВНОЙ МЕТОД)
     */
    public static void processAnswer(WordItem word, boolean isCorrect) {
        Log.d(TAG, "=== ОБРАБОТКА ОТВЕТА ===");
        Log.d(TAG, "Слово: " + word.getWord() + ", правильный: " + isCorrect);
        Log.d(TAG, "ДО: stage=" + word.getReviewStage() +
                ", shows=" + word.getConsecutiveShows());

        if (isCorrect) {
            handleCorrectAnswer(word);
        } else {
            handleIncorrectAnswer(word);
        }

        // Обновляем дату последнего повтора
        word.setLastReviewed(new Date());
        word.setReviewCount(word.getReviewCount() + 1);

        Log.d(TAG, "ПОСЛЕ: stage=" + word.getReviewStage() +
                ", shows=" + word.getConsecutiveShows() +
                ", nextReview=" + word.getNextReviewDate());
    }

    /**
     * Обрабатывает правильный ответ
     */
    private static void handleCorrectAnswer(WordItem word) {
        int currentStage = word.getReviewStage();

        if (currentStage == 0) {
            // Новое слово - увеличиваем счетчик показов
            int newShows = word.getConsecutiveShows() + 1;
            word.setConsecutiveShows(newShows);

            Log.d(TAG, "✅ Новое слово показано " + newShows + "/3 раз");

            // Если показали 3 раза - переходим к этапу 1
            if (newShows >= 3) {
                word.setReviewStage(1);
                word.setConsecutiveShows(0);
                Log.d(TAG, "✅ 3 показа! Переход к этапу 1");
            } else {
                // Еще не набрали 3 показа - показываем снова сегодня
                word.setNextReviewDate(new Date(System.currentTimeMillis() - 1000));
                return;
            }
        } else {
            // Уже не новое слово - переходим к следующему этапу
            int newStage = Math.min(currentStage + 1, MAX_STAGE);
            word.setReviewStage(newStage);
            word.setConsecutiveShows(0);
            Log.d(TAG, "✅ Переход с этапа " + currentStage + " на этап " + newStage);
        }

        // Обновляем дату следующего повторения
        updateNextReviewDate(word);
    }

    /**
     * Обрабатывает неправильный ответ
     */
    private static void handleIncorrectAnswer(WordItem word) {
        int currentStage = word.getReviewStage();

        if (currentStage == 0) {
            // Новое слово - сбрасываем счетчик показов
            word.setConsecutiveShows(0);
            Log.d(TAG, "❌ Новое слово - сброс счетчика показов");
        } else {
            // Сбрасываем на предыдущий этап (но не ниже 1, чтобы не возвращать к 3 показам)
            int newStage = Math.max(1, currentStage - 1);
            word.setReviewStage(newStage);
            word.setConsecutiveShows(0);
            Log.d(TAG, "❌ Сброс с этапа " + currentStage + " на этап " + newStage);
        }

        // Неправильный ответ - повторяем сегодня
        word.setNextReviewDate(new Date(System.currentTimeMillis() - 1000));
    }

    /**
     * Обновляет дату следующего повторения на основе текущего этапа
     */
    private static void updateNextReviewDate(WordItem word) {
        int stage = word.getReviewStage();
        Calendar calendar = Calendar.getInstance();

        if (stage == 0) {
            // Новое слово - показать сразу
            calendar.add(Calendar.MINUTE, -1);
            Log.d(TAG, "🔄 Новое слово - показать сразу");
        } else if (stage <= REVIEW_INTERVALS.length - 1) {
            int intervalDays = REVIEW_INTERVALS[stage];
            calendar.add(Calendar.DAY_OF_YEAR, intervalDays);
            Log.d(TAG, "📅 Следующее повторение через " + intervalDays + " дней (этап " + stage + ")");
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, 30);
            Log.d(TAG, "📅 Следующее повторение через 30 дней");
        }

        word.setNextReviewDate(calendar.getTime());
    }

    /**
     * Проверяет, нужно ли показывать слово в текущей сессии
     */
    public static boolean shouldShowInSession(WordItem word) {
        if (word == null) return false;

        // Выученные слова не показываем
        if (word.getReviewStage() >= MAX_STAGE) {
            Log.d(TAG, "❌ НЕ ПОКАЗЫВАЕМ (выучено): " + word.getWord());
            return false;
        }

        // Новые слова показываем (stage=0, даже если нет даты)
        if (word.getReviewStage() == 0) {
            Log.d(TAG, "✅ НОВОЕ слово: " + word.getWord());
            return true;
        }

        // Остальные слова - проверяем дату
        if (word.getNextReviewDate() == null) {
            Log.d(TAG, "✅ Нет даты - показываем: " + word.getWord());
            return true;
        }

        boolean isDue = word.getNextReviewDate().before(new Date());
        Log.d(TAG, "Слово: " + word.getWord() +
                ", stage=" + word.getReviewStage() +
                ", isDue=" + isDue);

        return isDue;
    }

    /**
     * Проверяет, является ли слово выученным
     */
    public static boolean isLearnedWord(WordItem word) {
        return word.getReviewStage() >= MAX_STAGE;
    }

    /**
     * Получает текст для отображения следующего повторения
     */
    public static String getNextReviewText(Context context, WordItem word) {
        if (word == null) return "";

        if (word.getReviewStage() >= MAX_STAGE) {
            return context.getString(R.string.word_status_learned);
        }

        if (word.getReviewStage() == 0) {
            int remainingShows = 3 - word.getConsecutiveShows();
            if (remainingShows > 0) {
                return context.getString(R.string.review_remaining_times, remainingShows);
            } else {
                return context.getString(R.string.review_now);
            }
        }

        if (word.getNextReviewDate() == null) {
            return context.getString(R.string.review_now);
        }

        long diff = word.getNextReviewDate().getTime() - new Date().getTime();
        long days = diff / (1000 * 60 * 60 * 24);

        if (days <= 0) {
            return context.getString(R.string.review_now);
        } else if (days == 1) {
            return "Повторить завтра";
        } else {
            return "Повторить через " + days + " дней";
        }
    }

    /**
     * Получает статус слова (для отображения на карточке)
     */
    public static int getStatusTextRes(WordItem word) {
        if (word.getReviewStage() == 0) {
            if (word.getConsecutiveShows() == 0) {
                return R.string.word_status_new;
            } else {
                return R.string.word_status_learning;
            }
        } else if (word.getReviewStage() >= MAX_STAGE) {
            return R.string.word_status_learned;
        } else {
            return R.string.word_status_learning;
        }
    }
    /**
     * Проверяет, является ли слово новым
     */
    public static boolean isNewWord(WordItem word) {
        return word.getReviewStage() == 0 && word.getConsecutiveShows() == 0;
    }

    /**
     * Проверяет, нужно ли показать слово еще раз (для новых слов)
     */
    public static boolean needsMoreShows(WordItem word) {
        return word.getReviewStage() == 0 && word.getConsecutiveShows() < 3;
    }
}