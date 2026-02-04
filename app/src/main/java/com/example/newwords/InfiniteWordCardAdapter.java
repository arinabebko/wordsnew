package com.example.newwords;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class InfiniteWordCardAdapter extends RecyclerView.Adapter<InfiniteWordCardAdapter.ViewHolder> {

    private List<WordItem> wordList;
    private OnWordActionListener listener;
    private static final int MAX_VALUE = 10000; // Большое число для бесконечности

    public InfiniteWordCardAdapter(List<WordItem> wordList, OnWordActionListener listener) {
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
        // Вычисляем реальную позицию в списке слов
        int realPosition = position % wordList.size();
        WordItem wordItem = wordList.get(realPosition);
        holder.bind(wordItem);
    }

    @Override
    public int getItemCount() {
        // Возвращаем большое число для создания иллюзии бесконечности
        return wordList.isEmpty() ? 0 : MAX_VALUE;
    }

    /**
     * Получить реальную позицию для бесконечного адаптера
     */
    public int getRealPosition(int position) {
        return position % wordList.size();
    }

    /**
     * Получить стартовую позицию в середине "бесконечного" списка
     */
    public int getStartPosition() {
        return MAX_VALUE / 2;
    }

    // ViewHolder остается таким же как в WordCardAdapter

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

                    // 1. ВМЕСТО background используем наш метод
                    updateStarIcon(newFavoriteState);

                    listener.onWordFavoriteToggled(currentWordItem, newFavoriteState);
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

            // 2. И здесь вызываем метод для иконки
            updateStarIcon(wordItem.isFavorite());
        }

        // 3. Добавляем сам метод смены иконок
        private void updateStarIcon(boolean isFavorite) {
            // Обязательно убираем тот самый желтый фон, если он застрял в памяти
            starButton.setBackgroundResource(android.R.color.transparent);

            if (isFavorite) {
                starButton.setImageResource(R.drawable.ic_full_heart_icon);
            } else {
                starButton.setImageResource(R.drawable.ic_empty_heart_icon);
            }
        }

    }

    public interface OnWordActionListener {
        void onWordLearned(WordItem word);

        void onWordNotLearned(WordItem word);

        void onWordFavoriteToggled(WordItem word, boolean isFavorite);
    }
}


