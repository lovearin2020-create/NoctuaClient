package dev.lumen.online;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

/** Calls the Lumen backend (Supabase). The key below is the public "anon" key: it can only run the functions the database allows. */
public final class Api {
    static final String BASE = System.getProperty("lumen.api", "https://nrmujjzxqjvmbvuwjvdv.supabase.co");
    private static final String KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5ybXVqanp4cWp2bWJ2dXdqdmR2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTUwMDI4MzAsImV4cCI6MjExMDU3ODgzMH0.O6Mz_BStP78lU-ObYVERcoXkQMzacgLze2JC8hTYnao";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private Api() {}

    /** The backend said no; {@link #getMessage()} is its code, e.g. LUMEN_NOT_ENOUGH. */
    static final class Refused extends Exception {
        Refused(String code) { super(code); }
    }

    /** Address of one of the backend's functions, and the public key every call to it carries. */
    public static String function(String name) { return BASE + "/functions/v1/" + name; }
    public static void sign(HttpURLConnection c) { c.setRequestProperty("apikey", KEY); c.setRequestProperty("Authorization", "Bearer " + KEY); }

    static JsonElement rpc(String function, JsonObject args) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(BASE + "/rest/v1/rpc/" + function).openConnection();
        try {
            c.setConnectTimeout(8000);
            c.setReadTimeout(12000);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("apikey", KEY);
            c.setRequestProperty("Authorization", "Bearer " + KEY);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("User-Agent", "NoctuaClient");
            OutputStream out = c.getOutputStream();
            out.write(args.toString().getBytes(UTF8));
            out.close();
            int code = c.getResponseCode();
            String body = read(code >= 400 ? c.getErrorStream() : c.getInputStream());
            if (code >= 400) {
                String msg = "HTTP " + code;
                try { msg = new JsonParser().parse(body).getAsJsonObject().get("message").getAsString(); } catch (Throwable ignored) {}
                throw new Refused(msg);
            }
            return body.isEmpty() ? null : new JsonParser().parse(body);
        } finally {
            c.disconnect();
        }
    }

    private static String read(InputStream in) throws Exception {
        if (in == null) return "";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int n; (n = in.read(buf)) > 0; ) bo.write(buf, 0, n);
        in.close();
        return new String(bo.toByteArray(), UTF8);
    }
}
