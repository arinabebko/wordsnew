package com.example.newwords;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

public class AddWordWithLibraryDialog extends DialogFragment {

    private EditText wordEditText;
    private EditText translationEditText;
    private EditText noteEditText;
    private Spinner librarySpinner;
    private Button addButton;
    private Button cancelButton;
    private TextView noLibrariesText;
    private Button goToLibrariesButton;
    private ProgressBar libLoadingProgress;
    private OnWordAddedListener listener;
    private WordRepository wordRepository;
    private List<WordLibrary> userLibraries = new ArrayList<>();

    public interface OnWordAddedListener {
        void onWordAdded(String word, String translation, String note, String libraryId);
    }

    public static AddWordWithLibraryDialog newInstance() {
        return new AddWordWithLibraryDialog();
    }

    public void setOnWordAddedListener(OnWordAddedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // 1. Создаем билдер
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // 2. Инфлейтим макет
        View view = inflater.inflate(R.layout.dialog_add_word_with_library, null);

        // Устанавливаем фон программно или он уже есть в XML (лучше в XML)
        view.setBackgroundColor(0xFF211B20);

        wordRepository = new WordRepository(getContext());
        initViews(view);
        loadUserLibraries();

        // 3. УСТАНАВЛИВАЕМ ТОЛЬКО VIEW (БЕЗ TITLE!)
        builder.setView(view);

        Dialog dialog = builder.create();

        // 4. ГЛАВНОЕ: делаем системный фон прозрачным
        // Без этого вокруг твоего темного диалога будут белые углы
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        return dialog;
    }

    private void initViews(View view) {
        // Находим все View по ID из нового layout файла
        wordEditText = view.findViewById(R.id.wordEditText);
        translationEditText = view.findViewById(R.id.translationEditText);
        noteEditText = view.findViewById(R.id.noteEditText);
        librarySpinner = view.findViewById(R.id.librarySpinner);
        addButton = view.findViewById(R.id.addButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        noLibrariesText = view.findViewById(R.id.noLibrariesText);
        goToLibrariesButton = view.findViewById(R.id.goToLibrariesButton);
        libLoadingProgress = view.findViewById(R.id.libLoadingProgress); // Теперь он под рукой
        // Сначала скрываем спиннер и показываем сообщение
        librarySpinner.setVisibility(View.INVISIBLE);
        noLibrariesText.setVisibility(View.GONE);
        goToLibrariesButton.setVisibility(View.GONE);

        addButton.setOnClickListener(v -> addWord());
        cancelButton.setOnClickListener(v -> dismiss());

        goToLibrariesButton.setOnClickListener(v -> {
            // Переход к Fragment2 (библиотекам)
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new Fragment2())
                        .addToBackStack(null)
                        .commit();
            }
            dismiss();
        });
    }

    private void loadUserLibraries() {
        showLoadingState();
        wordRepository.getCustomLibraries(new WordRepository.OnLibrariesLoadedListener() {
            @Override
            public void onLibrariesLoaded(List<WordLibrary> libraries) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    userLibraries.clear();
                    userLibraries.addAll(libraries);
                    if (userLibraries.isEmpty()) {
                        showNoLibrariesState();
                    } else {
                        showLibrariesSpinner();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    // ЗАМЕНА: Сообщение об ошибке
                    Toast.makeText(getContext(), R.string.lib_load_error_toast, Toast.LENGTH_SHORT).show();
                    showNoLibrariesState();
                });
            }
        });
    }

    private void showLoadingState() {
        librarySpinner.setVisibility(View.INVISIBLE);

        if (libLoadingProgress != null) {
            libLoadingProgress.setVisibility(View.VISIBLE);
        }

        noLibrariesText.setVisibility(View.GONE);
        goToLibrariesButton.setVisibility(View.GONE);
        addButton.setEnabled(false);
        addButton.setAlpha(0.5f);
    }

    private void showNoLibrariesState() {
        librarySpinner.setVisibility(View.INVISIBLE);
        noLibrariesText.setVisibility(View.VISIBLE);
        goToLibrariesButton.setVisibility(View.VISIBLE);

        // Делаем кнопку добавления неактивной
        addButton.setEnabled(false);
        addButton.setAlpha(0.5f);
    }

    private void showLibrariesSpinner() {

        if (libLoadingProgress != null) {
            libLoadingProgress.setVisibility(View.GONE);
        }

        librarySpinner.setVisibility(View.VISIBLE);
        noLibrariesText.setVisibility(View.GONE);
        goToLibrariesButton.setVisibility(View.GONE);
        addButton.setEnabled(true);
        addButton.setAlpha(1.0f);

        List<String> libraryNames = new ArrayList<>();
        for (WordLibrary library : userLibraries) {
            // ЗАМЕНА: Форматированная строка для Spinner (например, "Имя (5 слов)")
            String nameWithCount = getString(R.string.lib_spinner_format,
                    library.getName(), library.getWordCount());
            libraryNames.add(nameWithCount);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                libraryNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        librarySpinner.setAdapter(adapter);
    }

    private void addWord() {
        String word = wordEditText.getText().toString().trim();
        String translation = translationEditText.getText().toString().trim();
        String note = noteEditText.getText().toString().trim();

        // ЗАМЕНА: Валидация полей
        if (word.isEmpty()) {
            Toast.makeText(getContext(), R.string.error_empty_word, Toast.LENGTH_SHORT).show();
            return;
        }

        if (translation.isEmpty()) {
            Toast.makeText(getContext(), R.string.error_empty_translation, Toast.LENGTH_SHORT).show();
            return;
        }

        if (userLibraries.isEmpty()) {
            Toast.makeText(getContext(), R.string.error_no_libs, Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPosition = librarySpinner.getSelectedItemPosition();
        if (selectedPosition >= 0 && selectedPosition < userLibraries.size()) {
            WordLibrary selectedLibrary = userLibraries.get(selectedPosition);
            if (listener != null) {
                listener.onWordAdded(word, translation, note, selectedLibrary.getLibraryId());
            }
        } else {
            Toast.makeText(getContext(), R.string.error_select_lib, Toast.LENGTH_SHORT).show();
            return;
        }
        dismiss();
    }
}