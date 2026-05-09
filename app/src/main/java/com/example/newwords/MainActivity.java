package com.example.newwords;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import androidx.core.splashscreen.SplashScreen;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // ✅ ОБЪЯВЛЯЕМ РЕПОЗИТОРИЙ
    private WordRepository wordRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ ИНИЦИАЛИЗИРУЕМ РЕПОЗИТОРИЙ
        wordRepository = new WordRepository(this);

        // Инициализация App Check с Play Integrity API
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // ✅ ЗАПУСКАЕМ ПРЕДЗАГРУЗКУ КЕША (фоном)
        preloadCacheForAllLanguages();

        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        viewPager.setAdapter(new ViewPagerAdapter(this));

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_page1) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (id == R.id.navigation_page2) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (id == R.id.navigation_page3) {
                viewPager.setCurrentItem(2);
                return true;
            } else {
                return false;
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
            }
        });

        handleNotificationIntent(getIntent());
        requestNotificationPermission();
    }

    // ✅ МЕТОД ВЫНЕСЕН ИЗ onCreate (на уровень класса)
    private void preloadCacheForAllLanguages() {
        Log.d(TAG, "🚀 ПРЕДЗАГРУЗКА КЕША для всех языков");

        String[] languages = {"ba", "en", "ru"};

        for (String lang : languages) {
            wordRepository.syncWordsFromFirebaseForLanguage(lang, new WordRepository.OnWordsLoadedListener() {
                @Override
                public void onWordsLoaded(List<WordItem> words) {
                    Log.d(TAG, "✅ Предзагружено " + words.size() + " слов для языка " + lang);
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "❌ Ошибка предзагрузки для " + lang, e);
                }
            });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("OPEN_FRAGMENT")) {
            String fragmentToOpen = intent.getStringExtra("OPEN_FRAGMENT");

            if ("FRAGMENT_1".equals(fragmentToOpen)) {
                openFragment1();
                Toast.makeText(this, "Добро пожаловать! Пора учить слова! 📚", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Приложение открыто из уведомления, переходим на фрагмент 1");
            }
        }
    }

    private void openFragment1() {
        if (viewPager != null) {
            viewPager.setCurrentItem(0, false);
            if (bottomNavigationView != null) {
                bottomNavigationView.setSelectedItemId(R.id.navigation_page1);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Уведомления включены", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Уведомления отключены", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void switchToLibraryTab() {
        if (viewPager != null) {
            viewPager.setCurrentItem(1);
        }
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_page2);
        }
    }
}