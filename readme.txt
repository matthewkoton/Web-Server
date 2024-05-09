Overview
This document provides an overview of a simple web server implemented in Java. The server is designed to handle basic HTTP requests, including GET, POST, HEAD and TRACE methods, serving static files from a specified document root and handling simple form submissions.

Server Design
The design of this web server follows a multi-threaded architecture, allowing it to handle multiple client connections concurrently. By utilizing a thread pool, the server efficiently manages resource usage and scales to accommodate a varying number of incoming requests. The server's configuration, including the port number, document root, and default page, is read from an external configuration file (config.ini), providing flexibility and ease of configuration. The server is structured around key Java classes, each responsible for a distinct aspect of the server's functionality.

Classes and Their Roles
WebServer: This is the main class of the application. It initializes the server based on the configuration read from config.ini, listens for incoming connections on the specified port, and dispatches client requests to a thread pool for handling. This class acts as the entry point and orchestrator of the server.

ClientHandler: Implements the Runnable interface, making it suitable for execution by a thread pool. Each instance is responsible for handling a single client connection, reading the HTTP request, processing it based on the request method (GET, POST, HEAD, TRACE), and sending the appropriate response.

HTTPRequest: Represents an HTTP request, encapsulating details such as the request method, URI, headers, and body. This class simplifies request handling by providing structured access to request components.

Handling of HTTP Methods
GET: Serves static files from the document root or returns the default page if no specific file is requested. Supports chunked responses.
POST: Processes form submissions and sends back the submitted form data in an HTML response. Handles request bodies and dynamic response generation.
HEAD: Similar to GET, but only headers are returned without the body.
TRACE: Echoes the received request back to the client, including headers.

Configuration
The server's behavior can be customized through the config.ini file, allowing changes to the server's port, document root, and default served page without altering the code. This allows us to easily make changes to the server without having to change the actual code.