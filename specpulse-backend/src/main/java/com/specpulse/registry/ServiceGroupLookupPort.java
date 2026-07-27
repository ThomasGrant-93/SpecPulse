package com.specpulse.registry;

import com.specpulse.entity.ServiceGroup;

import java.util.Optional;

public interface ServiceGroupLookupPort {

    Optional<ServiceGroup> findById(Long id);
}
