package org.verapdf.crawler.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.verapdf.crawler.api.InvalidReportData;
import org.verapdf.crawler.api.ValidationJobData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ValidationLauncher implements Runnable {
    private String verapdfPath;
    private String errorReportPath;
    private LinkedList<ValidationJobData> queue;

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    private boolean isRunning;

    public ValidationLauncher(String verapdfPath, String errorReportPath) {
        this.queue = new LinkedList<>();
        this.verapdfPath = verapdfPath;
        this.errorReportPath = errorReportPath;
        isRunning = true;
    }

    public void addJob(ValidationJobData data) {
        queue.add(data);
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
                catch (IOException e1) {
                    e1.printStackTrace();
                }
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}