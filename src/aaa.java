import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

private void handleStatusRequest(BufferedWriter out) throws IOException {
    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    long freeMemory = osBean.getFreePhysicalMemorySize();
    long freeDiskSpace = new File(config.getRoot()).getFreeSpace();
    int processCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();

    out.write("HTTP/1.1 200 OK\r\n");
    out.write("Content-Type: text/html\r\n");
    out.write("\r\n");
    out.write("<html><body>");
    out.write("<h1>Server Status</h1>");
    out.write("<p>Free Memory: " + freeMemory + " bytes</p>");
    out.write("<p>Free Disk Space: " + freeDiskSpace + " bytes</p>");
    out.write("<p>Process Count: " + processCount + "</p>");
    out.write("</body></html>");
    out.flush();
}
