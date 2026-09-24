package org.halocambodia.services;

public record GazetteerFilter(String search, Integer level) {
    public boolean hasSearch() {
        return search != null && !search.trim().isEmpty();
    }
}
