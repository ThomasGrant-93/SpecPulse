package com.specpulse.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationSettingRepository extends JpaRepository<ApplicationSetting, Long> {

    /**
     * Найти все настройки категории
     */
    List<ApplicationSetting> findByCategoryOrderByKeyAsc(String category);

    /**
     * Найти все настройки
     */
    List<ApplicationSetting> findAllByOrderByCategoryAscKeyAsc();

    /**
     * Найти настройку по категории и ключу
     */
    Optional<ApplicationSetting> findByCategoryAndKey(String category, String key);

    /**
     * Найти публичные настройки категории
     */
    List<ApplicationSetting> findByCategoryAndIsPublicTrueOrderByKeyAsc(String category);

    /**
     * Получить все категории настроек
     */
    @Query("SELECT DISTINCT s.category FROM ApplicationSetting s ORDER BY s.category")
    List<String> findAllCategories();

    /**
     * Проверить существование настройки
     */
    boolean existsByCategoryAndKey(String category, String key);

    /**
     * Найти редактируемые настройки
     */
    List<ApplicationSetting> findByIsEditableTrueOrderByCategoryAscKeyAsc();
}
