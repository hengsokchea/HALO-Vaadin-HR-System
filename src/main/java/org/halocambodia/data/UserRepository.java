package org.halocambodia.data;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;


public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

	List<User> findAllByUsername(String username);
	List<User> findAllByUsernameIgnoreCase(String username);

	
	@Query(value = """
		    SELECT u.id
		    FROM core_system.application_user u
		    JOIN core_system.application_user_role ur ON u.id = ur.user_id
		    JOIN core_system.application_roles r ON ur.role_id = r.roles_id
		    JOIN core_system.application_role_permissions rp ON r.roles_id = rp.role_id
		    JOIN core_system.application_permissions p ON rp.permission_id = p.permissions_id
		    WHERE u.id = :userId
		    AND p.route_value = :routeValue
		    AND (
		        rp.admin_page = TRUE 
		        OR (
		            CASE 
		                WHEN :accessPageType = 'SELECTED_PAGE' THEN rp.selected_page
		                WHEN :accessPageType = 'INSERTED_PAGE' THEN rp.inserted_page
		                WHEN :accessPageType = 'UPDATED_PAGE' THEN rp.updated_page
		                WHEN :accessPageType = 'DELETED_PAGE' THEN rp.deleted_page
		                ELSE FALSE
		            END
		        ) = TRUE
		    )
		""", nativeQuery = true)
		List<Object[]> fetchUserPermissions(
		    @Param("userId") Long userId,
		    @Param("routeValue") String routeValue,
		    @Param("accessPageType") String accessPageType
		);

		
		@Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.username = :username")
		User findByUsernameWithRoles(@Param("username") String username);
		
		
		@Query("""
		        select u
		        from User u
		        where lower(u.name) like concat('%', lower(:filter), '%')
		        order by u.name
		    """)
	Page<User> searchByNameContains(@Param("filter") String filter, Pageable pageable);

	@Query("""
		        select count(u)
		        from User u
		        where lower(u.name) like concat('%', lower(:filter), '%')
		    """)
	long countByNameContains(@Param("filter") String filter);

		
}

