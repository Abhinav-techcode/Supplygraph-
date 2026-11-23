package com.supplygraph.web.error;

import com.supplygraph.application.error.ErrorClassifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Last line of defence for errors raised inside UI event handlers. {@link ErrorClassifier} decides what the failure means: expected ones (bad input,
 * missing item, refused action) become a short message; unexpected ones are logged with a stack trace and a reference the user can quote, and never
 * show internals.
 */
public class UiErrorHandler implements ErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(UiErrorHandler.class);

    @Override
    public void error(ErrorEvent event) {
        UI ui = UI.getCurrent();
        String shown = UiErrors.describe(event.getThrowable(), log);
        if (ui != null) ui.access(() -> Notification.show(shown, 6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR));
    }
}
