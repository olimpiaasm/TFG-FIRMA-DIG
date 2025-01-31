package com.example.signapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MostrarDocumentoActivity extends AppCompatActivity {

    private static final String PREF_IMPORTED_CERTIFICATE_PATH_PREFIX = "imported_certificate_";
    private static final String PREF_IMPORTED_CERTIFICATE_ALIAS = "imported_certificate_alias";
    private ImageView pdfImageView;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor parcelFileDescriptor;
    private FloatingActionButton fab;
    private FloatingActionButton saveFab;
    private Button buttonNext, buttonPrevious;
    private TextView textPageNumber;
    private int currentPageIndex = 0;
    private String selectedCertificatePath;
    private String importedCertificateAlias;
    private String documentName;
    private float signatureStartX, signatureStartY;
    private Bitmap pdfBitmap;
    private Canvas pdfCanvas;
    private Paint paint;
    private Bitmap signaturePreviewBitmap;
    private float scale = 1.0f; // Escala de la firma
    private ImageView signaturePreviewImageView;
    private ImageView documentPreviewImageView;
    private ScaleGestureDetector scaleGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mostrardocumento);

        pdfImageView = findViewById(R.id.pdfImageView);
        fab = findViewById(R.id.fab);
        saveFab = findViewById(R.id.save_fab);
        buttonNext = findViewById(R.id.buttonNext);
        buttonPrevious = findViewById(R.id.buttonPrevious);
        textPageNumber = findViewById(R.id.textPageNumber);

        String documentPath = getIntent().getStringExtra("documentPath");
        importedCertificateAlias = getIntent().getStringExtra("importedCertificateAlias"); // Obtener el alias del certificado importado
        documentName = new File(documentPath).getName(); // Obtener el nombre del documento
        openPdfRenderer(documentPath);
        showPage(currentPageIndex);

        fab.setOnClickListener(view -> showCertificateSelectionDialog());

        saveFab.setOnClickListener(view -> saveSignedPdf());

        buttonNext.setOnClickListener(v -> {
            if (currentPageIndex < pdfRenderer.getPageCount() - 1) {
                currentPageIndex++;
                showPage(currentPageIndex);
            }
        });

        buttonPrevious.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                currentPageIndex--;
                showPage(currentPageIndex);
            }
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
        textPageNumber.setText(String.format(Locale.getDefault(), "Página %d de %d", index + 1, pdfRenderer.getPageCount()));
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
        builder.setSingleChoiceItems(certificateArray, -1, (dialog, which) -> selectedCertificatePath = finalCertificatePathArray[which]);

        builder.setPositiveButton("Continuar", (dialog, which) -> {
            if (selectedCertificatePath != null) {
                showSignaturePreview();
            } else {
                Toast.makeText(MostrarDocumentoActivity.this, "Seleccione un certificado", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
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

        documentPreviewImageView.setImageBitmap(pdfBitmap);

        Bitmap signatureBitmap = createUpdatedSignatureBitmap();
        signaturePreviewBitmap = signatureBitmap;
        signaturePreviewImageView.setImageBitmap(signatureBitmap);

        signaturePreviewImageView.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            float startX, startY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        startX = view.getScaleX();
                        startY = view.getScaleY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        view.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        break;
                }
                return true;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scale *= detector.getScaleFactor();
                scale = Math.max(0.1f, Math.min(scale, 5.0f));
                signaturePreviewImageView.setScaleX(scale);
                signaturePreviewImageView.setScaleY(scale);
                return true;
            }
        });

        builder.setPositiveButton("Firmar", (dialog, which) -> {

            signatureStartX = signaturePreviewImageView.getX();
            signatureStartY = signaturePreviewImageView.getY();
            addSignatureToPdf();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private Bitmap createUpdatedSignatureBitmap() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean includeNombre = preferences.getBoolean("include_nombre", true);
        boolean includeFecha = preferences.getBoolean("include_fecha", true);
        boolean includeLogo = preferences.getBoolean("include_logo", true);

        String nombreDocumento = documentName;
        String nombreCertificado = importedCertificateAlias != null ? importedCertificateAlias : "Certificado no seleccionado";

        int width = 400;
        int height = 300;  // Aumentado el alto para permitir más texto
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
            drawTextWithWrap(canvas, paint, "Firmado digitalmente por: " + nombreCertificado, textPadding, y, width - 2 * textPadding);
            y += lineHeight + 20;
            drawTextWithWrap(canvas, paint, "Nombre del documento: " + nombreDocumento, textPadding, y, width - 2 * textPadding);
            y += lineHeight + 20;
        }

        if (includeFecha) {
            String fecha = LocalDateTime.now().toString();
            drawTextWithWrap(canvas, paint, "Fecha: " + fecha, textPadding, y, width - 2 * textPadding);
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
                lineY += lineHeight;
            } else {
                line.append(word).append(" ");
            }
        }
        canvas.drawText(line.toString(), x, lineY, paint);
    }

    private void addSignatureToPdf() {
        if (signaturePreviewBitmap != null) {
            // Aplicar escala y posición de la firma
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);

            // Convertir las coordenadas de la vista previa a las coordenadas del PDF
            float pdfWidth = currentPage.getWidth();
            float pdfHeight = currentPage.getHeight();
            float previewWidth = documentPreviewImageView.getWidth();
            float previewHeight = documentPreviewImageView.getHeight();

            float xRatio = pdfWidth / previewWidth;
            float yRatio = pdfHeight / previewHeight;

            float finalX = signatureStartX * xRatio;
            float finalY = signatureStartY * yRatio;

            matrix.postTranslate(finalX, finalY);
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
                    NotificacionActivity.showNotification(this, "Documento", "Documento firmado correctamente");
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






































