package com.example.newwords;

import android.util.Log;

import java.util.Calendar;
import java.util.Date;
public class SimpleRepetitionSystem {
    private static final String TAG = "RepetitionSystem";

    // Интервалы в днях: 0,1,3,7,14,30,60
    private static final int[] REVIEW_INTERVALS = {0, 1, 3, 7, 14, 30, 60};

    public static void processAnswer(WordItem word, boolean isLearned) {
        Log.d(TAG, "Обработка: " + word.getWord() +
                ", выучил: " + isLearned +
                ", этап: " + word.getReviewStage() +
                ", показов: " + word.getConsecutiveShows() +
                ", текущая сложность: " + word.getDifficulty());

        if (isLearned) {
            handleLearnedAnswer(word);
        } else {
            handleNotLearnedAnswer(word);
        }

        updateNextReviewDate(word);
        word.updateDifficultyBasedOnStage(); // Обновляем сложность ПОСЛЕ изменения этапа

        Log.d(TAG, "Результат: этап=" + word.getReviewStage() +
                ", показов=" + word.getConsecutiveShows() +
                ", сложность=" + word.getDifficulty());
    }

    private static void handleLearnedAnswer(WordItem word) {
        if (word.getReviewStage() == 0 && word.getConsecutiveShows() < 3) {
            // Новое слово - увеличиваем счетчик показов
            word.setConsecutiveShows(word.getConsecutiveShows() + 1);
            Log.d(TAG, "✅ Новое слово показано " + word.getConsecutiveShows() + "/3 раз");

            // Если показали 3 раза - переходим к первому интервалу
            if (word.getConsecutiveShows() >= 3) {
                word.setReviewStage(1); // Переходим к этапу 1 (1 день)
                word.setConsecutiveShows(0); // Сбрасываем счетчик
                Log.d(TAG, "✅ Слово показано 3 раза, переходим к этапу 1");
            }
        } else {
            // Уже не новое слово - переходим к следующему этапу
            if (word.getReviewStage() < REVIEW_INTERVALS.length - 1) {
                word.setReviewStage(word.getReviewStage() + 1);
                word.setConsecutiveShows(0); // Сбрасываем для следующего этапа
                Log.d(TAG, "✅ Переход к этапу " + word.getReviewStage());
            } else {
                Log.d(TAG, "✅ Слово полностью освоено!");
            }
        }
    }

    private static void handleNotLearnedAnswer(WordItem word) {
        // Полный сброс к началу
        word.setReviewStage(0);
        word.setConsecutiveShows(0);
        word.setDifficulty(3); // Возвращаем в "новые"
        Log.d(TAG, "❌ Полный сброс слова к началу");
    }

    private static void updateNextReviewDate(WordItem word) {
        Calendar calendar = Calendar.getInstance();

        if (word.getReviewStage() == 0 && word.getConsecutiveShows() < 3) {
            // Новое слово, которое нужно показать еще раз в ЭТОЙ ЖЕ сессии
            // Устанавливаем дату в прошлом, чтобы слово было готово к повторению сразу
            calendar.add(Calendar.MINUTE, -1);
            word.setNextReviewDate(calendar.getTime());
            Log.d(TAG, "📝 Покажем слово еще раз в этой сессии");
        } else {
            // Устанавливаем интервал по этапу
            int intervalDays = REVIEW_INTERVALS[word.getReviewStage()];
            calendar.add(Calendar.DAY_OF_YEAR, intervalDays);
            word.setNextReviewDate(calendar.getTime());
            Log.d(TAG, "📅 Следующее повторение через " + intervalDays + " дней");
        }
    }

    /**
     * Нужно ли показывать слово в текущей сессии
     */
    public static boolean shouldShowInSession(WordItem word) {
        // НЕ показываем если слово уже выучено (сложность 1)
        if (word.getDifficulty() == 1) {
            Log.d(TAG, "❌ Слово " + word.getWord() + " выучено, не показываем");
            return false;
        }

        // Показываем если слово готово к повторению по дате
        boolean isDue = word.isDueForReview();

        if (isDue) {
            Log.d(TAG, "✅ Слово " + word.getWord() + " готово к повторению");
            return true;
        } else {
            Log.d(TAG, "❌ Слово " + word.getWord() + " не готово к повторению, след. дата: " + word.getNextReviewDate());
            return false;
        }
    }

    public static String getNextReviewText(WordItem word) {
        if (word.getReviewStage() == 0 && word.getConsecutiveShows() < 3) {
            int remainingShows = 3 - word.getConsecutiveShows();
            return "Повторить: еще " + remainingShows + " раз";
        }

        if (word.getNextReviewDate() == null) return "Сейчас";

        if (word.getDifficulty() == 1) {
            return "Выучено!";
        }

        long diff = word.getNextReviewDate().getTime() - new Date().getTime();
        long days = diff / (1000 * 60 * 60 * 24);

        if (days <= 0) return "Сейчас";
        if (days == 1) return "Повторить: через 1 день";
        return "Повторить: через " + days + " дней";
    }
}