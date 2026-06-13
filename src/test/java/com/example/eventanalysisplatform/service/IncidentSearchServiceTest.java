package com.example.eventanalysisplatform.service;

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
}