private void sendFileResponse(BufferedWriter out, File file) throws IOException {
    String contentType = getContentType(file);
    if (file.getName().endsWith(".html")) {
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        content = processDynamicCode(content);
        out.write("HTTP/1.1 200 OK\r\n");
        out.write("Content-Type: text/html\r\n");
        out.write("\r\n");
        out.write(content);
    } else {
        // Existing code for base64 encoding
    }
    out.flush();
}

private String processDynamicCode(String content) {
    Pattern pattern = Pattern.compile("<code interpreteur=\"(.*?)\">(.*?)</code>", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(content);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
        String interpreter = matcher.group(1);
        String code = matcher.group(2).trim();
        String result = executeCode(interpreter, code);
        matcher.appendReplacement(sb, result);
    }
    matcher.appendTail(sb);
    return sb.toString();
}

private String executeCode(String interpreter, String code) {
    try {
        ProcessBuilder pb = new ProcessBuilder(interpreter);
        Process process = pb.start();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        writer.write(code);
        writer.close();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    } catch (IOException e) {
        e.printStackTrace();
        return "Error executing code";
    }
}
