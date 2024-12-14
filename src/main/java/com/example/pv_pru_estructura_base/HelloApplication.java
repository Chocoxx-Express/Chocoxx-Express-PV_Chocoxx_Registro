// src\main\java\com\example\pv_pru_estructura_base\HelloApplication.java
package com.example.pv_pru_estructura_base;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class HelloApplication {
    private static final int PORT = 8080;
    private static final String BASE_DIR = "src/main/resources/";
    private static final String DATA_FILE = BASE_DIR + "Data.json";
    private static final String CARD_FILE = BASE_DIR + "card.html";

    public static void main(String[] args) {
        new HelloApplication().startServer();
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado en http://localhost:" + PORT);
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     OutputStream out = clientSocket.getOutputStream()) {

                    String requestLine = in.readLine();
                    if (requestLine == null) continue;

                    System.out.println("Solicitud: " + requestLine);
                    String[] requestParts = requestLine.split(" ");
                    String method = requestParts[0];
                    String path = requestParts[1];

                    if (method.equals("POST") && path.equals("/submit")) {
                        String body = readRequestBody(in);
                        handlePostRequest(body, out);
                    } else if (method.equals("GET") && path.equals("/data")) {
                        handleDataRequest(out);
                    } else if (method.equals("GET")) {
                        handleGetRequest(path, out);
                    } else {
                        sendResponse(out, 404, "text/plain", "Not Found");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePostRequest(String body, OutputStream out) throws IOException {
        try {
            JSONObject formData = new JSONObject(body);
            String email = formData.getString("email");

            // Verificar si el correo ya está registrado
            if (isEmailRegistered(email)) {
                sendResponse(out, 400, "application/json", "{\"message\": \"El correo ya está registrado\"}");
                return;
            }

            // Guardar datos en Data.json
            saveToDataFile(formData);

            // Enviar correo de bienvenida
            sendWelcomeEmail(email, formData.getString("name"));

            sendResponse(out, 200, "application/json", "{\"message\": \"Registro completado y correo enviado\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(out, 500, "application/json", "{\"message\": \"Error en el servidor\"}");
        }
    }

    private boolean isEmailRegistered(String email) throws IOException {
        File file = new File(DATA_FILE);

        if (!file.exists()) {
            return false;
        }

        String content = new String(Files.readAllBytes(file.toPath()));
        JSONArray jsonArray = new JSONArray(content);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject user = jsonArray.getJSONObject(i);
            if (user.getString("email").equalsIgnoreCase(email)) {
                return true;
            }
        }

        return false;
    }

    private void saveToDataFile(JSONObject data) throws IOException {
        File file = new File(DATA_FILE);
        JSONArray jsonArray;

        if (file.exists()) {
            String content = new String(Files.readAllBytes(file.toPath()));
            jsonArray = new JSONArray(content);
        } else {
            jsonArray = new JSONArray();
        }

        jsonArray.put(data);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jsonArray.toString(4));
        }
    }

    private void sendWelcomeEmail(String recipient, String name) throws Exception {
        // Configuración del servidor SMTP
        String host = "smtp.gmail.com";
        String username = "brayanprendavindas@gmail.com";
        String password = "oemopxriudmzhbov";
        int port = 587;

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        // Leer el contenido de card.html y personalizarlo
        String cardHtml = new String(Files.readAllBytes(new File(CARD_FILE).toPath()));
        cardHtml = cardHtml.replace("{{name}}", name);

        // Configurar el mensaje
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username, "Brayan Prenda Vindas"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject("¡Bienvenid@ a nuestra página!");
        message.setContent(cardHtml, "text/html");

        // Enviar el mensaje
        Transport.send(message);
        System.out.println("Correo enviado a: " + recipient);
    }

    private void handleDataRequest(OutputStream out) throws IOException {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            String jsonData = new String(Files.readAllBytes(file.toPath()));
            sendResponse(out, 200, "application/json", jsonData.getBytes());
        } else {
            sendResponse(out, 200, "application/json", "[]".getBytes());
        }
    }

    private void handleGetRequest(String path, OutputStream out) throws IOException {
        File file = new File(BASE_DIR + (path.equals("/") ? "index.html" : path.substring(1)));
        if (file.exists() && !file.isDirectory()) {
            String contentType = Files.probeContentType(file.toPath());
            byte[] content = Files.readAllBytes(file.toPath());
            sendResponse(out, 200, contentType, content);
        } else {
            sendResponse(out, 404, "text/plain", "File Not Found".getBytes());
        }
    }

    private String readRequestBody(BufferedReader in) throws IOException {
        String line;
        StringBuilder body = new StringBuilder();
        while (!(line = in.readLine()).isEmpty()) {
            // Leer las cabeceras hasta la línea vacía
        }
        while (in.ready()) {
            body.append((char) in.read());
        }
        return body.toString();
    }

    private void sendResponse(OutputStream out, int statusCode, String contentType, String body) throws IOException {
        sendResponse(out, statusCode, contentType, body.getBytes());
    }

    private void sendResponse(OutputStream out, int statusCode, String contentType, byte[] body) throws IOException {
        out.write(("HTTP/1.1 " + statusCode + " OK\r\n").getBytes());
        out.write(("Content-Type: " + contentType + "\r\n").getBytes());
        out.write(("Content-Length: " + body.length + "\r\n").getBytes());
        out.write("\r\n".getBytes());
        out.write(body);
        out.flush();
    }
}
