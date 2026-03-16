package com.specpulse.group;

import com.specpulse.entity.GroupMember;
import com.specpulse.entity.ServiceGroup;
import com.specpulse.exception.ResourceNotFoundException;
import com.specpulse.registry.ServiceEntity;
import com.specpulse.registry.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ServiceGroupService {

    private final ServiceGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final ServiceRepository serviceRepository;

    /**
     * Получить все группы с иерархией
     */
    public List<ServiceGroupDTO> getAllGroups() {
        List<ServiceGroup> allGroups = groupRepository.findAll();
        return buildGroupTreeWithServiceCount(allGroups, null);
    }

    /**
     * Построить дерево групп с счетчиком сервисов
     */
    private List<ServiceGroupDTO> buildGroupTreeWithServiceCount(List<ServiceGroup> allGroups, Long parentId) {
        return allGroups.stream()
                .filter(g -> {
                    if (parentId == null) {
                        return g.getParentGroup() == null;
                    }
                    return g.getParentGroup() != null && g.getParentGroup().getId().equals(parentId);
                })
                .map(group -> {
                    ServiceGroupDTO dto = toDTOWithServiceCount(group);
                    dto.setChildGroups(buildGroupTreeWithServiceCount(allGroups, group.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Получить корневые группы
     */
    public List<ServiceGroupDTO> getRootGroups() {
        List<ServiceGroup> rootGroups = groupRepository.findByParentGroupIsNullOrderBySortOrderAsc();
        return rootGroups.stream()
                .map(this::toDTOWithServiceCount)
                .collect(Collectors.toList());
    }

    /**
     * Получить группу по ID с сервисами
     */
    public ServiceGroupDTO getGroupById(Long id, boolean includeServices) {
        ServiceGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group", id));

        ServiceGroupDTO dto = toDTO(group);
        dto.setServiceCount(groupRepository.countServicesByGroupId(id));

        if (includeServices) {
            List<GroupMember> members = memberRepository.findByGroupIdOrderByAddedAtDesc(id);
            dto.setServices(members.stream()
                    .map(this::toServiceDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * Создать новую группу
     */
    @Transactional
    public ServiceGroupDTO createGroup(CreateGroupRequest request) {
        // Проверка на максимальную глубину вложенности (максимум 10 уровней)
        if (request.getParentGroupId() != null) {
            int depth = calculateDepth(request.getParentGroupId());
            if (depth >= 10) {
                throw new IllegalArgumentException("Maximum nesting depth (10 levels) exceeded");
            }

            ServiceGroup parent = groupRepository.findById(request.getParentGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent group", request.getParentGroupId()));

            if (groupRepository.existsByNameAndParentGroup(request.getName(), parent)) {
                throw new IllegalArgumentException("Group with this name already exists under the parent");
            }
        } else {
            if (groupRepository.findByNameAndParentGroupIsNull(request.getName()).isPresent()) {
                throw new IllegalArgumentException("Root group with this name already exists");
            }
        }

        ServiceGroup group = new ServiceGroup();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setColor(request.getColor());
        group.setIcon(request.getIcon());
        group.setSortOrder(request.getSortOrder());

        if (request.getParentGroupId() != null) {
            ServiceGroup parent = groupRepository.findById(request.getParentGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent group", request.getParentGroupId()));
            group.setParentGroup(parent);
        }

        ServiceGroup saved = groupRepository.save(group);
        log.info("Created service group: id={}, name={}", saved.getId(), saved.getName());

        return toDTO(saved);
    }

    /**
     * Вычислить глубину вложенности группы
     */
    private int calculateDepth(Long groupId) {
        int depth = 0;
        ServiceGroup current = groupRepository.findById(groupId).orElse(null);

        while (current != null && current.getParentGroup() != null) {
            depth++;
            current = current.getParentGroup();
        }

        return depth;
    }

    /**
     * Обновить группу
     */
    @Transactional
    public ServiceGroupDTO updateGroup(Long id, UpdateGroupRequest request) {
        ServiceGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group", id));

        // Проверка на дублирование имени при изменении
        if (request.getName() != null && !request.getName().equals(group.getName())) {
            ServiceGroup parent = group.getParentGroup();
            if (groupRepository.existsByNameAndParentGroup(request.getName(), parent)) {
                throw new IllegalArgumentException("Group with this name already exists");
            }
            group.setName(request.getName());
        }

        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }

        if (request.getColor() != null) {
            group.setColor(request.getColor());
        }

        if (request.getIcon() != null) {
            group.setIcon(request.getIcon());
        }

        if (request.getSortOrder() != null) {
            group.setSortOrder(request.getSortOrder());
        }

        // Обновление родителя
        if (request.getParentGroupId() != null &&
                (group.getParentGroup() == null || !group.getParentGroup().getId().equals(request.getParentGroupId()))) {

            Long newParentId = request.getParentGroupId();

            // Проверка на цикл - родитель не может быть потомком
            if (isDescendant(newParentId, id)) {
                throw new IllegalArgumentException("Cannot set parent to a descendant of this group (would create a cycle)");
            }

            ServiceGroup newParent = groupRepository.findById(newParentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent group", newParentId));
            group.setParentGroup(newParent);
        } else if (request.getParentGroupId() == null && group.getParentGroup() != null) {
            // Сделать корневой группой
            group.setParentGroup(null);
        }

        ServiceGroup updated = groupRepository.save(group);
        log.info("Updated service group: id={}, name={}", updated.getId(), updated.getName());

        return toDTO(updated);
    }

    /**
     * Проверить, является ли потенциальный родитель потомком группы (для предотвращения циклов)
     */
    private boolean isDescendant(Long potentialParentId, Long groupId) {
        if (potentialParentId.equals(groupId)) {
            return true;
        }

        ServiceGroup potentialParent = groupRepository.findById(potentialParentId).orElse(null);
        if (potentialParent == null) {
            return false;
        }

        ServiceGroup current = potentialParent.getParentGroup();
        while (current != null) {
            if (current.getId().equals(groupId)) {
                return true;
            }
            current = current.getParentGroup();
        }

        return false;
    }

    /**
     * Удалить группу
     */
    @Transactional
    public void deleteGroup(Long id) {
        ServiceGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group", id));

        // Проверка, что группа не содержит подгрупп
        if (!group.getChildGroups().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete group with child groups. Move or delete child groups first.");
        }

        groupRepository.delete(group);
        log.info("Deleted service group: id={}, name={}", id, group.getName());
    }

    /**
     * Добавить сервисы в группу
     */
    @Transactional
    public ServiceGroupDTO addServicesToGroup(Long groupId, List<Long> serviceIds) {
        ServiceGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId));

        List<ServiceEntity> services = serviceRepository.findAllById(serviceIds);
        if (services.size() != serviceIds.size()) {
            throw new ResourceNotFoundException("Some services not found");
        }

        for (ServiceEntity service : services) {
            if (!memberRepository.existsByGroupIdAndServiceId(groupId, service.getId())) {
                GroupMember member = new GroupMember();
                member.setGroup(group);
                member.setService(service);
                memberRepository.save(member);

                // Установить основную группу для сервиса
                service.setGroup(group);
                serviceRepository.save(service);

                log.info("Added service {} to group {}", service.getId(), group.getId());
            }
        }

        return getGroupById(groupId, false);
    }

    /**
     * Удалить сервис из группы
     */
    @Transactional
    public void removeServiceFromGroup(Long groupId, Long serviceId) {
        if (!memberRepository.existsByGroupIdAndServiceId(groupId, serviceId)) {
            throw new ResourceNotFoundException("Service not in group");
        }

        memberRepository.deleteByGroupIdAndServiceId(groupId, serviceId);
        log.info("Removed service {} from group {}", serviceId, groupId);
    }

    /**
     * Получить группы сервиса
     */
    public List<ServiceGroupDTO> getServiceGroups(Long serviceId) {
        List<ServiceGroup> groups = memberRepository.findGroupsByServiceId(serviceId);
        return groups.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ========== Private methods ==========

    private List<ServiceGroupDTO> buildGroupTree(List<ServiceGroup> allGroups, Long parentId) {
        return allGroups.stream()
                .filter(g -> {
                    if (parentId == null) {
                        return g.getParentGroup() == null;
                    }
                    return g.getParentGroup() != null && g.getParentGroup().getId().equals(parentId);
                })
                .map(group -> {
                    ServiceGroupDTO dto = toDTOWithServiceCount(group);
                    dto.setChildGroups(buildGroupTree(allGroups, group.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private ServiceGroupDTO toDTO(ServiceGroup group) {
        return ServiceGroupDTO.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .parentGroupId(group.getParentGroup() != null ? group.getParentGroup().getId() : null)
                .parentGroupName(group.getParentGroup() != null ? group.getParentGroup().getName() : null)
                .color(group.getColor())
                .icon(group.getIcon())
                .sortOrder(group.getSortOrder())
                .createdAt(group.getCreatedAt() != null ? group.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toInstant() : null)
                .updatedAt(group.getUpdatedAt() != null ? group.getUpdatedAt().atZone(java.time.ZoneOffset.UTC).toInstant() : null)
                .build();
    }

    private ServiceGroupDTO toDTOWithServiceCount(ServiceGroup group) {
        ServiceGroupDTO dto = toDTO(group);
        dto.setServiceCount(groupRepository.countServicesByGroupId(group.getId()));
        return dto;
    }

    private ServiceGroupDTO.GroupServiceDTO toServiceDTO(GroupMember member) {
        ServiceEntity service = member.getService();
        return ServiceGroupDTO.GroupServiceDTO.builder()
                .id(service.getId())
                .name(service.getName())
                .openApiUrl(service.getOpenApiUrl())
                .description(service.getDescription())
                .enabled(service.isEnabled())
                .addedAt(member.getAddedAt() != null ? Instant.from(member.getAddedAt()) : null)
                .build();
    }
}
