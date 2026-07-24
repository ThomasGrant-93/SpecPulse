package com.specpulse.settings;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaApplicationSettingRepositoryAdapter implements ApplicationSettingRepositoryPort {

    private final ApplicationSettingRepository repository;

    public JpaApplicationSettingRepositoryAdapter(ApplicationSettingRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ApplicationSetting> findAllByOrderByCategoryAscKeyAsc() {
        return repository.findAllByOrderByCategoryAscKeyAsc();
    }

    @Override
    public List<ApplicationSetting> findByCategoryOrderByKeyAsc(String category) {
        return repository.findByCategoryOrderByKeyAsc(category);
    }

    @Override
    public Optional<ApplicationSetting> findByCategoryAndKey(String category, String key) {
        return repository.findByCategoryAndKey(category, key);
    }

    @Override
    public ApplicationSetting save(ApplicationSetting setting) {
        return repository.save(setting);
    }
}
