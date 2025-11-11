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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Находим все элементы в макете карточки
            wordText = itemView.findViewById(R.id.wordText);
            hintText = itemView.findViewById(R.id.hintText);
            starButton = itemView.findViewById(R.id.starButton);
            wordCard = itemView.findViewById(R.id.wordCard);

            // Обработчик клика на звезду (избранное)
            starButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    WordItem word = wordList.get(position);
                    boolean newFavoriteState = !word.isFavorite();
                    word.setFavorite(newFavoriteState);

                    // Меняем иконку звезды
                    updateStarIcon(newFavoriteState);

                    // Сообщаем слушателю
                    listener.onWordFavoriteToggled(word, newFavoriteState);
                }
            });

            // Обработчик клика на карточку (показать/скрыть перевод)
            wordCard.setOnClickListener(v -> {
                if (hintText.getVisibility() == View.VISIBLE) {
                    hintText.setVisibility(View.GONE);  // Скрываем
                } else {
                    hintText.setVisibility(View.VISIBLE); // Показываем
                }
            });



        }
        public void bindForList(WordItem wordItem) {
            currentWordItem = wordItem;
            wordText.setText(wordItem.getWord());
            hintText.setText(wordItem.getTranslation());
            hintText.setVisibility(View.VISIBLE); // В режиме списка всегда показываем перевод

            if (wordItem.isFavorite()) {
                starButton.setBackgroundColor(0x30FFD700);
            } else {
                starButton.setBackgroundColor(0x00000000);
            }
        }
        // Заполняем карточку данными слова
        public void bind(WordItem wordItem) {
            wordText.setText(wordItem.getWord());
            hintText.setText(wordItem.getTranslation());
            hintText.setVisibility(View.GONE); // Сначала скрываем перевод

            // Устанавливаем правильную иконку звезды
            updateStarIcon(wordItem.isFavorite());
            updateStarIcon(wordItem.isFavorite());

            // Сохраняем ссылку на текущее слово
            currentWordItem = wordItem;

        }
        private WordItem currentWordItem;
        // Обновляет иконку звезды в зависимости от состояния
        private void updateStarIcon(boolean isFavorite) {
            if (isFavorite) {
                // starButton.setImageResource(R.drawable.ic_star_filled);
                starButton.setBackgroundColor(0xFFFFFF00); // Желтый цвет
            } else {
                starButton.setImageResource(R.drawable.ic_star);
            }


            wordCard.setOnClickListener(v -> {
                // Анимация нажатия
                v.startAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.card_click));

                // Показ/скрытие перевода с задержкой
                v.postDelayed(() -> {
                    if (hintText.getVisibility() == View.VISIBLE) {
                        hintText.setVisibility(View.GONE);
                    } else {
                        hintText.setVisibility(View.VISIBLE);
                    }
                }, 50);
            });
        }




    }

}

