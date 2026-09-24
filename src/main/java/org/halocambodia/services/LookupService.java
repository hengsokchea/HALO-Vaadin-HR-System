package org.halocambodia.services;

import java.util.List;

import org.halocambodia.data.LookupRepository;
import org.halocambodia.data.LookupValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LookupService {

    @Autowired
    private LookupRepository lookupRepository;

    public List<LookupValue> getValues(String viewName) {
        return lookupRepository.getValuesFromView(viewName);
    }
    
    public List<LookupValue> getValues(String viewName, String parentId) {
        return lookupRepository.getValuesFromViewByParent(viewName, parentId);
    }

}

