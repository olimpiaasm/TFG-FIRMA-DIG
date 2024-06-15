package com.example.signapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Enumeration;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;

public class InicioActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1;
    private static final int FILE_SELECT_REQUEST_CODE = 100;
    private static final int CERTIFICATE_SELECT_REQUEST_CODE = 200;
    private static final String PREF_SELECTED_FILE_PATH = "selected_file_path";
    private static final String PREF_IMPORTED_CERTIFICATE_PATH_PREFIX = "imported_certificate_";
    private static final String PREF_IMPORTED_CERTIFICATE_ALIAS = "imported_certificate_alias";
    private DatabaseHelper dbHelper;
    private TextView textViewUserName;
    private TextView textViewUserEmail;
    private String selectedFilePath;
    private String selectedCertificatePath;
    private String importedCertificateAlias; // Añadido
    private PrivateKey privateKey;
    private Certificate[] certificateChain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        requestStoragePermission();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        dbHelper = new DatabaseHelper(this);
        textViewUserName = findViewById(R.id.button2);
        textViewUserEmail = findViewById(R.id.button3);

        Button buttonSelectFile = findViewById(R.id.button2);
        buttonSelectFile.setOnClickListener(v -> selectFile(FILE_SELECT_REQUEST_CODE));

        Button buttonSelectCertificate = findViewById(R.id.button3);
        buttonSelectCertificate.setOnClickListener(v -> selectFile(CERTIFICATE_SELECT_REQUEST_CODE));

        Button buttonSignDocument = findViewById(R.id.button4);
        buttonSignDocument.setOnClickListener(v -> signDocument());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String savedFilePath = preferences.getString(PREF_SELECTED_FILE_PATH, null);
        if (savedFilePath != null) {
            selectedFilePath = savedFilePath;
        }

        loadImportedCertificate(preferences);
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Permiso de almacenamiento ya concedido", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de almacenamiento concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_inicio) {
        } else if (id == R.id.nav_configuracion) {
            Intent intentConfig = new Intent(this, ConfiguracionActivity.class);
            intentConfig.putExtra("filePath", selectedFilePath);
            intentConfig.putExtra("certificatePath", selectedCertificatePath);
            startActivity(intentConfig);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == FILE_SELECT_REQUEST_CODE || requestCode == CERTIFICATE_SELECT_REQUEST_CODE) && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String filePath = getPathFromUri(uri);
                if (requestCode == FILE_SELECT_REQUEST_CODE) {
                    selectedFilePath = filePath;
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    preferences.edit().putString(PREF_SELECTED_FILE_PATH, selectedFilePath).apply();
                    Toast.makeText(this, "Documento seleccionado correctamente", Toast.LENGTH_SHORT).show();  // Mostrar mensaje de confirmación
                } else if (requestCode == CERTIFICATE_SELECT_REQUEST_CODE) {
                    selectedCertificatePath = filePath;
                    showPasswordDialog();
                }
            }
        }
    }

    private void selectFile(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        if (requestCode == CERTIFICATE_SELECT_REQUEST_CODE) {
            intent.setType("application/x-pkcs12");
        }
        startActivityForResult(intent, requestCode);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                    result = cursor.getString(columnIndex);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getPathFromUri(Uri uri) {
        String filePath = null;
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                File file = createTempFile(inputStream);
                filePath = file.getAbsolutePath();
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }

    private File createTempFile(InputStream inputStream) throws IOException {
        File tempFile = File.createTempFile("temp_file", null, getCacheDir());
        tempFile.deleteOnExit();
        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
        }
        return tempFile;
    }

    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Extraer certificado");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setMessage("Escribe la contraseña para extraer el certificado");

        builder.setPositiveButton("Aceptar", null);
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String password = input.getText().toString();
                        if (importCertificate(password)) {
                            dialog.dismiss();
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(InicioActivity.this);
                            preferences.edit().putString(PREF_IMPORTED_CERTIFICATE_PATH_PREFIX + System.currentTimeMillis(), selectedCertificatePath).apply();
                            preferences.edit().putString(PREF_IMPORTED_CERTIFICATE_ALIAS, importedCertificateAlias).apply(); // Guardar el alias del certificado importado
                            Toast.makeText(InicioActivity.this, "Certificado importado correctamente", Toast.LENGTH_SHORT).show();  // Mostrar mensaje de confirmación
                        } else {
                            input.setError("Clave incorrecta");
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    private boolean importCertificate(String password) {
        FileInputStream fis = null;
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            fis = new FileInputStream(selectedCertificatePath);
            keyStore.load(fis, password.toCharArray());

            String alias = null;
            Enumeration<String> aliases = keyStore.aliases();
            if (aliases.hasMoreElements()) {
                alias = aliases.nextElement();
            }

            if (alias != null) {
                importedCertificateAlias = alias; // Guardar el alias del certificado importado
                privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
                certificateChain = keyStore.getCertificateChain(alias);

                // Guardar alias y ruta del certificado
                saveImportedCertificate(alias, selectedCertificatePath);

                Toast.makeText(this, "Certificado importado correctamente", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(this, "No se encontró ningún alias en el certificado", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error importando certificado. Verifica la contraseña e inténtalo de nuevo.", Toast.LENGTH_SHORT).show();
            return false;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveImportedCertificate(String alias, String filePath) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_IMPORTED_CERTIFICATE_PATH_PREFIX + alias, filePath);
        editor.putString(PREF_IMPORTED_CERTIFICATE_ALIAS, alias); // Guardar el alias del certificado importado
        editor.apply();
    }

    private void loadImportedCertificate(SharedPreferences preferences) {
        importedCertificateAlias = preferences.getString(PREF_IMPORTED_CERTIFICATE_ALIAS, null);
        if (importedCertificateAlias != null) {
            String certPath = preferences.getString(PREF_IMPORTED_CERTIFICATE_PATH_PREFIX + importedCertificateAlias, null);
            if (certPath != null) {
                selectedCertificatePath = certPath;
            }
        }
    }

    private void signDocument() {
        if (selectedFilePath != null && !selectedFilePath.isEmpty()) {
            Log.d("InicioActivity", "Document path: " + selectedFilePath);
            Intent intent = new Intent(this, MostrarDocumentoActivity.class);
            intent.putExtra("documentPath", selectedFilePath);
            intent.putExtra("importedCertificateAlias", importedCertificateAlias); // Pasar el alias del certificado importado
            startActivity(intent);
        } else {
            Toast.makeText(this, "Por favor, seleccione un documento primero.", Toast.LENGTH_SHORT).show();
        }
    }
}
















