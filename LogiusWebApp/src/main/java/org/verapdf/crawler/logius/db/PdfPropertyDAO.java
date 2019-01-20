package org.verapdf.crawler.logius.db;


import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.verapdf.crawler.logius.validation.settings.PdfProperty;
import org.verapdf.crawler.logius.validation.settings.PdfProperty_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Repository
public class PdfPropertyDAO extends AbstractDAO<PdfProperty> {


    public PdfPropertyDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<PdfProperty> getEnabledPropertiesMap() {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<PdfProperty> criteriaQuery = builder.createQuery(PdfProperty.class);
        Root<PdfProperty> pdfProperty = criteriaQuery.from(PdfProperty.class);
        criteriaQuery.where(builder.equal(pdfProperty.get(PdfProperty_.enabled), true));
        List<PdfProperty> properties = currentSession().createQuery(requireNonNull(criteriaQuery)).getResultList();
        properties.forEach(pdf -> Hibernate.initialize(pdf.getXpathList()));
        return properties;
    }
}
