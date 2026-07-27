package com.specpulse.scheduler;

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
