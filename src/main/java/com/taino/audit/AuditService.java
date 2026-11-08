package com.taino.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(Long actorUserId, Long roomId, String action, Map<String, ?> meta) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setActorUserId(actorUserId);
        entity.setRoomId(roomId);
        entity.setAction(action);
        if (meta != null && !meta.isEmpty()) {
            try {
                entity.setMetaJson(objectMapper.writeValueAsString(meta));
            } catch (Exception ex) {
                log.debug("audit meta serialize failed");
            }
        }
        repository.save(entity);
    }
}
