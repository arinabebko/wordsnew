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

    public void updateLibraries(List<WordLibrary> newLibraries) {
        this.libraries.clear();
        this.libraries.addAll(newLibraries);
        notifyDataSetChanged();
    }

    public void updateActiveLibraries(Map<String, Boolean> activeLibraries) {
        this.activeLibraries.clear();
        this.activeLibraries.putAll(activeLibraries);
        notifyDataSetChanged();
    }

    public void filterLibraries(String query, List<WordLibrary> allLibraries) {
        this.libraries.clear();
        if (query == null || query.trim().isEmpty()) {
            this.libraries.addAll(allLibraries);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (WordLibrary library : allLibraries) {
                String name = library.getLocalizedName().toLowerCase();
                String description = library.getLocalizedDescription().toLowerCase();

                if (name.contains(lowerQuery) || description.contains(lowerQuery)) {
                    this.libraries.add(library);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateLibraryState(String libraryId, boolean isActive) {
        if (activeLibraries.containsKey(libraryId)) {
            activeLibraries.put(libraryId, isActive);
        }
        for (int i = 0; i < libraries.size(); i++) {
            if (libraries.get(i).getLibraryId().equals(libraryId)) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView libraryName;
        private TextView libraryDescription;
        private TextView libraryWordCount;
        private TextView libraryCategory;
        private TextView librarySubcategory;
        private Switch librarySwitch;
        private ImageButton manageButton;
        private WordLibrary currentLibrary;
        private boolean isUpdating = false;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            libraryName = itemView.findViewById(R.id.libraryName);
            libraryDescription = itemView.findViewById(R.id.libraryDescription);
            libraryWordCount = itemView.findViewById(R.id.libraryWordCount);
            libraryCategory = itemView.findViewById(R.id.libraryCategory);
            librarySubcategory = itemView.findViewById(R.id.librarySubcategory);
            librarySwitch = itemView.findViewById(R.id.librarySwitch);
            manageButton = itemView.findViewById(R.id.manageButton);

            // manageButton.setOnClickListener(v -> {
             //   if (listener != null && currentLibrary != null) {
            //        boolean isCustom = currentLibrary.getCreatedBy() != null &&
             //               !currentLibrary.getCreatedBy().equals("system");
            //        if (isCustom) {
             //           listener.onLibraryManageClicked(currentLibrary);
              //      } else {
              //          listener.onLibraryViewClicked(currentLibrary);
              //      }
             //   }
           // });

            librarySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isUpdating || currentLibrary == null || listener == null) {
                    return;
                }

                isUpdating = true;
                boolean originalState = !isChecked; // Сохраняем противоположное состояние

                if (isChecked) {
                    listener.getWordRepository().activateLibrary(
                            currentLibrary.getLibraryId(),
                            () -> {
                                isUpdating = false;
                                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                                    activeLibraries.put(currentLibrary.getLibraryId(), true);
                                    listener.onLibraryToggleSuccess(currentLibrary.getLibraryId(), true);
                                }
                            },
                            e -> {
                                isUpdating = false;
                                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                                    activeLibraries.put(currentLibrary.getLibraryId(), originalState);
                                    listener.onLibraryToggleError(currentLibrary.getLibraryId(), originalState);
                                }
                            }
                    );
                } else {
                    listener.getWordRepository().deactivateLibrary(
                            currentLibrary.getLibraryId(),
                            () -> {
                                isUpdating = false;
                                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                                    activeLibraries.put(currentLibrary.getLibraryId(), false);
                                    listener.onLibraryToggleSuccess(currentLibrary.getLibraryId(), false);
                                }
                            },
                            e -> {
                                isUpdating = false;
                                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                                    activeLibraries.put(currentLibrary.getLibraryId(), originalState);
                                    listener.onLibraryToggleError(currentLibrary.getLibraryId(), originalState);
                                }
                            }
                    );
                }
            });
        }

        public void bind(WordLibrary library) {
            currentLibrary = library;
            isUpdating = true;

            libraryName.setText(library.getLocalizedName());
            libraryDescription.setText(library.getLocalizedDescription());

            int count = library.getWordCount();
            String formattedWordCount = itemView.getContext().getResources().getQuantityString(
                    R.plurals.word_count_plurals,
                    count,
                    count
            );
            libraryWordCount.setText(formattedWordCount);

            libraryCategory.setText(getCategoryDisplayName(library.getCategory()));

            String sub = library.getLocalizedSubcategory();
            if (sub != null && !sub.isEmpty()) {
                librarySubcategory.setText(sub);
                librarySubcategory.setVisibility(View.VISIBLE);
            } else {
                librarySubcategory.setVisibility(View.GONE);
            }

            boolean isActive = activeLibraries.containsKey(library.getLibraryId())
                    ? activeLibraries.get(library.getLibraryId())
                    : library.isActive();
            librarySwitch.setChecked(isActive);

            // Показываем кнопку для всех библиотек
            manageButton.setVisibility(View.VISIBLE);

            // ⭐ РАЗНЫЕ ИКОНКИ ДЛЯ РАЗНЫХ ТИПОВ БИБЛИОТЕК
            boolean isCustomLibrary = library.getCreatedBy() != null &&
                    !library.getCreatedBy().equals("system");

            if (isCustomLibrary) {
                // Кастомная библиотека - иконка настроек (шестеренка)
                manageButton.setImageResource(R.drawable.ic_font_settings);
                manageButton.setScaleX(1.0f);  // обычный размер
                manageButton.setScaleY(1.0f);
                manageButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onLibraryManageClicked(library);
                    }
                });
            } else {
                // Публичная библиотека - иконка книги (просмотр)
                manageButton.setImageResource(R.drawable.ic_new_icon_book);
                manageButton.setScaleX(1.2f);  // увеличиваем на 20%
                manageButton.setScaleY(1.2f);
                manageButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onLibraryViewClicked(library);
                    }
                });
            }

            isUpdating = false;
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

    public interface OnLibraryActionListener {
        void onLibraryInfoClicked(WordLibrary library);
        void onLibraryManageClicked(WordLibrary library);
        void onLibraryViewClicked(WordLibrary library);
        WordRepository getWordRepository();
        void onLibraryToggleSuccess(String libraryId, boolean isActive);
        void onLibraryToggleError(String libraryId, boolean originalState);
    }
}