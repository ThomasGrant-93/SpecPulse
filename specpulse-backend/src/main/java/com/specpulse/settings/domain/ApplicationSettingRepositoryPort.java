package com.specpulse.settings.domain;

import java.util.List;
import java.util.Optional;

public interface ApplicationSettingRepositoryPort {

    List<ApplicationSetting> findAllByOrderByCategoryAscKeyAsc();

    List<ApplicationSetting> findByCategoryOrderByKeyAsc(String category);

    Optional<ApplicationSetting> findByCategoryAndKey(String category, String key);

    ApplicationSetting save(ApplicationSetting setting);
}
