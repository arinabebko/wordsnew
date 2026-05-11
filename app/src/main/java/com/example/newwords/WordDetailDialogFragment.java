package com.example.newwords;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WordDetailDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_WORD = "word";
    private WordItem wordItem;
    private OnWordActionListener actionListener;

    public interface OnWordActionListener {
        void onEditWord(WordItem word);
        void onDeleteWord(WordItem word);
        void onPlayPronunciation(String word);
    }

    public static WordDetailDialogFragment newInstance(WordItem wordItem) {
        WordDetailDialogFragment fragment = new WordDetailDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_WORD, wordItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            wordItem = (WordItem) getArguments().getSerializable(ARG_WORD);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_word_detail, container, false);

        TextView wordText = view.findViewById(R.id.wordText);
        TextView translationText = view.findViewById(R.id.translationText);
        TextView noteText = view.findViewById(R.id.noteText);
        TextView dateAddedText = view.findViewById(R.id.dateAddedText);
        TextView libraryText = view.findViewById(R.id.libraryText);
        Button editButton = view.findViewById(R.id.editButton);
        Button deleteButton = view.findViewById(R.id.deleteButton);
        Button playButton = view.findViewById(R.id.playButton);

        if (wordItem != null) {
            wordText.setText(wordItem.getWord());
            translationText.setText(wordItem.getTranslation());

            // Отображение заметки
            if (!TextUtils.isEmpty(wordItem.getNote())) {
                noteText.setText(wordItem.getNote());
                noteText.setVisibility(View.VISIBLE);
            } else {
                noteText.setVisibility(View.GONE);
            }

            // Отображение даты
            if (wordItem.getCreatedAt() != null) {
                dateAddedText.setText("Добавлено: " + formatDate(wordItem.getCreatedAt().getTime()));
                dateAddedText.setVisibility(View.VISIBLE);
            } else {
                dateAddedText.setVisibility(View.GONE);
            }

            // Отображение типа слова и библиотеки
            if (wordItem.isCustomWord()) {
                // Пользовательское слово
                if (!TextUtils.isEmpty(wordItem.getLibraryId())) {
                    libraryText.setText("📚 Моя коллекция");
                } else {
                    libraryText.setText("✏️ Мое слово");
                }
                libraryText.setVisibility(View.VISIBLE);

                // ПОКАЗЫВАЕМ кнопки редактирования и удаления ТОЛЬКО для пользовательских слов
                editButton.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);
            } else {
                // Публичное слово из библиотеки
                if (!TextUtils.isEmpty(wordItem.getLibraryId())) {
                    libraryText.setText("📖 Публичная коллекция");
                } else {
                    libraryText.setText("🌐 Из библиотеки");
                }
                libraryText.setVisibility(View.VISIBLE);

                // СКРЫВАЕМ кнопки редактирования и удаления для публичных слов
                editButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);
            }
        }

        editButton.setOnClickListener(v -> {
            if (actionListener != null && wordItem != null && wordItem.isCustomWord()) {
                actionListener.onEditWord(wordItem);
            }
            dismiss();
        });

        deleteButton.setOnClickListener(v -> {
            if (actionListener != null && wordItem != null && wordItem.isCustomWord()) {
                actionListener.onDeleteWord(wordItem);
            }
            dismiss();
        });

        playButton.setOnClickListener(v -> {
            if (actionListener != null && wordItem != null) {
                actionListener.onPlayPronunciation(wordItem.getWord());
            }
        });

        return view;
    }

    public void setOnWordActionListener(OnWordActionListener listener) {
        this.actionListener = listener;
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}