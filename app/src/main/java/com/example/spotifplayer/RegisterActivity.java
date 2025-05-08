package com.example.spotifplayer;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton, backButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        backButton = findViewById(R.id.resendVerificationButton); // artık geri butonu

        mAuth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(v -> registerUser());



        // 🔙 Geri Dön butonu
        backButton.setOnClickListener(v -> {
            finish(); // mevcut aktiviteyi kapatır → LoginActivity'ye döner
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


    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        StringBuilder errorMessage = new StringBuilder();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            errorMessage.append("Tüm alanları doldurun!\n");
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage.append("Geçerli bir e-posta adresi girin!\n");
        }

        if (!password.equals(confirmPassword)) {
            errorMessage.append("Şifreler uyuşmuyor!\n");
        }

        if (password.length() < 8) {
            errorMessage.append("Şifre en az 8 karakter olmalıdır!\n");
        }

        if (!password.matches(".*[A-Z].*")) {
            errorMessage.append("Şifre en az bir büyük harf içermelidir!\n");
        }

        if (!password.matches(".*[a-z].*")) {
            errorMessage.append("Şifre en az bir küçük harf içermelidir!\n");
        }

        if (!password.matches(".*\\d.*")) {
            errorMessage.append("Şifre en az bir rakam içermelidir!\n");
        }

        if (!password.matches(".*[!@#$%^&*+=?-].*")) {
            errorMessage.append("Şifre en az bir özel karakter içermelidir! (!@#$%^&*+=?-)\n");
        }

        if (errorMessage.length() > 0) {
            Toast.makeText(this, errorMessage.toString().trim(), Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification().addOnCompleteListener(verificationTask -> {
                                if (verificationTask.isSuccessful()) {
                                    Toast.makeText(this, "✅ Kayıt başarılı! Doğrulama e-postası gönderildi.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, "⚠️ Doğrulama e-postası gönderilemedi: " + verificationTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "❌ Kayıt başarısız: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
