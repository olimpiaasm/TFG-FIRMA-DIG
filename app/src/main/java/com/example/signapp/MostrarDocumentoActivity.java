package com.example.signapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MostrarDocumentoActivity extends AppCompatActivity {

    private static final String PREF_IMPORTED_CERTIFICATE_PATH_PREFIX = "imported_certificate_";
    private ImageView pdfImageView;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor parcelFileDescriptor;
    private FloatingActionButton fab;
    private FloatingActionButton saveFab;
    private String selectedCertificatePath;
    private float startX, startY, endX, endY;
    private Bitmap pdfBitmap;
    private Canvas pdfCanvas;
    private Paint paint;
    private View selectionOverlay;
    private boolean areaSelected = false; // Flag to control area selection

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mostrardocumento);

        pdfImageView = findViewById(R.id.pdfImageView);
        fab = findViewById(R.id.fab);
        saveFab = findViewById(R.id.save_fab);

        String documentPath = getIntent().getStringExtra("documentPath");
        openPdfRenderer(documentPath);
        showPage(0); // Muestra la primera página

        fab.setOnClickListener(view -> {
            if (!areaSelected) {
                Toast.makeText(this, "Seleccionar área en el PDF para firmar", Toast.LENGTH_SHORT).show();
                enablePdfSignatureMode();
            } else {
                Toast.makeText(this, "Área ya seleccionada. Por favor, firme el documento o cancele la selección.", Toast.LENGTH_SHORT).show();
            }
        });

        saveFab.setOnClickListener(view -> {
            saveSignedPdf();
        });

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(getResources().getColor(android.R.color.holo_blue_light));

        selectionOverlay = new View(this) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                if (startX != endX && startY != endY) {
                    canvas.drawRect(startX, startY, endX, endY, paint);
                }
            }
        };
        addContentView(selectionOverlay, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
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

    private void enablePdfSignatureMode() {
        pdfImageView.setOnTouchListener((v, event) -> {
            if (areaSelected) return false; // Prevent further selection if area already selected
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    endX = event.getX();
                    endY = event.getY();
                    selectionOverlay.invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    endX = event.getX();
                    endY = event.getY();
                    selectionOverlay.invalidate();
                    areaSelected = true;
                    showCertificateSelectionDialog();
                    return true;
            }
            return false;
        });
    }

    private void showCertificateSelectionDialog() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, ?> allEntries = preferences.getAll();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar Certificado");

        List<String> certificateAliases = new ArrayList<>();
        List<String> certificatePaths = new ArrayList<>();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith(PREF_IMPORTED_CERTIFICATE_PATH_PREFIX)) {
                certificateAliases.add(entry.getKey().replace(PREF_IMPORTED_CERTIFICATE_PATH_PREFIX, ""));
                certificatePaths.add((String) entry.getValue());
            }
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
            areaSelected = false; // Allow re-selection if canceled
        });
        builder.show();
    }

    private void showSignaturePreview() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Vista previa de la firma");

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(createSignaturePreviewBitmap());

        builder.setView(imageView);

        builder.setPositiveButton("Firmar", (dialog, which) -> {
            addSignatureToPdf();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.dismiss();
            areaSelected = false; // Allow re-selection if canceled
        });
        builder.show();
    }

    private Bitmap createSignaturePreviewBitmap() {
        Bitmap signatureBitmap = Bitmap.createBitmap((int) (endX - startX), (int) (endY - startY), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(signatureBitmap);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(android.R.color.holo_red_dark));
        paint.setTextSize(16);
        canvas.drawText("Firma Digital", 10, (signatureBitmap.getHeight() / 2), paint);
        canvas.drawText("Fecha: " + java.time.LocalDateTime.now(), 10, (signatureBitmap.getHeight() / 2) + 20, paint);
        return signatureBitmap;
    }

    private void addSignatureToPdf() {
        pdfCanvas.drawBitmap(createSignaturePreviewBitmap(), startX, startY, null);
        pdfImageView.setImageBitmap(pdfBitmap);
        Toast.makeText(this, "Firma añadida en el área seleccionada", Toast.LENGTH_SHORT).show();
    }

    private void saveSignedPdf() {
        try {
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pdfBitmap.getWidth(), pdfBitmap.getHeight(), 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            canvas.drawBitmap(pdfBitmap, 0, 0, null);
            document.finishPage(page);

            File signedPdfFile = new File(getExternalFilesDir(null), "signed_document.pdf");
            FileOutputStream fos = new FileOutputStream(signedPdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            Toast.makeText(this, "Documento firmado guardado en: " + signedPdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error guardando el documento firmado", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closePdfRenderer();
    }
}












