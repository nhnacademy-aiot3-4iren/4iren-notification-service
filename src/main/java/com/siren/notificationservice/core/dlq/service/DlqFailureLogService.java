package com.siren.notificationservice.core.dlq.service;

import com.siren.notificationservice.core.dlq.DlqFailureDocument;
import com.siren.notificationservice.core.dlq.repository.DlqFailureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqFailureLogService {

    private final DlqFailureRepository dlqFailureRepository;

    // ES 저장 자체가 DLQ recoverer 전체를 죽이면 안 되므로 여기서 삼킴
    public void save(DlqFailureDocument document) {
        try{
            dlqFailureRepository.save(document);
        } catch (Exception e) {
            log.error("[DlqFailureLogService] ES 적재 실패 (id={})", document.getId(), e);
        }
    }
}
