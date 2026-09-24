package org.halocambodia.data;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LookupRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<LookupValue> getValuesFromView(String viewName) {
        String sql = "SELECT lookup_id, lookup_description,parent_lookup_id FROM " + viewName;
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            new LookupValue(
                rs.getString("lookup_id"),  // always treat as string
                rs.getString("lookup_description"),
                rs.getString("parent_lookup_id")
            )
        );
    }
    
    public List<LookupValue> getValuesFromViewByParent(String viewName, String parentId) {
        String sql = "SELECT lookup_id, lookup_description, parent_lookup_id FROM " + viewName + " WHERE parent_lookup_id = ?";
        return jdbcTemplate.query(sql, new Object[]{parentId}, (rs, rowNum) ->
            new LookupValue(
                rs.getString("lookup_id"),
                rs.getString("lookup_description"),
                rs.getString("parent_lookup_id")
            )
        );
    }

    
}

