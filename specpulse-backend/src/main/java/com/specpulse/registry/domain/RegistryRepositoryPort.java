package com.specpulse.registry.domain;

import com.specpulse.registry.domain.ServiceEntity;

import java.util.List;
import java.util.Optional;

public interface RegistryRepositoryPort {

    List<ServiceEntity> findAll();

    List<ServiceEntity> findByEnabledTrue();

    Optional<ServiceEntity> findById(Long id);

    ServiceEntity save(ServiceEntity entity);

    void deleteById(Long id);

    boolean existsById(Long id);

    boolean existsByName(String name);

    List<ServiceEntity> search(String query);

    List<String> suggestServiceNames(String query);

    List<String> suggestByDescription(String query);
}
