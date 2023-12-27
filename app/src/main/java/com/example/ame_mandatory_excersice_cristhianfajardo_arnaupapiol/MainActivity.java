package com.example.ame_mandatory_excersice_cristhianfajardo_arnaupapiol;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    final String apiKey = "1da2abd6dfa94b88b25f5ac67aef5bd6";
    TextView textTempActual;
    TextView textEstadoCielo;
    TextView textDescription;
    TextView textSunriseSunset;
    Button btAfegir;
    EditText texto;
    ImageView image;
    Spinner listaCiudades;
    List<String> ciudades;
    String ciudadEscogida;  //Se guarda en formato City:Country --> Tarragona:ES
    String apiUrl;

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private Button connectButton;

    int necesidad = 0; //1 temperatura actual, 2 estado del cielo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textEstadoCielo = findViewById(R.id.textNubosidad);
        textTempActual = findViewById(R.id.textTemp);
        listaCiudades = findViewById(R.id.CityList);
        textDescription = findViewById(R.id.textDescription);
        textSunriseSunset = findViewById(R.id.textSunriseSunset);
        btAfegir = findViewById(R.id.btAfegir);
        texto = findViewById(R.id.newCity);
        connectButton = findViewById(R.id.btInfoPlaca);
        image = findViewById(R.id.imageWeather);

        //TEXTOS//
        ciudades = new ArrayList<>();

        ciudades.add("Tarragona:ES");
        ciudades.add("Barcelona:ES");
        ciudades.add("Lleida:ES");
        ciudades.add("Girona:ES");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ciudades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        listaCiudades.setAdapter(adapter);
        listaCiudades.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ciudadEscogida = listaCiudades.getSelectedItem().toString();
                new WheatherDataTask().execute();
                Log.d("task", "entro");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast("El dispositivo no es compatible con Bluetooth");
            //finish();
        } else {
            showToast("El dispositivo es compatible");
            if (!bluetoothAdapter.isEnabled()) {
                showToast("Bluetooth desactivado, solicitando activación...");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conectToDevice();
            }
        });


        btAfegir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String ciudad = texto.getText().toString();
                    if (comprobarEstructura(ciudad)) {
                        if (ciudad.split(":")[1].length() == 2) {
                            if (!ciudades.contains(ciudad)) {
                                ciudades.add(ciudad);
                                Toast.makeText(getApplicationContext(), "ciudad añadida a la lista", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "ciudad duplicada", Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            Toast.makeText(getApplicationContext(), "El pais tiene que tener 2 letras en el codigo", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Formato de texto incorrecto", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Error en el formato", Toast.LENGTH_SHORT).show();

                }

            }
        });
    }

    private void conectToDevice() {        // Aquí deberías manejar la lógica para descubrir dispositivos y conectar al dispositivo específico
        // En este ejemplo, asumimos que ya tienes la dirección MAC del dispositivo
        String deviceAddress = "00:0E:EA:CF:61:3D"; // Reemplaza con la dirección MAC de tu módulo

        // UUID estándar para módulos Bluetooth serie (SPP)
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {
            // Antes de conectar, asegúrate de que el dispositivo no esté ocupado
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                showToast("Permisos de Bluetooth no otorgados. Solicitando permisos...");
                // Solicitud de permisos
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_PERMISSION);
                return;
            }
            bluetoothAdapter.cancelDiscovery();
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();

            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();

            showToast("Conexión establecida con éxito");
            startReadingThread(); // Inicia un hilo para leer datos entrantes

        } catch (IOException e) {
            Log.d("error", e.toString());
            e.printStackTrace();
        }
    }

    private void startReadingThread() {
        // Este hilo se encargará de leer datos en segundo plano
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;

                while (true) {
                    try {
                        bytes = inputStream.read(buffer);
                        String receivedMessage = new String(buffer, 0, bytes);
                        Log.d("env", "funciona");
                        // Envía el mensaje recibido al hilo principal para actualizar la interfaz de usuario
                        handler.obtainMessage(0, receivedMessage).sendToTarget();

                    } catch (IOException e) {
                        break; // Si hay un error, salimos del bucle
                    }
                }
            }
        });

        thread.start();
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            String receivedMessage = (String) msg.obj;
            Log.d("env", receivedMessage);
            return true;
        }
    });

    protected void onDestroy() {
        super.onDestroy();
        // Cerrar la conexión Bluetooth al salir de la aplicación
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean comprobarEstructura(String ciudad) {
        int contador = 0;
        for (int i = 0; i < ciudad.length(); i++) {
            if (ciudad.charAt(i) == ':') {
                contador++;
            }
        }
        return contador == 1;
    }

    private class WheatherDataTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            try {
                apiUrl = "http://api.weatherbit.io/v2.0/current?city=" + ciudadEscogida.split(":")[0] + "&country=" + ciudadEscogida.split(":")[1] + "&key=" + apiKey;
                URL url = new URL(apiUrl);
                Log.d("task", apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                urlConnection.disconnect();
                return result.toString();
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("task", e.toString());
                return null;
            }

        }

        @Override
        protected void onPostExecute(String response) {
            String datos = "";
            try {
                if (response != null) {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray dataArray = jsonResponse.getJSONArray("data");
                    int icono = 0;
                    if (dataArray.length() > 0) {
                        //Recoger datos Temperatura
                        JSONObject dataObject = dataArray.getJSONObject(0);
                        String datoJson = String.valueOf(dataObject.getDouble("app_temp"));
                        datos = "La temperatura en " + ciudadEscogida.split(":")[0] + " es de " + datoJson + "C";
                        textTempActual.setText(datos+"\n");

                        //Recoger datos nubosidad
                        datoJson = String.valueOf(dataObject.getInt("clouds"));
                        datos = "La nubosidad en " + ciudadEscogida.split(":")[0] + " es del " + datoJson + "%";
                        textEstadoCielo.setText(datos+"\n");

                        //Recoger datos sunrise/sunset
                        String sunrise = dataObject.getString("sunrise");
                        String sunset = dataObject.getString("sunset");
                        datos = "La puesta de sol en " + ciudadEscogida.split(":")[0] + " es a las " + sunset + "\n" +
                                "La salida del sol en" + ciudadEscogida.split(":")[0] + " es a las " + sunrise;
                        textSunriseSunset.setText(datos+"\n");

                        //Recoger datos descrición
                        JSONObject weatherObject = dataObject.getJSONObject("weather");
                        datoJson = weatherObject.getString("description");
                        datos = "La descripción de la API para " + ciudadEscogida.split(":")[0] + " es " + datoJson;
                        String iconCode = weatherObject.getString("icon");
                        String imageUrl = "https://cdn.weatherbit.io/static/img/icons/" + iconCode + ".png";
                        textDescription.setText(datos+"\n");
                        new LoadImageTask(image).execute(imageUrl);


                    } else {
                        datos = "Parece que no hay datos de la ciudad introducida i/o no existen";
                        textTempActual.setText(datos);
                        textEstadoCielo.setText("");
                        textSunriseSunset.setText("");
                        textDescription.setText("");
                        image.setImageBitmap(null);
                    }
                } else {
                    datos = "Se ha llegado al maximo de requests";
                    textTempActual.setText(datos);
                    textEstadoCielo.setText("");
                    textSunriseSunset.setText("");
                    textDescription.setText("");
                    image.setImageBitmap(null);
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private WeakReference<ImageView> imageViewReference;

        LoadImageTask(ImageView imageView) {
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                return BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView imageView = imageViewReference.get();
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}