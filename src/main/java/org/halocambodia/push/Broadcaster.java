package org.halocambodia.push;

import com.vaadin.flow.component.UI;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class Broadcaster {
    private static final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public interface Listener {
        UI ui();
        Consumer<Object> consumer(); // receives the payload you broadcast
    }

    public static Registration register(UI ui, Consumer<Object> consumer) {
        Listener l = new Listener() {
            @Override public UI ui() { return ui; }
            @Override public Consumer<Object> consumer() { return consumer; }
        };
        listeners.add(l);
        return () -> listeners.remove(l);
    }

    public static void broadcast(Object payload) {
        for (Listener l : listeners) {
            l.ui().access(() -> l.consumer().accept(payload));
        }
    }

    public interface Registration {
        void remove();
    }
}
