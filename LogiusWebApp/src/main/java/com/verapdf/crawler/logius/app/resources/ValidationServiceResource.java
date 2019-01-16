package com.verapdf.crawler.logius.app.resources;

import com.verapdf.crawler.logius.app.validation.ValidationJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.verapdf.crawler.logius.app.monitoring.ValidationQueueStatus;
import com.verapdf.crawler.logius.app.validation.VeraPDFValidationResult;
import com.verapdf.crawler.logius.app.db.ValidationJobDAO;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Maksim Bezrukov
 */


@RestController
@RequestMapping("/validation-service")
public class ValidationServiceResource {

    private final ValidationJobDAO validationJobDAO;

    @Autowired
    public ValidationServiceResource(ValidationJobDAO validationJobDAO) {
        this.validationJobDAO = validationJobDAO;
    }

    @GetMapping("/queue-status")
    public ValidationQueueStatus getQueueStatus() {
        Long count = validationJobDAO.count(null);
        List<ValidationJob> documents = validationJobDAO.getDocuments(null, 10);
        return new ValidationQueueStatus(count, documents);
    }

    @GetMapping("/result")
    public void setValidationResult(@NotNull @Valid VeraPDFValidationResult result) {
        // todo: interrupt sleep
    }
}
