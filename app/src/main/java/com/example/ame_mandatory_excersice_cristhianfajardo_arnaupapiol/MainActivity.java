package com.example.ame_mandatory_excersice_cristhianfajardo_arnaupapiol;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.os.AsyncTask;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    final String apiKey = "1da2abd6dfa94b88b25f5ac67aef5bd6";
    Button btTempActual;
    Button btEstadoCielo;
    Button btDescription;
    Button btSunriseSunset;
    Button btAfegir;
    EditText texto;
    Spinner listaCiudades;
    List<String> ciudades;
    String ciudadEscogida;  //Se guarda en formato City:Country --> Tarragona:ES
    String apiUrl;

    int necesidad = 0; //1 temperatura actual, 2 estado del cielo
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btEstadoCielo = findViewById(R.id.bt2);
        btTempActual = findViewById(R.id.bt1);
        listaCiudades = findViewById(R.id.CityList);
        btDescription = findViewById(R.id.btDescription);
        btSunriseSunset = findViewById(R.id.btSunsetSunrise);
        btAfegir = findViewById(R.id.btAfegir);
        texto = findViewById(R.id.newCity);

        ciudades = new ArrayList<>();

        ciudades.add("Tarragona:ES");
        ciudades.add("Barcelona:ES");
        ciudades.add("Lleida:ES");
        ciudades.add("Girona:ES");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, ciudades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        listaCiudades.setAdapter(adapter);


        btTempActual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ciudadEscogida = listaCiudades.getSelectedItem().toString();
                necesidad = 1;
                new WheatherDataTask().execute();
            }
        });

        btEstadoCielo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ciudadEscogida = listaCiudades.getSelectedItem().toString();
                necesidad = 2;
                new WheatherDataTask().execute();
            }
        });

        btDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ciudadEscogida = listaCiudades.getSelectedItem().toString();
                necesidad = 4;
                new WheatherDataTask().execute();
            }
        });

        btSunriseSunset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ciudadEscogida = listaCiudades.getSelectedItem().toString();
                necesidad = 3;
                new WheatherDataTask().execute();
            }
        });

        btAfegir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    String ciudad = texto.getText().toString();
                    if(comprobarEstructura(ciudad)){
                        if(ciudad.split(":")[1].length()==2){
                            if(!ciudades.contains(ciudad)){
                                ciudades.add(ciudad);
                                listaCiudades.setAdapter(adapter);
                                Toast.makeText(getApplicationContext(), "ciudad añadida a la lista", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(getApplicationContext(), "ciudad duplicada", Toast.LENGTH_SHORT).show();
                            }

                        }else{
                            Toast.makeText(getApplicationContext(), "El pais tiene que tener 2 letras en el codigo", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(getApplicationContext(), "Formato de texto incorrecto", Toast.LENGTH_SHORT).show();
                    }
                }catch(Exception e){
                    Toast.makeText(getApplicationContext(), "Error en el formato", Toast.LENGTH_SHORT).show();

                }

            }
        });
    }

    private boolean comprobarEstructura(String ciudad) {
        boolean encontrado = false;
        int i = 0;
        while(i<ciudad.length() && !encontrado){
            if(ciudad.charAt(i) == ':'){
                encontrado = true;
            }
            i++;
        }
        return encontrado;
    }

    private class WheatherDataTask extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void... voids) {
            try{
                apiUrl = "http://api.weatherbit.io/v2.0/current?city="+ciudadEscogida.split(":")[0]+"&country="+ciudadEscogida.split(":")[1]+"&key="+apiKey;
                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();

                String line;
                while((line = reader.readLine()) != null){
                    result.append(line);
                    Log.d("json","Vuelta: "+result);
                }

                urlConnection.disconnect();
                return result.toString();
            }catch(Exception e){
                return null;
            }

        }

        @Override
        protected void onPostExecute(String response){
            String datos = "";
            CustomDialog custom;
            try{
                if(response != null){
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray dataArray = jsonResponse.getJSONArray("data");
                    int icono = 0;
                    if(dataArray.length() > 0){
                        if(necesidad == 1){
                            JSONObject dataObject = dataArray.getJSONObject(0);
                            String tempActual = String.valueOf(dataObject.getDouble("app_temp"));
                            datos = "La temperatura en "+ciudadEscogida.split(":")[0]+" es de "+tempActual+"C";
                            custom = new CustomDialog(MainActivity.this, datos);
                            custom.show();
                        }else if(necesidad == 2){
                            JSONObject dataObject = dataArray.getJSONObject(0);
                            String nubosidad = String.valueOf(dataObject.getInt("clouds"));
                            datos = "La nubosidad en "+ciudadEscogida.split(":")[0]+" es del "+nubosidad+"%";
                            custom = new CustomDialog(MainActivity.this, datos);
                            custom.show();
                        }else if(necesidad == 3){
                            JSONObject dataObject = dataArray.getJSONObject(0);
                            String sunrise = dataObject.getString("sunrise");
                            String sunset = dataObject.getString("sunset");
                            datos = "La puesta de sol en "+ciudadEscogida.split(":")[0]+" es a las "+sunset+"\n" +
                                    "La salida del sol en"+ciudadEscogida.split(":")[0]+" es a las "+sunrise;
                            custom = new CustomDialog(MainActivity.this, datos);
                            custom.show();
                        }else{
                            JSONObject dataObject = dataArray.getJSONObject(0);
                            JSONObject weatherObject = dataObject.getJSONObject("weather");
                            String description = weatherObject.getString("description");
                            datos = "La descripción de la API para " + ciudadEscogida.split(":")[0] + " es " + description;
                            String iconCode = weatherObject.getString("icon");
                            String imageUrl = "https://cdn.weatherbit.io/static/img/icons/" + iconCode + ".png";
                            Log.d("json", imageUrl+" // "+ iconCode);
                            custom = new CustomDialog(MainActivity.this, datos,imageUrl);
                            custom.show();

                        }

                    }else{
                        datos = "Parece que no hay datos de la ciudad introducida i/o no existen";
                        custom = new CustomDialog(MainActivity.this, datos);
                        custom.show();
                    }
                }else{
                    custom = new CustomDialog(MainActivity.this, "Se ha llegado al maximo de requests");
                    custom.show();
                }


            }catch(Exception e){
                e.printStackTrace();
            }

        }
    }
}