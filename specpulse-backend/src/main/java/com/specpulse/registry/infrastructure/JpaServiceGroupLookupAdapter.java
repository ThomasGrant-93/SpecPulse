package com.specpulse.registry.infrastructure;

import com.specpulse.group.domain.ServiceGroup;
import com.specpulse.group.infrastructure.ServiceGroupRepository;
import com.specpulse.registry.domain.ServiceGroupLookupPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaServiceGroupLookupAdapter implements ServiceGroupLookupPort {

    private final ServiceGroupRepository groupRepository;

    public JpaServiceGroupLookupAdapter(ServiceGroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Override
    public Optional<ServiceGroup> findById(Long id) {
        return groupRepository.findById(id);
    }
}
