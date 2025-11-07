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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class AddLibraryDialog extends DialogFragment {

    private EditText nameEditText;
    private EditText descriptionEditText;
    private Spinner categorySpinner;
    private Button createButton;
    private Button cancelButton;

    private OnLibraryCreatedListener listener;

    public interface OnLibraryCreatedListener {
        void onLibraryCreated(String name, String description, String category);
    }

    public void setOnLibraryCreatedListener(OnLibraryCreatedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_library, null);

        initViews(view);
        setupCategorySpinner();

        builder.setView(view)
                .setTitle("Создать новую библиотеку");

        return builder.create();
    }

    private void initViews(View view) {
        nameEditText = view.findViewById(R.id.nameEditText);
        descriptionEditText = view.findViewById(R.id.descriptionEditText);
        categorySpinner = view.findViewById(R.id.categorySpinner);
        createButton = view.findViewById(R.id.createButton);
        cancelButton = view.findViewById(R.id.cancelButton);

        createButton.setOnClickListener(v -> createLibrary());
        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void setupCategorySpinner() {
        String[] categories = {"basic", "business", "travel", "food", "academic", "custom"};
        String[] displayCategories = {"Базовый", "Деловой", "Путешествия", "Еда", "Академический", "Другое"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                displayCategories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void createLibrary() {
        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String category = getSelectedCategory();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Введите название библиотеки", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(getContext(), "Введите описание библиотеки", Toast.LENGTH_SHORT).show();
            return;
        }

        if (listener != null) {
            listener.onLibraryCreated(name, description, category);
        }

        dismiss();
    }

    private String getSelectedCategory() {
        String[] categories = {"basic", "business", "travel", "food", "academic", "custom"};
        int position = categorySpinner.getSelectedItemPosition();
        return categories[position];
    }
}