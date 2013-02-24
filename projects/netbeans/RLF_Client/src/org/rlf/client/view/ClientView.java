/*
 * ClientView.java
 */
package org.rlf.client.view;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import org.rlf.client.view.dialog.ErrorBox;
import org.rlf.client.view.dialog.LogoutDialog;
import org.rlf.client.view.dialog.WorkingBox;
import org.rlf.client.view.dialog.LoginDialog;
import org.rlf.client.view.dialog.AboutBox;
import org.rlf.client.RLF_ClientApp;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import java.util.EventObject;
import java.util.LinkedList;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import org.jdesktop.application.Application.ExitListener;
import org.rlf.client.ClientContext;
import org.rlf.client.ClientContext.ClientStatus;
import org.rlf.client.NotificationManager;
import org.rlf.client.data.Tool;
import org.rlf.client.provider.ProviderController;
import org.rlf.client.provider.ProviderException;
import org.rlf.client.view.dialog.TimeBox;

/**
 * The application's main frame.
 */
public class ClientView extends FrameView {

    // Atributos:
    /** Contexto de la aplicación. */
    private ClientContext context;

    // Constructor:
    public ClientView(SingleFrameApplication app, ClientContext context) {
        super(app);

        this.context = context;

        initComponents();

        // Botón de cerrar.
        this.getApplication().addExitListener(new ExitListener() {

            public boolean canExit(EventObject arg0) {
                return exit();
            }

            public void willExit(EventObject arg0) {
            }
        });

        // Icono.
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(ClientView.class);
        String filename = resourceMap.getResourcesDir() + "icon.png";
        URL url = resourceMap.getClassLoader().getResource(filename);
        Image icon = Toolkit.getDefaultToolkit().createImage(url);
        this.getFrame().setIconImage(icon);

    }

    // Métodos varios:
    /**
     * Limpia todas las tablas de las herramientas.
     */
    private void clearToolsPanel() {
        this.toolsPanel.removeAll();
        if (context.getTools() == null) {
            return;
        }
        for (Tool tool : context.getTools().values()) {
            tool.component(null);
        }

    }

    /**
     * Lanza la ventana de error. Es bloqueante.
     * @param msg Mensaje de error.
     */
    protected void errorWindow(String msg) {
        JFrame mainFrame = RLF_ClientApp.getApplication().getMainFrame();
        ErrorBox w = new ErrorBox(mainFrame, true, msg);
        w.setLocationRelativeTo(mainFrame);
        RLF_ClientApp.getApplication().show(w);
    }

