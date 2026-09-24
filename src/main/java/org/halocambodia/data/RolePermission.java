package org.halocambodia.data;

import jakarta.persistence.*;

@Entity
@Table(name = "application_role_permissions",schema  = "core_system")
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_system.application_role_permissions_seq")
    // The initial value is to account for data.sql demo data ids
    @SequenceGenerator(name = "core_system.application_role_permissions_seq", initialValue = 1,allocationSize = 1)
    @Column(name = "role_permissions_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "permission_id", nullable = false)
    private Permissions permission;

    @Column(name = "admin_page")
    private Boolean adminPage;

    @Column(name = "selected_page")
    private Boolean selectedPage;

    @Column(name = "inserted_page")
    private Boolean insertedPage;

    @Column(name = "updated_page")
    private Boolean updatedPage;

    @Column(name = "deleted_page")
    private Boolean deletedPage;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Permissions getPermission() {
        return permission;
    }

    public void setPermission(Permissions permission) {
        this.permission = permission;
    }

    public Boolean getAdminPage() {
    	return adminPage != null ? adminPage : false; 
    }

    public void setAdminPage(Boolean adminPage) {
        this.adminPage = adminPage;
    }

    public Boolean getSelectedPage() {
        return selectedPage != null ? selectedPage : false; 
    }

    public void setSelectedPage(Boolean selectedPage) {
        this.selectedPage = selectedPage;
    }

    public Boolean getInsertedPage() {
        return insertedPage != null ? insertedPage : false; 
    }

    public void setInsertedPage(Boolean insertedPage) {
        this.insertedPage = insertedPage;
    }

    public Boolean getUpdatedPage() {
        return updatedPage != null ? updatedPage : false; 
    }

    public void setUpdatedPage(Boolean updatedPage) {
        this.updatedPage = updatedPage;
    }

    public Boolean getDeletedPage() {
        return deletedPage != null ? deletedPage : false; 
    }

    public void setDeletedPage(Boolean deletedPage) {
        this.deletedPage = deletedPage;
    }
}
