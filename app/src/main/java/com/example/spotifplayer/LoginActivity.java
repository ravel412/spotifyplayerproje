package com.example.spotifplayer;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;
    private FirebaseAuth mAuth;
    private Button registerbuton1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase Auth instance
        mAuth = FirebaseAuth.getInstance();
        registerbuton1 = findViewById(R.id.registerButton);

        // UI components
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);





        Spinner languageSpinner = findViewById(R.id.languageSpinner);

// İlk yüklemede tetiklenmesini engelle
        final boolean[] isFirstSelection = {true};



        // Kayıtlı dili bul ve Spinner'ı ayarla
        String savedLang = LocaleHelper.getSavedLanguage(this);
        languageSpinner.setSelection(savedLang.equals("tr") ? 0 : 1);

        final boolean[] isFirst = {true};
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFirst[0]) {
                    isFirst[0] = false;
                    return;
                }

                String selectedLang = position == 0 ? "tr" : "en";
                LocaleHelper.setLocale(LoginActivity.this, selectedLang);

                // Uygulama baştan yüklensin
                Intent intent = getIntent();

                finish();

                overridePendingTransition(0, 0); // 🎯 animasyonu KAPAT
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0); // 🎯 tekrar kapat, yeni activity açılırken de

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });






        // Giriş butonu
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "E-posta ve şifre boş olamaz", Toast.LENGTH_SHORT).show();
                return;
            }
            signInUser(email, password);
        });

        // Kayıt ekranına geçiş
        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        String lang = LocaleHelper.getSavedLanguage(newBase);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        super.attachBaseContext(newBase.createConfigurationContext(config));
    }
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.isEmailVerified()) {
            goToMainActivity();
        }
    }

    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            Toast.makeText(this, "Giriş başarılı!", Toast.LENGTH_SHORT).show();
                            goToMainActivity();
                        } else {
                            mAuth.signOut();
                            Toast.makeText(this, "E-posta doğrulaması gerekli.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Giriş başarısız: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void goToMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        overridePendingTransition(0, 0); // 🚫 Animasyon yok
        finish();
    }
}
