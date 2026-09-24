package org.halocambodia.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;

/**
 * Navbar button that switches between HALO light and dark modes.
 *
 * Replace the package declaration with your application's package.
 */
public class ThemeToggleButton extends Button {

    private final Span icon = new Span();
    private ThemeMode.Mode currentMode = ThemeMode.Mode.LIGHT;

    public ThemeToggleButton() {
        addClassName("theme-toggle-button");
        addThemeVariants(ButtonVariant.TERTIARY);

        icon.addClassName("theme-toggle-icon");
        icon.getElement().setAttribute("aria-hidden", "true");
        setIcon(icon);

        updateAppearance();

        UI ui = UI.getCurrentOrThrow();

        ThemeMode.loadSavedMode(ui, mode -> {
            currentMode = mode;
            updateAppearance();
        });

        addClickListener(event -> {
            currentMode =
                    currentMode == ThemeMode.Mode.DARK
                            ? ThemeMode.Mode.LIGHT
                            : ThemeMode.Mode.DARK;

            ThemeMode.apply(ui, currentMode, true);
            updateAppearance();
        });
    }

    private void updateAppearance() {
        boolean darkMode = currentMode == ThemeMode.Mode.DARK;

        /*
         * Show the action that will happen when the user clicks:
         * sun = change to light, moon = change to dark.
         */
        icon.setText(darkMode ? "☀" : "☾");

        String label =
                darkMode
                        ? "Use light mode"
                        : "Use dark mode";

        setAriaLabel(label);
        getElement().setAttribute("title", label);

        getElement().getClassList().set(
                "dark-mode-active",
                darkMode
        );
    }
}
