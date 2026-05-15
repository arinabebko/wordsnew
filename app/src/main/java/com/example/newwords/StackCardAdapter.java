package com.example.newwords;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StackCardAdapter extends RecyclerView.Adapter<StackCardAdapter.ViewHolder> {

    private static final String PREFS_NAME = "StackCardPrefs";
    private static final String KEY_SPEAK_ENABLED = "speak_enabled";

    private List<WordItem> wordList;
    private OnCardActionListener listener;
    private int currentPosition = 0;
    private WordRepository wordRepository;
    private Context context;
    private TextToSpeechManager ttsManager;
    private boolean isSpeakEnabled = false; // 🔇 ПО УМОЛЧАНИЮ ВЫКЛЮЧЕНА!

    public StackCardAdapter(List<WordItem> wordList, OnCardActionListener listener, WordRepository wordRepository) {
        this.wordList = wordList;
        this.listener = listener;
        this.wordRepository = wordRepository;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.context = recyclerView.getContext();
        this.ttsManager = TextToSpeechManager.getInstance(context);

        // Загружаем настройку озвучки (по умолчанию false)
        loadSpeakPreference();
    }

    /**
     * Загружает настройку озвучки из SharedPreferences
     */
    private void loadSpeakPreference() {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            isSpeakEnabled = prefs.getBoolean(KEY_SPEAK_ENABLED, false); // ПО УМОЛЧАНИЮ false
            Log.d("StackCardAdapter", "Настройка озвучки загружена: " + (isSpeakEnabled ? "ВКЛ" : "ВЫКЛ"));
        }
    }

    /**
     * Сохраняет настройку озвучки
     */
    private void saveSpeakPreference(boolean enabled) {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_SPEAK_ENABLED, enabled).apply();
            Log.d("StackCardAdapter", "Настройка озвучки сохранена: " + (enabled ? "ВКЛ" : "ВЫКЛ"));
        }
    }

    /**
     * Переключает состояние озвучки
     */
    public void toggleSpeak() {
        isSpeakEnabled = !isSpeakEnabled;
        saveSpeakPreference(isSpeakEnabled);

        // Уведомляем все видимые карточки об изменении иконки
        notifyItemRangeChanged(0, getItemCount());

        String message = isSpeakEnabled ? "🔊 Озвучка включена" : "🔇 Озвучка выключена";
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Проверяет, включена ли озвучка
     */
    public boolean isSpeakEnabled() {
        return isSpeakEnabled;
    }

    /**
     * Озвучивает слово (если включена озвучка)
     */
    private void speakWord(String word) {
        if (isSpeakEnabled && ttsManager != null) {
            Log.d("StackCardAdapter", "🔊 Озвучиваем слово: " + word);
            ttsManager.speak(word);
        }
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
        if (position == 0 && currentPosition < wordList.size()) {
            WordItem wordItem = wordList.get(currentPosition);
            holder.bind(wordItem);
            holder.itemView.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return Math.min(wordList.size() - currentPosition, 3);
    }

    private void moveToNextCard() {
        currentPosition++;
        notifyDataSetChanged();

        if (currentPosition >= wordList.size()) {
            if (listener != null) {
                listener.onAllCardsCompleted();
            }
        }
    }

    public WordItem getCurrentWord() {
        if (currentPosition < wordList.size()) {
            return wordList.get(currentPosition);
        }
        return null;
    }

    public int getCurrentProgress() {
        return currentPosition;
    }

    public int getTotalCards() {
        return wordList.size();
    }

    public void swipeRight() {
        if (currentPosition < wordList.size()) {
            WordItem currentWord = wordList.get(currentPosition);

            Log.d("PROGRESS_DEBUG", "🔴 1. swipeRight вызван для: " + currentWord.getWord());
            Log.d("PROGRESS_DEBUG", "🔴 1a. Было ли слово выучено? " + SimpleRepetitionSystem.isLearnedWord(currentWord));

            SimpleRepetitionSystem.processAnswer(currentWord, true);

            Log.d("PROGRESS_DEBUG", "🔴 2. После processAnswer, reviewStage=" + currentWord.getReviewStage());
            Log.d("PROGRESS_DEBUG", "🔴 2a. Теперь слово выучено? " + SimpleRepetitionSystem.isLearnedWord(currentWord));

            wordRepository.updateWord(currentWord);

            if (listener != null) {
                listener.onCardLearned(currentWord);
                Log.d("PROGRESS_DEBUG", "🔴 3. listener.onCardLearned вызван");
            }
            moveToNextCard();
        }
    }

    public void swipeLeft() {
        if (currentPosition < wordList.size()) {
            WordItem currentWord = wordList.get(currentPosition);

            SimpleRepetitionSystem.processAnswer(currentWord, false);
            wordRepository.updateWord(currentWord);

            if (listener != null) {
                listener.onCardNotLearned(currentWord);
            }
            moveToNextCard();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView wordText;
        private TextView hintText;
        private TextView statusText;
        private TextView nextReviewText;
        private ImageButton starButton;
        private ImageButton speakToggleButton; // 🆕 Кнопка озвучки
        private WordItem currentWordItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            wordText = itemView.findViewById(R.id.wordText);
            hintText = itemView.findViewById(R.id.hintText);
            statusText = itemView.findViewById(R.id.statusText);
            nextReviewText = itemView.findViewById(R.id.nextReviewText);
            starButton = itemView.findViewById(R.id.starButton);
            speakToggleButton = itemView.findViewById(R.id.speakToggleButton); // 🆕

            setupClickListeners();
        }

        public void bind(WordItem wordItem) {
            currentWordItem = wordItem;
            wordText.setText(wordItem.getWord());
            hintText.setText(wordItem.getTranslation());
            hintText.setVisibility(View.GONE);

            updateRepetitionUI(wordItem);
            updateStarIcon(wordItem.isFavorite());
            updateSpeakButtonIcon(); // 🆕 Обновляем иконку кнопки озвучки
        }

        private void updateStarIcon(boolean isFavorite) {
            starButton.setBackgroundResource(android.R.color.transparent);
            if (isFavorite) {
                starButton.setImageResource(R.drawable.ic_full_heart_icon);
                starButton.setColorFilter(0xFFCE5D5D);
            } else {
                starButton.setImageResource(R.drawable.ic_empty_heart_icon);
                starButton.setColorFilter(0xFFFFFFFF);
            }
        }

        /**
         * 🆕 Обновляет иконку кнопки озвучки в зависимости от состояния
         */
        private void updateSpeakButtonIcon() {
            if (speakToggleButton != null) {
                if (isSpeakEnabled) {
                    speakToggleButton.setImageResource(R.drawable.ic_volume_up);
                    speakToggleButton.setColorFilter(0xFF4CAF50); // Зеленый - включено
                } else {
                    speakToggleButton.setImageResource(R.drawable.ic_volume_off);
                    speakToggleButton.setColorFilter(0xFFFFFFFF); // Белый - выключено
                }
            }
        }

        private void updateRepetitionUI(WordItem word) {
            if (statusText != null) {
                statusText.setText(word.getStatusText());
                statusText.setBackgroundColor(getStatusColor(word));
            }

            if (nextReviewText != null && itemView.getContext() != null) {
                nextReviewText.setText(SimpleRepetitionSystem.getNextReviewText(itemView.getContext(), word));
            }

            Log.d("CardDebug", "Слово: " + word.getWord() +
                    ", этап: " + word.getReviewStage());
        }

        private int getStatusColor(WordItem word) {
            switch(word.getReviewStage()) {
                case 0: return 0xFF625fba; // Новое
                case 1:
                case 2:
                case 3: return 0xFFbabba9; // Изучается
                case 4:
                case 5:
                case 6: return 0xFF4CAF50; // Выучено
                default: return 0xFF625fba;
            }
        }

        private void setupClickListeners() {
            // 🆕 Кнопка включения/выключения озвучки
            speakToggleButton.setOnClickListener(v -> {
                toggleSpeak();
                updateSpeakButtonIcon();
            });

            // Кнопка избранного
            starButton.setOnClickListener(v -> {
                if (currentWordItem != null && listener != null) {
                    boolean newFavoriteState = !currentWordItem.isFavorite();
                    currentWordItem.setFavorite(newFavoriteState);
                    updateStarIcon(newFavoriteState);
                    wordRepository.updateWord(currentWordItem);
                    listener.onCardFavoriteToggled(currentWordItem, newFavoriteState);
                }
            });

            // 🆕 Клик по карточке - показывает перевод и ОЗВУЧИВАЕТ (если включено)
            itemView.findViewById(R.id.wordCard).setOnClickListener(v -> {
                // Переключаем видимость перевода
                if (hintText.getVisibility() == View.VISIBLE) {
                    hintText.setVisibility(View.GONE);
                } else {
                    hintText.setVisibility(View.VISIBLE);

                    // 🆕 Если озвучка включена - произносим слово при показе перевода
                    if (isSpeakEnabled && currentWordItem != null) {
                        speakWord(currentWordItem.getWord());
                    }
                }
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