package org.halocambodia.data;


import org.halocambodia.data.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "report_query_paramater",schema  = "core_system")
public class ReportQueryParamater extends AbstractEntity{
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_system.report_query_paramater_report_query_paramater_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "core_system.report_query_paramater_report_query_paramater_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "report_query_paramater_id")
	    private Long id;

	    
	    @Column(name = "name", nullable = false)
	    @NotBlank(message = "Paramater name cannot be null or empty")
	    private String paramaterName;

	    @Column(name = "type", nullable = false)
	    @NotBlank(message = "paramater type  cannot be null or empty")
	    private String paramaterType;

	    @Column(name = "label", nullable = false)
	    @NotBlank(message = "paramater label  cannot be null or empty")
	    private String paramaterLabel;
	    
	    @Column(name = "lookup")
	    private String paramaterLookup;
	    
	    @Column(name = "sort_order")
	    private Integer paramaterSortOrder;
	    
	    @Column(name = "parent_paramater_name")
	    private String parentParamaterName;
	    
	    @Column(name = "required_field", nullable = false)
	    @NotNull(message = "Required Field cannot be null or empty")
	    private Boolean requiredField=true;
	    
	    
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "report_query_id")
	    private ReportQuery reportQuery;


		public Long getId() {
			return id;
		}


		public void setId(Long id) {
			this.id = id;
		}


		public String getParamaterName() {
			return paramaterName;
		}


		public void setParamaterName(String paramaterName) {
			this.paramaterName = paramaterName;
		}


		public String getParamaterType() {
			return paramaterType;
		}


		public void setParamaterType(String paramaterType) {
			this.paramaterType = paramaterType;
		}


		public String getParamaterLabel() {
			return paramaterLabel;
		}


		public void setParamaterLabel(String paramaterLabel) {
			this.paramaterLabel = paramaterLabel;
		}


		public String getParamaterLookup() {
			return paramaterLookup;
		}


		public void setParamaterLookup(String paramaterLookup) {
			this.paramaterLookup = paramaterLookup;
		}


		public Integer getParamaterSortOrder() {
			return paramaterSortOrder;
		}


		public void setParamaterSortOrder(Integer paramaterSortOrder) {
			this.paramaterSortOrder = paramaterSortOrder;
		}


		public ReportQuery getReportQuery() {
			return reportQuery;
		}


		public void setReportQuery(ReportQuery reportQuery) {
			this.reportQuery = reportQuery;
		}


		public String getParentParamaterName() {
			return parentParamaterName;
		}


		public void setParentParamaterName(String parentParamaterName) {
			this.parentParamaterName = parentParamaterName;
		}


		public Boolean getRequiredField() {
			return requiredField;
		}


		public void setRequiredField(Boolean requiredField) {
			this.requiredField = requiredField;
		}
	    
	    


	    
}
