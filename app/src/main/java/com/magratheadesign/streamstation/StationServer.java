package com.magratheadesign.streamstation;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by ejntoo on 03/10/14.
 * HTTP server
 */
public class StationServer extends Thread {
    final String TAG = Constants.APPLICATION_TAG;
    Context context;
    Handler handler;
    AssetManager assets;
    boolean running = false;
    ServerSocket serverSocket = null;
    int port = 9090;

    public StationServer(Context _context, Handler _handler) {
        context = _context;
        handler = _handler;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        port = Integer.parseInt(sharedPref.getString("pref_port", "9090"));
    }

    @Override
    public void run() {
        running = true;
        assets = context.getAssets();
        try {
            serverSocket = new ServerSocket(port);
            Log.i(TAG, "Server started");

            // repeatedly wait for connections, and process
            while (running) {
                Socket clientSocket = serverSocket.accept();
                new RequestThread(clientSocket).start();
            }

        } catch (IOException e) {
            Log.w(TAG, "Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeSocket();
        }
    }

    private void sendMessage(String text, HashMap<String, String> params) {
        Message msg = handler.obtainMessage();
        Bundle b = new Bundle();
        if (text.equals("download")) {
            b.putString("action", "download");
            b.putString("url", params.get("url"));
        } else if (text.equals("single")) {
            b.putString("action", params.get("action"));
            b.putString("id", params.get("id"));
        }
        msg.setData(b);
        handler.sendMessage(msg);
    }

    public void closeSocket() {
        running = false;
        try {
            serverSocket.close();
            Log.i(TAG, "Socket closed");
        } catch (Exception e) {
            Log.w(TAG, "Could not close server socket: " + e.getMessage());
        }
    }

    public class RequestThread extends Thread {

        private Socket clientSocket;

        public RequestThread(Socket _clientSocket) {
            clientSocket = _clientSocket;
        }

        @Override
        public void run() {
            Log.i(TAG, "New client connected");

            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedOutputStream out = new BufferedOutputStream(clientSocket.getOutputStream());

                String requestHeader = "";
                String line = in.readLine();
                while (line != null && in.ready()) {
                    if (line.equals("")) break;
                    requestHeader += line + "\n";
                    line = in.readLine();
                }

                // reads the POST requests, disabled
                String requestBody = "";
                /*char ch;
                Log.i(TAG, "Starting reading...");
                if (in.ready()) ch = (char) in.read();
                Log.i(TAG, "Started reading...");
                while (in.ready()) {
                    requestBody += ch;
                    ch = (char) in.read();
                }
                requestBody += ch;*/
                Log.i(TAG, requestBody);
                requestHeader += requestBody;

                BufferedInputStream webPageReader = null; // stream for the web page to load
                String mimeType= ""; // mimetype to pass to the server

                String requestString = ServerUtils.parseRequest("GET", requestHeader, requestBody); // returns GET line, e.g. 'GET /page?key=param HTTP/1.1'
                if (requestString != null) {
                    /*
                    Parsing GET request
                    */
                    String requestPage = ServerUtils.parseRequestLine(requestString); // gets requested page
                    Log.i(TAG, "Page: " + requestPage);

                    HashMap<String, String> map = ServerUtils.parseRequestParams(requestString); // gets key-value request params
                    if (map != null) Log.i(TAG, "Params:" + map.toString());

                    String defaultFile = "index.html";
                    /*
                    Detects the page to load and the mimetype
                     */
                    if (requestPage.equals("")) {
                        webPageReader = new BufferedInputStream(assets.open(defaultFile));
                        mimeType= "text/html";
                    } else if (requestPage.equals("download")) { // adds a new download
                        if (map != null) sendMessage("download", map);
                        webPageReader = new BufferedInputStream(assets.open(defaultFile));
                        mimeType= "text/html";
                    } else if (requestPage.equals("single")) { // performs an action on a single download; generally to be called through AJAX
                        if (map != null) sendMessage("single", map);
                        webPageReader = ServerUtils.getActionResponse(map);
                        mimeType= "text/html";
                    } else if (requestPage.equals("list")) {
                        webPageReader = ServerUtils.getJsonDownloadList(context); // returns the list of the current downloads
                        mimeType= "application/json";
                    } else if (Arrays.asList(assets.list("")).contains(requestPage)) {
                        webPageReader = new BufferedInputStream(assets.open(requestPage));
                        mimeType = URLConnection.guessContentTypeFromName(requestPage);
                        if (mimeType == null) {
                            String[] requestPageArr = requestPage.split("\\.");
                            if (requestPageArr.length > 0 && requestPageArr[requestPageArr.length - 1].equals("json")) mimeType = "application/json";
                        }
                    } else {
                        webPageReader = new BufferedInputStream(assets.open("404.html"));
                        mimeType= "text/html";
                    }
                }

                byte[] buffer = new byte[2048];
                int length;

                Log.i(TAG, "Writing response...");
                out.write("HTTP/1.1 200 OK\r\n".getBytes());
                out.write("Cache-Control: no-cache\r\n".getBytes());
                out.write(("Content-Type: " + mimeType + "; charset=utf-8\r\n").getBytes());
                out.write("Connection: Keep-Alive\r\n".getBytes());
                out.write("Keep-Alive: timeout=15, max=100\r\n".getBytes());
                out.write("Server: StreamStation\r\n".getBytes());
                out.write("\r\n".getBytes());

                try {
                    while ((length = webPageReader.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                } catch (NullPointerException e) {}
                Log.i(TAG, "Response written");

                out.close();
                in.close();
                clientSocket.close();
            } catch (IOException e) {
                Log.w(TAG, "Could not execute action: " + e.toString() + " " + e.getMessage());
            }
        }
    }
}
