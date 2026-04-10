package de.labystudio.viaupdater.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Scanner;

public class HttpUtils {

    private static final String USER_AGENT = "ViaUpdater/1.0 (Java/" + System.getProperty("java.version") + ")";

    private static final Gson GSON = new GsonBuilder().create();

    @SuppressWarnings("unchecked")
    public static <T> T request(String url, Class<?> clazz) throws IOException {
        InputStream inputStream = openInputStream(url);
        StringBuilder json = new StringBuilder();
        Scanner scanner = new Scanner(inputStream);
        while (scanner.hasNextLine()) {
            json.append(scanner.nextLine());
        }
        return (T) GSON.fromJson(json.toString(), clazz);
    }

    public static InputStream openInputStream(
            String url,
            Map<String, String> headers
    ) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) URI.create(url).toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.addRequestProperty("User-Agent", USER_AGENT);
        headers.forEach(connection::addRequestProperty);

        int responseCode = connection.getResponseCode();

        if (responseCode == 301 || responseCode == 302 || responseCode == 307 || responseCode == 308) {
            String location = connection.getHeaderField("Location");
            HttpsURLConnection redirect = (HttpsURLConnection) URI.create(location).toURL().openConnection();
            redirect.setConnectTimeout(5000);
            redirect.setReadTimeout(5000);
            redirect.addRequestProperty("User-Agent", USER_AGENT);
            return redirect.getInputStream();
        }

        if (responseCode / 100 != 2) {
            InputStream errorStream = connection.getErrorStream();
            String body = errorStream != null ? new Scanner(errorStream).useDelimiter("\\A").next() : "(no body)";
            throw new IOException("Status code " + responseCode + " for " + url + ": " + body);
        }

        return connection.getInputStream();
    }

    public static InputStream openInputStream(String url) throws IOException {
        return openInputStream(url, Map.of());
    }
}

