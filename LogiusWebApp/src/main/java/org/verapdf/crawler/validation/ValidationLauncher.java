package org.verapdf.crawler.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.verapdf.crawler.api.InvalidReportData;
import org.verapdf.crawler.api.ValidationJobData;
import org.verapdf.crawler.resources.CrawlJobResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ValidationLauncher implements Runnable {
    private String verapdfPath;
    private String errorReportPath;
    private LinkedList<ValidationJobData> queue;
    private CrawlJobResource resource;

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    private boolean isRunning;
    public ValidationLauncher(String verapdfPath, String errorReportPath, CrawlJobResource resource) {
        this.queue = new LinkedList<>();
        this.verapdfPath = verapdfPath;
        this.errorReportPath = errorReportPath;
        this.resource = resource;
        isRunning = true;
        try {
            manageQueue(false);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void addJob(ValidationJobData data) throws IOException {
        queue.add(data);
        manageQueue(true);
    }

    public Integer getQueueSize() {
        return queue.size();
    }

    @Override
    public void run() {
        while (isRunning) {
            String currentUrl = "";
            try {
                if(!queue.isEmpty()) {
                    ValidationJobData data = queue.remove();
                    manageQueue(true);
                    System.out.println("Validating " + data.getUri());
                    currentUrl = data.getUri();
                    // Launch verapdf CLI with pdf file as argument
                    String[] cmd = {verapdfPath, "--format", "mrr", data.getFilepath()};
                    ProcessBuilder pb = new ProcessBuilder().inheritIO();
                    File output = new File("output");
                    File error = new File("error");
                    output.createNewFile();
                    error.createNewFile();
                    pb.redirectOutput(output);
                    pb.redirectError(error);
                    pb.command(cmd);
                    Scanner resultScanner = new Scanner(new File("output"));
                    if(pb.start().waitFor(20, TimeUnit.MINUTES) && resultScanner.hasNext()) { // Validation finished successfully in time

                        File fXmlFile = new File("output");
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(fXmlFile);
                        Element validationReportNode = (Element) ((Element)((Element)doc.getElementsByTagName("jobs") .item(0)).
                                getElementsByTagName("job").item(0)).
                                getElementsByTagName("validationReport").item(0);

                        FileWriter fw;
                        if(validationReportNode.getAttribute("isCompliant").equals("true")) {
                            fw = new FileWriter(data.getJobDirectory() + File.separator + "Valid_PDF_Report.txt", true);
                            fw.write(data.getUri() + ", ");
                            fw.write(data.getTime());
                        }
                        else {
                            fw = new FileWriter(data.getJobDirectory() + File.separator + "Invalid_PDF_Report.txt", true);
                            InvalidReportData reportData = new InvalidReportData();
                            reportData.setUrl(data.getUri());
                            reportData.setLastModified(data.getTime());
                            Element details = (Element) validationReportNode.getElementsByTagName("details").item(0);
                            String profileName = details.getAttribute("profileName");
                            countFailedRules(details, data.errorOccurances, profileName.contains("1"));
                            reportData.setFailedRules(Integer.parseInt(details.getAttribute("failedRules")));
                            reportData.setPassedRules(Integer.parseInt(details.getAttribute("passedRules")));
                            ObjectMapper mapper = new ObjectMapper();
                            fw.write(mapper.writeValueAsString(reportData));
                        }
                        fw.write(System.lineSeparator());
                        fw.close();
                    }
                    else {
                        if(!resultScanner.hasNext()) {
                            System.out.println("Error: verapdf output is empty.");
                        }
                        Scanner errorScanner = new Scanner(new File("error"));
                        FileWriter fw = new FileWriter(data.getJobDirectory() + File.separator + "Error_Report.txt", true);
                        fw.write("The following errors occured on url " + data.getUri() + ":");
                        while(errorScanner.hasNextLine()) {
                            String line = errorScanner.nextLine();
                            fw.write(line);
                            fw.write(System.lineSeparator());
                        }
                        fw.close();
                    }
                    new File(data.getFilepath()).delete();
                    resultScanner.close();
                }
                else {
                    System.out.println("No jobs, snoozing for a minute...");
                    Thread.sleep(60000);
                }
            } catch (Exception e) {
                try {
                    FileWriter fw = new FileWriter(errorReportPath + "Validation_errors.txt", true);
                    fw.write("Error processing url " + currentUrl);
                    fw.write(System.lineSeparator());
                    fw.write(e.getMessage());
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    fw.write(sw.toString());
                    fw.write(System.lineSeparator());
                    fw.close();
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void countFailedRules(Element details, HashMap<String, Integer> errorOccurances, boolean isPartOneRule) {
        NodeList failedRules = details.getElementsByTagName("rule");
        for(int i = 0; i < failedRules.getLength(); i++) {
            String partRule = isPartOneRule ? "PDFA-Part-1-rules" : "PDFA-Parts-2-and-3-rules";
            String clause = ((Element)failedRules.item(i)).getAttribute("clause");
            String specification = ((Element)failedRules.item(i)).getAttribute("specification");
            String testNumber = ((Element)failedRules.item(i)).getAttribute("testNumber");
            String description = ((Element)failedRules.item(i)).getElementsByTagName("description").item(0).getTextContent();
            String ruleId = "<p><a href =\"" + "https://github.com/veraPDF/veraPDF-validation-profiles/wiki/" + "" +
                    partRule + "#rule-" +
                    clause.replaceAll("\\.","") + "-" + testNumber +
                    "\">Specification: " + specification +
                    ", Clause: " + clause +
                    ", Test number: " + testNumber + "</a></p><p>" +
                    description + "</p>";
            if(errorOccurances.containsKey(ruleId)) {
                errorOccurances.put(ruleId, errorOccurances.get(ruleId) + 1);
            }
            else {
                errorOccurances.put(ruleId, 1);
            }
        }

    }

    private synchronized void manageQueue(boolean isWrite) throws IOException {
        if(isWrite) {
            writeQueue();
        }
        else {
            loadQueue();
        }
    }

    private void writeQueue() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        FileWriter writer = new FileWriter(errorReportPath + "validation-jobs.txt");
        for(ValidationJobData data: queue) {
            writer.write(mapper.writeValueAsString(data));
            writer.write(System.lineSeparator());
        }
        writer.close();
    }

    private void loadQueue() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Scanner scanner = new Scanner(new File(errorReportPath + "validation-jobs.txt"));
        while(scanner.hasNextLine()) {
            ValidationJobData data = mapper.readValue(scanner.nextLine(), ValidationJobData.class);
            String[] parts = data.getJobDirectory().split("/");
            data.errorOccurances = resource.getJobById(parts[parts.length - 3]).getErrorOccurances();
            queue.add(data);
        }
        scanner.close();
    }
}