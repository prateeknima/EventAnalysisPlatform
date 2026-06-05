package com.example.eventanalysisplatform.search;

import com.example.eventanalysisplatform.repository.IncidentRepository;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.cdi.ElasticsearchRepositoryBean;

import java.util.List;

public interface IncidentSearchRepository extends ElasticsearchRepository<IncidentSearchDocument, String> {
    List<IncidentSearchDocument> findByMessageContainingIgnoreCase(String message);
}
