package com.example.eventanalysisplatform.exception;

public class IncidentConflictException extends RuntimeException{

    public IncidentConflictException(String incidentId){
        super("Incident conflict for id: " + incidentId);
    }
}
