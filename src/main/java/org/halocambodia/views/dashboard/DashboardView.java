package org.halocambodia.views.dashboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.halocambodia.data.DataExplorerUser;
import org.halocambodia.data.DataExplorerUserRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;

import com.vaadin.flow.server.VaadinSession;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSetMetaData;
import java.util.*;

@PageTitle("Dashboard")
@Route(value = "", layout = MainLayout.class)
@PermitAll
public class DashboardView extends FlexLayout {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final DataExplorerUserRepository dataExplorerUserRepository;
    private final Optional<User> currentUserLogin;

    private final Map<Card, DataExplorerUser> cardUserMap = new LinkedHashMap<>();

    public DashboardView(AuthenticatedUser authenticatedUser, DataExplorerUserRepository dataExplorerUserRepository) {
        this.currentUserLogin = authenticatedUser.get();
        this.dataExplorerUserRepository = dataExplorerUserRepository;

        setFlexWrap(FlexWrap.WRAP);
        getStyle().set("gap", "0.2rem");
        setJustifyContentMode(JustifyContentMode.START);
        
        getElement().executeJs(
        	    "const updateLayout = () => {" +
        	    "  const width = window.innerWidth;" +
        	    "  const cards = document.querySelectorAll('vaadin-card');" +
        	    "  cards.forEach(card => {" +
        	    "    if(width < 768) {" +
        	    "      card.style.width = '100%';" +
        	    "    }" +
        	    "  });" +
        	    "};" +
        	    "window.addEventListener('resize', updateLayout);" +
        	    "updateLayout();"
        	);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI.getCurrent().access(() -> {
            currentUserLogin.ifPresent(currentUser -> {
                try {
                    List<DataExplorerUser> userRecords = dataExplorerUserRepository.findByUserOrderBySortOrder(currentUser);
                    for (DataExplorerUser dataExplorerUser : userRecords) {
                        Card card = createCard(dataExplorerUser);
                        cardUserMap.put(card, dataExplorerUser);
                        add(card);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    
                }
            });
        });
    }


    private Card createCard(DataExplorerUser dashboardUser) {
        Card card = new Card();

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> configMap = mapper.readValue(dashboardUser.getConfig(), new TypeReference<>() {});
            String width = configMap.getOrDefault("width", "24").toString() + "%";
            card.setWidth(width);
        } catch (Exception e) {
            card.setWidth("24%");
        }

        card.getStyle().set("margin", "0.3rem");
        card.getStyle().set("border-radius", "12px 12px 0 0");
        card.getStyle().set("padding", "0");
        card.getStyle().set("--lumo-card-border-radius", "0");
        card.getStyle().set("--lumo-card-padding", "0");
        
        // Add this to remove any gaps between card sections
        card.getStyle().set("display", "flex");
        card.getStyle().set("flex-direction", "column");
        card.getStyle().set("overflow", "hidden");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setMargin(false);
        header.setPadding(false); // Remove header padding

        Span title = new Span(dashboardUser.getDataExplorer().getDataExplorerName());
        title.getStyle().set("font-weight", "bold");
        title.getStyle().set("font-size", "1rem");

        HorizontalLayout actions = createCardActions(dashboardUser.getDataExplorer().getQuery(), card, dashboardUser);

        header.add(title, actions);
        
        actions.getChildren().forEach(component -> {
            if (component instanceof Button button) {
                button.getStyle().set("color", "white");
                button.getStyle().set("background", "transparent");
            }
        });

        Grid<Map<String, Object>> gridCard = createGridFromQuery(dashboardUser.getDataExplorer().getQuery());
        
        // Remove any default spacing from the grid
        gridCard.getStyle().set("padding", "0");
        gridCard.getStyle().set("margin", "0");
        gridCard.getStyle().set("border-top", "none"); // Remove border top if any

        card.setHeader(header);
        
        // Style the header with gradient and rounded top corners
        header.getStyle().set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)");
        header.getStyle().set("color", "white");
        header.getStyle().set("padding", "7px");
        header.getStyle().set("border-radius", "12px 12px 0 0");
        header.getStyle().set("margin", "0"); // Ensure no margin
        
        // Add the grid directly without any wrapper that might add spacing
        card.add(gridCard);
        
