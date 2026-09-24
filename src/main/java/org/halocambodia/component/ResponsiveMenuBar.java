package org.halocambodia.component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

@CssImport("./styles/menu-styles.css")
public class ResponsiveMenuBar extends MenuBar {

    private final Map<String, Runnable> actions = new LinkedHashMap<>();
    private final Map<String, Supplier<Component>> overflowComponents = new LinkedHashMap<>();
    private final Map<String, String> overflowCaptions = new LinkedHashMap<>();

    private final MenuItem dots;
    private final SubMenu dotsMenu;

    private boolean overflowObserverInitialized = false;
    private int iconOnlySeq = 0;

    public ResponsiveMenuBar() {
        addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        addClassName("responsive-menubar");

        getStyle()
                .set("overflow", "hidden")
                .set("min-width", "0")
                .set("flex-shrink", "1");

        dots = addItem(new Icon(VaadinIcon.ELLIPSIS_DOTS_V));
        dots.getElement().setAttribute("data-overflow-dots", "true");
        dotsMenu = dots.getSubMenu();
        dots.setVisible(false);
    }

    public MenuItem addAction(VaadinIcon icon, String label, Runnable action) {
        return addAction(icon, label, action, null);
    }

    /**
     * If label is null/blank -> icon-only item (recommended to pass tooltip).
     * tooltip can be: "Add new | បន្ថែមថ្មី"
     */
    public MenuItem addAction(VaadinIcon icon, String label, Runnable action, String tooltip) {
        Objects.requireNonNull(icon, "icon must not be null");
        Objects.requireNonNull(action, "action must not be null");

        boolean iconOnly = (label == null || label.isBlank());

        // Key used by overflow detector and server maps
        String key = iconOnly
                ? "__ICON__" + icon.name() + "__" + (++iconOnlySeq)
                : label.trim();

        Icon i = icon.create();
        if (!iconOnly) {
            i.getStyle().set("margin-right", "6px");
        }

        HorizontalLayout content = new HorizontalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        content.setAlignItems(Alignment.CENTER);

        content.add(i);

        if (!iconOnly) {
            content.add(new Span(label.trim()));
        }

        // IMPORTANT: JS reads this to know which buttons are hidden
        content.getElement().setAttribute("data-overflow-label", key);

        MenuItem item = addItem(content);

        if (tooltip != null && !tooltip.isBlank()) {
            setTooltipTextCompat(item, tooltip);
        }

        item.addClickListener(e -> action.run());

        actions.put(key, action);

        // Caption shown in the "..." overflow menu
        String caption = iconOnly ? captionFromTooltipOrIcon(tooltip, icon) : label.trim();
        overflowCaptions.put(key, caption);

        return item;
    }

    public MenuItem addComponentItem(Component component, String overflowKey) {
        return addComponentItem(component, overflowKey, null);
    }

    /**
     * Use overflowKey to match registerOverflowComponent(overflowKey, supplier).
     */
    public MenuItem addComponentItem(Component component, String overflowKey, String tooltip) {
        Objects.requireNonNull(component, "component must not be null");
        if (overflowKey == null || overflowKey.isBlank()) {
            throw new IllegalArgumentException("overflowKey must not be blank");
        }
        String key = overflowKey.trim();

        component.getElement().setAttribute("data-overflow-label", key);

        MenuItem item = addItem(component);
        if (tooltip != null && !tooltip.isBlank()) {
            setTooltipTextCompat(item, tooltip);
        }
        return item;
    }

    public void registerAction(String overflowKey, Runnable action) {
        if (overflowKey == null || overflowKey.isBlank() || action == null) return;
        String key = overflowKey.trim();
        actions.put(key, action);
        overflowCaptions.putIfAbsent(key, key);
    }

    /**
     * When a component item is hidden by overflow, we add a NEW component in dots menu using this supplier.
     * IMPORTANT: overflowKey must match the data-overflow-label you put on the visible component item.
     */
    public void registerOverflowComponent(String overflowKey, Supplier<Component> componentSupplier) {
        if (overflowKey == null || overflowKey.isBlank() || componentSupplier == null) return;
        overflowComponents.put(overflowKey.trim(), componentSupplier);
    }

