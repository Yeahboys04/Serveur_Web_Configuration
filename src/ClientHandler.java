public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ConfigLoader config;

    public ClientHandler(Socket clientSocket, ConfigLoader config) {
        this.clientSocket = clientSocket;
        this.config = config;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            String requestLine = in.readLine();
            if (requestLine == null || !requestLine.startsWith("GET")) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length != 3) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            String filePath = config.getRoot() + requestParts[1];
            File file = new File(filePath);
            if (!file.exists()) {
                sendErrorResponse(out, 404, "Not Found");
                return;
            }

            sendFileResponse(out, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendErrorResponse(BufferedWriter out, int statusCode, String message) throws IOException {
        out.write("HTTP/1.1 " + statusCode + " " + message + "\r\n");
        out.write("Content-Type: text/html\r\n");
        out.write("\r\n");
        out.write("<html><body><h1>" + message + "</h1></body></html>\r\n");
        out.flush();
    }

    private void sendFileResponse(BufferedWriter out, File file) throws IOException {
        out.write("HTTP/1.1 200 OK\r\n");
        out.write("Content-Type: " + getContentType(file) + "\r\n");
        out.write("\r\n");
        out.flush();

        try (BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(new String(buffer, 0, bytesRead));
            }
        }
        out.flush();
    }

    private String getContentType(File file) {
        String fileName = file.getName();
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".mp3")) return "audio/mpeg";
        // Add more types as needed
        return "application/octet-stream";
    }
}
