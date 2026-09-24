package org.halocambodia.data;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "application_permissions",schema  = "core_system")
public class Permissions {


	
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_system.permissions_permissions_id")
    // The initial value is to account for data.sql demo data ids
    @SequenceGenerator(name = "core_system.permissions_permissions_id", initialValue = 1,allocationSize = 1)
    @Column(name = "permissions_id")
    private Long id;
    

    @Column(name = "route_value",nullable = false, unique = true)
    private String routeValue;
    
    @Column(name = "route_name",nullable = false, unique = true)
    private String routeName;
    
    @Column(name = "sort_order",nullable = false)
    private Integer sortOrder;

    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<RolePermission> rolePermissions;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getRouteValue() {
		return routeValue;
	}

	public void setRouteValue(String routeValue) {
		this.routeValue = routeValue;
	}

	public String getRouteName() {
		return routeName;
	}

	public void setRouteName(String routeName) {
		this.routeName = routeName;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public Set<RolePermission> getRolePermissions() {
		return rolePermissions;
	}

	public void setRolePermissions(Set<RolePermission> rolePermissions) {
		this.rolePermissions = rolePermissions;
	}

    // Getters and Setters

    
}

