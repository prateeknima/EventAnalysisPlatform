package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.search.IncidentRankedSearchResult;
import com.example.eventanalysisplatform.search.IncidentSearchDocument;
import com.example.eventanalysisplatform.search.IncidentSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IncidentSearchServiceTest {

    private IncidentSearchRepository repository;
    private ElasticsearchOperations elasticsearchOperations;
    private IncidentSearchService service;

    @BeforeEach
    void setUp() {
        repository = mock(IncidentSearchRepository.class);
        elasticsearchOperations = mock(ElasticsearchOperations.class);

        service = new IncidentSearchService(
                repository,
                elasticsearchOperations
        );
    }

    @Test
    void indexSavesDocument() {
        IncidentSearchDocument document = new IncidentSearchDocument(
                "INC-SEARCH-1",
                "payment",
                "HIGH",
                "timeout"
        );

        service.index(document);

        verify(repository).save(document);
    }

    @Test
    void searchReturnsDocumentsFromSearchHits() {
        IncidentSearchDocument document = new IncidentSearchDocument(
                "INC-SEARCH-2",
                "checkout",
                "LOW",
                "slow checkout"
        );

        SearchHit<IncidentSearchDocument> searchHit = mock(SearchHit.class);
        SearchHits<IncidentSearchDocument> searchHits = mock(SearchHits.class);

        when(searchHit.getContent()).thenReturn(document);
        when(searchHits.stream()).thenReturn(List.of(searchHit).stream());

        when(elasticsearchOperations.search(
                any(NativeQuery.class),
                eq(IncidentSearchDocument.class)
        )).thenReturn(searchHits);

        List<IncidentSearchDocument> result = service.search("checkout");

        assertThat(result).containsExactly(document);

        verify(elasticsearchOperations).search(
                any(NativeQuery.class),
                eq(IncidentSearchDocument.class)
        );
    }

    @Test
    void rankedSearchSortsByStoredOperationalPriorityBeforeSearchScore() {
        IncidentSearchDocument lowPriorityHighSearchScore = new IncidentSearchDocument(
                "INC-RANK-LOW",
                "checkout",
                "LOW",
                "checkout slow",
                20,
                "LOW",
                4,
                List.of("checkout", "inventory", "shipping", "notification")
        );

        IncidentSearchDocument highPriorityLowerSearchScore = new IncidentSearchDocument(
                "INC-RANK-HIGH",
                "payment",
                "HIGH",
                "payment timeout",
                81,
                "HIGH",
                11,
                List.of("payment", "fraud", "checkout", "notification")
        );

        SearchHit<IncidentSearchDocument> lowHit = mock(SearchHit.class);
        SearchHit<IncidentSearchDocument> highHit = mock(SearchHit.class);
        SearchHits<IncidentSearchDocument> searchHits = mock(SearchHits.class);

        when(lowHit.getContent()).thenReturn(lowPriorityHighSearchScore);
        when(lowHit.getScore()).thenReturn(9.5f);
        when(highHit.getContent()).thenReturn(highPriorityLowerSearchScore);
        when(highHit.getScore()).thenReturn(3.0f);
        when(searchHits.stream()).thenReturn(List.of(lowHit, highHit).stream());

        when(elasticsearchOperations.search(
                any(NativeQuery.class),
                eq(IncidentSearchDocument.class)
        )).thenReturn(searchHits);

        List<IncidentRankedSearchResult> result =
                service.searchRanked("timeout", 10);

        assertThat(result)
                .extracting(IncidentRankedSearchResult::incidentId)
                .containsExactly("INC-RANK-HIGH", "INC-RANK-LOW");

        assertThat(result.getFirst().priorityScore()).isEqualTo(81);
        assertThat(result.getFirst().riskLevel()).isEqualTo("HIGH");
        assertThat(result.getFirst().affectedServiceCount()).isEqualTo(11);
        assertThat(result.getFirst().affectedServices())
                .contains("payment", "fraud", "checkout", "notification");
    }

    @Test
    void rankedSearchAppliesRequestedLimit() {
        IncidentSearchDocument first = new IncidentSearchDocument(
                "INC-RANK-1",
                "payment",
                "HIGH",
                "first",
                81,
                "HIGH",
                11,
                List.of("payment")
        );

        IncidentSearchDocument second = new IncidentSearchDocument(
                "INC-RANK-2",
                "payment",
                "HIGH",
                "second",
                80,
                "HIGH",
                11,
                List.of("payment")
        );

        SearchHit<IncidentSearchDocument> firstHit = mock(SearchHit.class);
        SearchHit<IncidentSearchDocument> secondHit = mock(SearchHit.class);
        SearchHits<IncidentSearchDocument> searchHits = mock(SearchHits.class);

        when(firstHit.getContent()).thenReturn(first);
        when(firstHit.getScore()).thenReturn(2.0f);
        when(secondHit.getContent()).thenReturn(second);
        when(secondHit.getScore()).thenReturn(1.0f);
        when(searchHits.stream()).thenReturn(List.of(firstHit, secondHit).stream());

        when(elasticsearchOperations.search(
                any(NativeQuery.class),
                eq(IncidentSearchDocument.class)
        )).thenReturn(searchHits);

        List<IncidentRankedSearchResult> result =
                service.searchRanked("payment", 1);

        assertThat(result)
                .extracting(IncidentRankedSearchResult::incidentId)
                .containsExactly("INC-RANK-1");
    }
}