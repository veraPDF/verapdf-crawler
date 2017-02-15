package org.verapdf.crawler.extentionModules;

import org.apache.commons.httpclient.Header;
import org.archive.modules.CrawlURI;
import org.archive.modules.writer.MirrorWriterProcessor;
import org.verapdf.core.VeraPDFException;
import org.verapdf.features.FeatureFactory;
import org.verapdf.metadata.fixer.FixerFactory;
import org.verapdf.pdfa.PdfBoxFoundryProvider;
import org.verapdf.pdfa.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.validation.validators.ValidatorFactory;
import org.verapdf.processor.*;
import org.verapdf.processor.plugins.PluginsCollectionConfig;

import java.net.HttpURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;

public class PDFProcessor extends MirrorWriterProcessor {
    @Override
    protected void innerProcess(CrawlURI curi) {
        super.innerProcess(curi);
        String baseDir = getPath().getFile().getAbsolutePath();
        String mps = (String)curi.getData().get(A_MIRROR_PATH);
        String time = "Last-Modified: Thu, 01 Jan 1970 00:00:01 GMT";
        Header header = curi.getHttpMethod().getResponseHeader("Last-Modified");
        if(header != null) {
            time = header.toString().substring(0, header.toString().length() - 2);
        }

        try {
            VeraGreenfieldFoundryProvider.initialise();
            File validationFile = new File(baseDir + "/../../../../validation/validation-jobs.txt");
            FileWriter writer = new FileWriter(validationFile, true);
            String data = "{\"filepath\":\"" + baseDir + File.separator + mps +
                    "\", \"jobDirectory\":\"" + baseDir + "\", \"" +
                    "time\":\"" + time + "\", \"uri\":\"" + curi.getURI() + "\"}";
            writer.write(data);
            writer.write(System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
/*
        VeraGreenfieldFoundryProvider.initialise();
        try(ItemProcessor processor = ProcessorFactory.createProcessor(
                ProcessorFactory.fromValues(
                ValidatorFactory.defaultConfig(),
                FeatureFactory.defaultConfig(),
                PluginsCollectionConfig.defaultConfig(),
                FixerFactory.defaultConfig(),
                EnumSet.of(TaskType.VALIDATE))) ) {


            ProcessorResult res = processor.process(new File(baseDir + File.separator + mps));
            Boolean isValid = res.getResultForTask(TaskType.VALIDATE).isExecuted() &&
                    res.getResultForTask(TaskType.VALIDATE).isSuccess()  &&
                    res.getValidationResult().isCompliant();
            FileWriter fw;
            if(isValid) {
                fw = new FileWriter(baseDir + File.separator + "Valid_PDF_Report.txt", true);
                fw.write(curi.getURI() + ", ");
                fw.write(res.getValidationResult().getPDFAFlavour().toString() + ", ");
            }
            else {
                fw = new FileWriter(baseDir + File.separator + "Invalid_PDF_Report.txt", true);
                fw.write(curi.getURI() + ", ");
            }
            fw.write(time);
            fw.close();
            new File(baseDir + File.separator + mps).delete();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (VeraPDFException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
*/
    }
}