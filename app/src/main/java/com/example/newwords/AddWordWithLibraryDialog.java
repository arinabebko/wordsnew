package com.example.newwords;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // ВАЖНО: используем правильный layout файл
        View view = inflater.inflate(R.layout.dialog_add_word_with_library, null);
        view.setBackgroundColor(0xFF322b36); // HEX в формате ARGB
        // Инициализируем репозиторий
        wordRepository = new WordRepository(getContext());

        initViews(view);
        loadUserLibraries();

        builder.setView(view)
                .setTitle("Добавить новое слово");

        return builder.create();
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

        // Сначала скрываем спиннер и показываем сообщение
        librarySpinner.setVisibility(View.GONE);
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
                        // Нет библиотек - показываем сообщение и кнопку
                        showNoLibrariesState();
                    } else {
                        // Есть библиотеки - показываем спиннер
                        showLibrariesSpinner();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Ошибка загрузки библиотек", Toast.LENGTH_SHORT).show();
                    showNoLibrariesState();
                });
            }
        });
    }

    private void showLoadingState() {
        librarySpinner.setVisibility(View.GONE);
        noLibrariesText.setVisibility(View.GONE);
        goToLibrariesButton.setVisibility(View.GONE);

        // Временно деактивируем кнопку добавления
        addButton.setEnabled(false);
        addButton.setAlpha(0.5f);
    }

    private void showNoLibrariesState() {
        librarySpinner.setVisibility(View.GONE);
        noLibrariesText.setVisibility(View.VISIBLE);
        goToLibrariesButton.setVisibility(View.VISIBLE);

        // Делаем кнопку добавления неактивной
        addButton.setEnabled(false);
        addButton.setAlpha(0.5f);
    }

    private void showLibrariesSpinner() {
        noLibrariesText.setVisibility(View.GONE);
        goToLibrariesButton.setVisibility(View.GONE);
        librarySpinner.setVisibility(View.VISIBLE);

        // Активируем кнопку добавления
        addButton.setEnabled(true);
        addButton.setAlpha(1.0f);

        // Создаем адаптер для спиннера
        List<String> libraryNames = new ArrayList<>();
        for (WordLibrary library : userLibraries) {
            libraryNames.add(library.getName() + " (" + library.getWordCount() + " слов)");
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

        if (word.isEmpty()) {
            Toast.makeText(getContext(), "Введите слово", Toast.LENGTH_SHORT).show();
            return;
        }

        if (translation.isEmpty()) {
            Toast.makeText(getContext(), "Введите перевод", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userLibraries.isEmpty()) {
            Toast.makeText(getContext(), "Сначала создайте библиотеку", Toast.LENGTH_SHORT).show();
            return;
        }

        // Получаем выбранную библиотеку
        int selectedPosition = librarySpinner.getSelectedItemPosition();
        if (selectedPosition >= 0 && selectedPosition < userLibraries.size()) {
            WordLibrary selectedLibrary = userLibraries.get(selectedPosition);
            String selectedLibraryId = selectedLibrary.getLibraryId();

            if (listener != null) {
                listener.onWordAdded(word, translation, note, selectedLibraryId);
            }
        } else {
            Toast.makeText(getContext(), "Выберите библиотеку", Toast.LENGTH_SHORT).show();
            return;
        }

        dismiss();
    }
}