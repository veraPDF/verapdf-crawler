package org.verapdf.crawler.extentionModules;

import org.archive.modules.CrawlURI;
import org.archive.modules.writer.MirrorWriterProcessor;
import org.verapdf.core.VeraPDFException;
import org.verapdf.features.FeatureFactory;
import org.verapdf.metadata.fixer.FixerFactory;
import org.verapdf.pdfa.PdfBoxFoundryProvider;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.validation.validators.ValidatorFactory;
import org.verapdf.processor.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumSet;

public class PDFProcessor extends MirrorWriterProcessor {
    @Override
    protected void innerProcess(CrawlURI curi) {
        super.innerProcess(curi);
        PdfBoxFoundryProvider.initialise();
        ItemProcessor processor = ProcessorFactory.createProcessor(
                ProcessorFactory.fromValues(
                        ValidatorFactory.defaultConfig(),
                        FeatureFactory.defaultConfig(),
                        FixerFactory.defaultConfig(),
                        EnumSet.of(TaskType.VALIDATE)));
        String baseDir = getPath().getFile().getAbsolutePath();
        String mps = (String)curi.getData().get(A_MIRROR_PATH);
        try {
            ProcessorResult res = processor.process(new File(baseDir + File.separator + mps));
            FileWriter fw = new FileWriter(baseDir + File.separator + "PDFReport.txt", true);
            Boolean isValid = res.getResultForTask(TaskType.VALIDATE).isExecuted() &&
                    res.getResultForTask(TaskType.VALIDATE).isSuccess();
            isValid = isValid && res.getValidationResult().isCompliant();
            fw.write(curi.getURI() + " " + isValid.toString() + " \n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (VeraPDFException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}