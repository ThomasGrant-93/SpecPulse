package com.specpulse.diff.domain;

public interface SpecDiffPort {

    void analyzeAndStore(Long serviceId, Long fromVersionId, Long toVersionId);
}
