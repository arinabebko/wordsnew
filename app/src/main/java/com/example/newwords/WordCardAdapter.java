package com.example.newwords;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WordCardAdapter extends RecyclerView.Adapter<WordCardAdapter.ViewHolder> {

    // Список слов, которые будем показывать
    private List<WordItem> wordList;

    // Слушатель для обработки действий (клики, свайпы)
    private OnWordActionListener listener;

    // Интерфейс для связи с фрагментом
    public interface OnWordActionListener {
        void onWordLearned(WordItem word);

        void onWordNotLearned(WordItem word);

        void onWordFavoriteToggled(WordItem word, boolean isFavorite);
    }

    // Конструктор адаптера
    public WordCardAdapter(List<WordItem> wordList, OnWordActionListener listener) {
        this.wordList = wordList;
        this.listener = listener;
    }

    // Создает новую карточку (вызывается для первых нескольких карточек)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "Надуваем" макет карточки из XML
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    // Заполняет карточку данными (вызывается при прокрутке)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WordItem wordItem = wordList.get(position);

        // Если это ViewPager2 - используем обычный bind, если список - bindForList
        if (isViewPagerMode) {
            holder.bind(wordItem);
        } else {
            holder.bindForList(wordItem);
        }
    }

    // Добавьте поле и метод для режима:
    private boolean isViewPagerMode = true;

    public void setViewPagerMode(boolean isViewPagerMode) {
        this.isViewPagerMode = isViewPagerMode;
    }

    // Сколько всего карточек
    @Override
    public int getItemCount() {
        return wordList.size();
    }

    // ViewHolder - хранит ссылки на элементы одной карточки
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

            // Клик по сердечку
            starButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    WordItem word = wordList.get(position);
                    boolean newFavoriteState = !word.isFavorite();
                    word.setFavorite(newFavoriteState);
                    updateStarIcon(newFavoriteState);
                    listener.onWordFavoriteToggled(word, newFavoriteState);
                }
            });

            // Клик по всей карточке (перенос сюда из updateStarIcon)
            wordCard.setOnClickListener(v -> {
                // Анимация
                v.startAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.card_click));

                // Переключение видимости перевода
                v.postDelayed(() -> {
                    if (hintText.getVisibility() == View.VISIBLE) {
                        hintText.setVisibility(View.GONE);
                    } else {
                        hintText.setVisibility(View.VISIBLE);
                    }
                }, 50);
            });
        }

        public void bindForList(WordItem wordItem) {
            currentWordItem = wordItem;
            wordText.setText(wordItem.getWord());
            hintText.setText(wordItem.getTranslation());
            hintText.setVisibility(View.VISIBLE);
            updateStarIcon(wordItem.isFavorite());
        }

        public void bind(WordItem wordItem) {
            currentWordItem = wordItem;
            wordText.setText(wordItem.getWord());
            hintText.setText(wordItem.getTranslation());
            hintText.setVisibility(View.GONE);
            updateStarIcon(wordItem.isFavorite());
        }

        // Теперь этот метод отвечает ТОЛЬКО за картинку. Просто и чисто!
        private void updateStarIcon(boolean isFavorite) {
            if (isFavorite) {
                starButton.setImageResource(R.drawable.ic_full_heart_icon);
            } else {
                starButton.setImageResource(R.drawable.ic_empty_heart_icon);
            }
        }
    }
}

