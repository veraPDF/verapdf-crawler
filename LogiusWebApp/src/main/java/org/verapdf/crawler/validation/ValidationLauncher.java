package org.verapdf.crawler.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.verapdf.crawler.api.ValidationJobData;
import org.verapdf.crawler.helpers.synchronization.FileAccessManager;

import java.io.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ValidationLauncher implements Runnable {
    private String jobFile;
    private String verapdfPath;
    private String errorReportPath;

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public String getJobFile() { return jobFile; }

    private boolean isRunning;

    public ValidationLauncher(String jobFile, String verapdfPath, String errorReportPath) {
        this.jobFile = jobFile;
        this.verapdfPath = verapdfPath;
        this.errorReportPath = errorReportPath;
        isRunning = true;
    }

    @Override
    public void run() {
        while (isRunning) {
            String currentUrl = "";
            try {
                String validationJobJson = FileAccessManager.getInstance().makeRecord(jobFile, "");
                if(!validationJobJson.equals("")) {
                    ObjectMapper mapper = new ObjectMapper();
                    ValidationJobData data = mapper.readValue(validationJobJson, ValidationJobData.class);
                    System.out.println("Validating " + data.getUri());
                    currentUrl = data.getUri();
                    // Launch verapdf CLI with pdf file as argument
                    String[] cmd = {verapdfPath, "--format", "text", data.getFilepath()};
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
                        FileWriter fw;
                        if(resultScanner.next().equals("PASS")) {
                            fw = new FileWriter(data.getJobDirectory() + File.separator + "Valid_PDF_Report.txt", true);
                        }
                        else {
                            fw = new FileWriter(data.getJobDirectory() + File.separator + "Invalid_PDF_Report.txt", true);
                        }
                        fw.write(data.getUri() + ", ");
                        fw.write(data.getTime());
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
                }
                else {
                    Thread.sleep(60000);
                    System.out.println("No jobs, snoozing for a minute...");
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
