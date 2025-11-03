package com.example.newwords;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.List;

public class WordsFragment extends Fragment implements WordCardAdapter.OnWordActionListener {

    private ViewPager2 viewPager2;
    private WordCardAdapter adapter;
    private int previousPosition = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_words, container, false);

        // Инициализируем компоненты ПЕРЕДАВАЯ view
        setupViewPager(view);
        setupBackButton(view);

        return view;
    }

    private void setupViewPager(View view) {
        // Используем переданный view, а не getView()
        viewPager2 = view.findViewById(R.id.viewPager2);

        List<WordItem> wordList = createWordList();
        adapter = new WordCardAdapter(wordList, this);
        viewPager2.setAdapter(adapter);

        // Упрощенный обработчик свайпов - пока без логики выучено/не выучено
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Простая логика для начала
                if (position > previousPosition) {
                    // Свайпнули влево
                    Log.d("Swipe", "Свайп влево - предыдущее слово: " + wordList.get(previousPosition).getWord());
                } else if (position < previousPosition) {
                    // Свайпнули вправо
                    Log.d("Swipe", "Свайп вправо - предыдущее слово: " + wordList.get(previousPosition).getWord());
                }
                previousPosition = position;
            }
        });
    }

    private void setupBackButton(View view) {
        ImageButton backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private List<WordItem> createWordList() {
        List<WordItem> wordList = new ArrayList<>();
        wordList.add(new WordItem("spring", "весна", "Сезон года"));
        wordList.add(new WordItem("start", "начало", "Противоположность концу"));
        wordList.add(new WordItem("note", "примечание", "Дополнительная информация"));
        wordList.add(new WordItem("hello", "привет", "Приветствие"));
        wordList.add(new WordItem("world", "мир", "Планета Земля"));
        wordList.add(new WordItem("apple", "яблоко", "Фрукт"));
        wordList.add(new WordItem("book", "книга", "Для чтения"));
        return wordList;
    }

    @Override
    public void onWordLearned(WordItem word) {
        // Пока просто показываем сообщение
        Toast.makeText(getContext(), "✅ " + word.getWord() + " - выучено!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onWordNotLearned(WordItem word) {
        // Пока просто показываем сообщение
        Toast.makeText(getContext(), "❌ " + word.getWord() + " - нужно повторить", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onWordFavoriteToggled(WordItem word, boolean isFavorite) {
        String message = isFavorite ? "★ Добавлено в избранное" : "☆ Убрано из избранного";
        Toast.makeText(getContext(), message + ": " + word.getWord(), Toast.LENGTH_SHORT).show();
    }
}