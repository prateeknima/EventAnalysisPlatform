package com.example.eventanalysisplatform.repository;

import com.example.eventanalysisplatform.exception.IncidentConflictException;
import com.example.eventanalysisplatform.record.IncidentEvent;
import com.example.eventanalysisplatform.record.IncidentRequest;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

@Service
public class IncidentRepository {

    private final DSLContext dsl;

    public IncidentRepository(DSLContext dslContext){
        this.dsl = dslContext;
    }

    public void save(IncidentEvent request){
        int inserted = dsl.insertInto(table("incidents"))
                .columns(
                        field("incident_id"),
                        field("source"),
                        field("severity"),
                        field("message")
                )
                .values(
                        request.incidentId(),
                        request.source(),
                        request.severity(),
                        request.message()
                )
                .onConflict(field("incident_id"))
                .doNothing()
                .execute();

        if (inserted == 1){
            return;
        }

        IncidentRequest existing = findById(request.incidentId());

        if (!existing.equals(request)){
            throw new IncidentConflictException(request.incidentId());
        }
    }

    public IncidentRequest findById(String incidentId){
        return dsl.select(
                field("incident_id", String.class),
                field("source", String.class),
                field("severity", String.class),
                field("message", String.class)
        )
                .from(table("incidents"))
                .where(field("incident_id").eq(incidentId))
                .fetchOne(record -> new IncidentRequest(
                        record.get("incident_id", String.class),
                        record.get("source", String.class),
                        record.get("severity", String.class),
                        record.get("message", String.class)
                ));
    }
}
