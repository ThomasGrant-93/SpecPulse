package com.specpulse.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationSettingRepository extends JpaRepository<ApplicationSetting, Long> {

    /**
     * Find all settings for a category
     */
    List<ApplicationSetting> findByCategoryOrderByKeyAsc(String category);

    /**
     * Find all settings
     */
    List<ApplicationSetting> findAllByOrderByCategoryAscKeyAsc();

    /**
     * Find a setting by category and key
     */
    Optional<ApplicationSetting> findByCategoryAndKey(String category, String key);

    /**
     * Find public settings for a category
     */
    List<ApplicationSetting> findByCategoryAndIsPublicTrueOrderByKeyAsc(String category);

    /**
     * Get all settings categories
     */
    @Query("SELECT DISTINCT s.category FROM ApplicationSetting s ORDER BY s.category")
    List<String> findAllCategories();

    /**
     * Check whether a setting exists
     */
    boolean existsByCategoryAndKey(String category, String key);

    /**
     * Find editable settings
     */
    List<ApplicationSetting> findByIsEditableTrueOrderByCategoryAscKeyAsc();
}
