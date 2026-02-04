package com.example.newwords;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StackCardAdapter extends RecyclerView.Adapter<StackCardAdapter.ViewHolder> {

    private List<WordItem> wordList;
    private OnCardActionListener listener;
    private int currentPosition = 0;

    // ДОБАВЬ ЭТО ПОЛЕ:
    private WordRepository wordRepository;

    // ОБНОВИ КОНСТРУКТОР:
    public StackCardAdapter(List<WordItem> wordList, OnCardActionListener listener, WordRepository wordRepository) {
        this.wordList = wordList;
        this.listener = listener;
        this.wordRepository = wordRepository; // ДОБАВЬ ЭТУ СТРОКУ
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Показываем только текущую карточку
        if (position == 0 && currentPosition < wordList.size()) {
            WordItem wordItem = wordList.get(currentPosition);
            holder.bind(wordItem);
            holder.itemView.setVisibility(View.VISIBLE);
        } else {
            // Остальные карточки скрыты
            holder.itemView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        // Всегда показываем 3 карточки для анимаций, но только одна видима
        return Math.min(wordList.size() - currentPosition, 3);
    }

    /**
     * Переход к следующей карточке
     */
    private void moveToNextCard() {
        currentPosition++;
        notifyDataSetChanged();

        // Если карточки закончились - уведомляем сразу
        if (currentPosition >= wordList.size()) {
            if (listener != null) {
                listener.onAllCardsCompleted();
            }
        }
    }

    /**
     * Получить текущее слово
     */
    public WordItem getCurrentWord() {
        if (currentPosition < wordList.size()) {
            return wordList.get(currentPosition);
        }
        return null;
    }

    /**
     * Получить прогресс
     */
    public int getCurrentProgress() {
        return currentPosition;
    }

    public int getTotalCards() {
        return wordList.size();
    }

    /**
     * Свайп вправо - выучил
     */
    public void swipeRight() {
        if (currentPosition < wordList.size()) {
            WordItem currentWord = wordList.get(currentPosition);

            // Обрабатываем в системе повторений
            SimpleRepetitionSystem.processAnswer(currentWord, true);

            // Сохраняем в базу
            wordRepository.updateWord(currentWord);

            if (listener != null) {
                listener.onCardLearned(currentWord);
            }
            moveToNextCard();
        }
    }

    /**
     * Свайп влево - не выучил
     */
    public void swipeLeft() {
        if (currentPosition < wordList.size()) {
            WordItem currentWord = wordList.get(currentPosition);

            // Обрабатываем в системе повторений
            SimpleRepetitionSystem.processAnswer(currentWord, false);

            // Сохраняем в базу
            wordRepository.updateWord(currentWord);

            if (listener != null) {
                listener.onCardNotLearned(currentWord);
            }
            moveToNextCard();
        }
    }

    // ViewHolder
    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView wordText;
        private TextView hintText;
        private TextView statusText;
        private TextView nextReviewText;
        private ImageButton starButton;
        private WordItem currentWordItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            wordText = itemView.findViewById(R.id.wordText);
            hintText = itemView.findViewById(R.id.hintText);
            statusText = itemView.findViewById(R.id.statusText);
            nextReviewText = itemView.findViewById(R.id.nextReviewText);
            starButton = itemView.findViewById(R.id.starButton);

            setupClickListeners();
        }

        public void bind(WordItem wordItem) {
            currentWordItem = wordItem;
            wordText.setText(wordItem.getWord());
            hintText.setText(wordItem.getTranslation());
            hintText.setVisibility(View.GONE);

            updateRepetitionUI(wordItem);

            // ВМЕСТО setBackgroundColor используем иконку:
            updateStarIcon(wordItem.isFavorite());
        }

        // НОВЫЙ МЕТОД:
        private void updateStarIcon(boolean isFavorite) {
            starButton.setBackgroundResource(android.R.color.transparent);
            if (isFavorite) {
                starButton.setImageResource(R.drawable.ic_full_heart_icon);
                // Если в XML остался tint, он может мешать.
                // Можно управлять цветом программно:
                starButton.setColorFilter(0xFFCE5D5D); // Красный
            } else {
                starButton.setImageResource(R.drawable.ic_empty_heart_icon);
                starButton.setColorFilter(0xFFFFFFFF); // Белый
            }
        }


        private void updateRepetitionUI(WordItem word) {
            if (statusText != null) {
                statusText.setText(word.getStatusText());
                statusText.setBackgroundColor(getStatusColor(word));
            }
            if (nextReviewText != null) {
                nextReviewText.setText(SimpleRepetitionSystem.getNextReviewText(word));
            }


            // Отладочная информация
            Log.d("CardDebug", "Слово: " + word.getWord() +
                    ", этап: " + word.getReviewStage() +
                    ", показов: " + word.getConsecutiveShows() +
                    ", сложность: " + word.getDifficulty() +
                    ", след. дата: " + word.getNextReviewDate());
        }

        // ВРЕМЕННЫЕ МЕТОДЫ (пока нет полноценной системы):
        private String getStatusText(WordItem word) {
            // Временная логика - используем difficulty
            if (word.getDifficulty() == 3) return "НОВОЕ СЛОВО";
            if (word.getDifficulty() == 2) return "ИЗУЧАЕТСЯ";
            if (word.getDifficulty() == 1) return "ВЫУЧЕНО";
            return "НЕИЗВЕСТНО";
        }

        private String getNextReviewText(WordItem word) {
            // Временная заглушка
            if (word.getDifficulty() == 3) return "Повторить: сегодня";
            if (word.getDifficulty() == 2) return "Повторить: через 3 дня";
            if (word.getDifficulty() == 1) return "Повторить: через 7 дней";
            return "Следующее: скоро";
        }

        private int getStatusColor(WordItem word) {
            // Цвета в твоей теме
            if (word.getDifficulty() == 3) return 0xFF625fba; // Фиолетовый - новые
            if (word.getDifficulty() == 2) return 0xFFbabba9; // Серый - изучается
            if (word.getDifficulty() == 1) return 0xFF4CAF50; // Зеленый - выучено
            return 0xFF625fba; // Фиолетовый по умолчанию
        }

        private void setupClickListeners() {
            starButton.setOnClickListener(v -> {
                if (currentWordItem != null && listener != null) {
                    boolean newFavoriteState = !currentWordItem.isFavorite();
                    currentWordItem.setFavorite(newFavoriteState);

                    // Теперь используем только метод с иконками
                    updateStarIcon(newFavoriteState);

                    wordRepository.updateWord(currentWordItem);
                    listener.onCardFavoriteToggled(currentWordItem, newFavoriteState);
                }
            });

            itemView.findViewById(R.id.wordCard).setOnClickListener(v -> {
                hintText.setVisibility(hintText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            });
        }


    }

    public interface OnCardActionListener {
        void onCardLearned(WordItem word);
        void onCardNotLearned(WordItem word);
        void onCardFavoriteToggled(WordItem word, boolean isFavorite);
        void onAllCardsCompleted();
    }
}