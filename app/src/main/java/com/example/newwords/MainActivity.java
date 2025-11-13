package com.example.newwords;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.example.newwords.ViewPagerAdapter;

public class MainActivity extends AppCompatActivity {


    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private static final int PERMISSION_REQUEST_CODE = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация App Check с Play Integrity API должна идти до любых вызовов Firebase
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Пользователь не авторизован — запускаем LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish(); // чтобы закрыть MainActivity
            return; // важно завершить метод, чтобы не показать основной экран
        }

        // если пользователь авторизован — остаёмся в MainActivity и показываем основной контент

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
            }
else
                return false;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
            }
        });


        requestNotificationPermission();
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
                // Разрешение получено
                Toast.makeText(this, "Уведомления включены", Toast.LENGTH_SHORT).show();
            } else {
                // Разрешение не получено
                Toast.makeText(this, "Уведомления отключены", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Переключает на вкладку библиотек (Fragment2)
     */
    public void switchToLibraryTab() {
        if (viewPager != null) {
            viewPager.setCurrentItem(1); // 1 - это индекс Fragment2
        }

        // Также можно добавить анимацию или дополнительную логику
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_page2);
        }
    }



}