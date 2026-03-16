package com.specpulse.diff;

import com.specpulse.version.VersionService;
import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.model.DiffResult;
import org.openapitools.openapidiff.core.output.ConsoleRender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DiffService {

    private static final Logger log = LoggerFactory.getLogger(DiffService.class);

    private final SpecDiffRepository repository;
    private final VersionService versionService;

    public DiffService(SpecDiffRepository repository, VersionService versionService) {
        this.repository = repository;
        this.versionService = versionService;
    }

    public DiffResultDTO compare(String oldSpec, String newSpec) {
        log.debug("Comparing two OpenAPI specifications");

        ChangedOpenApi changedOpenApi = OpenApiCompare.fromContents(oldSpec, newSpec);
        DiffResult diffResult = changedOpenApi.isChanged();

        String diffOutput = new ConsoleRender().render(changedOpenApi);
        boolean hasBreakingChanges = diffResult.isIncompatible();
        int breakingChangesCount = countBreakingChanges(changedOpenApi);

        if (hasBreakingChanges) {
            log.warn("Breaking changes detected: count={}, compatible={}",
                    breakingChangesCount, diffResult.isCompatible());
        } else {
            log.info("No breaking changes detected: compatible={}, unchanged={}",
                    diffResult.isCompatible(), diffResult.isUnchanged());
        }

        return new DiffResultDTO(
                diffOutput,
                hasBreakingChanges,
                breakingChangesCount,
                diffResult.isCompatible(),
                diffResult.isIncompatible(),
                diffResult.isUnchanged()
        );
    }

    @Transactional
    public SpecDiffEntity compareAndStore(Long serviceId, Long fromVersionId, Long toVersionId) {
        log.info("Comparing versions for service: serviceId={}, fromVersionId={}, toVersionId={}",
                serviceId, fromVersionId, toVersionId);

        // Get spec content from both versions
        String fromSpecContent = versionService.getSpecContentById(fromVersionId);
        String toSpecContent = versionService.getSpecContentById(toVersionId);

        // Compare the specs
        DiffResultDTO diffResult = compare(fromSpecContent, toSpecContent);

        // Save diff result to database
        SpecDiffEntity diffEntity = new SpecDiffEntity(
                serviceId,
                fromVersionId,
                toVersionId,
                diffResult.diff(),
                diffResult.hasBreakingChanges(),
                diffResult.breakingChangesCount()
        );

        SpecDiffEntity saved = repository.save(diffEntity);

        if (diffResult.hasBreakingChanges()) {
            log.warn("Breaking changes stored: diffId={}, serviceId={}, breakingCount={}",
                    saved.getId(), serviceId, diffResult.breakingChangesCount());
        } else {
            log.info("Diff stored successfully: diffId={}, serviceId={}, hasBreaking={}",
                    saved.getId(), serviceId, diffResult.hasBreakingChanges());
        }

        return saved;
    }

    public List<SpecDiffDTO> getDiffsByServiceId(Long serviceId) {
        return repository.findByServiceIdOrderByCreatedAtDesc(serviceId).stream()
                .map(SpecDiffDTO::fromEntity)
                .toList();
    }

    public SpecDiffDTO getDiffById(Long id) {
        SpecDiffEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Diff not found with id: " + id));
        return SpecDiffDTO.fromEntity(entity);
    }

    private int countBreakingChanges(ChangedOpenApi changedOpenApi) {
        // Simplified breaking changes count based on diff result
        // openapi-diff library marks incompatible changes in the result
        return changedOpenApi.isIncompatible() ? 1 : 0;
    }
}
