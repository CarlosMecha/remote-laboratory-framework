/*
 * RLF_ClientApp.java
 */

package org.rlf.client;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import org.rlf.client.view.ClientView;

/**
 * The main class of the application.
 */
public class RLF_ClientApp extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        ClientContext c = ClientContext.Instance();
        c.setMainView(new ClientView(this, c));
        show(c.getMainView());
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of RLF_ClientApp
     */
    public static RLF_ClientApp getApplication() {
        return Application.getInstance(RLF_ClientApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(RLF_ClientApp.class, args);
    }
}
