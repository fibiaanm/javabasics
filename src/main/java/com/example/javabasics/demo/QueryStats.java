package com.example.javabasics.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManagerFactory;

/**
 * Thin wrapper over Hibernate Statistics: take a snapshot, run something,
 * then ask "what did JPA do since the snapshot?".
 */
@Component
public class QueryStats {

    private final Statistics statistics;

    public QueryStats(EntityManagerFactory entityManagerFactory) {
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        this.statistics.setStatisticsEnabled(true);
    }

    public Snapshot take() {
        return new Snapshot(
                statistics.getPrepareStatementCount(),
                statistics.getEntityInsertCount(),
                statistics.getEntityUpdateCount(),
                statistics.getEntityDeleteCount(),
                statistics.getEntityLoadCount(),
                statistics.getCollectionLoadCount());
    }

    public Map<String, Long> since(Snapshot start) {
        Snapshot now = take();
        Map<String, Long> delta = new LinkedHashMap<>();
        delta.put("sqlStatements", now.statements() - start.statements());
        delta.put("inserts", now.inserts() - start.inserts());
        delta.put("updates", now.updates() - start.updates());
        delta.put("deletes", now.deletes() - start.deletes());
        delta.put("entitiesLoaded", now.entityLoads() - start.entityLoads());
        delta.put("collectionsLoaded", now.collectionLoads() - start.collectionLoads());
        return delta;
    }

    public record Snapshot(long statements, long inserts, long updates, long deletes,
            long entityLoads, long collectionLoads) {
    }
}
