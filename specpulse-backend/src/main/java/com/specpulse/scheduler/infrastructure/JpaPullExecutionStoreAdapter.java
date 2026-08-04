package com.specpulse.scheduler.infrastructure;

import com.specpulse.scheduler.domain.PullExecutionEntity;
import com.specpulse.scheduler.domain.PullExecutionStorePort;
import org.springframework.stereotype.Component;

@Component
public class JpaPullExecutionStoreAdapter implements PullExecutionStorePort {

    private final PullExecutionRepository repository;

    public JpaPullExecutionStoreAdapter(PullExecutionRepository repository) {
        this.repository = repository;
    }

    @Override
    public PullExecutionEntity save(PullExecutionEntity execution) {
        return repository.save(execution);
    }
}
