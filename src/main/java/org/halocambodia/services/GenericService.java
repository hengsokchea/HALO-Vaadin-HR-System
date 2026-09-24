package org.halocambodia.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Set;

public interface GenericService<T> {
	Page<T> list(Pageable pageable, Specification<T> specification);
    long count(Specification<T> specification);
    T update(T entity);
    
    void delete(Set<T> entities);
    
	List<T> findAll(Specification<T> specification);
    
}
