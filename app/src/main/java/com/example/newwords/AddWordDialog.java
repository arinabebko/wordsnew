package com.example.newwords;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class AddWordDialog extends DialogFragment {

    private EditText wordEditText;
    private EditText translationEditText;
    private EditText noteEditText;
    private Button addButton;
    private Button cancelButton;

    private String libraryId;
    private String libraryName;
    private OnWordAddedListener listener;

    public interface OnWordAddedListener {
        void onWordAdded(String word, String translation, String note);
    }

    public static AddWordDialog newInstance(String libraryId, String libraryName) {
        AddWordDialog dialog = new AddWordDialog();
        Bundle args = new Bundle();
        args.putString("libraryId", libraryId);
        args.putString("libraryName", libraryName);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnWordAddedListener(OnWordAddedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_word, null);

        if (getArguments() != null) {
            libraryId = getArguments().getString("libraryId");
            libraryName = getArguments().getString("libraryName");
        }

        initViews(view);

        // Используем форматную строку: "Добавить слово в: %1$s"
        String title = getString(R.string.word_add_dialog_title, libraryName);

        builder.setView(view)
                .setTitle(title);

        return builder.create();
    }

    private void initViews(View view) {
        wordEditText = view.findViewById(R.id.wordEditText);
        translationEditText = view.findViewById(R.id.translationEditText);
        noteEditText = view.findViewById(R.id.noteEditText);
        addButton = view.findViewById(R.id.addButton);
        cancelButton = view.findViewById(R.id.cancelButton);

        addButton.setOnClickListener(v -> addWord());
        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void addWord() {
        String word = wordEditText.getText().toString().trim();
        String translation = translationEditText.getText().toString().trim();
        String note = noteEditText.getText().toString().trim();

        if (word.isEmpty()) {
            // Заменили на ресурс
            Toast.makeText(getContext(), R.string.word_add_error_empty_word, Toast.LENGTH_SHORT).show();
            return;
        }

        if (translation.isEmpty()) {
            // Заменили на ресурс
            Toast.makeText(getContext(), R.string.word_add_error_empty_translation, Toast.LENGTH_SHORT).show();
            return;
        }

        if (note.isEmpty()) {
            // Заменили на ресурс
            note = getString(R.string.word_add_default_note);
        }

        if (listener != null) {
            listener.onWordAdded(word, translation, note);
        }

        dismiss();
    }
}