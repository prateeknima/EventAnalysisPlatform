package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.search.IncidentRankScore;
import com.example.eventanalysisplatform.search.IncidentRankedSearchResult;
import com.example.eventanalysisplatform.search.IncidentSearchDocument;
import com.example.eventanalysisplatform.search.IncidentSearchRepository;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class IncidentSearchService {

    private static final int MAX_RANKED_SEARCH_LIMIT = 500;

    private final IncidentSearchRepository repository;
    private final ElasticsearchOperations elasticsearchOperations;

    public IncidentSearchService(
            IncidentSearchRepository repository,
            ElasticsearchOperations elasticsearchOperations
    ){
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
                .map(SearchHit::getContent)
                .toList();
    }

    public List<IncidentRankedSearchResult> searchRanked(String query, int limit) {
        int safeLimit = Math.clamp(limit, 1, MAX_RANKED_SEARCH_LIMIT);

        return searchHits(query)
                .stream()
                .map(this::rank)
                .sorted(Comparator
                        .comparing(IncidentRankedSearchResult::priorityScore)
                        .thenComparing(IncidentRankedSearchResult::searchScore)
                        .reversed()
                        .thenComparing(IncidentRankedSearchResult::incidentId))
                .limit(safeLimit)
                .toList();
    }

    private List<SearchHit<IncidentSearchDocument>> searchHits(String query) {
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
                .toList();
    }

    private IncidentRankedSearchResult rank(SearchHit<IncidentSearchDocument> hit) {
        IncidentSearchDocument document = hit.getContent();

        IncidentRankScore rankScore = IncidentRankScore.from(
                document.priorityScore(),
                document.affectedServiceCount(),
                hit.getScore()
        );

        return new IncidentRankedSearchResult(
                document.incidentId(),
                document.source(),
                document.severity(),
                document.message(),
                rankScore.priorityScore(),
                document.riskLevel(),
                rankScore.affectedServiceCount(),
                rankScore.searchScore(),
                document.affectedServices()
        );
    }
}
