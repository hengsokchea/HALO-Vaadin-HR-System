package org.halocambodia.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ColorScheme;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Applies and stores the Cam-HRIS light/dark preference.
 *
 * Replace the package declaration with your application's package.
 */
public final class ThemeMode {

    private static final String STORAGE_KEY = "camhris-color-scheme";

    private ThemeMode() {
        // Utility class
    }

    public enum Mode {
        LIGHT,
        DARK
    }

    /**
     * Loads the saved preference. When nothing has been saved yet, the
     * browser/operating-system preference is used.
     */
    public static void loadSavedMode(UI ui, Consumer<Mode> afterApply) {
        ui.getPage()
                .executeJs("""
                        const saved = window.localStorage.getItem($0);

                        if (saved === 'light' || saved === 'dark') {
                            return saved;
                        }

                        return window.matchMedia
                                && window.matchMedia(
                                        '(prefers-color-scheme: dark)'
                                   ).matches
                                ? 'dark'
                                : 'light';
                        """, STORAGE_KEY)
                .then(String.class, value -> {
                    Mode mode = parse(value);
                    apply(ui, mode, false);

                    if (afterApply != null) {
                        afterApply.accept(mode);
                    }
                });
    }

    /**
     * Applies a mode and optionally saves it in the browser.
     */
    public static void apply(UI ui, Mode mode, boolean savePreference) {
        ColorScheme.Value colorScheme =
                mode == Mode.DARK
                        ? ColorScheme.Value.DARK
                        : ColorScheme.Value.LIGHT;

        ui.getPage().setColorScheme(colorScheme);

        if (savePreference) {
            ui.getPage().executeJs(
                    "window.localStorage.setItem($0, $1);",
                    STORAGE_KEY,
                    mode.name().toLowerCase(Locale.ROOT)
            );
        }
    }

    private static Mode parse(String value) {
        return "dark".equalsIgnoreCase(value)
                ? Mode.DARK
                : Mode.LIGHT;
    }
}
