package org.halocambodia.upload.util;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

public final class ClientDisconnectUtil {
    private ClientDisconnectUtil() {}

    public static boolean isClientDisconnect(Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth++ < 20) {
            if (current instanceof ClientAbortException
                    || current instanceof AsyncRequestNotUsableException
                    || current instanceof ClosedChannelException
                    || current instanceof EOFException) {
                return true;
            }

            if (current instanceof SocketException || current instanceof IOException) {
                String message = current.getMessage();
                if (message != null) {
                    String text = message.toLowerCase(Locale.ROOT);
                    if (text.contains("broken pipe")
                            || text.contains("connection reset")
                            || text.contains("connection aborted")
                            || text.contains("forcibly closed")
                            || text.contains("client abort")) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
