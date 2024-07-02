package com.example.signapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RecuperarContraseñaActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private Button buttonSendEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperarcontrasena);

        editTextEmail = findViewById(R.id.editTextEmail);
        buttonSendEmail = findViewById(R.id.buttonSendEmail);

        buttonSendEmail.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(RecuperarContraseñaActivity.this, "Por favor, introduce tu correo electrónico", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RecuperarContraseñaActivity.this, "Correo de recuperación enviado", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