    /**
     * Abre la ventana de trabajo.
     */
    protected void openWorkingBox() {
        JFrame mainFrame = RLF_ClientApp.getApplication().getMainFrame();
        this.workingBox = new WorkingBox(mainFrame, true);
        this.workingBox.setLocationRelativeTo(mainFrame);

        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {

                workingBox.setVisible(true);
                RLF_ClientApp.getApplication().show(workingBox);
            }
        });
    }

    /**
     * Completa la visualización de las pestañas y actualiza el estado.
     */
    private void build() {
        clearToolsPanel();

        // 1. Petición al proveedor.
        ProviderController p = new ProviderController(this.context);
        try {
            if (!p.toolsRequest()) {
                workingBox.close();
                errorWindow("Unknow problem.");
                return;
            }
        } catch (ProviderException ex) {
            workingBox.close();
            errorWindow("Severe error in the connection. The program will close.");
            // Salida completa.
            System.exit(-1);
            return;
        }

        // 2. Comprobación de estado.
        try {
            if (!p.updateRequest()) {
                workingBox.close();
                errorWindow("Unknow problem.");
                return;
            }
        } catch (ProviderException ex) {
            workingBox.close();
            errorWindow("Severe error in the connection. The program will close.");
            // Salida completa.
            System.exit(-1);
            return;
        }

        // 3. Insercción de las pestañas.
        for (Tool tool : this.context.getTools().values()) {
            ToolTab tab = new ToolTab(tool, this.context);
            tool.component(tab);
            tab.changeStatus();
            this.toolsPanel.add(tab.getName(), tab);
        }
        updateSelectedTools();
        workingBox.close();
    }

    /**
     * Cierra por completo la aplicación. Fuerza un deslogueo pero no da error
     * sin no puede realizarlo.
     */
    private boolean exit() {
        if (this.context.status() == ClientStatus.NOTLOGGED) {
            // Salida completa.
            return true;
        }

        if (logoutDialog == null) {
            JFrame mainFrame = RLF_ClientApp.getApplication().getMainFrame();
            logoutDialog = new LogoutDialog(mainFrame, true);
            logoutDialog.setLocationRelativeTo(mainFrame);
        }
        RLF_ClientApp.getApplication().show(logoutDialog);
        if (logoutDialog.getReturnStatus() != LogoutDialog.RET_OK) {
            return false;
        }

        ProviderController p = new ProviderController(this.context);
        try {
            p.logoutRequest();
        } catch (ProviderException ex) {
        }

        NotificationManager nm = context.notificationManager();
        if (nm != null) {
            nm.stopManager();
            try {
                nm.join(1000);
            } catch (InterruptedException ex) {
            }
            for (Tool tool : this.context.getTools().values()) {
                if (tool.running() != null) {
                    tool.running().forceStop();
                }
            }
        }

        return true;
    }

    /**
     * Obtiene la lista de las herramientas seleccionadas y lo muestra en el indicador.
     */
    protected void updateSelectedTools() {
        int selected = 0;
        String description = "<html>";
        for (Tool tool : context.getTools().values()) {
            if (tool.component().isSelected()) {
                selected++;
                description += tool.getName() + " (" + tool.getId() + ")<br>";
            }
        }
        description += "</html>";

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(ClientView.class);
        statusLabel.setIcon(resourceMap.getIcon("selected.icon"));

        if (selected == 0) {
            statusLabel.setText("No tools selected");
            statusLabel.setToolTipText(null);
        } else if (selected == 1) {
            statusLabel.setText("1 tool selected");
            statusLabel.setToolTipText(description);
        } else {
            statusLabel.setText(selected + " tools selected");
            statusLabel.setToolTipText(description);
        }

    }

    /**
     * Ocurre cuando todas las herramientas que se están usando se han desconectado
     * por varios motivos.
     */
    public void notMoreTools() {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                timer.cancel();
                timer.purge();
                context.status(ClientStatus.LOGGED);
                updateMenuItem.setEnabled(true);
                refreshLabel.setEnabled(true);
                rebuildMenuItem.setEnabled(true);
                rebuildLabel.setEnabled(true);
                takeToolsMenuItem.setEnabled(true);
                takeToolsLabel.setEnabled(true);
                try {
                    context.notificationManager().join();
                } catch (InterruptedException ex) {
                }
                // Lanzar aviso y cerrar ventanas de ejecución.
                errorWindow("All tools have been disconnected.");
            }
        });
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                updateAction(null);
                updateSelectedTools();
            }
        });
    }

    /**
     * Cambia el estado a logeado cuando se produce un timeout enviado por los
     * laboratorios.
     */
    public void timeout() {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                timer.cancel();
                timer.purge();
                context.status(ClientStatus.LOGGED);
                updateMenuItem.setEnabled(true);
                refreshLabel.setEnabled(true);
                rebuildMenuItem.setEnabled(true);
                rebuildLabel.setEnabled(true);
                takeToolsMenuItem.setEnabled(true);
                takeToolsLabel.setEnabled(true);
                try {
                    context.notificationManager().join();
                } catch (InterruptedException ex) {
                }
                // Lanzar aviso y cerrar ventanas de ejecución.
                JFrame mainFrame = RLF_ClientApp.getApplication().getMainFrame();
                if (timeBox != null) {
                    timeBox.dispose();
                }
                timeBox = new TimeBox(mainFrame);
                timeBox.setLocationRelativeTo(mainFrame);


                RLF_ClientApp.getApplication().show(timeBox);
            }
        });
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                updateAction(null);
                updateSelectedTools();
            }
        });

    }

    /**
     * Indica a la interfaz que se le ha acabado el tiempo al usuario.
     */
    private void timeoutWarning() {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                // Lanzar aviso y cerrar ventanas de ejecución.
                JFrame mainFrame = RLF_ClientApp.getApplication().getMainFrame();
                if (timeBox != null) {
                    timeBox.dispose();
                }
                timeBox = new TimeBox(mainFrame, true);
                timeBox.setLocationRelativeTo(mainFrame);


                RLF_ClientApp.getApplication().show(timeBox);
                statusLabel.setText("Less than 1 min...");
            }
        });

    }

    /**
     * Actualiza el contador de la interfaz.
     * @param minutes Minutos.
     */
    private void updateTime(int minutes) {
        final int min = minutes;
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {

                if (min == 5) {
                    // Lanzar aviso.
                    JFrame mainFrame = RLF_ClientApp.getApplication().getMainFrame();
                    if (timeBox != null) {
                        timeBox.dispose();
                    }
                    timeBox = new TimeBox(mainFrame, false);
                    timeBox.setLocationRelativeTo(mainFrame);
                    RLF_ClientApp.getApplication().show(timeBox);
                } else if (min < 5) {
                    org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(ClientView.class);
                    statusLabel.setIcon(resourceMap.getIcon("badTime.icon")); // NOI18N
                }
                statusLabel.setText(min + " min remaining...");
            }
        });

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        toolsBar = new javax.swing.JPanel();
        loginLabel = new javax.swing.JLabel();
        barSeparator1 = new javax.swing.JSeparator();
        refreshLabel = new javax.swing.JLabel();
        rebuildLabel = new javax.swing.JLabel();
        barSeparator2 = new javax.swing.JSeparator();
        takeToolsLabel = new javax.swing.JLabel();
        statusLabel = new javax.swing.JLabel();
        toolsPanel = new javax.swing.JTabbedPane();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        loginMenuItem = new javax.swing.JMenuItem();
        logoutMenuItem = new javax.swing.JMenuItem();
        menuSeparator1 = new javax.swing.JPopupMenu.Separator();
        updateMenuItem = new javax.swing.JMenuItem();
        menuSeparator2 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        toolsMenu = new javax.swing.JMenu();
        rebuildMenuItem = new javax.swing.JMenuItem();
        takeToolsMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();

        mainPanel.setMinimumSize(new java.awt.Dimension(1047, 546));
        mainPanel.setName("mainPanel"); // NOI18N

        toolsBar.setName("toolsBar"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(ClientView.class);
        loginLabel.setFont(resourceMap.getFont("loginLabel.font")); // NOI18N
        loginLabel.setIcon(resourceMap.getIcon("loginLabel.icon")); // NOI18N
        loginLabel.setText(resourceMap.getString("loginLabel.text")); // NOI18N
        loginLabel.setToolTipText(resourceMap.getString("loginLabel.toolTipText")); // NOI18N
        loginLabel.setName("loginLabel"); // NOI18N
        loginLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                loginLogoutAction(evt);
            }
        });

        barSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        barSeparator1.setName("barSeparator1"); // NOI18N

        refreshLabel.setFont(resourceMap.getFont("refreshLabel.font")); // NOI18N
        refreshLabel.setIcon(resourceMap.getIcon("refreshLabel.icon")); // NOI18N
        refreshLabel.setText(resourceMap.getString("refreshLabel.text")); // NOI18N
        refreshLabel.setToolTipText(resourceMap.getString("refreshLabel.toolTipText")); // NOI18N
        refreshLabel.setEnabled(false);
        refreshLabel.setName("refreshLabel"); // NOI18N
        refreshLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                updateButtonAction(evt);
            }
        });

        rebuildLabel.setFont(resourceMap.getFont("rebuildLabel.font")); // NOI18N
        rebuildLabel.setIcon(resourceMap.getIcon("rebuildLabel.icon")); // NOI18N
        rebuildLabel.setText(resourceMap.getString("rebuildLabel.text")); // NOI18N
        rebuildLabel.setToolTipText(resourceMap.getString("rebuildLabel.toolTipText")); // NOI18N
        rebuildLabel.setEnabled(false);
        rebuildLabel.setName("rebuildLabel"); // NOI18N
        rebuildLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                buildButtonAction(evt);
            }
        });

        barSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
        barSeparator2.setName("barSeparator2"); // NOI18N

        takeToolsLabel.setFont(resourceMap.getFont("takeToolsLabel.font")); // NOI18N
        takeToolsLabel.setIcon(resourceMap.getIcon("takeToolsLabel.icon")); // NOI18N
        takeToolsLabel.setText(resourceMap.getString("takeToolsLabel.text")); // NOI18N
        takeToolsLabel.setToolTipText(resourceMap.getString("takeToolsLabel.toolTipText")); // NOI18N
        takeToolsLabel.setEnabled(false);
        takeToolsLabel.setName("takeToolsLabel"); // NOI18N
        takeToolsLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                takeToolsButtonAction(evt);
            }
        });

        statusLabel.setFont(resourceMap.getFont("statusLabel.font")); // NOI18N
        statusLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusLabel.setText(resourceMap.getString("statusLabel.text")); // NOI18N
        statusLabel.setIconTextGap(6);
        statusLabel.setName("statusLabel"); // NOI18N

        javax.swing.GroupLayout toolsBarLayout = new javax.swing.GroupLayout(toolsBar);
        toolsBar.setLayout(toolsBarLayout);
        toolsBarLayout.setHorizontalGroup(
            toolsBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(toolsBarLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(loginLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(barSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(refreshLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rebuildLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(barSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(takeToolsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 391, Short.MAX_VALUE)
                .addComponent(statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 186, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        toolsBarLayout.setVerticalGroup(
            toolsBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, toolsBarLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(toolsBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, toolsBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(takeToolsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 56, Short.MAX_VALUE)
                        .addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE))
                    .addComponent(barSeparator2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
                    .addComponent(refreshLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
                    .addComponent(rebuildLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
                    .addComponent(barSeparator1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
                    .addComponent(loginLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE))
                .addContainerGap())
        );

        toolsPanel.setName("toolsPanel"); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(toolsBar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(toolsPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1023, Short.MAX_VALUE))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(toolsBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(toolsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 447, Short.MAX_VALUE)
                .addContainerGap())
        );

        menuBar.setFont(resourceMap.getFont("menuBar.font")); // NOI18N
        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setFont(resourceMap.getFont("fileMenu.font")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        loginMenuItem.setText(resourceMap.getString("loginMenuItem.text")); // NOI18N
        loginMenuItem.setName("loginMenuItem"); // NOI18N
        loginMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginAction(evt);
            }
        });
        fileMenu.add(loginMenuItem);

        logoutMenuItem.setText(resourceMap.getString("logoutMenuItem.text")); // NOI18N
        logoutMenuItem.setEnabled(false);
        logoutMenuItem.setName("logoutMenuItem"); // NOI18N
        logoutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutAction(evt);
            }
        });
        fileMenu.add(logoutMenuItem);

        menuSeparator1.setName("menuSeparator1"); // NOI18N
        fileMenu.add(menuSeparator1);

        updateMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        updateMenuItem.setText(resourceMap.getString("updateMenuItem.text")); // NOI18N
        updateMenuItem.setEnabled(false);
        updateMenuItem.setName("updateMenuItem"); // NOI18N
        updateMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateAction(evt);
            }
        });
        fileMenu.add(updateMenuItem);

        menuSeparator2.setName("menuSeparator2"); // NOI18N
        fileMenu.add(menuSeparator2);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getActionMap(ClientView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitAction(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        toolsMenu.setText(resourceMap.getString("toolsMenu.text")); // NOI18N
        toolsMenu.setFont(resourceMap.getFont("toolsMenu.font")); // NOI18N
        toolsMenu.setName("toolsMenu"); // NOI18N

        rebuildMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, java.awt.event.InputEvent.SHIFT_MASK));
        rebuildMenuItem.setText(resourceMap.getString("rebuildMenuItem.text")); // NOI18N
        rebuildMenuItem.setEnabled(false);
        rebuildMenuItem.setName("rebuildMenuItem"); // NOI18N
        rebuildMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buildAction(evt);
            }
        });
        toolsMenu.add(rebuildMenuItem);

        takeToolsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_MASK));
        takeToolsMenuItem.setText(resourceMap.getString("takeToolsMenuItem.text")); // NOI18N
        takeToolsMenuItem.setEnabled(false);
        takeToolsMenuItem.setName("takeToolsMenuItem"); // NOI18N
        takeToolsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                takeToolsAction(evt);
            }
        });
        toolsMenu.add(takeToolsMenuItem);

        menuBar.add(toolsMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setFont(resourceMap.getFont("helpMenu.font")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setText(resourceMap.getString("aboutMenuItem.text")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAboutBox(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setComponent(mainPanel);
        setMenuBar(menuBar);
    }// </editor-fold>//GEN-END:initComponents

private void showAboutBox(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showAboutBox
    if (aboutBox == null) {
        JFrame mainFrame = RLF_ClientApp.getApplication().getMainFrame();
        aboutBox = new AboutBox(mainFrame);
        aboutBox.setLocationRelativeTo(mainFrame);
    }
    RLF_ClientApp.getApplication().show(aboutBox);
}//GEN-LAST:event_showAboutBox

private void logoutAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoutAction

    if (this.context.status() == ClientStatus.NOTLOGGED) {
        return;
    }

    if (logoutDialog == null) {
        JFrame mainFrame = RLF_ClientApp.getApplication().getMainFrame();
        logoutDialog = new LogoutDialog(mainFrame, true);
        logoutDialog.setLocationRelativeTo(mainFrame);
    }
    RLF_ClientApp.getApplication().show(logoutDialog);
    if (logoutDialog.getReturnStatus() != LogoutDialog.RET_OK) {
        return;
    }

    openWorkingBox();

    final SwingWorker worker = new SwingWorker() {

        @Override
        protected Object doInBackground() throws Exception {

            NotificationManager nm = context.notificationManager();
            if (nm != null) {
                timer.cancel();
                timer.purge();
                nm.stopManager();
                try {
                    nm.join();
                } catch (InterruptedException ex) {
                }
            }

            ProviderController p = new ProviderController(context);
            try {
                if (!p.logoutRequest()) {
                    workingBox.close();
                    System.exit(-1);
                    return null;
                }
            } catch (ProviderException ex) {
                workingBox.close();
                errorWindow("Severe error in the connection. The program will close.");
                // Salida completa.
                System.exit(-1);
                return null;
            }

            clearToolsPanel();
            loginLabel.setText("Login");
            loginLabel.setToolTipText("Login");
            loginMenuItem.setEnabled(true);
            logoutMenuItem.setEnabled(false);
            updateMenuItem.setEnabled(false);
            refreshLabel.setEnabled(false);
            rebuildMenuItem.setEnabled(false);
            rebuildLabel.setEnabled(false);
            takeToolsMenuItem.setEnabled(false);
            takeToolsLabel.setEnabled(false);
            statusLabel.setText(null);
            statusLabel.setIcon(null);
            context.status(ClientStatus.NOTLOGGED);
            
            workingBox.close();
            
            return null;
        }

    };

    worker.execute();

}//GEN-LAST:event_logoutAction

private void loginAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginAction

    if (this.context.status() != ClientStatus.NOTLOGGED) {
        return;
    }

    if (loginDialog == null) {
        JFrame mainFrame = RLF_ClientApp.getApplication().getMainFrame();
        loginDialog = new LoginDialog(mainFrame, true);
        loginDialog.setLocationRelativeTo(mainFrame);
    }
    RLF_ClientApp.getApplication().show(loginDialog);
    if (loginDialog.getReturnStatus() != LoginDialog.RET_OK) {
        return;
    }

    openWorkingBox();
    final SwingWorker worker = new SwingWorker() {

        @Override
        protected Object doInBackground() throws Exception {
            ProviderController p = new ProviderController(context);

            try {
                if (!p.loginRequest(loginDialog.getUser(), loginDialog.getPass())) {
                    workingBox.close();
                    errorWindow("Invalid user or password. Try again.");
                    return null;
                }
            } catch (ProviderException ex) {
                workingBox.close();
                errorWindow("Severe error in the connection. The program will close.");
                // Salida completa.
                System.exit(-1);
                return null;
            }

            build();

            loginLabel.setText("Logout");
            loginLabel.setToolTipText("Logout");
            loginMenuItem.setEnabled(false);
            logoutMenuItem.setEnabled(true);
            updateMenuItem.setEnabled(true);
            refreshLabel.setEnabled(true);
            rebuildMenuItem.setEnabled(true);
            rebuildLabel.setEnabled(true);
            takeToolsMenuItem.setEnabled(true);
            takeToolsLabel.setEnabled(true);
            context.status(ClientStatus.LOGGED);

            return null;
        }

    };

    worker.execute();

}//GEN-LAST:event_loginAction

private void exitAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitAction
    if (exit()) {
        System.exit(0);
    }
    return;

}//GEN-LAST:event_exitAction

