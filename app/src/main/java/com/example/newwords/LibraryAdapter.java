package com.example.newwords;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    public void filterLibraries(String query, List<WordLibrary> allLibraries, String languageCode) {
        this.libraries.clear();

        for (WordLibrary library : allLibraries) {
            // Проверяем язык библиотеки
            String libraryLanguage = library.getLanguageTo();
            boolean languageMatches = languageCode == null ||
                    libraryLanguage == null ||
                    libraryLanguage.equals(languageCode);

            // Проверяем поисковый запрос
            boolean queryMatches = query.isEmpty() ||
                    library.getName().toLowerCase().contains(query.toLowerCase()) ||
                    library.getDescription().toLowerCase().contains(query.toLowerCase());

            if (languageMatches && queryMatches) {
                this.libraries.add(library);
            }
        }
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
    /**
     * Принудительно обновляет состояние переключателя для библиотеки
     */
    public void updateLibraryState(String libraryId, boolean isActive) {
        if (activeLibraries.containsKey(libraryId)) {
            activeLibraries.put(libraryId, isActive);
        }

        // Находим и обновляем соответствующий ViewHolder
        for (int i = 0; i < libraries.size(); i++) {
            if (libraries.get(i).getLibraryId().equals(libraryId)) {
                notifyItemChanged(i);
                break;
            }
        }
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView descriptionText;
        private TextView wordCountText;
        private TextView categoryText;
        private Switch activeSwitch;
        private ImageButton infoButton;
        private ImageButton manageButton;
        private WordLibrary currentLibrary;

        private boolean isUpdating = false; // Флаг чтобы избежать рекурсии





        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            nameText = itemView.findViewById(R.id.libraryName);
            descriptionText = itemView.findViewById(R.id.libraryDescription);
            wordCountText = itemView.findViewById(R.id.libraryWordCount);
            categoryText = itemView.findViewById(R.id.libraryCategory);
            activeSwitch = itemView.findViewById(R.id.librarySwitch);
            infoButton = itemView.findViewById(R.id.infoButton);
            manageButton = itemView.findViewById(R.id.manageButton);

            // Обработчик кнопки информации
            infoButton.setOnClickListener(v -> {
                if (listener != null && currentLibrary != null) {
                    listener.onLibraryInfoClicked(currentLibrary);
                }
            });

            // Обработчик кнопки управления
            manageButton.setOnClickListener(v -> {
                if (listener != null && currentLibrary != null) {
                    listener.onLibraryManageClicked(currentLibrary);
                }
            });

            activeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isUpdating || currentLibrary == null || listener == null) {
                    return;
                }

                isUpdating = true;

                // Сохраняем ТЕКУЩЕЕ состояние до изменения
                boolean originalState = activeSwitch.isChecked();
                // Или более надежно:
                // boolean originalState = activeLibraries.getOrDefault(currentLibrary.getLibraryId(), false);

                Log.d("LibraryAdapter", "Переключение библиотеки: " + currentLibrary.getName() +
                        " с " + originalState + " на " + isChecked);


                if (isChecked) {
                    // Активируем библиотеку
                    listener.getWordRepository().activateLibrary(
                            currentLibrary.getLibraryId(),
                            () -> {
                                isUpdating = false;
                                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                                    // ОБНОВЛЯЕМ activeLibraries при успехе
                                    activeLibraries.put(currentLibrary.getLibraryId(), true);
                                    listener.onLibraryToggleSuccess(currentLibrary.getLibraryId(), true);
                                }
                                Log.d("LibraryAdapter", "Библиотека активирована: " + currentLibrary.getName());
                            },
                            e -> {
                                isUpdating = false;
                                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                                    // ВОССТАНАВЛИВАЕМ предыдущее состояние при ошибке
                                    activeLibraries.put(currentLibrary.getLibraryId(), originalState);
                                    listener.onLibraryToggleError(currentLibrary.getLibraryId(), originalState);
                                }
                                Log.e("LibraryAdapter", "Ошибка активации библиотеки: " + currentLibrary.getName(), e);
                            }
                    );
                } else {
                    // Деактивируем библиотеку
                    listener.getWordRepository().deactivateLibrary(
                            currentLibrary.getLibraryId(),
                            () -> {
                                isUpdating = false;
                                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                                    // ОБНОВЛЯЕМ activeLibraries при успехе
                                    activeLibraries.put(currentLibrary.getLibraryId(), false);
                                    listener.onLibraryToggleSuccess(currentLibrary.getLibraryId(), false);
                                }
                                Log.d("LibraryAdapter", "Библиотека деактивирована: " + currentLibrary.getName());
                            },
                            e -> {
                                isUpdating = false;
                                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                                    // ВОССТАНАВЛИВАЕМ предыдущее состояние при ошибке
                                    activeLibraries.put(currentLibrary.getLibraryId(), originalState);
                                    listener.onLibraryToggleError(currentLibrary.getLibraryId(), originalState);
                                }
                                Log.e("LibraryAdapter", "Ошибка деактивации библиотеки: " + currentLibrary.getName(), e);
                            }
                    );
                }
            });
        }

        public void bind(WordLibrary library) {
            currentLibrary = library;
            isUpdating = true;

            nameText.setText(library.getName());
            descriptionText.setText(library.getDescription());
            wordCountText.setText(library.getWordCount() + " слов");
            categoryText.setText(getCategoryDisplayName(library.getCategory()));

            // ИСПРАВЛЕНИЕ: используем activeLibraries как основной источник
            boolean isActive = activeLibraries.containsKey(library.getLibraryId())
                    ? activeLibraries.get(library.getLibraryId())
                    : library.isActive();

            activeSwitch.setChecked(isActive);

            Log.d("LibraryAdapter", "Привязка библиотеки: " + library.getName() +
                    ", активна: " + isActive + " (из activeLibraries: " +
                    activeLibraries.containsKey(library.getLibraryId()) +
                    "), ID: " + library.getLibraryId());

            isUpdating = false;

            // Показываем badge для пользовательских библиотек
            if (library.getCreatedBy() != null && !library.getCreatedBy().equals("system")) {
                showCustomLibraryBadge();
            } else {
                hideCustomLibraryBadge();
            }

            // Показываем кнопку управления только для пользовательских библиотек
            if (library.getCreatedBy() != null && !library.getCreatedBy().equals("system")) {
                manageButton.setVisibility(View.VISIBLE);
            } else {
                manageButton.setVisibility(View.GONE);
            }
        }

        private void showCustomLibraryBadge() {
            GradientDrawable badge = new GradientDrawable();
            badge.setShape(GradientDrawable.RECTANGLE);
            badge.setCornerRadius(16f);
            badge.setColor(0xFFFF6B6B);
            badge.setStroke(1, 0xFFFF5252);

            categoryText.setBackground(badge);
            categoryText.setText("Моя");
            categoryText.setTextColor(Color.WHITE);
        }

        private void hideCustomLibraryBadge() {
            GradientDrawable normalBg = new GradientDrawable();
            normalBg.setShape(GradientDrawable.RECTANGLE);
            normalBg.setCornerRadius(16f);
            normalBg.setColor(0xFFE8E6F2);
            normalBg.setStroke(1, 0xFF625fba);

            categoryText.setBackground(normalBg);
            categoryText.setText(getCategoryDisplayName(currentLibrary.getCategory()));
            categoryText.setTextColor(0xFF625fba);
        }

        private String getCategoryDisplayName(String category) {
            switch (category) {
                case "basic": return "Базовый";
                case "business": return "Деловой";
                case "travel": return "Путешествия";
                case "food": return "Еда";
                case "academic": return "Академия";
                case "custom": return "Другое";
                default: return category;
            }
        }
    }

    /**
     * Фильтрует библиотеки по запросу
     */
    public void filterLibraries(String query, List<WordLibrary> allLibraries) {
        if (query.isEmpty()) {
            this.libraries.clear();
            this.libraries.addAll(allLibraries);
        } else {
            this.libraries.clear();
            for (WordLibrary library : allLibraries) {
                if (library.getName().toLowerCase().contains(query.toLowerCase()) ||
                        library.getDescription().toLowerCase().contains(query.toLowerCase())) {
                    this.libraries.add(library);
                }
            }
        }
        notifyDataSetChanged();
    }

    public interface OnLibraryActionListener {
        // Убрали старый метод onLibraryToggled - больше не нужен

        void onLibraryInfoClicked(WordLibrary library);
        void onLibraryManageClicked(WordLibrary library);

        // Новые методы для работы с репозиторием
        WordRepository getWordRepository();
        void onLibraryToggleSuccess(String libraryId, boolean isActive);
        void onLibraryToggleError(String libraryId, boolean originalState);
    }
}