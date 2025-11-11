package com.example.newwords;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WordListAdapter extends RecyclerView.Adapter<WordListAdapter.ViewHolder> {

    private List<WordItem> wordList;
    private WordRepository wordRepository;
    private boolean showDeleteButton;
    private OnWordDeleteListener deleteListener;
    private String currentLibraryId; // ID текущей библиотеки (если есть)

    public WordListAdapter(List<WordItem> wordList, WordRepository wordRepository, boolean showDeleteButton) {
        this.wordList = wordList;
        this.wordRepository = wordRepository;
        this.showDeleteButton = showDeleteButton;
    }

    // Конструктор с библиотекой
    public WordListAdapter(List<WordItem> wordList, WordRepository wordRepository,
                           boolean showDeleteButton, String libraryId) {
        this.wordList = wordList;
        this.wordRepository = wordRepository;
        this.showDeleteButton = showDeleteButton;
        this.currentLibraryId = libraryId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_word_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WordItem wordItem = wordList.get(position);
        holder.bind(wordItem);
    }

    @Override
    public int getItemCount() {
        return wordList.size();
    }

    public void updateWords(List<WordItem> newWords) {
        wordList.clear();
        wordList.addAll(newWords);
        notifyDataSetChanged();
    }

    // Удаляем слово из списка по позиции
    public void removeWord(int position) {
        if (position >= 0 && position < wordList.size()) {
            wordList.remove(position);
            notifyItemRemoved(position);
        }
    }

    // Устанавливаем слушатель удаления
    public void setOnWordDeleteListener(OnWordDeleteListener listener) {
        this.deleteListener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView wordText;
        private TextView translationText;
        private TextView noteText;
        private ImageButton favoriteButton;
        private ImageButton deleteButton;
        private WordItem currentWordItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            wordText = itemView.findViewById(R.id.wordText);
            translationText = itemView.findViewById(R.id.translationText);
            noteText = itemView.findViewById(R.id.noteText);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);

            // Обработчик избранного
            favoriteButton.setOnClickListener(v -> toggleFavorite());

            // Обработчик удаления
            deleteButton.setOnClickListener(v -> deleteWord());

            // Обработчик клика на всю строку
            itemView.setOnClickListener(v -> showWordDetails());
        }

        public void bind(WordItem wordItem) {
            currentWordItem = wordItem;

            wordText.setText(wordItem.getWord());
            translationText.setText(wordItem.getTranslation());

            // Устанавливаем примечание, если есть
            if (wordItem.getNote() != null && !wordItem.getNote().isEmpty()) {
                noteText.setText(wordItem.getNote());
                noteText.setVisibility(View.VISIBLE);
            } else {
                noteText.setVisibility(View.GONE);
            }

            // Обновляем иконку избранного
            updateFavoriteIcon(wordItem.isFavorite());

            // Показываем/скрываем кнопку удаления
            if (showDeleteButton && wordItem.isCustomWord()) {
                deleteButton.setVisibility(View.VISIBLE);
            } else {
                deleteButton.setVisibility(View.GONE);
            }
        }

        private void toggleFavorite() {
            if (currentWordItem != null) {
                boolean newFavoriteState = !currentWordItem.isFavorite();
                currentWordItem.setFavorite(newFavoriteState);

                updateFavoriteIcon(newFavoriteState);
                wordRepository.updateWord(currentWordItem);

                String message = newFavoriteState ? "★ Добавлено в избранное" : "☆ Убрано из избранного";
                Toast.makeText(itemView.getContext(), message + ": " + currentWordItem.getWord(), Toast.LENGTH_SHORT).show();
            }
        }

        private void deleteWord() {
            if (currentWordItem != null && currentWordItem.getWordId() != null) {
                int position = getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                // Показываем подтверждение удаления
                showDeleteConfirmationDialog(position);
            }
        }

        private void showDeleteConfirmationDialog(int position) {
            new android.app.AlertDialog.Builder(itemView.getContext())
                    .setTitle("Удаление слова")
                    .setMessage("Вы уверены, что хотите удалить слово \"" + currentWordItem.getWord() + "\"?")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        performDelete(position);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        }

        private void performDelete(int position) {
            if (currentWordItem == null || currentWordItem.getWordId() == null) return;

            // Определяем тип слова и вызываем соответствующий метод удаления
            if (currentWordItem.isCustomWord()) {
                if (currentWordItem.getLibraryId() != null && !currentWordItem.getLibraryId().isEmpty()) {
                    // Слово из пользовательской библиотеки
                    wordRepository.deleteWordFromLibrary(
                            currentWordItem.getLibraryId(),
                            currentWordItem.getWordId(),
                            () -> {
                                // Успех
                                handleDeleteSuccess(position);
                                Toast.makeText(itemView.getContext(),
                                        "Слово удалено", Toast.LENGTH_SHORT).show();
                            },
                            e -> {
                                // Ошибка
                                Toast.makeText(itemView.getContext(),
                                        "Ошибка удаления слова", Toast.LENGTH_SHORT).show();
                            }
                    );
                } else {
                    // Обычное кастомное слово
                    wordRepository.deleteCustomWord(
                            currentWordItem.getWordId(),
                            () -> {
                                // Успех
                                handleDeleteSuccess(position);
                                Toast.makeText(itemView.getContext(),
                                        "Слово удалено", Toast.LENGTH_SHORT).show();
                            },
                            e -> {
                                // Ошибка
                                Toast.makeText(itemView.getContext(),
                                        "Ошибка удаления слова", Toast.LENGTH_SHORT).show();
                            }
                    );
                }
            } else {
                // Слова из публичных библиотек нельзя удалять
                Toast.makeText(itemView.getContext(),
                        "Нельзя удалять слова из публичных библиотек", Toast.LENGTH_SHORT).show();
            }
        }

        private void handleDeleteSuccess(int position) {
            // Удаляем из списка
            removeWord(position);

            // Уведомляем слушателя
            if (deleteListener != null) {
                deleteListener.onWordDeleted(currentWordItem);
            }
        }

        private void showWordDetails() {
            if (currentWordItem != null) {
                String details = currentWordItem.getWord() + " - " + currentWordItem.getTranslation();
                if (currentWordItem.getNote() != null && !currentWordItem.getNote().isEmpty()) {
                    details += "\n\n" + currentWordItem.getNote();
                }
                Toast.makeText(itemView.getContext(), details, Toast.LENGTH_LONG).show();
            }
        }

        private void updateFavoriteIcon(boolean isFavorite) {
            if (isFavorite) {
                favoriteButton.setBackgroundColor(0x30FFD700); // Желтый фон
                favoriteButton.setColorFilter(0xFFFFD700); // Желтая иконка
            } else {
                favoriteButton.setBackgroundColor(0x00000000); // Прозрачный фон
                favoriteButton.setColorFilter(0xFF888888); // Сервая иконка
            }
        }
    }

    // Интерфейс для обработки удаления слов
    public interface OnWordDeleteListener {
        void onWordDeleted(WordItem word);
    }
}