private void buildAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buildAction
    if (this.context.status() != ClientStatus.LOGGED) {
        return;
    }

    openWorkingBox();

    final SwingWorker worker = new SwingWorker() {

        @Override
        protected Object doInBackground() throws Exception {
            build();
            return null;
        }
    };
    worker.execute();

}//GEN-LAST:event_buildAction

private void updateAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateAction
    if (this.context.status() != ClientStatus.LOGGED) {
        return;
    }

    openWorkingBox();

    final SwingWorker worker = new SwingWorker() {

        @Override
        protected Object doInBackground() throws Exception {
            // 1. Petición al proveedor.
            ProviderController p = new ProviderController(context);

            // 2. Comprobación de estado.
            try {
                if (!p.updateRequest()) {
                    workingBox.close();
                    errorWindow("Unknow problem.");
                    return null;
                }
            } catch (ProviderException ex) {
                workingBox.close();
                errorWindow("Severe error in the connection. The program will close.");
                // Salida completa.
                System.exit(-1);
                return null;
            }

            // 3. Insercción de las pestañas.
            for (Tool tool : context.getTools().values()) {
                tool.component().changeStatus();
            }

            updateSelectedTools();
            workingBox.close();
            return null;
        }
    };
    worker.execute();

}//GEN-LAST:event_updateAction

