package org.halocambodia.data;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.halocambodia.data.ReportQueryParamater;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(name = "report_query",schema  = "core_system")
public class ReportQuery extends AbstractEntity{
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_system.report_query_report_query_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "core_system.report_query_report_query_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "report_query_id")
	    private Long id;

	    
	    @Column(name = "report_slug", nullable = false, unique = true)
	    @NotBlank(message = "report slug cannot be null or empty")
	    private String reportSlug;

	    @Column(name = "report_name", nullable = false)
	    @NotBlank(message = "report name cannot be null or empty")
	    private String reportName;

	    
	    @Column(name = "jasper_part_report")
	    private String jasperPartReport;
	    
	    @Column(name = "jrxml_part_report")
	    private String jrxmlPartReport;
	    
	    @Column(name = "notes")
	    private String notes;
	    
	    @Column(name = "report_group_name",nullable = false)
	    @NotBlank(message = "Group name cannot be null or empty")
	    private String reportGroupName;
		
	    @Column(name = "sort_order")
	    private Integer sortOrder;
	    
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "deleted_by",insertable = false, nullable = true,updatable = true)
	    private User userDelated;
	    
		@UpdateTimestamp
		@Column(name = "deleted_at",insertable = false, nullable = true, columnDefinition = "TIMESTAMP(0) WITH TIME ZONE")
		private ZonedDateTime deletedAt;
		
		@OneToMany(mappedBy = "reportQuery", cascade = CascadeType.ALL, orphanRemoval = true)
		@OrderBy("paramaterSortOrder ASC")
	    private List<ReportQueryParamater> reportQueryParamaterList = new ArrayList<>();
	 
	    @Column(name = "report_path")
	    private String reportPath;
	    
	    @Column(name = "report_type",nullable = false)
	    @NotBlank(message = "report type cannot be null or empty")
	    private String reportType;
	    
	    @Column(name = "query")
	    private String query;
	    
	    @NotEmpty(message = "At least one role must be selected")
	    @ManyToMany(fetch = FetchType.LAZY)
	    @JoinTable(
	        name = "report_query_role",schema  = "core_system",
	        joinColumns = @JoinColumn(name = "report_query_id"),
	        inverseJoinColumns = @JoinColumn(name = "roles_id")
	    )
	    private Set<Role> roles;
	    

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getReportSlug() {
			return reportSlug;
		}
		private static String slugify(String s) {
		    if (s == null) return null;
		    String out = s.trim().toLowerCase(java.util.Locale.ROOT)
		        .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "-") // non-alnum → '-'
		        .replaceAll("(^-+|-+$)", "")                           // trim hyphens
		        .replaceAll("-{2,}", "-");                             // collapse
		    return out.isEmpty() ? null : out;
		}
		public void setReportSlug(String reportSlug) {
		    String normalized = slugify(reportSlug);
		    if (normalized == null) {
		        normalized = slugify(this.reportName);
		    }
		    this.reportSlug = normalized; // may still be null; lifecycle will enforce before persist/update
		}
		
		@PrePersist @PreUpdate
		private void ensureSlug() {
		    // Prefer explicitly set slug; else derive from name.
		    String normalized = slugify(this.reportSlug);
		    if (normalized == null) {
		        normalized = slugify(this.reportName);
		    }
		    if (normalized == null) {
		        // Throwing here prevents NPEs later and surfaces a clear message
		        throw new IllegalStateException("Report slug cannot be empty (need report name or slug).");
		    }
		    this.reportSlug = normalized;
		}



		public String getReportName() {
			return reportName;
		}

		public void setReportName(String reportName) {
			this.reportName = reportName;
		}


		public String getJasperPartReport() {
			return jasperPartReport;
		}

		public void setJasperPartReport(String jasperPartReport) {
			this.jasperPartReport = jasperPartReport;
		}

		public String getJrxmlPartReport() {
			return jrxmlPartReport;
		}

		public void setJrxmlPartReport(String jrxmlPartReport) {
			this.jrxmlPartReport = jrxmlPartReport;
		}

		public String getNotes() {
			return notes;
		}

		public void setNotes(String notes) {
			this.notes = notes;
		}

		public String getReportGroupName() {
			return reportGroupName;
		}

		public void setReportGroupName(String reportGroupName) {
			this.reportGroupName = reportGroupName;
		}

		public Integer getSortOrder() {
			return sortOrder;
		}

		public void setSortOrder(Integer sortOrder) {
			this.sortOrder = sortOrder;
		}

		public User getUserDelated() {
			return userDelated;
		}

		public void setUserDelated(User userDelated) {
			this.userDelated = userDelated;
		}

		public ZonedDateTime getDeletedAt() {
			return deletedAt;
		}

		public void setDeletedAt(ZonedDateTime deletedAt) {
			this.deletedAt = deletedAt;
		}

		public List<ReportQueryParamater> getReportQueryParamaterList() {
			return reportQueryParamaterList;
		}

		public void setReportQueryParamaterList(List<ReportQueryParamater> reportQueryParamaterList) {
			this.reportQueryParamaterList = reportQueryParamaterList;
		}

		public String getReportPath() {
			return reportPath;
		}

		public void setReportPath(String reportPath) {
			this.reportPath = reportPath;
		}

		public String getReportType() {
			return reportType;
		}

		public void setReportType(String reportType) {
			this.reportType = reportType;
		}

		public String getQuery() {
			return query;
		}

		public void setQuery(String query) {
			this.query = query;
		}

		public Set<Role> getRoles() {
			return roles;
		}

		public void setRoles(Set<Role> roles) {
			this.roles = roles;
		}
	    
		
	    
}
