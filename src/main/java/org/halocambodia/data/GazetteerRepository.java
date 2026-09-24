package org.halocambodia.data;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface GazetteerRepository extends JpaRepository<Gazetteer, Long>, JpaSpecificationExecutor<Gazetteer> {

    Optional<Gazetteer> findByCode(String code);

    // Roots (Province) => parent is null
    Page<Gazetteer> findByParentIsNull(Pageable pageable);
    long countByParentIsNull();

    // Children
    Page<Gazetteer> findByParent_Id(Long parentId, Pageable pageable);
    long countByParent_Id(Long parentId);

    boolean existsByParent_Id(Long parentId);

    // For parent dropdown (level-1)
    List<Gazetteer> findByLevelOrderByNameEnAsc(Integer level);
    
    List<Gazetteer> findByParent_Id(Long parentId, Sort sort);
}
