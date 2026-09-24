package org.halocambodia.data;

import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;

import org.halocambodia.component.PaginationControls;
import org.halocambodia.services.GenericService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.stream.Stream;

public class SmartDataProvider<T> extends CallbackDataProvider<T, Void> {
    
    private final PaginationControls pagination;
    private final GenericService<T> service;
    private final java.util.function.Supplier<Specification<T>> specSupplier;
    private static final int VAADIN_MAX_ITEMS_PER_FETCH = 500; // Vaadin's hard limit
    private static final int DATABASE_SCROLL_PAGE_SIZE = 500;
    
    public SmartDataProvider(PaginationControls pagination,
                           GenericService<T> service,
                           java.util.function.Supplier<Specification<T>> specSupplier) {
        this(
            pagination,
            service,
            specSupplier,
            Sort.by(Sort.Direction.DESC, "updatedAt")
        );
    }

    public SmartDataProvider(PaginationControls pagination,
                           GenericService<T> service,
                           java.util.function.Supplier<Specification<T>> specSupplier,
                           Sort defaultSort) {
        super(
            query -> fetchCallback(
                query,
                pagination,
                service,
                specSupplier,
                normalizeDefaultSort(defaultSort)
            ),
            query -> countCallback(query, pagination, service, specSupplier)
        );
        this.pagination = pagination;
        this.service = service;
        this.specSupplier = specSupplier;
    }
    
    private static <T> Stream<T> fetchCallback(Query<T, Void> query, 
                                             PaginationControls pagination,
                                             GenericService<T> service,
                                             java.util.function.Supplier<Specification<T>> specSupplier,
                                             Sort defaultSort) {
        if (pagination.isShowingAll()) {
            // For "All" mode, handle virtual scrolling
            Sort sort = toSpringSort(query.getSortOrders());
            if (sort.isUnsorted()) {
                sort = defaultSort;
            }
            
            // Vaadin will ask for items in chunks (e.g., offset 0-500, 500-1000, etc.)
            int requestedOffset = query.getOffset();
            int requestedLimit = query.getLimit();
            
            // IMPORTANT: Ensure we don't exceed Vaadin's 500-item limit per fetch
            int safeLimit = Math.min(requestedLimit, VAADIN_MAX_ITEMS_PER_FETCH);
            
            // Fetch only once. The previous implementation executed an unused
            // 1,000-row database query before calling this method.
            return fetchWithVirtualScrolling(
                query, sort, service, specSupplier,
                requestedOffset, safeLimit
            );
                
        } else {
            // Normal pagination mode
            int pageIndex = pagination.getCurrentPageIndex();
            int pageSize = pagination.getPageSizeValue();
            
            Sort sort = toSpringSort(query.getSortOrders());
            if (sort.isUnsorted()) {
                sort = defaultSort;
            }
            
            Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);
            Page<T> page = service.list(pageable, specSupplier.get());
            
            int innerOffset = Math.max(0, Math.min(query.getOffset(), pageSize));
            int innerLimit = Math.max(0, Math.min(query.getLimit(), pageSize - innerOffset));
            
            // Safety check for empty result
            if (innerLimit == 0) {
                return Stream.empty();
            }
            
            return page.getContent().stream()
                .skip(innerOffset)
                .limit(innerLimit);
        }
    }
    
    private static <T> Stream<T> fetchWithVirtualScrolling(
        Query<T, Void> query,
        Sort sort,
        GenericService<T> service,
        java.util.function.Supplier<Specification<T>> specSupplier,
        int requestedOffset,
        int requestedLimit
    ) {
        // A fixed page size keeps offset-to-page mapping correct even when Vaadin
        // changes the requested chunk size while the user scrolls.
        int databasePageSize = DATABASE_SCROLL_PAGE_SIZE;
        int databasePage = requestedOffset / databasePageSize;
        int offsetInPage = requestedOffset % databasePageSize;
        
        Pageable pageable = PageRequest.of(databasePage, databasePageSize, sort);
        Page<T> page = service.list(pageable, specSupplier.get());
        List<T> pageItems = page.getContent();
        
        // If we have enough items in this page
        if (offsetInPage + requestedLimit <= pageItems.size()) {
            return pageItems.stream()
                .skip(offsetInPage)
                .limit(requestedLimit);
        }
        
        // Otherwise, we need to fetch from multiple pages
        return fetchFromMultiplePages(
            sort, service, specSupplier,
            requestedOffset, requestedLimit,
            databasePage, offsetInPage, pageItems
        );
    }
    
    private static <T> Stream<T> fetchFromMultiplePages(
        Sort sort,
        GenericService<T> service,
        java.util.function.Supplier<Specification<T>> specSupplier,
        int requestedOffset,
        int requestedLimit,
        int startPage,
        int offsetInFirstPage,
        List<T> firstPageItems
    ) {
        java.util.List<T> combinedResults = new java.util.ArrayList<>();
        int itemsNeeded = requestedLimit;
        int currentPage = startPage;
        
        // Add items from first page
        int availableInFirstPage = firstPageItems.size() - offsetInFirstPage;
        int takeFromFirst = Math.min(itemsNeeded, availableInFirstPage);
        
        if (takeFromFirst > 0) {
            combinedResults.addAll(firstPageItems.subList(
                offsetInFirstPage, offsetInFirstPage + takeFromFirst
            ));
            itemsNeeded -= takeFromFirst;
        }
        
        // Fetch from additional pages if needed
        while (itemsNeeded > 0) {
            currentPage++;
            Pageable pageable = PageRequest.of(currentPage, DATABASE_SCROLL_PAGE_SIZE, sort);
            Page<T> page = service.list(pageable, specSupplier.get());
            List<T> pageItems = page.getContent();
            
            if (pageItems.isEmpty()) {
                break;
            }
            
            int take = Math.min(itemsNeeded, pageItems.size());
            combinedResults.addAll(pageItems.subList(0, take));
            itemsNeeded -= take;
        }
        
        return combinedResults.stream();
    }
    
    private static <T> int countCallback(Query<T, Void> query,
                                       PaginationControls pagination,
                                       GenericService<T> service,
                                       java.util.function.Supplier<Specification<T>> specSupplier) {
        if (pagination.isShowingAll()) {
            // For "All" mode, return the actual total count
            long total = service.count(specSupplier.get());
            pagination.setTotal(total);
            return (int) Math.min(Integer.MAX_VALUE, total);
        } else {
            long total = service.count(specSupplier.get());
            pagination.setTotal(total);
            
            int pageIndex = pagination.getCurrentPageIndex();
            int pageSize = pagination.getPageSizeValue();
            
            long start = (long) pageIndex * pageSize;
            long remaining = Math.max(0, total - start);
            
            return (int) Math.min(pageSize, remaining);
        }
    }
    
    private static Sort toSpringSort(List<QuerySortOrder> sortOrders) {
        if (sortOrders == null || sortOrders.isEmpty()) return Sort.unsorted();
        
        Sort sort = Sort.unsorted();
        for (QuerySortOrder so : sortOrders) {
            Sort.Direction dir = (so.getDirection() == com.vaadin.flow.data.provider.SortDirection.ASCENDING)
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            sort = sort.and(Sort.by(dir, so.getSorted()));
        }
        return sort;
    }

    private static Sort normalizeDefaultSort(Sort defaultSort) {
        if (defaultSort == null || defaultSort.isUnsorted()) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
        return defaultSort;
    }
}