        // If card has a content area that adds padding, remove it
        card.getChildren().forEach(child -> {
            if (child != header) {
                child.getStyle().set("padding", "0");
                child.getStyle().set("margin", "0");
            }
        });
        
        // Drag and drop setup
        DragSource<Card> dragSource = DragSource.create(card);
        dragSource.setDraggable(true);

        DropTarget<Card> dropTarget = DropTarget.create(card);
        dropTarget.setDropEffect(DropEffect.MOVE);
        dropTarget.addDropListener(event -> {
            event.getDragSourceComponent().ifPresent(source -> {
                int sourceIndex = this.indexOf(source);
                int targetIndex = this.indexOf(card);
                if (sourceIndex != targetIndex) {
                    this.remove(source);
                    this.addComponentAtIndex(targetIndex, source);

                    List<DataExplorerUser> reorderedList = new ArrayList<>();
                    List<Component> orderedCards = this.getChildren()
                        .filter(Card.class::isInstance)
                        .toList();

                    int index = 0;
                    for (Component c : orderedCards) {
                        if (c instanceof Card currentCard && cardUserMap.containsKey(currentCard)) {
                            DataExplorerUser userCard = cardUserMap.get(currentCard);
                            userCard.setSortOrder(index++);
                            reorderedList.add(userCard);
                        }
                    }

                    dataExplorerUserRepository.saveAll(reorderedList);
                }
            });
        });

