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
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.time.LocalDateTime;

public class ConfiguracionActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView textViewRutaDocumento;
    private TextView textViewRutaCertificate;
    private CheckBox checkBoxNombre;
    private CheckBox checkBoxFecha;
    private CheckBox checkBoxLogo;
    private EditText editTextNombre;
    private ImageView imageViewPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        textViewRutaDocumento = findViewById(R.id.textViewRutaDocumento);
        textViewRutaCertificate = findViewById(R.id.textViewRutaCertificate);
        checkBoxNombre = findViewById(R.id.checkBoxNombre);
        checkBoxFecha = findViewById(R.id.checkBoxFecha);
        checkBoxLogo = findViewById(R.id.checkBoxLogo);
        editTextNombre = findViewById(R.id.editTextNombre);
        imageViewPreview = findViewById(R.id.imageViewPreview);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra("filePath");
        String certificatePath = intent.getStringExtra("certificatePath");

        textViewRutaDocumento.setText(filePath != null ? filePath : "");
        textViewRutaCertificate.setText(certificatePath != null ? certificatePath : "");

        // Cargar las preferencias
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        checkBoxNombre.setChecked(preferences.getBoolean("include_nombre", true));
        checkBoxFecha.setChecked(preferences.getBoolean("include_fecha", true));
        checkBoxLogo.setChecked(preferences.getBoolean("include_logo", true));
        editTextNombre.setText(preferences.getString("signature_name", ""));

        // Mostrar vista previa de la firma
        updateSignaturePreview();

        // Asignar listeners para los cambios en los CheckBox
        checkBoxNombre.setOnCheckedChangeListener((buttonView, isChecked) -> updateSignaturePreview());
        checkBoxFecha.setOnCheckedChangeListener((buttonView, isChecked) -> updateSignaturePreview());
        checkBoxLogo.setOnCheckedChangeListener((buttonView, isChecked) -> updateSignaturePreview());
        editTextNombre.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updateSignaturePreview();
            }
        });

        Button btnSave = findViewById(R.id.button2);
        btnSave.setOnClickListener(v -> {
            // Guardar las preferencias
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("include_nombre", checkBoxNombre.isChecked());
            editor.putBoolean("include_fecha", checkBoxFecha.isChecked());
            editor.putBoolean("include_logo", checkBoxLogo.isChecked());
            editor.putString("signature_name", editTextNombre.getText().toString());
            editor.apply();

            // Volver a la actividad principal
            Intent intentReturn = new Intent(ConfiguracionActivity.this, MostrarDocumentoActivity.class);
            intentReturn.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentReturn);
            finish();
        });
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

        canvas.drawColor(Color.WHITE); // Limpiar el canvas antes de dibujar

        if (includeLogo) {
            Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
            Bitmap scaledLogoBitmap = Bitmap.createScaledBitmap(logoBitmap, 100, 100, true);
            Paint logoPaint = new Paint();
            logoPaint.setAlpha(50);
            canvas.drawBitmap(scaledLogoBitmap, (width - scaledLogoBitmap.getWidth()) / 2, 20, logoPaint);
        }

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setTextSize(20);

        int textPadding = 10;
        int lineHeight = (int) (paint.descent() - paint.ascent());

        int y = 140; // Espacio superior para el logo

        if (includeNombre) {
            drawTextWithWrap(canvas, paint, "Firmado digitalmente por:", textPadding, y, width - 2 * textPadding);
            y += lineHeight;
            drawTextWithWrap(canvas, paint, "Nombre del documento: ", textPadding, y, width - 2 * textPadding);
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
                lineY += lineHeight + 10;  // Añadir espacio extra entre las líneas
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












