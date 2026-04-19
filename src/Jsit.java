//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.Executors;

/**
 * jsit — Just Serve It!
 * <p>A minimal static file HTTP server (JDK-only, zero dependencies).</p>
 *
 * <p>This CLI exposes a file or directory over HTTP with a very small surface area.
 * It is designed for quick, local use cases such as sharing a file, previewing
 * generated artifacts, or testing HTTP integrations.</p>
 *
 * <h2>Defaults</h2>
 * <ul>
 *   <li>Host: {@code localhost}</li>
 *   <li>Port: {@code 8080}</li>
 *   <li>Base path: {@code /}</li>
 * </ul>
 *
 * <h2>Usages</h2>
 *
 * <h3>1) Serve a directory (minimum)</h3>
 * <pre>{@code
 * jsit ./
 * }</pre>
 * <ul>
 *   <li>Serves the current directory at {@code http://localhost:8080/}</li>
 *   <li>Directory listing is generated for folders</li>
 *   <li>Files are served as binary with best-effort content type detection</li>
 * </ul>
 *
 * <h3>2) Serve a single file</h3>
 * <pre>{@code
 * jsit ./file.ext
 * }</pre>
 * <ul>
 *   <li>Exposes the file at {@code http://localhost:8080/file.ext}</li>
 *   <li>Response is the raw file content (no wrapping HTML)</li>
 *   <li>Content-Type is inferred via {@code Files.probeContentType}</li>
 * </ul>
 *
 * <h3>3) Serve under a custom base path</h3>
 * <pre>{@code
 * jsit ./ 123
 * }</pre>
 * <ul>
 *   <li>Serves the directory under {@code http://localhost:8080/123}</li>
 *   <li>All routes are prefixed with {@code /123}</li>
 *   <li>Example: {@code ./docs/a.pdf -> http://localhost:8080/123/docs/a.pdf}</li>
 * </ul>
 *
 * <h3>4) Custom base path and port</h3>
 * <pre>{@code
 * jsit ./ 123 8888
 * }</pre>
 * <ul>
 *   <li>Serves the directory under {@code http://localhost:8888/123}</li>
 *   <li>Port is configurable via the third argument</li>
 * </ul>
 *
 * <h2>Behavior</h2>
 * <ul>
 *   <li>Uses {@code com.sun.net.httpserver.HttpServer}</li>
 *   <li>Directory requests return a simple HTML listing</li>
 *   <li>File requests stream content directly (no full buffering required)</li>
 *   <li>Paths are matched using prefix semantics of {@code HttpServer}</li>
 * </ul>
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>No authentication, caching, or range requests by design</li>
 *   <li>Intended for local/development usage, not production deployment</li>
 * </ul>
 *
 * @author yavuztas
 */
public class Jsit {

    static class DirectoryList {

        final StringBuilder sb;

        private DirectoryList() {
            this.sb = new StringBuilder(4096);
        }

        static String joinPath(String base, String name) {
            if (base.endsWith("/")) return base + name;
            return base + "/" + name;
        }

        static String urlEncode(String s) {
            return URLEncoder.encode(s, StandardCharsets.UTF_8)
                .replace("+", "%20"); // keep spaces readable
        }

        static void escapeHtml(StringBuilder sb, String s) {
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '&' -> sb.append("&amp;");
                    case '<' -> sb.append("&lt;");
                    case '>' -> sb.append("&gt;");
                    case '"' -> sb.append("&quot;");
                    default -> sb.append(c);
                }
            }
        }

        String render(Path dir, String requestPath) throws IOException {
            sb.setLength(0); // clear

            sb.append("<!DOCTYPE html>\n");
            sb.append("<html><head><meta charset=\"UTF-8\"><title>Index of ");
            escapeHtml(sb, requestPath);
            sb.append("</title></head><body>\n");

            sb.append("<h1>Index of ");
            sb.append("<small>");
            escapeHtml(sb, requestPath);
            sb.append("</small>");
            sb.append("</h1>\n");
            sb.append("<ul>\n");

            // Parent link (if not root)
            if (!"/".equals(requestPath)) {
                sb.append("<li><a href=\"../\">..</a></li>\n");
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    String name = entry.getFileName().toString();
                    boolean isDir = Files.isDirectory(entry);

                    sb.append("<li><a href=\"");
                    sb.append(joinPath(requestPath, urlEncode(name)));
                    if (isDir) sb.append("/"); // trailing slash for dirs
                    sb.append("\">");

                    escapeHtml(sb, name);
                    if (isDir) sb.append("/");
                    sb.append("</a></li>\n");
                }
            }

            sb.append("</ul>\n");
            sb.append("</body></html>\n");

            return sb.toString();
        }

    }

    public static void main(String[] args) throws Exception {

        if (args.length > 0 && args[0].equals("--")) { // safe for jbang with "--"
            args = Arrays.copyOfRange(args, 1, args.length);
        }

        if (args.length < 1) {
            System.out.println("Usage: jsit <files> <route|/> <port|8080>");
            return;
        }

        Path files = Paths.get(args[0]);
        if (!Files.exists(files)) {
            System.err.printf("file or folder is not found: %s%n", files);
            return;
        }

        String route = (args.length > 1) ? args[1] : "/";
        String port = (args.length > 2) ? args[2] : "8080";
        final int portInteger;
        try {
            portInteger = Integer.parseInt(port) & 0xFFFF; // limit to range [0 - 65535]
        } catch (Exception e) {
            System.err.println("port must be an integer");
            return;
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(portInteger), 0);
        server.createContext(route, exchange -> handle(exchange, files, route, new DirectoryList()));
        if (!"/".equals(route)) {
            server.createContext("/", Jsit::defaultHandler); // fallback
        }
        server.setExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        server.start();

        printBranding();
        System.out.println("Serving: " + files);
        System.out.printf("URL: http://localhost:%d%s%n", portInteger, route);
    }

    private static void printBranding() {
        System.out.print("""
                 ██╗ ███████╗██╗████████╗
                 ██║ ██╔════╝██║╚══██╔══╝
                 ██║ ███████╗██║   ██║  \s
            ██   ██║ ╚════██║██║   ██║  \s
            ╚█████╔╝ ███████║██║   ██║  \s
             ╚════╝  ╚══════╝╚═╝   ╚═╝  \s
            """);
    }

    private static void defaultHandler(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(404, -1);
        exchange.close();
    }

    private static void handle(HttpExchange exchange, Path files, String basePath, DirectoryList directoryList) throws IOException {
        final URI requestURI = exchange.getRequestURI();

        if (!Files.isDirectory(files)) { // single file mapping
            if (!requestURI.getPath().equals(basePath)) { // apply exact match
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
        }

        // resolve relative path
        final String requestPath = requestURI.getPath();
        final Path relativized = Path.of(basePath).relativize(Path.of(requestPath));
        Path resolvedPath = files.resolve(relativized);

        if (!Files.exists(resolvedPath)) { // not found
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        if (Files.isDirectory(resolvedPath)) {
            final String html = directoryList.render(resolvedPath, requestURI.getPath());
            final byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
            return;
        }

        // file matched, serve the content
        exchange.getResponseHeaders().add("Content-Type", detectContentType(resolvedPath));
        exchange.sendResponseHeaders(200, Files.size(resolvedPath));
        try (OutputStream os = exchange.getResponseBody()) {
            Files.copy(resolvedPath, os);
        }
    }

    private static String detectContentType(Path file) throws IOException {
        String type = Files.probeContentType(file);
        return type != null ? type : "application/octet-stream";
    }

}