        applyCardStyling(card);
        return card;
    }

    private HorizontalLayout createCardActions(String query, Card card, DataExplorerUser dashboardUser) {
        Button btnRefresh = new Button(VaadinIcon.REFRESH.create());
        btnRefresh.getElement().setAttribute("title", "Refresh data");
        btnRefresh.addClickListener(e -> {
            Grid<Map<String, Object>> grid = getGridInsideCard(btnRefresh);
            if (grid != null) {
                List<Map<String, Object>> updatedData = fetchQueryData(query);
                grid.setItems(updatedData);
            }
        });

        
        Button btnExport = new Button(VaadinIcon.DOWNLOAD_ALT.create());
        btnExport.getElement().setAttribute("title", "Export to Excel");
        
        btnExport.addClickListener(e -> {
            List<Map<String, Object>> data = fetchQueryData(query);
            StreamResource resource = exportToExcel(data, "DashboardExport");
            if (resource == null) return;

            StreamRegistration reg = VaadinSession.getCurrent()
                .getResourceRegistry()
                .registerResource(resource);

            String url = reg.getResourceUri().toString();
            UI.getCurrent().getPage().open(url); // no target needed for downloads
            // Optional: reg.unregister() later if you store it; otherwise it’s cleaned up with the session.
        });

        

        Button btnRemove = new Button(VaadinIcon.TRASH.create());
        btnRemove.getElement().setAttribute("title", "Remove from Dashboard");
        btnRemove.getElement().getStyle().set("color", "red");
        btnRemove.addClickListener(e -> confirmDelete(card, dashboardUser));

        Button btnMenu = new Button(VaadinIcon.ELLIPSIS_DOTS_V.create());
        btnMenu.getElement().setAttribute("title", "Edit card width");
        btnMenu.addClickListener(e -> editCardWidth(card, dashboardUser));
        
        HorizontalLayout actionLayout= new HorizontalLayout(btnRefresh, btnExport, btnRemove, btnMenu);
        actionLayout.setSpacing(false);
        return actionLayout;
    }

    private void confirmDelete(Card card, DataExplorerUser dashboardUser) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Confirm Removal");

        Span message = new Span("Are you sure you want to remove this dashboard card?");
        Button cancel = new Button("Cancel", e -> confirmDialog.close());
        Button confirm = new Button("Remove", e -> {
            dataExplorerUserRepository.delete(dashboardUser);
            remove(card);
            cardUserMap.remove(card);
            confirmDialog.close();
        });

        confirm.getStyle().set("color", "red");

        HorizontalLayout buttons = new HorizontalLayout(cancel, confirm);
        confirmDialog.add(message, buttons);
        confirmDialog.open();
    }

    private void editCardWidth(Card card, DataExplorerUser dashboardUser) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Card Width");

        ComboBox<String> widthSelector = new ComboBox<>("Select width (%)");
        widthSelector.setItems("24","32", "49","67","72", "100");
        widthSelector.setValue(card.getWidth().replace("%", ""));

        Button save = new Button("Save", e -> {
            String selectedWidth = widthSelector.getValue();
            card.setWidth(selectedWidth + "%");

            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> configMap = mapper.readValue(dashboardUser.getConfig(), new TypeReference<>() {});
                configMap.put("width", selectedWidth);
                dashboardUser.setConfig(mapper.writeValueAsString(configMap));
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            dataExplorerUserRepository.save(dashboardUser);
            dialog.close();
        });

        Button cancel = new Button("Cancel", e -> dialog.close());

        dialog.add(widthSelector, new HorizontalLayout(cancel, save));
        dialog.open();
    }

    private Grid<Map<String, Object>> createGridFromQuery(String query) {
        Grid<Map<String, Object>> grid = new Grid<>();
        grid.setHeight("300px");

        try {
            List<Map<String, Object>> rows = fetchQueryData(query);

            if (!rows.isEmpty()) {
                Map<String, Object> firstRow = rows.get(0);
                for (String col : firstRow.keySet()) {
                    grid.addColumn(map -> String.valueOf(map.getOrDefault(col, ""))).setHeader(col);
                }
            }

            grid.getColumns().forEach(column -> {
                column.setResizable(true);
                column.setSortable(true);
                //column.setTextAlign(ColumnTextAlign.CENTER);
                column.setAutoWidth(true);
            });

            grid.setItems(rows);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return grid;
    }


    private List<Map<String, Object>> fetchQueryData(String query) {
        try {
            return jdbcTemplate.query(query, (rs, rowNum) -> {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                Map<String, Object> rowMap = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    rowMap.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                return rowMap;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList(); // Prevent crash on error
        }
    }


    private Grid<Map<String, Object>> getGridInsideCard(Component component) {
        Component parent = component.getParent().orElse(null);
        while (parent != null && !(parent instanceof Card)) {
            parent = parent.getParent().orElse(null);
        }

        if (parent instanceof Card card) {
            return card.getChildren()
                       .filter(Grid.class::isInstance)
                       .map(child -> (Grid<Map<String, Object>>) child)
                       .findFirst().orElse(null);
        }
        return null;
    }

    private StreamResource exportToExcel(List<Map<String, Object>> data, String fileName) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");

            if (!data.isEmpty()) {
                Row headerRow = sheet.createRow(0);
                Map<String, Object> firstRow = data.get(0);
                int colIndex = 0;
                for (String col : firstRow.keySet()) {
                    headerRow.createCell(colIndex++).setCellValue(col);
                }

                int rowIndex = 1;
                for (Map<String, Object> rowData : data) {
                    Row row = sheet.createRow(rowIndex++);
                    int cellIndex = 0;
                    for (Object value : rowData.values()) {
                        row.createCell(cellIndex++).setCellValue(value != null ? value.toString() : "");
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            byte[] bytes = out.toByteArray();

            String base = (fileName == null || fileName.isBlank()) ? "report" : fileName;
            String finalName = base + ".xlsx";

            StreamResource resource = new StreamResource(finalName, () -> new ByteArrayInputStream(bytes));
            resource.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            resource.setCacheTime(0);
            return resource;

        } catch (IOException e) {
            e.printStackTrace();
            this.showError("Failed to export Excel.");
            return null;
        }
    }

	 private void showError(String message) {
		    Notification.show(message, 9000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
	}
	 
	// Example: Add consistent card styling
	 private void applyCardStyling(Card card) {
	     card.getStyle()
	         .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)")
	         .set("transition", "transform 0.2s, box-shadow 0.2s")
	         .set("cursor", "pointer");
	     
	     // Add hover effect
	     card.addAttachListener(e -> {
	         card.getElement().addEventListener("mouseenter", event -> {
	             card.getStyle().set("transform", "translateY(-2px)");
	             card.getStyle().set("box-shadow", "0 4px 8px rgba(0,0,0,0.15)");
	         });
	         card.getElement().addEventListener("mouseleave", event -> {
	             card.getStyle().set("transform", "translateY(0)");
	             card.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");
	         });
	     });
	 }
}
