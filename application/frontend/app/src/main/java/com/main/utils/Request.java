package com.main.utils;

import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
public class Request implements  Runnable{
    String urlString=null;
    String imageName=null;
    byte[] imageBytes=null;
    SharedResource sr=null;
    public Request(String urlString,String imageName,byte[] imageBytes,SharedResource sr){
        this.urlString=urlString;
        this.imageName=imageName;
        this.imageBytes=imageBytes;
        this.sr=sr;
    }
    @Override
    public void run() {
        JSONObject jo=new JSONObject();
        try{
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(2000);
            // Set headers
            String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream oStream=conn.getOutputStream();

            // Create a multipart/form-data request
            DataOutputStream outputStream = new DataOutputStream(oStream);
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + imageName + "\"\r\n");
            outputStream.writeBytes("\r\n");

            // Send the image bytes
            outputStream.write(imageBytes);
            outputStream.writeBytes("\r\n");
            outputStream.writeBytes("--" + boundary + "--\r\n");

            // Get the response
            String jsonResponse = null;
            int responseCode = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            // Handle the response
            if (responseCode == HttpURLConnection.HTTP_OK) {
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                jsonResponse = responseBuilder.toString();
                jo=new JSONObject(jsonResponse);
            }else{
                jo.put("error",true);
                jo.put("result","Network error");
            }
            sr.setResponse(jo);
            conn.disconnect();
            outputStream.close();
        }catch (Exception e){
            try {
                jo.put("error",true);
                jo.put("result","Network error");
            } catch (JSONException ex) {
                throw new RuntimeException(ex);
            }
            sr.setResponse(jo);
            Log.d("Netword Error : ",e.getMessage());
        }
    }
}