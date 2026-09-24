package org.halocambodia.data;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.Formula;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "application_user",schema  = "core_system")
public class User extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_system.application_user_id_seq")
    // The initial value is to account for data.sql demo data ids
    @SequenceGenerator(name = "core_system.application_user_id_seq", initialValue = 1,allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false)
    @NotBlank(message = "User name cannot be null or empty")
    private String username;
    
    @Column(name = "name", nullable = false)
    @NotBlank(message = "Display name cannot be null or empty")
    private String name;
    
    @Column(name = "email")
    //@NotBlank(message = "Email cannot be null or empty")
    //@Email(message = "Email should be valid")
    private String email;

   
    @Column(name = "hashed_password", nullable = false)
    @NotBlank(message = "Password cannot be null or empty")
    @JsonIgnore
    private String hashedPassword;

    
    //@NotEmpty(message = "At least one role must be selected")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "application_user_role",schema  = "core_system",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
    
    @Column(name = "can_login", nullable = false)
    @NotNull(message = "Can login cannot be null")
    private Boolean canLogin = true;

   
    @Column(name = "access_store_check", nullable = false)
    @NotNull(message = "API cannot be null")
    private Boolean api = false;
   
    @Column(name = "profile_image_path")
    private String profileImagePath;
    
    @Column(name = "insurance_no")
    private String insurance;
    
    
    @Column(name = "fingerprint_enabled", nullable = false)
    @NotNull(message = "Finger print Enabled cannot be null")
    private Boolean fingerprintEnabled = false;
    
	@Column(name = "last_fingerprint_login")
    private LocalDateTime lastFingerprintLogin;

    

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	



	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHashedPassword() {
		return hashedPassword;
	}

	public void setHashedPassword(String hashedPassword) {
		this.hashedPassword = hashedPassword;
	}



	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> role) {
		this.roles = role;
	}
	


	public Boolean getCanLogin() {
		return canLogin;
	}

	public void setCanLogin(Boolean canLogin) {
		this.canLogin = canLogin;
	}

	public Boolean getApi() {
		return api;
	}

	public void setApi(Boolean storeCheck) {
		this.api = storeCheck;
	}

	public String getProfileImagePath() {
		return profileImagePath;
	}

	public void setProfileImagePath(String profileImagePath) {
		this.profileImagePath = profileImagePath;
	}

	public String getInsurance() {
		return insurance;
	}

	public void setInsurance(String insurance) {
		this.insurance = insurance;
	}



	@Formula("(SELECT STRING_AGG(r.name, ', ') " +
	         "FROM core_system.application_user_role ur " +
	         "JOIN core_system.application_roles r ON r.roles_id = ur.role_id " +
	         "WHERE ur.user_id = id)")
	private String roleNames;



	public String getRoleNames() {
		return roleNames;
	}

	public void setRoleNames(String roleNames) {
		this.roleNames = roleNames;
	}
	
	
	@Transient
	public boolean hasRoleId(Long... ids) {
	    if (ids == null || ids.length == 0) return false;

	    Set<Long> allowed = new HashSet<>(Arrays.asList(ids));
	    return Optional.ofNullable(getRoles()).orElse(Collections.emptySet())
	            .stream()
	            .map(Role::getId)
	            .anyMatch(allowed::contains);
	}

	@Transient
	public boolean hasRoleName(String... names) {
	    if (names == null || names.length == 0) return false;

	    Set<String> allowed = Arrays.stream(names)
	            .filter(Objects::nonNull)
	            .map(String::toLowerCase)
	            .collect(Collectors.toSet());

	    return Optional.ofNullable(getRoles()).orElse(Collections.emptySet())
	            .stream()
	            .map(Role::getName) // adjust if your field is different
	            .filter(Objects::nonNull)
	            .map(String::toLowerCase)
	            .anyMatch(allowed::contains);
	}

	public Boolean getFingerprintEnabled() {
		return fingerprintEnabled;
	}

	public void setFingerprintEnabled(Boolean fingerprintEnabled) {
		this.fingerprintEnabled = fingerprintEnabled;
	}

	public LocalDateTime getLastFingerprintLogin() {
		return lastFingerprintLogin;
	}

	public void setLastFingerprintLogin(LocalDateTime lastFingerprintLogin) {
		this.lastFingerprintLogin = lastFingerprintLogin;
	}

	
	
    
}
