package org.halocambodia.services;

import org.halocambodia.data.Gazetteer;
import org.halocambodia.data.GazetteerRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class GazetteerService {

    private final GazetteerRepository repo;

    public GazetteerService(GazetteerRepository repo) {
        this.repo = repo;
    }

    public Gazetteer save(Gazetteer g) {
        // Basic hierarchy rule (optional)
        if (g.getLevel() != null && g.getLevel() == 1) {
            g.setParent(null);
        }
        return repo.save(g);
    }

    public void delete(Gazetteer g) {
        repo.delete(g);
    }

    public boolean hasChildren(Long id) {
        return repo.existsByParent_Id(id);
    }

    /**
     * Parent options for editor (childLevel-1)
     */
    public List<Gazetteer> listPossibleParents(Integer childLevel) {
        if (childLevel == null || childLevel <= 1) return List.of();
        return repo.findByLevelOrderByNameEnAsc(childLevel - 1);
    }

    // ✅ Used by GazetteerField (Province combo)
    public List<Gazetteer> listByLevel(int level) {
        return repo.findByLevelOrderByNameEnAsc(level);
    }

    // ✅ Used by GazetteerField (load District/Commune/Village by parent)
    public List<Gazetteer> listChildren(Long parentId) {
        if (parentId == null) return List.of();
        // you can sort by nameEn; if you want Khmer sort, change this
        return repo.findByParent_Id(parentId, Sort.by(Sort.Direction.ASC, "nameEn"));
    }

    // ---------- For TreeGrid Lazy Loading ----------
    public long countChildren(Gazetteer parent, GazetteerFilter filter) {
        return repo.count(buildSpec(parent, filter));
    }

    public List<Gazetteer> fetchChildren(Gazetteer parent, GazetteerFilter filter, int offset, int limit, Sort sort) {
        int page = offset / Math.max(limit, 1);
        Pageable pageable = PageRequest.of(page, Math.max(limit, 1),
                sort == null ? Sort.by("nameEn").ascending() : sort);
        return repo.findAll(buildSpec(parent, filter), pageable).getContent();
    }

    // ---------- Specification ----------
    private Specification<Gazetteer> buildSpec(Gazetteer parent, GazetteerFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            // parent condition (root items when parent == null)
            if (parent == null) {
                ps.add(cb.isNull(root.get("parent")));
            } else {
                ps.add(cb.equal(root.get("parent").get("id"), parent.getId()));
            }

            if (filter != null) {
                if (filter.level() != null) {
                    ps.add(cb.equal(root.get("level"), filter.level()));
                }
                if (filter.hasSearch()) {
                    String like = "%" + filter.search().trim().toLowerCase() + "%";
                    ps.add(cb.or(
                            cb.like(cb.lower(root.get("code")), like),
                            cb.like(cb.lower(root.get("nameEn")), like),
                            cb.like(cb.lower(root.get("nameKh")), like)
                    ));
                }
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
