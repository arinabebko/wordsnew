package com.example.newwords;

public class WordItem {
    // Поля класса - это данные, которые мы храним
    private String word;          // английское слово (например "spring")
    private String translation;   // перевод (например "весна")
    private String note;          // примечание (например "Сезон года")
    private boolean isFavorite;   // добавлено в избранное? (true/false)

    // Конструктор - метод для создания нового объекта WordItem
    public WordItem(String word, String translation, String note) {
        this.word = word;
        this.translation = translation;
        this.note = note;
        this.isFavorite = false; // по умолчанию не в избранном
    }

    // ↓↓↓ ГЕТТЕРЫ - методы для получения данных ↓↓↓

    // Получить слово
    public String getWord() {
        return word;
    }

    // Получить перевод
    public String getTranslation() {
        return translation;
    }

    // Получить примечание
    public String getNote() {
        return note;
    }

    // Проверить, в избранном ли слово
    public boolean isFavorite() {
        return isFavorite;
    }

    // ↓↓↓ СЕТТЕРЫ - методы для изменения данных ↓↓↓

    // Изменить слово
    public void setWord(String word) {
        this.word = word;
    }

    // Изменить перевод
    public void setTranslation(String translation) {
        this.translation = translation;
    }

    // Изменить примечание
    public void setNote(String note) {
        this.note = note;
    }

    // Добавить/убрать из избранного
    public void setFavorite(boolean favorite) {
        this.isFavorite = favorite;
    }
}