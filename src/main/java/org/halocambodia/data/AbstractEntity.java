package org.halocambodia.data;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRules;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import org.halocambodia.utility.HasId;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;

@MappedSuperclass
public abstract class AbstractEntity implements HasId{
    @Override
    public abstract Long getId();
	
	//@ManyToOne(fetch = FetchType.EAGER)
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by",nullable = false, updatable = false)
    private User userCreated;
    
    
    @CreationTimestamp
    @Column(name = "created_at",nullable = false, updatable = false, columnDefinition = "TIMESTAMP(0) WITH TIME ZONE")
    private ZonedDateTime createdAt;   
    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", nullable = false,updatable = true)
    private User userUpdated;
	
	@UpdateTimestamp
	//@Column(name = "updated_at",insertable = false, nullable = true, columnDefinition = "TIMESTAMP(0) WITH TIME ZONE")
	@Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP(0) WITH TIME ZONE")
	private ZonedDateTime updatedAt;

	
	public User getUserCreated() {
		return userCreated;
	}

	public void setUserCreated(User userCreated) {
		this.userCreated = userCreated;
	}
	
	public ZonedDateTime getCreatedAt() {
		return createdAt != null  ? createdAt.withZoneSameInstant(ZoneId.of("Asia/Phnom_Penh")) : null;
		//return createdAt != null ? createdAt.withZoneSameInstant(ZoneRules) : null;
    }
	public void setCreatedAt(ZonedDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public User getUserUpdated() {
		return userUpdated;
	}

	public void setUserUpdated(User userUpdated) {
		this.userUpdated = userUpdated;
	}
	public ZonedDateTime getUpdatedAt() {
		return updatedAt != null  ? updatedAt.withZoneSameInstant(ZoneId.of("Asia/Phnom_Penh")) : null;
		//return updatedAt != null  ? ZonedDateTime.parse(updatedAt.toString()) : null;
	}
	public void setUpdatedAt(ZonedDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
