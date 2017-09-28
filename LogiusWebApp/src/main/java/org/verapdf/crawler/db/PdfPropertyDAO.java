package org.verapdf.crawler.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.verapdf.crawler.api.validation.settings.PdfProperty;
import org.verapdf.crawler.api.validation.settings.PdfProperty_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class PdfPropertyDAO extends AbstractDAO<PdfProperty> {
    public PdfPropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<PdfProperty> getEnabledPropertiesMap() {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<PdfProperty> criteriaQuery = builder.createQuery(PdfProperty.class);
        Root<PdfProperty> pdfProperty = criteriaQuery.from(PdfProperty.class);
        criteriaQuery.where(builder.equal(pdfProperty.get(PdfProperty_.enabled), true));
        return list(criteriaQuery);
    }
}
