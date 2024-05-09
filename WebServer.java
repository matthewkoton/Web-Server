import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;

// Defines a class to handle HTTP requests, encapsulating method, URI, headers, and body.
class HTTPRequest {
    private String method;
    private String uri;
    private Map<String, String> headers;
    private String body;

    // Constructor initializes an HTTPRequest with method, URI, headers, and body.
    public HTTPRequest(String method, String uri, Map<String, String> headers, String body) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.body = body;
    }
}

// Main class for the web server.
public class WebServer {
    private int port;
    private String documentRoot;
    private ExecutorService threadPool;
    private String defaultPage;
    private Integer maxThreads;

    // Constructor initializes the server, reads configuration, and sets up the
    // thread pool.
    public WebServer() {
        readConfig(); // Load server configuration from a file.
        threadPool = Executors.newFixedThreadPool(maxThreads); // Setup a thread pool for handling requests.
    }

    // Reads server configuration from a properties file.
    private void readConfig() {
        Properties config = new Properties();
        try (InputStream input = new FileInputStream("config.ini")) {
            config.load(input);
            port = Integer.parseInt(config.getProperty("Port")); // Server port.
            documentRoot = config.getProperty("RootDirectory"); // Root directory for documents.
            defaultPage = config.getProperty("DefaultPage"); // Default page to serve.
            maxThreads = Integer.parseInt(config.getProperty("MaxThreads"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Starts the server and handles incoming connections.
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Web Server started on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept an incoming connection.
                // Submit the handling of the client connection to the thread pool.
                threadPool.submit(new ClientHandler(clientSocket, documentRoot, defaultPage));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Inner class to handle client connections.
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String documentRoot;
        private String defaultPage;

        // Constructor initializes the handler with the client socket, document root,
        // and default page.
        public ClientHandler(Socket socket, String root, String page) {
            this.clientSocket = socket;
            this.documentRoot = root;
            this.defaultPage = page;
        }

        // Main method run by the thread pool for handling the client request.
        @Override
        public void run() {
            HTTPRequest(); // Process the HTTP request.
        }

        // Parses the Content-Length header to determine the size of the request body.
        private int getContentLength(String headers) {
            String[] lines = headers.split("\n");
            for (String line : lines) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    return Integer.parseInt(line.substring(line.indexOf(":") + 1).trim());
                }
            }
            return 0;
        }

        // Parses raw header strings into a map for easy access.
        private Map<String, String> parseHeaders(String headersString) {
            Map<String, String> headers = new HashMap<>();
            String[] lines = headersString.split("\n");
            for (String line : lines) {
                String[] parts = line.split(": ");
                if (parts.length == 2) {
                    headers.put(parts[0], parts[1]);
                }
            }
            return headers;
        }

        // Main method for processing HTTP requests.
        public void HTTPRequest() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    OutputStream out = clientSocket.getOutputStream();
                    PrintWriter writer = new PrintWriter(out, true)) {

                // Read the request line (e.g., "GET /index.html HTTP/1.1").
                String requestLine = in.readLine();
                // Read headers.
                StringBuilder requestHeaders = new StringBuilder();
                String newline;
                while (!(newline = in.readLine()).isEmpty()) {
                    requestHeaders.append(newline).append("\n");
                }

                // Read the request body if any.
                StringBuilder requestBody = new StringBuilder();
                int contentLength = getContentLength(requestHeaders.toString());
                if (contentLength > 0) {
                    char[] buffer = new char[contentLength];
                    in.read(buffer, 0, contentLength);
                    requestBody.append(buffer);
                }

                // Process the request if it's valid.
                if (requestLine != null && !requestLine.isEmpty()) {
                    System.out.println("Request:" + "\n" + requestLine + "\n" + requestHeaders);
                    String[] tokens = requestLine.split(" ");
                    if (tokens.length >= 2) {
                        Map<String, String> headersMap = parseHeaders(requestHeaders.toString());
                        HTTPRequest httpRequest = new HTTPRequest(tokens[0], tokens[1], headersMap,
                                requestBody.toString());
                        // Handle different types of HTTP methods.
                        switch (tokens[0]) {
                            case "GET":
                                boolean isChunked = "chunked".equalsIgnoreCase(headersMap.get("Transfer-Encoding"));
                                handleGetRequest(tokens[1], out, writer, true, isChunked);
                                break;
                            case "POST":
                                handlePostRequest(requestBody.toString(), out, writer);
                                break;
                            case "HEAD":
                                handleGetRequest(tokens[1], out, writer, false, false);
                                break;
                            case "TRACE":
                                handleTraceRequest(requestLine, requestHeaders.toString(), writer);
                                break;
                            default:
                                sendErrorResponse(writer, 501, "Not Implemented");
                                break;
                        }
                    } else {
                        sendErrorResponse(writer, 400, "Bad Request");
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
                    sendErrorResponse(writer, 500, "Internal Server Error");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        // Handles POST requests by parsing form data and generating a response.
        private void handlePostRequest(String requestBody, OutputStream out, PrintWriter writer) throws IOException {
            // Parse form data.
            String[] formItems = requestBody.split("&");
            // Build an HTML response.
            StringBuilder htmlResponse = new StringBuilder();
            htmlResponse.append("<!DOCTYPE html>")
                    .append("<html>")
                    .append("<head>")
                    .append("<title>Form Submission Response</title>")
                    .append("</head>")
                    .append("<body>")
                    .append("<h1>Form Submission Response</h1>")
                    .append("<p>Form data received:</p>")
                    .append("<ul>");

            for (String formItem : formItems) {
                String[] keyValue = formItem.split("=");
                if (keyValue.length == 2) {
                    htmlResponse.append("<li>").append(keyValue[0]).append(": ").append(keyValue[1]).append("</li>");
                }
            }

            htmlResponse.append("</ul>")
                    .append("</body>")
                    .append("</html>");

            // Send the HTML response.
            writer.println("HTTP/1.1 200 OK");
            writer.println("Content-Type: text/html");
            writer.println("Content-Length: " + htmlResponse.length());
            writer.println(); // End of headers.
            writer.print(htmlResponse.toString());
            writer.flush();

            // send to terminal
            System.out.println("Response: ");
            System.out.println("HTTP/1.1 200 OK");
            System.out.println("Content-Type: text/html");
            System.out.println("Content-Length: " + htmlResponse.length());
            System.out.println();
        }

        // Handles GET requests by serving static files or generating dynamic content.
        private void handleGetRequest(String path, OutputStream out, PrintWriter writer, Boolean isGetRequest,
                Boolean isChunked) throws IOException {
            if (path.equals("/")) {
                path = "/" + defaultPage; // Serve the default page if no specific file is requested.
            }

            try {
                // Normalize the file path to serve.
                Path normalizedPath = Paths.get(documentRoot).resolve(path.substring(1)).normalize();

                // Security check to prevent directory traversal attacks.
                if (!normalizedPath.startsWith(Paths.get(documentRoot))) {
                    sendErrorResponse(writer, 403, "403 Forbidden");
                } else if (Files.exists(normalizedPath) && !Files.isDirectory(normalizedPath)) {
                    // File exists and is not a directory; serve it.
                    String contentType = Files.probeContentType(normalizedPath);
                    if (contentType == null) {
                        contentType = "application/octet-stream"; // Default MIME type.
                    }

                    // Send the response headers.
                    writer.println("HTTP/1.1 200 OK");
                    writer.println("Content-Type: " + contentType);
                    if (isChunked) {
                        writer.println("Transfer-Encoding: chunked");
                    } else {
                        writer.println("Content-Length: " + Files.size(normalizedPath));
                    }

                    writer.println(); // End of headers.
                    writer.flush();

                    // Send to terminal
                    System.out.println("Response: ");
                    System.out.println("HTTP/1.1 200 OK");
                    System.out.println("Content-Type: " + contentType);
                    if (isChunked) {
                        System.out.println("Transfer-Encoding: chunked");
                    } else {
                        System.out.println("Content-Length: " + Files.size(normalizedPath));
                    }
                    System.out.println();

                    // Serve the file content.
                    if (isGetRequest) {
                        try (InputStream fileInputStream = Files.newInputStream(normalizedPath)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                                if (isChunked) {
                                    // For chunked transfer, send each chunk with its size.
                                    String chunkSize = Integer.toHexString(bytesRead);
                                    out.write((chunkSize + "\r\n").getBytes());
                                    out.write(buffer, 0, bytesRead);
                                    out.write("\r\n".getBytes());
                                } else {
                                    out.write(buffer, 0, bytesRead);
                                }
                            }
                            out.flush();
                        }
                    }
                } else {
                    // Requested file does not exist or is a directory; send a 404 response.
                    sendErrorResponse(writer, 404, "Not Found");
                }
            } catch (IOException e) {
                // Handle IO exceptions by sending a server error response.
                sendErrorResponse(writer, 500, "Internal Server Error");
                e.printStackTrace();
            }
        }

        // Handles TRACE requests by echoing back the received request line and headers.
        private void handleTraceRequest(String requestLine, String headers, PrintWriter writer) {

            writer.println("HTTP/1.1 200 OK");
            writer.println("Content-Type: message/http");
            writer.println(); // End of headers.
            writer.println(requestLine); // Echo the request line.
            writer.println(headers); // Echo the received headers.
            writer.flush();

            // send to terminal
            System.out.println("Response: ");
            System.out.println("HTTP/1.1 200 OK");
            System.out.println("Content-Type: message/http");
            System.out.println(); // End of headers.
            System.out.println(requestLine); // Echo the request line.
            System.out.println(headers); // Echo the received headers.

        }

        // Sends an HTTP error response with the specified status code and message.
        private void sendErrorResponse(PrintWriter writer, int statusCode, String statusMessage) {
            writer.println("HTTP/1.1 " + statusCode + " " + statusMessage);
            writer.println("Content-Type: text/html");
            writer.println(); // End of headers.
            writer.println("<html><body><h1>Error " + statusCode + " " + statusMessage + "</h1></body></html>");
            writer.flush();

            System.out.println("Response: ");
            System.out.println("HTTP/1.1 " + statusCode + " " + statusMessage);
            System.out.println("Content-Type: text/html");
            System.out.println(); // End of headers.

        }
    }

    // Main method to start the server.
    public static void main(String[] args) {
        WebServer server = new WebServer();
        server.start(); // Start the web server.
    }
}
