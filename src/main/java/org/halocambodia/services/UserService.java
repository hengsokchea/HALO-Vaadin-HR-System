package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.Department;
import org.halocambodia.data.DepartmentRepository;
import org.halocambodia.data.Role;
import org.halocambodia.data.User;
import org.halocambodia.data.UserRepository;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.admin.user_management.UserManagementView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService  implements GenericService<User>{

    private final UserRepository repository;
    private final DepartmentRepository departmentRepository;
    
    private final AuthenticatedUser authenticatedUser;

    public UserService(UserRepository repository,DepartmentRepository departmentRepository,AuthenticatedUser authenticatedUser) {
        this.repository = repository;
        this.departmentRepository = departmentRepository;
        this.authenticatedUser = authenticatedUser;
    }

    public Optional<User> get(Long id) {
        return repository.findById(id);
    }

    public User update(User entity) {
    	
	      User currentUserLogin = authenticatedUser.get()
		          .orElseThrow(() -> new IllegalArgumentException("User not logged in"));
	      
    	 boolean isUpdating = entity.getId() != null;
    	AccessPageType accessPageType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
        if (!authenticatedUser.hasPage(UserManagementView.class, accessPageType)) {
            throw new IllegalArgumentException("You don't have permission to do this operation " + (isUpdating ? "update" : "insert"));
        }
        
	      // Set audit fields
        entity.setUserCreated(currentUserLogin); // Only if it’s a new record
        entity.setUserUpdated(currentUserLogin);
        
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword =isUpdating? entity.getHashedPassword():passwordEncoder.encode(entity.getHashedPassword());
        
        entity.setHashedPassword(hashedPassword);
        
        return repository.save(entity);
    }



    public void delete(Set <User> entity) {
    	 if (!authenticatedUser.hasPage(UserManagementView.class, AccessPageType.DELETED_PAGE)) {
             throw new IllegalArgumentException("You don't have permission to do this operation ");
         }
    	 
        repository.deleteAllInBatch(entity);
    }
    
    public Page<User> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<User> list(Pageable pageable, Specification<User> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }
    public List<User> getAllUser() {
        return repository.findAll();
    }
    public long count(Specification<User> filter) {
        Specification<User> spec = (filter != null) ? filter : null;
        return repository.count(spec);
    }
    
    public List<User> findAll(Specification<User> filter){
    	 return repository.findAll(filter != null ? filter : null);
    }

    public List<User> searchUsersByName(String filter, Pageable pageable) {
        String f = (filter == null) ? "" : filter.trim();
        return repository.searchByNameContains(f, pageable).getContent();
    }

    public long countUsersByName(String filter) {
        String f = (filter == null) ? "" : filter.trim();
        return repository.countByNameContains(f);
    }
}
