package com.example.newwords;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

public class Converters {
    // Твои существующие конвертеры для даты
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    // НОВЫЕ: Конвертеры для Map (используем библиотеку GSON)
    @TypeConverter
    public static Map<String, String> fromString(String value) {
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        return new Gson().fromJson(value, mapType);
    }

    @TypeConverter
    public static String fromMap(Map<String, String> map) {
        return new Gson().toJson(map);
    }
}