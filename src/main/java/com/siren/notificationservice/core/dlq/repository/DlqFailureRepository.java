package com.siren.notificationservice.core.dlq.repository;

import com.siren.notificationservice.core.dlq.DlqFailureDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DlqFailureRepository extends ElasticsearchRepository<DlqFailureDocument, String> {
}
