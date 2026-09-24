package org.halocambodia.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "product_unit",schema  = "public")
public class Unit {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.product_unit_unit_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.product_unit_unit_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "unit_id")
	    private Long id;


	    
	    @Column(name = "unit_name", nullable = false, unique = true)
	    @NotBlank(message = "Unit cannot be null or empty")
	    private String unitName;
	    
	    private String note;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getUnitName() {
			return unitName;
		}

		public void setUnitName(String unitName) {
			this.unitName = unitName;
		}

		public String getNote() {
			return note;
		}

		public void setNote(String note) {
			this.note = note;
		}
	    
	    
	    

}
