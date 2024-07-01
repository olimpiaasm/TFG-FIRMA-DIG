package com.example.signapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class DatosUsuarioActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextEmail;
    private Button buttonChangePassword;
    private Button buttonSave;
    private SharedPreferences preferences;
    private DatabaseHelper dbHelper;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datosusuario);

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        buttonChangePassword = findViewById(R.id.buttonChangePassword);
        buttonSave = findViewById(R.id.buttonSave);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        dbHelper = new DatabaseHelper(this);

        // Cargar datos del usuario
        userEmail = preferences.getString("user_email", "");
        loadUserData();

        buttonChangePassword.setOnClickListener(v -> openChangePasswordDialog());

        buttonSave.setOnClickListener(v -> saveUserData());
    }

    private void loadUserData() {
        User user = dbHelper.getUser(userEmail);
        if (user != null) {
            editTextName.setText(user.getName());
            editTextEmail.setText(user.getEmail());
        }
    }

    private void saveUserData() {
        String currentName = editTextName.getText().toString();
        String currentEmail = editTextEmail.getText().toString();

        String newName = editTextName.getText().toString();
        String newEmail = editTextEmail.getText().toString();

        boolean isNameUpdated = !newName.equals(currentName);
        boolean isEmailUpdated = !newEmail.equals(userEmail);

        if (dbHelper.updateUser(userEmail, newName, newEmail)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("user_name", newName);
            editor.putString("user_email", newEmail);
            editor.apply();

            Toast.makeText(this, "Datos guardados", Toast.LENGTH_SHORT).show();

            if (isNameUpdated) {
                NotificacionActivity.showNotification(this, "Actualización de Usuario", "Nombre actualizado correctamente");
            }

            if (isEmailUpdated) {
                NotificacionActivity.showNotification(this, "Actualización de Usuario", "Correo electrónico actualizado correctamente");
            }

            // Actualiza userEmail para reflejar el nuevo correo
            userEmail = newEmail;
        } else {
            Toast.makeText(this, "Error al guardar datos", Toast.LENGTH_SHORT).show();
        }
    }

    private void openChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cambiar Contraseña");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            String newPassword = input.getText().toString();
            // Guardar la nueva contraseña en la base de datos
            if (dbHelper.updateUserPassword(userEmail, newPassword)) {
                Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show();
                NotificacionActivity.showNotification(this, "Actualización de Usuario", "Contraseña actualizada correctamente");
            } else {
                Toast.makeText(this, "Error al actualizar la contraseña", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}




