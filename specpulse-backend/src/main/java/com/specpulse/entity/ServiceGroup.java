package com.specpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_groups")
@Getter
@Setter
public class ServiceGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    private ServiceGroup parentGroup;

    @OneToMany(mappedBy = "parentGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ServiceGroup> childGroups = new ArrayList<>();

    @Column(length = 7)
    private String color;

    @Column(length = 50)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> members = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Добавить сервис в группу
     */
    public void addMember(GroupMember member) {
        members.add(member);
        member.setGroup(this);
    }

    /**
     * Удалить сервис из группы
     */
    public void removeMember(GroupMember member) {
        members.remove(member);
        member.setGroup(null);
    }

    /**
     * Получить полный путь группы (например: "production/backend")
     */
    public String getFullPath() {
        if (parentGroup == null) {
            return name;
        }
        return parentGroup.getFullPath() + "/" + name;
    }

    /**
     * Получить уровень вложенности
     */
    public int getLevel() {
        if (parentGroup == null) {
            return 0;
        }
        return parentGroup.getLevel() + 1;
    }
}