private void takeToolsAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_takeToolsAction

    if (this.context.status() != ClientStatus.LOGGED) {
        return;
    }

    // 1. Obtención de las herramientas seleccionadas.
    final LinkedList<String> ids = new LinkedList<String>();
    for (Tool tool : context.getTools().values()) {
        if (tool.component().isSelected()) {
            ids.add(tool.getId());
        }
    }

    if (ids.size() == 0) {
        return;
    }

    openWorkingBox();

    final SwingWorker worker = new SwingWorker() {

        @Override
        protected Object doInBackground() throws Exception {

            // 2. Petición al proveedor.
            ProviderController p = new ProviderController(context);
            try {
                if (!p.takeRequest(ids)) {
                    workingBox.close();
                    errorWindow("Some of the tools selected are no longer available, update the status and try again.");
                    return null;
                }
            } catch (ProviderException ex) {
                workingBox.close();
                errorWindow("Severe error in the connection. The program will close.");
                // Salida completa.
                System.exit(-1);
                return null;
            }

            // 3. Cronómetro.
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {

                    int minutes = context.getUseTime();
                    timer = new java.util.Timer();
                    org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(ClientView.class);
                    statusLabel.setIcon(resourceMap.getIcon("goodTime.icon")); // NOI18N
                    statusLabel.setText(minutes + " min remaining...");
                    timer.scheduleAtFixedRate(new UserTimerTask(minutes, context), 0, 60 * 1000);
                }
            });


            // 4. Cambios en la interfaz
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    loginMenuItem.setEnabled(false);
                    logoutMenuItem.setEnabled(true);
                    updateMenuItem.setEnabled(false);
                    refreshLabel.setEnabled(false);
                    rebuildMenuItem.setEnabled(false);
                    rebuildLabel.setEnabled(false);
                    takeToolsMenuItem.setEnabled(false);
                    takeToolsLabel.setEnabled(false);
                }
            });
            workingBox.close();
            context.status(ClientStatus.TAKED);
            for (Tool tool : context.getTools().values()) {
                tool.component().changeStatus();
            }

            return null;
        }
    };
    worker.execute();


}//GEN-LAST:event_takeToolsAction

