package org.halocambodia.data;

import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "emp_language",schema  = "public")
@Getter
@Setter
public class EmployeeLanguage  extends AbstractEntity{
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_language_emp_lang_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_language_emp_lang_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "emp_lang_id")
	    private Long id; 
	 
		@Override
		public Long getId() {
		    return id;
		}
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_id",nullable = false)	    
	    @NotNull(message = "Employee cannot be null or empty. Please select only one.")
	    private Employee employee;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "language_id",nullable = false)	    
	    @NotNull(message = "Language cannot be null or empty. Please select only one.")
	    private Language language;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "reading",nullable = false)	    
	    @NotNull(message = "Language Reading cannot be null or empty. Please select only one.")
	    private LanguageSkill languageReading;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "speaking",nullable = false)	    
	    @NotNull(message = "Language Speaking cannot be null or empty. Please select only one.")
	    private LanguageSkill languageSpeaking;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "listening",nullable = false)	    
	    @NotNull(message = "Language Listening cannot be null or empty. Please select only one.")
	    private LanguageSkill languageListening;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "writing",nullable = false)	    
	    @NotNull(message = "Language Writing cannot be null or empty. Please select only one.")
	    private LanguageSkill languageWriting;

	    
		@Version
		@Column(name = "version")
		private Long version;
	

}
