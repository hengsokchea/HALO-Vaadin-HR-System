package org.halocambodia.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Entity
@Table(name = "application_roles",schema  = "core_system")
public class Role  extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_system.application_roles_rols_id_seq")
    // The initial value is to account for data.sql demo data ids
    @SequenceGenerator(name = "core_system.application_roles_rols_id_seq", initialValue = 1,allocationSize = 1)
    @Column(name = "roles_id")
    private Long id;

    @Column(nullable = false, unique = true)
    @Size(min = 1, message = "Role cannot be empty")
    @NotBlank(message = "Role cannot be null or empty")
    private String name;   

    @Column(name="can_see_salary", nullable = false)
    private Boolean canSeeSalary=Boolean.FALSE; 
    
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.EAGER,orphanRemoval = true)
    //@OneToMany(mappedBy = "role", fetch = FetchType.EAGER)
    private Set<RolePermission> rolePermissions;

    @ManyToMany(mappedBy = "roles")
    private Set<User> user;
    
    @NotEmpty(message = "At least one branch must be selected")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "application_role_branch",schema  = "core_system",
        joinColumns = @JoinColumn(name = "roles_id"),
        inverseJoinColumns = @JoinColumn(name = "branch_id")
    )
    private Set<Branch> branchs;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<RolePermission> getRolePermissions() {
        return rolePermissions;
    }

    public void setRolePermissions(Set<RolePermission> rolePermissions) {
        this.rolePermissions = rolePermissions;
    }

    public Set<User> getUsers() {
        return user;
    }

    public void setUsers(Set<User> user) {
        this.user = user;
    }

	public Set<Branch> getBranchs() {
		return branchs;
	}

	public void setBranchs(Set<Branch> branchs) {
		this.branchs = branchs;
	}

	public Boolean getCanSeeSalary() {
		return canSeeSalary;
	}

	public void setCanSeeSalary(Boolean canSeeSalary) {
		this.canSeeSalary = canSeeSalary;
	}
    
    
	
}
