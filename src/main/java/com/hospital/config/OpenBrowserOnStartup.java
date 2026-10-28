package com.hospital.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// When the server is ready, open the website in the
// default browser (Chrome, Safari...).
// Only on your own computer: set OPEN_BROWSER=true in .env
@Component
@ConditionalOnProperty(name = "app.open-browser", havingValue = "true")
public class OpenBrowserOnStartup {

    private static final Logger log =
            LoggerFactory.getLogger(OpenBrowserOnStartup.class);

    // DevTools restarts the app when code changes;
    // a JVM-wide flag stops a new tab opening each time
    private static final String OPENED_FLAG = "hospital.browser.opened";

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser(ApplicationReadyEvent event) {
        if (System.getProperty(OPENED_FLAG) != null) {
            return;
        }
        System.setProperty(OPENED_FLAG, "true");

        Environment env = event.getApplicationContext().getEnvironment();
        String url = "http://localhost:"
                + env.getProperty("local.server.port", "8080");

        String os = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT);
        String[] command;
        if (os.contains("mac")) {
            command = new String[] {"open", url};
        } else if (os.contains("win")) {
            command = new String[] {"rundll32",
                    "url.dll,FileProtocolHandler", url};
        } else {
            command = new String[] {"xdg-open", url};
        }

        try {
            new ProcessBuilder(command).start();
            log.info("Opened {} in your browser", url);
        } catch (Exception ex) {
            log.info("Server ready — open {} in your browser", url);
        }
    }
}
