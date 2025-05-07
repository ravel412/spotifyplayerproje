package com.example.spotifplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton, resendVerificationButton;
    private FirebaseAuth mAuth;
    private Handler handler;
    private Runnable runnable;
    private static final int RESEND_DELAY = 15000; // 15 saniye

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        mAuth = FirebaseAuth.getInstance();
        handler = new Handler();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();

            }
        });


    }

/*    private void registerUser(String email, String password) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verificationTask -> {
                                        if (verificationTask.isSuccessful()) {
                                            Toast.makeText(getApplicationContext(),
                                                    "Doğrulama e-postası gönderildi. Lütfen e-postanızı kontrol edin.",
                                                    Toast.LENGTH_LONG).show();
                                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(getApplicationContext(),
                                                    "Doğrulama e-postası gönderilemedi.",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Kayıt başarısız: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
*/



    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Hata mesajlarını saklamak için StringBuilder kullanıyoruz
        StringBuilder errorMessage = new StringBuilder();

        // 🛠️ 1. Boş alan kontrolü
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            errorMessage.append("Tüm alanları doldurun!\n");
        }

        // 🛠️ 2. Geçerli e-posta kontrolü
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage.append("Geçerli bir e-posta adresi girin!\n");
        }

        // 🛠️ 3. Şifreler eşleşiyor mu?
        if (!password.equals(confirmPassword)) {
            errorMessage.append("Şifreler uyuşmuyor!\n");
        }

        // 🛠️ 4. Şifre uzunluğu kontrolü (min 8 karakter)
        if (password.length() < 8) {
            errorMessage.append("Şifre en az 8 karakter olmalıdır!\n");
        }

        // 🛠️ 5. Şifre güvenliği kontrolü
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

        // Eğer hata mesajı varsa, kullanıcıya göster ve işlemi durdur
        if (errorMessage.length() > 0) {
            Toast.makeText(this, errorMessage.toString().trim(), Toast.LENGTH_LONG).show();
            return;
        }

        // 🛠️ 6. Firebase ile kayıt işlemi
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        startActivity(intent);
                        finish();

                        Toast.makeText(this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show();
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verificationTask -> {
                                        if (verificationTask.isSuccessful()) {
                                            Toast.makeText(this, "Doğrulama e-postası gönderildi!", Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Kayıt başarısız: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void sendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "E-posta doğrulama bağlantısı gönderildi!", Toast.LENGTH_SHORT).show();
                    startResendCooldown(); // 15 saniye bekleme başlat
                } else {
                    Toast.makeText(RegisterActivity.this, "E-posta gönderme hatası: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void startResendCooldown() {
        resendVerificationButton.setEnabled(false);
        resendVerificationButton.setText("Tekrar Gönder (15s)");

        runnable = new Runnable() {
            int seconds = 15;
            @Override
            public void run() {
                if (seconds > 0) {
                    resendVerificationButton.setText("Tekrar Gönder (" + seconds + "s)");
                    seconds--;
                    handler.postDelayed(this, 1000);
                } else {
                    resendVerificationButton.setEnabled(true);
                    resendVerificationButton.setText("Tekrar Gönder");
                }
            }
        };

        handler.post(runnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}

