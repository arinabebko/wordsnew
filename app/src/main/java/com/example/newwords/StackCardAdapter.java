package com.example.newwords;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StackCardAdapter extends RecyclerView.Adapter<StackCardAdapter.ViewHolder> {

    private List<WordItem> wordList;
    private OnCardActionListener listener;
    private int currentPosition = 0;

    public StackCardAdapter(List<WordItem> wordList, OnCardActionListener listener) {
        this.wordList = wordList;
        this.listener = listener;
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
     * Свайп вправо - слово выучено
     */
    public void swipeRight() {
        if (currentPosition < wordList.size()) {
            WordItem currentWord = wordList.get(currentPosition);
            if (listener != null) {
                listener.onCardLearned(currentWord);
            }
            moveToNextCard();
        }
    }

    /**
     * Свайп влево - слово не выучено
     */
    public void swipeLeft() {
        if (currentPosition < wordList.size()) {
            WordItem currentWord = wordList.get(currentPosition);
            if (listener != null) {
                listener.onCardNotLearned(currentWord);
            }
            moveToNextCard();
        }
    }

    /**
     * Переход к следующей карточке
     */
    private void moveToNextCard() {
        currentPosition++;
        notifyDataSetChanged();

        // Если карточки закончились
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

    // ViewHolder
    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView wordText;
        private TextView hintText;
        private ImageButton starButton;
        private View wordCard;
        private WordItem currentWordItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            wordText = itemView.findViewById(R.id.wordText);
            hintText = itemView.findViewById(R.id.hintText);
            starButton = itemView.findViewById(R.id.starButton);
            wordCard = itemView.findViewById(R.id.wordCard);

            starButton.setOnClickListener(v -> {
                if (currentWordItem != null && listener != null) {
                    boolean newFavoriteState = !currentWordItem.isFavorite();
                    currentWordItem.setFavorite(newFavoriteState);

                    if (newFavoriteState) {
                        starButton.setBackgroundColor(0x30FFD700);
                    } else {
                        starButton.setBackgroundColor(0x00000000);
                    }

                    listener.onCardFavoriteToggled(currentWordItem, newFavoriteState);
                }
            });

            wordCard.setOnClickListener(v -> {
                if (hintText.getVisibility() == View.VISIBLE) {
                    hintText.setVisibility(View.GONE);
                } else {
                    hintText.setVisibility(View.VISIBLE);
                }
            });
        }

        public void bind(WordItem wordItem) {
            currentWordItem = wordItem;
            wordText.setText(wordItem.getWord());
            hintText.setText(wordItem.getTranslation());
            hintText.setVisibility(View.GONE);

            if (wordItem.isFavorite()) {
                starButton.setBackgroundColor(0x30FFD700);
            } else {
                starButton.setBackgroundColor(0x00000000);
            }
        }
    }

    public interface OnCardActionListener {
        void onCardLearned(WordItem word);
        void onCardNotLearned(WordItem word);
        void onCardFavoriteToggled(WordItem word, boolean isFavorite);
        void onAllCardsCompleted();
    }
}