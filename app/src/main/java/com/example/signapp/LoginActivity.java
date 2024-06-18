package com.example.signapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;
    private Button buttonRegister;
    private TextView textViewForgotPassword;
    private ImageView imageViewBiometricLogin;
    private DatabaseHelper dbHelper;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextText);
        editTextPassword = findViewById(R.id.editTextText2);
        buttonLogin = findViewById(R.id.button);
        buttonRegister = findViewById(R.id.button1);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        imageViewBiometricLogin = findViewById(R.id.imageViewBiometricLogin);

        dbHelper = new DatabaseHelper(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        buttonLogin.setOnClickListener(view -> {
            String email = editTextEmail.getText().toString();
            String password = editTextPassword.getText().toString();

            if (dbHelper.checkUser(email, password)) {
                User user = dbHelper.getUser(email);
                if (user != null) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("user_name", user.getName());
                    editor.putString("user_email", user.getEmail());
                    editor.apply();
                }
                Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, InicioActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            }
        });

        buttonRegister.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegistroActivity.class);
            startActivity(intent);
        });

        textViewForgotPassword.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RecuperarContraseñaActivity.class);
            startActivity(intent);
        });

        imageViewBiometricLogin.setOnClickListener(view -> {
            BiometricManager biometricManager = BiometricManager.from(this);
            switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
                case BiometricManager.BIOMETRIC_SUCCESS:
                    showBiometricPrompt();
                    break;
                case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                    Toast.makeText(this, "No hay funciones biométricas disponibles en este dispositivo.", Toast.LENGTH_SHORT).show();
                    break;
                case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                    Toast.makeText(this, "Las funciones biométricas no están disponibles actualmente.", Toast.LENGTH_SHORT).show();
                    break;
                case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                    Toast.makeText(this, "No hay credenciales biométricas registradas.", Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Error de autenticación: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(), "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, InicioActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Fallo de autenticación", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Acceso Biométrico")
                .setSubtitle("Accede con tu biometría a SignApp")
                .setNegativeButtonText("Cancelar")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}







