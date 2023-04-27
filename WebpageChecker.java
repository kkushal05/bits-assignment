import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
Note that in order for this program to work, you need to have a file named webpages.txt in the same directory as the Java file, and the URLs in the file should be in the format specified in the prompt. The program reads the file line by line and performs the checks for each URL. The isDomainExists method checks if the domain exists by trying to resolve its IP address. The isWebServerRunning method checks if a web server is running on port 80 by trying to establish a connection to it. The printOpenPorts method prints which other ports in the range [1-10000] are open on the server. The isFileExists method checks if the file specified in the URL exists by sending a HTTP HEAD request to the server.
**/
public class WebpageChecker {

    public static void main(String[] args) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                WebpageChecker.class.getResourceAsStream("webpages.txt")))) {
            String line;
            while ((line = br.readLine()) != null) {
                URL url = new URL(line);
                String domain = url.getHost();
                System.out.println("Checking domain: " + domain);
                if (isDomainExists(domain)) {
                    System.out.println("Domain exists.");
                    if (isWebServerRunning(domain)) {
                        System.out.println("Web server is running at port 80.");
                        printOpenPorts(domain);
                        if (isFileExists(url)) {
                            System.out.println("File exists.");
                        } else {
                            System.out.println("File does not exist.");
                        }
                    } else {
                        System.out.println("Web server is not running at port 80.");
                    }
                } else {
                    System.out.println("Domain does not exist.");
                }
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isDomainExists(String domain) {
        try {
            InetAddress.getByName(domain);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static boolean isWebServerRunning(String domain) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(domain, 80), 1000);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void printOpenPorts(String domain) {
        for (int port = 1; port <= 10000; port++) {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(domain, port), 1000);
                System.out.println("Port " + port + " is open.");
                socket.close();
            } catch (IOException e) {
                // Port is closed
            }
        }
    }

    private static boolean isFileExists(URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return (responseCode == HttpURLConnection.HTTP_OK);
        } catch (IOException e) {
            return false;
        }
    }

}
