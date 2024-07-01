package com.example.signapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfiguracionActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private CheckBox checkBoxNombre;
    private CheckBox checkBoxFecha;
    private CheckBox checkBoxLogo;
    private EditText editTextNombre;
    private ImageView imageViewPreview;
    private SharedPreferences preferences;
    private List<String> certificados;

    private static final String PREF_IMPORTED_CERTIFICATE_PATH_PREFIX = "imported_certificate_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Button buttonManageCertificates = findViewById(R.id.buttonManageCertificates);
        Button buttonSaveChanges = findViewById(R.id.buttonSaveChanges);

        checkBoxNombre = findViewById(R.id.checkBoxNombre);
        checkBoxFecha = findViewById(R.id.checkBoxFecha);
        checkBoxLogo = findViewById(R.id.checkBoxLogo);
        editTextNombre = findViewById(R.id.editTextNombre);
        imageViewPreview = findViewById(R.id.imageViewPreview);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        loadPreferences();

        buttonManageCertificates.setOnClickListener(v -> openManageCertificatesDialog());
        buttonSaveChanges.setOnClickListener(v -> savePreferences());

        checkBoxNombre.setOnCheckedChangeListener((buttonView, isChecked) -> updateSignaturePreview());
        checkBoxFecha.setOnCheckedChangeListener((buttonView, isChecked) -> updateSignaturePreview());
        checkBoxLogo.setOnCheckedChangeListener((buttonView, isChecked) -> updateSignaturePreview());
        editTextNombre.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updateSignaturePreview();
            }
        });

        updateSignaturePreview();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.configuracion, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_datosusuario) {
            openUserInfoActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openUserInfoActivity() {
        Intent intent = new Intent(this, DatosUsuarioActivity.class);
        startActivity(intent);
    }

    private void openManageCertificatesDialog() {
        // Cargar los certificados importados
        loadImportedCertificates();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Lista de Certificados Importados");

        // Crear un adaptador para la lista de certificados
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, certificados);
        ListView listView = new ListView(this);
        listView.setAdapter(adapter);

        // Configurar el evento de clic en los elementos de la lista para eliminar certificados
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String alias = certificados.get(position);
            showDeleteCertificateDialog(alias, adapter);
        });

        builder.setView(listView);
        builder.setNegativeButton("Cerrar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showDeleteCertificateDialog(String alias, ArrayAdapter<String> adapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Eliminar Certificado");
        builder.setMessage("¿Estás seguro de que deseas eliminar el certificado seleccionado?");
        builder.setPositiveButton("Sí", (dialog, which) -> {
            // Eliminar el certificado
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(PREF_IMPORTED_CERTIFICATE_PATH_PREFIX + alias);
            editor.apply();
            loadImportedCertificates();
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Certificado eliminado", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void loadImportedCertificates() {
        certificados = new ArrayList<>();
        Map<String, ?> allEntries = preferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith(PREF_IMPORTED_CERTIFICATE_PATH_PREFIX)) {
                certificados.add(entry.getKey().replace(PREF_IMPORTED_CERTIFICATE_PATH_PREFIX, ""));
            }
        }
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("include_nombre", checkBoxNombre.isChecked());
        editor.putBoolean("include_fecha", checkBoxFecha.isChecked());
        editor.putBoolean("include_logo", checkBoxLogo.isChecked());
        editor.putString("signature_name", editTextNombre.getText().toString());
        editor.apply();

        Toast.makeText(this, "Preferencias guardadas", Toast.LENGTH_SHORT).show();

    }

    private void loadPreferences() {
        checkBoxNombre.setChecked(preferences.getBoolean("include_nombre", true));
        checkBoxFecha.setChecked(preferences.getBoolean("include_fecha", true));
        checkBoxLogo.setChecked(preferences.getBoolean("include_logo", true));
        editTextNombre.setText(preferences.getString("signature_name", ""));
    }

    private void updateSignaturePreview() {
        imageViewPreview.setImageBitmap(createSignaturePreviewBitmap());
    }

    private Bitmap createSignaturePreviewBitmap() {
        boolean includeNombre = checkBoxNombre.isChecked();
        boolean includeFecha = checkBoxFecha.isChecked();
        boolean includeLogo = checkBoxLogo.isChecked();
        String nombre = editTextNombre.getText().toString();

        int width = 400;
        int height = 200;
        Bitmap signatureBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(signatureBitmap);

        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setTextSize(20);

        int textPadding = 10;
        int lineHeight = (int) (paint.descent() - paint.ascent());

        if (includeLogo) {
            Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
            Bitmap scaledLogoBitmap = Bitmap.createScaledBitmap(logoBitmap, width, height, true);
            Paint logoPaint = new Paint();
            logoPaint.setAlpha(50);
            canvas.drawBitmap(scaledLogoBitmap, 0, 0, logoPaint);
        }

        int y = 20;

        if (includeNombre) {
            drawTextWithWrap(canvas, paint, "Firmado digitalmente por:", textPadding, y, width - 2 * textPadding);
            y += lineHeight;
            drawTextWithWrap(canvas, paint, "Nombre del documento: " + nombre, textPadding, y, width - 2 * textPadding);
            y += lineHeight;
        }

        if (includeFecha) {
            drawTextWithWrap(canvas, paint, "Fecha: ", textPadding, y, width - 2 * textPadding);
        }

        return signatureBitmap;
    }

    private void drawTextWithWrap(Canvas canvas, Paint paint, String text, float x, float y, float maxWidth) {
        int lineHeight = (int) (paint.descent() - paint.ascent());
        float lineY = y;
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String testLine = line + word + " ";
            float testWidth = paint.measureText(testLine);
            if (testWidth > maxWidth) {
                canvas.drawText(line.toString(), x, lineY, paint);
                line = new StringBuilder(word + " ");
                lineY += lineHeight + 10;
            } else {
                line.append(word).append(" ");
            }
        }
        canvas.drawText(line.toString(), x, lineY, paint);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_inicio) {
            Intent intentInicio = new Intent(this, InicioActivity.class);
            startActivity(intentInicio);
        } else if (id == R.id.nav_configuracion) {
            // Ya estamos en ConfiguracionActivity
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
















