package com.specpulse.scheduler;

public interface PullExecutionStorePort {

    PullExecutionEntity save(PullExecutionEntity execution);
}
