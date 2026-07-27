package com.specpulse.registry;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaRegistryRepositoryAdapter implements RegistryRepositoryPort {

    private final ServiceSearchRepository repository;

    public JpaRegistryRepositoryAdapter(ServiceSearchRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ServiceEntity> findAll() {
        return repository.findAll();
    }

    @Override
    public List<ServiceEntity> findByEnabledTrue() {
        return repository.findByEnabledTrue();
    }

    @Override
    public Optional<ServiceEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public ServiceEntity save(ServiceEntity entity) {
        return repository.save(entity);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByName(name);
    }

    @Override
    public List<ServiceEntity> search(String query) {
        return repository.search(query);
    }

    @Override
    public List<String> suggestServiceNames(String query) {
        return repository.suggestServiceNames(query);
    }

    @Override
    public List<String> suggestByDescription(String query) {
        return repository.suggestByDescription(query);
    }
}
