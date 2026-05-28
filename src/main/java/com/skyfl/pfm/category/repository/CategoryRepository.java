package com.skyfl.pfm.category.repository;

import com.skyfl.pfm.category.entity.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("""
            select c from Category c
            where c.deleted = false and (c.user.id = :userId or c.user is null)
            order by c.system desc, c.name asc
            """)
    List<Category> findVisibleCategories(UUID userId);

    @Query("""
            select c from Category c
            where c.id = :id and c.deleted = false and (c.user.id = :userId or c.user is null)
            """)
    Optional<Category> findAccessibleById(UUID id, UUID userId);

    @Query("""
            select c from Category c
            where c.deleted = false
              and lower(c.name) = lower(:name)
              and (c.user.id = :userId or c.user is null)
            order by c.system desc, c.createdAt asc
            """)
    List<Category> findAccessibleByName(UUID userId, String name);

    boolean existsByParentIdAndDeletedFalse(UUID parentId);
}
