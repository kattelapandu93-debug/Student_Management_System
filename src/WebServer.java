import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;

/** Small web layer on top of the existing console project. Uses only the JDK + your JDBC code. */
public class WebServer {

    static final Set<String> tokens = ConcurrentHashMap.newKeySet();
    static final Path WEB_DIR = Paths.get("frontend").toAbsolutePath().normalize();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/", WebServer::route);
        server.start();
        System.out.println("Open http://localhost:8081 in your browser");
    }

    static void route(HttpExchange ex) throws IOException {
        try {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            String method = ex.getRequestMethod(), path = ex.getRequestURI().getPath();

            if (method.equals("OPTIONS")) { send(ex, 204, "text/plain", ""); return; }
            if (!path.startsWith("/api/")) { serveFile(ex, path); return; }

            if (path.equals("/api/login") && method.equals("POST")) { login(ex); return; }

            String auth = ex.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !tokens.contains(auth)) { error(ex, 401, "Please log in."); return; }

            String[] p = path.split("/");              // ["", "api", "students", "12"]
            String kind = p.length > 2 ? p[2] : "";
            String id = p.length > 3 ? p[3] : null;
            if (!kind.equals("students") && !kind.equals("teachers")) { error(ex, 404, "Unknown route."); return; }

            if (method.equals("GET") && id == null) list(ex, kind);
            else if (method.equals("POST") && id == null) add(ex, kind);
            else if (method.equals("DELETE") && id != null) delete(ex, kind, id);
            else error(ex, 405, "Method not allowed.");
        } catch (Exception e) {
            e.printStackTrace();
            error(ex, 500, "Server error: " + e.getMessage());
        }
    }

    static void login(HttpExchange ex) throws IOException {
        String body = body(ex);
        String id = field(body, "id"), pw = field(body, "password");
        if (id != null && pw != null && Login.verify(id, pw)) {
            String t = UUID.randomUUID().toString();
            tokens.add(t);
            send(ex, 200, "application/json", "{\"token\":\"" + t + "\"}");
        } else error(ex, 401, "Wrong login ID or password.");
    }

    static void list(HttpExchange ex, String kind) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(kind.equals("students") ? "SELECT * FROM student" : "SELECT * FROM teacher")) {
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;
                if (kind.equals("students"))
                    sb.append("{\"rollno\":").append(rs.getInt(1)).append(",\"name\":").append(q(rs.getString(2))).append("}");
                else
                    sb.append("{\"subject_id\":").append(rs.getInt(1)).append(",\"name\":").append(q(rs.getString(2)))
                      .append(",\"subject\":").append(q(rs.getString(3))).append("}");
            }
        }
        send(ex, 200, "application/json", sb.append("]").toString());
    }

    static void add(HttpExchange ex, String kind) throws Exception {
        String b = body(ex), name = field(b, "name");
        if (name == null || name.isBlank()) { error(ex, 400, "Name is required."); return; }
        try (Connection con = DBConnection.getConnection()) {
            if (kind.equals("students")) {
                String roll = field(b, "rollno");
                if (roll == null) { error(ex, 400, "Roll number is required."); return; }
                PreparedStatement ps = con.prepareStatement("INSERT INTO student VALUES (?, ?)");
                ps.setInt(1, Integer.parseInt(roll));
                ps.setString(2, name.trim());
                ps.executeUpdate();
            } else {
                String sid = field(b, "subject_id"), sub = field(b, "subject");
                if (sid == null || sub == null || sub.isBlank()) { error(ex, 400, "Subject ID and subject are required."); return; }
                PreparedStatement ps = con.prepareStatement("INSERT INTO teacher(subject_id, name, Subject) VALUES (?, ?, ?)");
                ps.setInt(1, Integer.parseInt(sid));
                ps.setString(2, name.trim());
                ps.setString(3, sub.trim());
                ps.executeUpdate();
            }
            send(ex, 201, "application/json", "{\"ok\":true}");
        } catch (SQLIntegrityConstraintViolationException e) {
            error(ex, 409, "That ID already exists.");
        } catch (NumberFormatException e) {
            error(ex, 400, "ID must be a number.");
        }
    }

    static void delete(HttpExchange ex, String kind, String id) throws Exception {
        String sql = kind.equals("students") ? "DELETE FROM student WHERE rollno=?" : "DELETE FROM teacher WHERE subject_id=?";
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, Integer.parseInt(id));
            if (ps.executeUpdate() > 0) send(ex, 200, "application/json", "{\"ok\":true}");
            else error(ex, 404, "Not found.");
        } catch (NumberFormatException e) {
            error(ex, 400, "ID must be a number.");
        }
    }

    // ---------- helpers ----------
    static void serveFile(HttpExchange ex, String path) throws IOException {
        if (path.equals("/")) path = "/index.html";
        Path f = WEB_DIR.resolve("." + path).normalize();
        if (!f.startsWith(WEB_DIR) || !Files.isRegularFile(f)) { send(ex, 404, "text/plain", "Not found"); return; }
        String n = f.getFileName().toString();
        String type = n.endsWith(".html") ? "text/html; charset=utf-8" : n.endsWith(".css") ? "text/css"
                    : n.endsWith(".js") ? "text/javascript" : "application/octet-stream";
        byte[] data = Files.readAllBytes(f);
        ex.getResponseHeaders().add("Content-Type", type);
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    static String body(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    static String field(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(?:\"((?:[^\"\\\\]|\\\\.)*)\"|(-?\\d+))").matcher(json);
        if (!m.find()) return null;
        return m.group(1) != null ? m.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : m.group(2);
    }

    static String q(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ").replace("\t", " ") + "\"";
    }

    static void error(HttpExchange ex, int code, String msg) throws IOException {
        send(ex, code, "application/json", "{\"error\":" + q(msg) + "}");
    }

    static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type.startsWith("application/json") ? "application/json; charset=utf-8" : type);
        ex.sendResponseHeaders(code, code == 204 ? -1 : b.length);
        if (code != 204) try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        ex.close();
    }
}
