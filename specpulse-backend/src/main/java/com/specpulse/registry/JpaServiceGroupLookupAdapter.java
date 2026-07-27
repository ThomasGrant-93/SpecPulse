package com.specpulse.registry;

import com.specpulse.entity.ServiceGroup;
import com.specpulse.group.ServiceGroupRepository;
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
