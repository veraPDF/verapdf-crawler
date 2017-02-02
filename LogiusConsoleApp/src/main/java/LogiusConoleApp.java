import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public class LogiusConoleApp {

    public static void main(String[] args) {
        try {
            String[] domains = {"http://localhost:8080/", "logius.nl"};
            String endpointUrl = "http://localhost:9000/";
            String date = "01-01-2017";
            String reportEmail = "shem_anton@tut.by";

            Pattern pattern = Pattern.compile("\\d\\d-\\d\\d-\\d\\d\\d\\d");
            if (!pattern.matcher(date).matches()) {
                System.out.println("Invalid date format, enter date in dd-MM-yyyy format.");
                return;
            }

            URL url = new URL(endpointUrl + "crawl-job/batch");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput( true );
            conn.setInstanceFollowRedirects( false );

            StringBuilder postData = new StringBuilder();
            postData.append("{\"domains\":[\"");
            for(String domain : domains) {
                postData.append(domain);
                postData.append("\"");
                if(!domain.equals(domains[domains.length - 1])) {
                    postData.append(",\"");
                }
            }
            postData.append("],\"date\":\"");
            postData.append(date);
            postData.append("\",\"reportEmail\":\"");
            postData.append(reportEmail);
            postData.append("\"}");
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(postData.toString());
            wr.flush();
            wr.close();

            conn.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println(response.toString());

        } catch (MalformedURLException e) {
            System.out.println("Invalid URL was provided");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