    @ClientCallable
    private void updateOverflow(JsonNode hiddenLabels) {
        dotsMenu.removeAll();

        if (hiddenLabels != null && hiddenLabels.isArray() && hiddenLabels.size() > 0) {
            for (JsonNode node : hiddenLabels) {
                String key = node.asText();

                Supplier<Component> supplier = overflowComponents.get(key);
                if (supplier != null) {
                    Component c = supplier.get();
                    MenuItem mi = dotsMenu.addItem(c);
                    mi.setKeepOpen(true);
                    continue;
                }

                Runnable action = actions.get(key);
                String caption = overflowCaptions.getOrDefault(key, key);

                if (action != null) {
                    dotsMenu.addItem(caption, e -> action.run());
                } else {
                    dotsMenu.addItem(caption);
                }
            }
            dots.setVisible(true);
        } else {
            dots.setVisible(false);
        }
    }

    public void initOverflowObserver() {
        if (overflowObserverInitialized) return;
        overflowObserverInitialized = true;

        getElement().executeJs("""
            const menuBar = this;

            const debounce = (fn, wait) => {
              let t;
              return (...args) => {
                clearTimeout(t);
                t = setTimeout(() => fn(...args), wait);
              };
            };

            const getLabel = (btn) => {
              const el = btn.querySelector('[data-overflow-label]');
              if (el) {
                const v = el.getAttribute('data-overflow-label');
                if (v && v.trim()) return v.trim();
              }
              return (btn.textContent || '').trim();
            };

            const measure = () => {
              if (!menuBar.shadowRoot) return;

              const buttons = menuBar.shadowRoot.querySelectorAll('vaadin-menu-bar-button');
              const items = menuBar.querySelectorAll('vaadin-menu-bar-item');
              if (!buttons.length || !items.length) return;

              let dotsIndex = -1;
              items.forEach((it, idx) => {
                if (it.hasAttribute('data-overflow-dots')) dotsIndex = idx;
              });
              if (dotsIndex < 0 || dotsIndex >= buttons.length) return;

              const dotsBtn = buttons[dotsIndex];

              // reset
              for (let i = 0; i < buttons.length; i++) {
                if (i === dotsIndex) continue;
                buttons[i].style.display = '';
                buttons[i].style.visibility = 'visible';
              }

              dotsBtn.style.display = 'none';

              let totalWidth = 0;
              for (let i = 0; i < buttons.length; i++) {
                if (i === dotsIndex) continue;
                totalWidth += buttons[i].offsetWidth;
              }

              const menuBarWidth = menuBar.offsetWidth;
              const padding = 16;
              const availableWidth = menuBarWidth - padding;

              if (totalWidth <= availableWidth) {
                menuBar.$server.updateOverflow([]);
                return;
              }

              dotsBtn.style.display = '';
              dotsBtn.style.visibility = 'visible';

              const dotsWidth = dotsBtn.offsetWidth;
              const availableWithDots = menuBarWidth - dotsWidth - padding;

              let currentWidth = 0;
              const hidden = [];

              for (let i = buttons.length - 1; i >= 0; i--) {
                if (i === dotsIndex) continue;

                const btn = buttons[i];
                const w = btn.offsetWidth;

                if (currentWidth + w > availableWithDots) {
                  btn.style.display = 'none';
                  const label = getLabel(btn);
                  if (label) hidden.unshift(label);
                } else {
                  currentWidth += w;
                }
              }

              menuBar.$server.updateOverflow(hidden);
            };

            const debounced = debounce(measure, 50);

            const ro = new ResizeObserver(debounced);
            ro.observe(menuBar);
            if (menuBar.parentElement) ro.observe(menuBar.parentElement);

            setTimeout(measure, 150);

            menuBar.__rm_ro = ro;
        """);
    }

    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        getElement().executeJs("""
            if (this.__rm_ro) this.__rm_ro.disconnect();
            this.__rm_ro = null;
        """);
        super.onDetach(detachEvent);
    }

    // ---------------- helpers ----------------

    private void setTooltipTextCompat(MenuItem item, String tooltip) {
        try {
            // Preferred (Vaadin MenuBar API)
            MenuBar.class.getMethod("setTooltipText", MenuItem.class, String.class)
                    .invoke(this, item, tooltip);
        } catch (Exception ignore) {
            // Fallback to native browser tooltip
            item.getElement().setProperty("title", tooltip);
        }
    }

    private static String captionFromTooltipOrIcon(String tooltip, VaadinIcon icon) {
        if (tooltip != null && !tooltip.isBlank()) {
            // if "English | Khmer" -> show only English in overflow menu
            String[] parts = tooltip.split("\\|", 2);
            String p = parts[0].trim();
            return p.isEmpty() ? tooltip.trim() : p;
        }
        return humanize(icon.name());
    }

    private static String humanize(String iconName) {
        String s = iconName.toLowerCase().replace('_', ' ');
        String[] parts = s.split("\\s+");
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return b.toString().trim();
    }
}
