package com.example.newwords;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder> {

    private List<WordLibrary> libraries;
    private Map<String, Boolean> activeLibraries;
    private OnLibraryActionListener listener;

    public LibraryAdapter(List<WordLibrary> libraries, OnLibraryActionListener listener) {
        this.libraries = new ArrayList<>(libraries);
        this.activeLibraries = new HashMap<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_library, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WordLibrary library = libraries.get(position);
        holder.bind(library);
    }

    @Override
    public int getItemCount() {
        return libraries.size();
    }

    /**
     * Обновляет список библиотек
     */
    public void updateLibraries(List<WordLibrary> newLibraries) {
        this.libraries.clear();
        this.libraries.addAll(newLibraries);
        notifyDataSetChanged();
    }

    /**
     * Обновляет карту активных библиотек
     */
    public void updateActiveLibraries(Map<String, Boolean> activeLibraries) {
        this.activeLibraries.clear();
        this.activeLibraries.putAll(activeLibraries);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView descriptionText;
        private TextView wordCountText;
        private TextView categoryText;
        private Switch activeSwitch;
        private ImageButton infoButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            nameText = itemView.findViewById(R.id.libraryName);
            descriptionText = itemView.findViewById(R.id.libraryDescription);
            wordCountText = itemView.findViewById(R.id.libraryWordCount);
            categoryText = itemView.findViewById(R.id.libraryCategory);
            activeSwitch = itemView.findViewById(R.id.librarySwitch);
            infoButton = itemView.findViewById(R.id.infoButton);
        }

        public void bind(WordLibrary library) {
            nameText.setText(library.getName());
            descriptionText.setText(library.getDescription());
            wordCountText.setText(library.getWordCount() + " слов");
            categoryText.setText(getCategoryDisplayName(library.getCategory()));

            // Устанавливаем состояние переключателя
            boolean isActive = activeLibraries.getOrDefault(library.getLibraryId(), false);
            activeSwitch.setChecked(isActive);

            // Обработчик переключателя
            activeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (listener != null) {
                        listener.onLibraryToggled(library.getLibraryId(), isChecked);
                    }
                }
            });

            // Обработчик кнопки информации
            infoButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLibraryInfoClicked(library);
                }
            });
        }

        /**
         * Возвращает читаемое название категории
         */
        private String getCategoryDisplayName(String category) {
            switch (category) {
                case "basic":
                    return "Базовый уровень";
                case "business":
                    return "Деловой английский";
                case "travel":
                    return "Путешествия";
                case "academic":
                    return "Академический";
                default:
                    return category;
            }
        }
    }

    public interface OnLibraryActionListener {
        void onLibraryToggled(String libraryId, boolean isActive);
        void onLibraryInfoClicked(WordLibrary library);
    }
}