package com.specpulse.diff;

public interface SpecDiffPort {

    void analyzeAndStore(Long serviceId, Long fromVersionId, Long toVersionId);
}
