package org.halocambodia.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "list_blood_group",schema  = "public")
@Getter
@Setter
public class BloodGroup {
    @Id
    @NotBlank(message = "Blood group is required")
    @Column(name = "blood_group", nullable = false, unique = true, length = 20)
    private String bloodGroup;
}
