package org.verapdf.crawler.logius.db;


import org.hibernate.*;
import org.hibernate.query.Query;
import org.hibernate.query.internal.AbstractProducedQuery;

import javax.persistence.criteria.CriteriaQuery;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class AbstractDAO<E> {
    private final SessionFactory sessionFactory;
    private final Class<?> entityClass;

    public AbstractDAO(SessionFactory sessionFactory) {
        this.sessionFactory = requireNonNull(sessionFactory);
        this.entityClass = getTypeParameter(getClass());
    }

    public static Class<?> getTypeParameter(Class<?> klass) {
        return getTypeParameter(klass, Object.class);
    }

    public static <T> Class<T> getTypeParameter(Class<?> klass, Class<? super T> bound) {
        Type t;
        for (t = (Type) Objects.requireNonNull(klass); t instanceof Class; t = ((Class) t).getGenericSuperclass()) {
        }

        if (t instanceof ParameterizedType) {
            Type[] var3 = ((ParameterizedType) t).getActualTypeArguments();
            int var4 = var3.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                Type param = var3[var5];
                if (param instanceof Class) {
                    Class<T> cls = determineClass(bound, param);
                    if (cls != null) {
                        return cls;
                    }
                } else if (param instanceof TypeVariable) {
                    Type[] var7 = ((TypeVariable) param).getBounds();
                    int var8 = var7.length;

                    for (int var9 = 0; var9 < var8; ++var9) {
                        Type paramBound = var7[var9];
                        if (paramBound instanceof Class) {
                            Class<T> cls = determineClass(bound, paramBound);
                            if (cls != null) {
                                return cls;
                            }
                        }
                    }
                }
            }
        }

        throw new IllegalStateException("Cannot figure out type parameterization for " + klass.getName());
    }

    private static <T> Class<T> determineClass(Class<? super T> bound, Type candidate) {
        if (candidate instanceof Class<?>) {
            final Class<?> cls = (Class<?>) candidate;
            if (bound.isAssignableFrom(cls)) {
                return (Class<T>) cls;
            }
        }

        return null;
    }

    protected Session currentSession() {
        Session session = sessionFactory.getCurrentSession();
        return session;
    }

    protected Criteria criteria() {
        return currentSession().createCriteria(entityClass);
    }

    protected CriteriaQuery<E> criteriaQuery() {
        return this.currentSession().getCriteriaBuilder().createQuery(getEntityClass());
    }

    protected Query namedQuery(String queryName) throws HibernateException {
        return currentSession().getNamedQuery(requireNonNull(queryName));
    }

    protected Query<E> query(String queryString) {
        return currentSession().createQuery(requireNonNull(queryString), getEntityClass());
    }

    @SuppressWarnings("unchecked")
    public Class<E> getEntityClass() {
        return (Class<E>) entityClass;
    }

    protected E uniqueResult(CriteriaQuery<E> criteriaQuery) throws HibernateException {
        return AbstractProducedQuery.uniqueElement(
                currentSession()
                        .createQuery(requireNonNull(criteriaQuery))
                        .getResultList()
        );
    }

    @SuppressWarnings("unchecked")
    protected E uniqueResult(Criteria criteria) throws HibernateException {
        return (E) requireNonNull(criteria).uniqueResult();
    }

    protected E uniqueResult(Query<E> query) throws HibernateException {
        return requireNonNull(query).uniqueResult();
    }

    @SuppressWarnings("unchecked")
    protected List<E> list(Criteria criteria) throws HibernateException {
        return requireNonNull(criteria).list();
    }

    protected List<E> list(CriteriaQuery<E> criteria) throws HibernateException {
        return currentSession().createQuery(requireNonNull(criteria)).getResultList();
    }

    protected List<E> list(Query<E> query) throws HibernateException {
        return requireNonNull(query).list();
    }

    @SuppressWarnings("unchecked")
    protected E get(Serializable id) {
        return (E) currentSession().get(entityClass, requireNonNull(id));
    }

    protected E persist(E entity) throws HibernateException {
        currentSession().saveOrUpdate(requireNonNull(entity));
        return entity;
    }

    protected <T> T initialize(T proxy) throws HibernateException {
        if (!Hibernate.isInitialized(proxy)) {
            Hibernate.initialize(proxy);
        }
        return proxy;
    }
}

