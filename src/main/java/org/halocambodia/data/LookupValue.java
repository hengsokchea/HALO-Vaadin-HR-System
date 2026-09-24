package org.halocambodia.data;

public class LookupValue {
    private String id;
    private String description;
    private String parentLookupId;

    public LookupValue(String id, String description,String parentLookupId) {
        this.id = id;
        this.description = description;
        this.parentLookupId=parentLookupId;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
    

    public String getParentLookupId() {
		return parentLookupId;
	}


	@Override
    public String toString() {
        return description;
    }
}

