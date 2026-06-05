package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.search.IncidentSearchDocument;
import com.example.eventanalysisplatform.search.IncidentSearchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidentSearchService {

    private final IncidentSearchRepository repository;

    public IncidentSearchService(IncidentSearchRepository repository){
        this.repository = repository;
    }

    public void index(IncidentSearchDocument document){
        repository.save(document);
    }

    public List<IncidentSearchDocument> searchByMessage(String query){
        return repository.findByMessageContainingIgnoreCase(query);
    }
}
