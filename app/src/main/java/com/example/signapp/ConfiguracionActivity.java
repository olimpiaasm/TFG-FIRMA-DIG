package com.example.signapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class ConfiguracionActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView textViewRutaDocumento;
    private TextView textViewRutaCertificate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        textViewRutaDocumento = findViewById(R.id.textViewRutaDocumento);
        textViewRutaCertificate = findViewById(R.id.textViewRutaCertificate);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra("filePath");
        String certificatePath = intent.getStringExtra("certificatePath");

        if (filePath != null) {
            textViewRutaDocumento.setText(filePath);
        }
        if (certificatePath != null) {
            textViewRutaCertificate.setText(certificatePath);
        }
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
