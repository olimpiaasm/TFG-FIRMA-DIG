package com.example.signapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MostrarDocumentoActivity extends AppCompatActivity {

    private static final String PREF_IMPORTED_CERTIFICATE_PATH_PREFIX = "imported_certificate_";
    private static final String PREF_IMPORTED_CERTIFICATE_ALIAS = "imported_certificate_alias";
    private ImageView pdfImageView;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor parcelFileDescriptor;
    private FloatingActionButton fab;
    private FloatingActionButton saveFab;
    private String selectedCertificatePath;
    private String importedCertificateAlias;
    private String documentName;
    private float signatureStartX, signatureStartY;
    private Bitmap pdfBitmap;
    private Canvas pdfCanvas;
    private Paint paint;
    private Bitmap signaturePreviewBitmap;
    private float scale = 1.0f; // Escala de la firma
    private ImageView signaturePreviewImageView; // Declaración de la vista de la firma
    private ImageView documentPreviewImageView; // Declaración de la vista del documento
    private SeekBar scaleSeekBar;
    private SeekBar verticalSeekBar;
    private SeekBar horizontalSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mostrardocumento);

        pdfImageView = findViewById(R.id.pdfImageView);
        fab = findViewById(R.id.fab);
        saveFab = findViewById(R.id.save_fab);

        String documentPath = getIntent().getStringExtra("documentPath");
        importedCertificateAlias = getIntent().getStringExtra("importedCertificateAlias"); // Obtener el alias del certificado importado
        documentName = new File(documentPath).getName(); // Get the document name
        openPdfRenderer(documentPath);
        showPage(0); // Muestra la primera página

        fab.setOnClickListener(view -> {
            showCertificateSelectionDialog();
        });

        saveFab.setOnClickListener(view -> {
            saveSignedPdf();
        });

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(getResources().getColor(android.R.color.holo_blue_light));
    }

    private void openPdfRenderer(String filePath) {
        try {
            File file = new File(filePath);
            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closePdfRenderer() {
        try {
            if (currentPage != null) {
                currentPage.close();
            }
            pdfRenderer.close();
            parcelFileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }

        if (currentPage != null) {
            currentPage.close();
        }

        currentPage = pdfRenderer.openPage(index);

        pdfBitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
        pdfCanvas = new Canvas(pdfBitmap);
        currentPage.render(pdfBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        pdfImageView.setImageBitmap(pdfBitmap);
    }

    private void showCertificateSelectionDialog() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar Certificado");

        List<String> certificateAliases = new ArrayList<>();
        List<String> certificatePaths = new ArrayList<>();

        // Mostrar solo el certificado importado
        if (importedCertificateAlias != null) {
            certificateAliases.add(importedCertificateAlias);
            certificatePaths.add(preferences.getString(PREF_IMPORTED_CERTIFICATE_PATH_PREFIX + importedCertificateAlias, null));
        }

        String[] certificateArray = new String[certificateAliases.size()];
        certificateArray = certificateAliases.toArray(certificateArray);
        String[] certificatePathArray = new String[certificatePaths.size()];
        certificatePathArray = certificatePaths.toArray(certificatePathArray);

        String[] finalCertificatePathArray = certificatePathArray;
        builder.setSingleChoiceItems(certificateArray, -1, (dialog, which) -> {
            selectedCertificatePath = finalCertificatePathArray[which];
        });

        builder.setPositiveButton("Continuar", (dialog, which) -> {
            if (selectedCertificatePath != null) {
                showSignaturePreview();
            } else {
                Toast.makeText(MostrarDocumentoActivity.this, "Seleccione un certificado", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.show();
    }

    private void showSignaturePreview() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Vista previa de la firma");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.activity_firmadocumento, null);
        builder.setView(dialogView);

        documentPreviewImageView = dialogView.findViewById(R.id.documentPreviewImageView);
        signaturePreviewImageView = dialogView.findViewById(R.id.signaturePreviewImageView);
        scaleSeekBar = dialogView.findViewById(R.id.scaleSeekBar);
        verticalSeekBar = dialogView.findViewById(R.id.verticalSeekBar);
        horizontalSeekBar = dialogView.findViewById(R.id.horizontalSeekBar);

        documentPreviewImageView.setImageBitmap(pdfBitmap);

        Bitmap signatureBitmap = createSignaturePreviewBitmap();
        signaturePreviewBitmap = signatureBitmap;
        signaturePreviewImageView.setImageBitmap(signatureBitmap);

        scaleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scale = 1 + (progress / 50.0f); // Actualizar escala
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                Bitmap scaledBitmap = Bitmap.createBitmap(signatureBitmap, 0, 0, signatureBitmap.getWidth(), signatureBitmap.getHeight(), matrix, true);
                signaturePreviewImageView.setImageBitmap(scaledBitmap);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        verticalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float newY = progress * (documentPreviewImageView.getHeight() / 100.0f);
                signaturePreviewImageView.setY(newY);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        horizontalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float newX = progress * (documentPreviewImageView.getWidth() / 100.0f);
                signaturePreviewImageView.setX(newX);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setPositiveButton("Firmar", (dialog, which) -> {
            // Guardar las coordenadas y el tamaño de la firma para usarlo en el PDF
            signatureStartX = signaturePreviewImageView.getX();
            signatureStartY = signaturePreviewImageView.getY();
            addSignatureToPdf();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    private Bitmap createSignaturePreviewBitmap() {
        // Crear un bitmap más grande y cuadrado para la firma
        int width = 400;
        int height = 400;
        Bitmap signatureBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(signatureBitmap);

        // Dibujar logo difuminado de fondo
        Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo); // Asegúrate de tener un recurso drawable llamado logo
        Paint logoPaint = new Paint();
        logoPaint.setAlpha(50); // Hacer el logo difuminado
        int logoSize = Math.min(width, height) / 2; // Ajustar el tamaño del logo
        Rect logoRect = new Rect((width - logoSize) / 2, (height - logoSize) / 4, (width + logoSize) / 2, (height + logoSize) / 4);
        canvas.drawBitmap(logoBitmap, null, logoRect, logoPaint);

        // Dibujar texto de la firma
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setTextSize(30); // Aumentar el tamaño del texto para mayor visibilidad

        String certificadoAlias = importedCertificateAlias != null ? importedCertificateAlias : "NOMBRE DEL CERTIFICADO";
        String fecha = LocalDateTime.now().toString();

        float textX = 10;
        float textY = (height / 2) + (logoSize / 2); // Ajustar la posición del texto

        canvas.drawText("Firmado digitalmente por:", textX, textY, paint);
        textY += 40; // Aumentar la distancia entre líneas de texto
        canvas.drawText(certificadoAlias, textX, textY, paint);
        textY += 40;
        canvas.drawText("Fecha: " + fecha, textX, textY, paint);

        return signatureBitmap;
    }


    private void addSignatureToPdf() {
        if (signaturePreviewBitmap != null) {
            // Aplicar escala y posición de la firma
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            matrix.postTranslate(signatureStartX, signatureStartY);
            pdfCanvas.drawBitmap(signaturePreviewBitmap, matrix, null);
            pdfImageView.setImageBitmap(pdfBitmap);
            Toast.makeText(this, "Firma añadida en el área seleccionada", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error al añadir la firma", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSignedPdf() {
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_TITLE, "signed_document.pdf");
            startActivityForResult(intent, 101);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error guardando el documento firmado", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                if (uri != null) {
                    try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                        PdfDocument document = new PdfDocument();
                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pdfBitmap.getWidth(), pdfBitmap.getHeight(), 1).create();
                        PdfDocument.Page page = document.startPage(pageInfo);
                        Canvas canvas = page.getCanvas();
                        canvas.drawBitmap(pdfBitmap, 0, 0, null);
                        document.finishPage(page);
                        document.writeTo(outputStream);
                        document.close();
                    }
                    Toast.makeText(this, "Documento firmado guardado en: " + uri.getPath(), Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error guardando el documento firmado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closePdfRenderer();
    }
}


























