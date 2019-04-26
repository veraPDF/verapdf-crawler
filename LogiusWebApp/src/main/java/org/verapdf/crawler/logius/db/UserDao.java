package org.verapdf.crawler.logius.db;


import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.model.Role;
import org.verapdf.crawler.logius.model.User;
import org.verapdf.crawler.logius.model.User_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class UserDao extends AbstractDAO<User> {
    private List<Role> excludedRoles = Arrays.asList(Role.ADMIN, Role.ANONYMOUS);

    public UserDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public User getByEmail(String email) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<User> criteriaQuery = builder.createQuery(User.class);
        Root<User> crawlJob = criteriaQuery.from(User.class);

        criteriaQuery.where(builder.equal(crawlJob.get(User_.email), email));
        return uniqueResult(criteriaQuery);
    }

    public User getById(UUID uuid) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<User> criteriaQuery = builder.createQuery(User.class);
        Root<User> crawlJob = criteriaQuery.from(User.class);

        criteriaQuery.where(builder.equal(crawlJob.get(User_.id), uuid));
        return uniqueResult(criteriaQuery);
    }

    public User save(User user) {
        return persist(user);
    }


    public List<User> getUsers(String emailFilter,Integer start, Integer limit) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<User> criteriaQuery = builder.createQuery(User.class);
        Root<User> rootEntry = criteriaQuery.from(User.class);
        if (emailFilter != null) {
            criteriaQuery.where(builder.like(rootEntry.get(User_.email), "%" + emailFilter + "%"));
        }
        criteriaQuery.where(builder.not(rootEntry.get(User_.role).in(excludedRoles)));
        Query<User> query = this.currentSession().createQuery(criteriaQuery);
        setOffset(query, start, limit);
        return list(query);
    }

    public long count(String emailFilter) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
        Root<User> user = criteriaQuery.from(User.class);
        criteriaQuery.select(builder.count(user));
        if (emailFilter != null) {
            criteriaQuery.where(builder.like(user.get(User_.email), "%" + emailFilter + "%"));
        }
        return this.currentSession().createQuery(criteriaQuery).getSingleResult();
    }

}