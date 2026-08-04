package com.specpulse.scheduler.domain;

import com.specpulse.scheduler.domain.PullExecutionEntity;
public interface PullExecutionStorePort {

    PullExecutionEntity save(PullExecutionEntity execution);
}
