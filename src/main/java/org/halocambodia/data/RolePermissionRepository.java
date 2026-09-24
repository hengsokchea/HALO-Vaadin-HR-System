package org.halocambodia.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RolePermissionRepository  extends JpaRepository<RolePermission, Long>, JpaSpecificationExecutor<RolePermission> {
	
}