private void loginLogoutAction(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_loginLogoutAction

    if (this.context.status() == ClientStatus.NOTLOGGED) {
        loginAction(null);
    } else {
        logoutAction(null);
    }

}//GEN-LAST:event_loginLogoutAction

private void updateButtonAction(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_updateButtonAction
    if (!refreshLabel.isEnabled()) {
        return;
    }
    updateAction(null);
}//GEN-LAST:event_updateButtonAction

private void buildButtonAction(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_buildButtonAction
    if (!rebuildLabel.isEnabled()) {
        return;
    }
    buildAction(null);
}//GEN-LAST:event_buildButtonAction

private void takeToolsButtonAction(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_takeToolsButtonAction
    if (!takeToolsLabel.isEnabled()) {
        return;
    }
    takeToolsAction(null);
}//GEN-LAST:event_takeToolsButtonAction
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JSeparator barSeparator1;
    private javax.swing.JSeparator barSeparator2;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JLabel loginLabel;
    private javax.swing.JMenuItem loginMenuItem;
    private javax.swing.JMenuItem logoutMenuItem;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JPopupMenu.Separator menuSeparator1;
    private javax.swing.JPopupMenu.Separator menuSeparator2;
    private javax.swing.JLabel rebuildLabel;
    private javax.swing.JMenuItem rebuildMenuItem;
    private javax.swing.JLabel refreshLabel;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JLabel takeToolsLabel;
    private javax.swing.JMenuItem takeToolsMenuItem;
    private javax.swing.JPanel toolsBar;
    private javax.swing.JMenu toolsMenu;
    private javax.swing.JTabbedPane toolsPanel;
    private javax.swing.JMenuItem updateMenuItem;
    // End of variables declaration//GEN-END:variables
    private AboutBox aboutBox;
    private LogoutDialog logoutDialog;
    private LoginDialog loginDialog;
    private java.util.Timer timer;
    private WorkingBox workingBox;
    private TimeBox timeBox;

    /**
     * Cronómetro de tiempo.
     */
    private class UserTimerTask extends java.util.TimerTask {

        // Atributos:
        /** Número de minutos restantes. */
        private int minutes;
        /** Contexto de la aplicación. */
        private ClientContext context;

        // Constructor:
        /**
         * Constructor del cronómetro.
         * @param minutes Número de minutos restantes.
         * @param context Contexto de la aplicación.
         */
        public UserTimerTask(int minutes, ClientContext context) {
            super();
            this.context = context;
            if (minutes < 0) {
                this.minutes = 0;
            } else {
                this.minutes = minutes;
            }
        }

        /**
         * Actualiza la etiqueta y muestra la información.
         */
        public void run() {

            // 1. Resta los minutos.
            context.setUseTime(minutes);
            context.getMainView().updateTime(minutes);
            if (minutes <= 1) {
                context.setUseTime(0);
                context.getMainView().timeoutWarning();
                timer.cancel();
                timer.purge();
            }
            this.minutes--;
            return;

        }
    }
}
