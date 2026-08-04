package com.specpulse.registry.domain;

import com.specpulse.group.domain.ServiceGroup;

import java.util.Optional;

public interface ServiceGroupLookupPort {

    Optional<ServiceGroup> findById(Long id);
}
