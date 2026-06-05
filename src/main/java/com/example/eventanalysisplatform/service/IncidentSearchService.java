package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.search.IncidentSearchDocument;
import com.example.eventanalysisplatform.search.IncidentSearchRepository;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidentSearchService {

    private final IncidentSearchRepository repository;
    private final ElasticsearchOperations elasticsearchOperations;

    public IncidentSearchService(IncidentSearchRepository repository, ElasticsearchOperations elasticsearchOperations){
        this.repository = repository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public void index(IncidentSearchDocument document){
        repository.save(document);
    }

    public List<IncidentSearchDocument> search(String query) {
        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(q -> q
                        .multiMatch(m -> m
                                .query(query)
                                .fields("incidentId", "source", "severity", "message")
                        )
                )
                .build();

        return elasticsearchOperations.search(searchQuery, IncidentSearchDocument.class)
                .stream()
                .map(hit -> hit.getContent())
                .toList();
    }
}
