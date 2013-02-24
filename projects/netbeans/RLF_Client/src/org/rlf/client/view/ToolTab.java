/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rlf.client.view;

import org.rlf.client.view.dialog.LogoutDialog;
import java.util.HashMap;
import org.rlf.client.RLF_ClientApp;
import org.rlf.client.ClientContext;
import org.rlf.client.ClientContext.ClientStatus;
import org.rlf.client.data.Action;
import org.rlf.client.data.Tool;
import org.rlf.client.data.Tool.ToolStatus;
import org.rlf.client.view.exec.ActionView;
import org.rlf.client.view.exec.ExecDialog;

/**
 * Panel que muestra la información de una herramienta así como las acciones a ejecutar.
 * @author rodriguezmecha
 */
public class ToolTab extends javax.swing.JPanel {

    // Atributos:
    /** Herramienta del panel. */
    private Tool tool;
    /** Contexto de la aplicación. */
    private ClientContext context;
    /** Acción en ejecución. */
    private ActionView actionView;
    // Atributos gráficos:
    private javax.swing.JScrollPane actDescriptionPane;
    private javax.swing.JTextPane actDescriptionTextPane;
    private javax.swing.JComboBox actionNamesPane;
    private javax.swing.JPanel actionPane;
    private javax.swing.JLabel dataImage;
    private javax.swing.JLabel dataLabel;
    private javax.swing.JScrollPane descriptionPane;
    private javax.swing.JTextPane descriptionTextPane;
    private javax.swing.JButton execButton;
    private javax.swing.JLabel inImage;
    private javax.swing.JLabel inLabel;
    private javax.swing.JPanel infoPane;
    private javax.swing.JLabel infoTakeLabel;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JLabel outImage;
    private javax.swing.JLabel outLabel;
    private javax.swing.JLabel statusImage;
    private javax.swing.JCheckBox takeBox;
    private ExecDialog execDialog;

    // Constructor:
    /**
     * Constructor del panel.
     * @param tool Herramienta asociada a la pestaña.
     * @param context Contexto de la aplicación.
     */
    public ToolTab(Tool tool, ClientContext context) {
        super();
        this.tool = tool;
        this.context = context;
        this.execDialog = null;
        initComponents();

    }

    /**
     * Inicializa todos los componentes de la pestaña.
     */
    private void initComponents() {

        setName(this.tool.getName() + " (" + this.tool.getId() + ")");

        // 1. Iniciación de componentes.
        nameLabel = new javax.swing.JLabel();
        statusImage = new javax.swing.JLabel();
        descriptionPane = new javax.swing.JScrollPane();
        descriptionTextPane = new javax.swing.JTextPane();
        actionPane = new javax.swing.JPanel();
        actionNamesPane = new javax.swing.JComboBox();
        actDescriptionPane = new javax.swing.JScrollPane();
        actDescriptionTextPane = new javax.swing.JTextPane();
        execButton = new javax.swing.JButton();
        infoPane = new javax.swing.JPanel();
        outImage = new javax.swing.JLabel();
        inImage = new javax.swing.JLabel();
        dataImage = new javax.swing.JLabel();
        outLabel = new javax.swing.JLabel();
        inLabel = new javax.swing.JLabel();
        dataLabel = new javax.swing.JLabel();
        infoTakeLabel = new javax.swing.JLabel();
        takeBox = new javax.swing.JCheckBox();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(ToolTab.class);
        nameLabel.setFont(resourceMap.getFont("nameLabel.font")); // NOI18N
        nameLabel.setText(this.tool.getName() + " (" + this.tool.getId() + ")"); // NOI18N
        nameLabel.setName("nameLabel"); // NOI18N

        statusImage.setFont(resourceMap.getFont("statusImage.font")); // NOI18N
        statusImage.setIcon(resourceMap.getIcon("busy.icon")); // NOI18N
        statusImage.setToolTipText(resourceMap.getString("statusImage.toolTipText")); // NOI18N
        statusImage.setName("statusImage"); // NOI18N

        descriptionPane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("descriptionPane.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("descriptionPane.border.titleFont")));
        descriptionPane.setName("descriptionPane"); // NOI18N

        descriptionTextPane.setBorder(null);
        descriptionTextPane.setEditable(false);
        descriptionTextPane.setFont(resourceMap.getFont("descriptionTextPane.font")); // NOI18N
        descriptionTextPane.setText(this.tool.getDescription());
        descriptionTextPane.setName("descriptionTextPane"); // NOI18N
        descriptionPane.setViewportView(descriptionTextPane);

        actionPane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("actionPane.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("actionPane.border.titleFont"))); // NOI18N
        actionPane.setName("actionPane"); // NOI18N

        actionNamesPane.setFont(resourceMap.getFont("actionNamesPane.font")); // NOI18N
        HashMap<String, Action> actions = this.tool.getActions();
        String[] actionNames = new String[actions.size()];
        int i = 0;
        for (String actionName : actions.keySet()) {
            actionNames[i++] = actionName;
        }

        actionNamesPane.setModel(new javax.swing.DefaultComboBoxModel(actionNames));
        actionNamesPane.setEditor(null);
        actionNamesPane.setName("actionNamesPane"); // NOI18N
        actionNamesPane.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeAction(evt);
            }
        });

        actDescriptionPane.setName("actDescriptionPane"); // NOI18N

        actDescriptionTextPane.setEditable(false);
        actDescriptionTextPane.setFont(resourceMap.getFont("actDescriptionTextPane.font")); // NOI18N
        actDescriptionTextPane.setName("actDescriptionTextPane"); // NOI18N
        if (actions.size() > 0) {
            actDescriptionTextPane.setText(actions.get(actionNames[0]).getDescription());
        }
        actDescriptionPane.setViewportView(actDescriptionTextPane);

        execButton.setText(resourceMap.getString("execButton.text")); // NOI18N
        execButton.setName("execButton"); // NOI18N
        execButton.setFont(resourceMap.getFont("execButton.font"));
        execButton.setEnabled(false);
        execButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeAction(evt);
            }
        });

        javax.swing.GroupLayout actionPaneLayout = new javax.swing.GroupLayout(actionPane);
        actionPane.setLayout(actionPaneLayout);
        actionPaneLayout.setHorizontalGroup(
                actionPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(actionPaneLayout.createSequentialGroup().addContainerGap().addGroup(actionPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(actDescriptionPane, javax.swing.GroupLayout.DEFAULT_SIZE, 784, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, actionPaneLayout.createSequentialGroup().addComponent(actionNamesPane, 0, 696, Short.MAX_VALUE).addGap(18, 18, 18).addComponent(execButton))).addContainerGap()));
        actionPaneLayout.setVerticalGroup(
                actionPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(actionPaneLayout.createSequentialGroup().addContainerGap().addGroup(actionPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(actionNamesPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(execButton)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(actDescriptionPane, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE).addContainerGap()));

        infoPane.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        infoPane.setName("infoPane"); // NOI18N

        outImage.setFont(resourceMap.getFont("outImage.font")); // NOI18N
        outImage.setIcon(tool.isOutStream() ? resourceMap.getIcon("yes.icon") : resourceMap.getIcon("no.icon")); // NOI18N
        outImage.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        outImage.setName("outImage"); // NOI18N

        inImage.setFont(resourceMap.getFont("inImage.font")); // NOI18N
        inImage.setIcon(tool.isInStream() ? resourceMap.getIcon("yes.icon") : resourceMap.getIcon("no.icon")); // NOI18N
        inImage.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        inImage.setName("inImage"); // NOI18N

        dataImage.setFont(resourceMap.getFont("dataImage.font")); // NOI18N
        dataImage.setIcon(tool.isDataTool() ? resourceMap.getIcon("yes.icon") : resourceMap.getIcon("no.icon")); // NOI18N
        dataImage.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        dataImage.setName("dataImage"); // NOI18N

        outLabel.setFont(resourceMap.getFont("outLabel.font")); // NOI18N
        outLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        outLabel.setText(resourceMap.getString("outLabel.text")); // NOI18N
        outLabel.setToolTipText(resourceMap.getString("outLabel.toolTipText")); // NOI18N
        outLabel.setName("outLabel"); // NOI18N

        inLabel.setFont(resourceMap.getFont("inLabel.font")); // NOI18N
        inLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        inLabel.setText(resourceMap.getString("inLabel.text")); // NOI18N
        inLabel.setToolTipText(resourceMap.getString("inLabel.toolTipText")); // NOI18N
        inLabel.setName("inLabel"); // NOI18N

        dataLabel.setFont(resourceMap.getFont("dataLabel.font")); // NOI18N
        dataLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        dataLabel.setText(resourceMap.getString("dataLabel.text")); // NOI18N
        dataLabel.setToolTipText(resourceMap.getString("dataLabel.toolTipText")); // NOI18N
        dataLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        dataLabel.setName("dataLabel"); // NOI18N

        javax.swing.GroupLayout infoPaneLayout = new javax.swing.GroupLayout(infoPane);
        infoPane.setLayout(infoPaneLayout);
        infoPaneLayout.setHorizontalGroup(
                infoPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(infoPaneLayout.createSequentialGroup().addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(infoPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, infoPaneLayout.createSequentialGroup().addComponent(dataImage, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(inImage, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(outImage, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, infoPaneLayout.createSequentialGroup().addComponent(dataLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(inLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(outLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))).addContainerGap()));
        infoPaneLayout.setVerticalGroup(
                infoPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, infoPaneLayout.createSequentialGroup().addGroup(infoPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(outLabel).addComponent(inLabel).addComponent(dataLabel)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(infoPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(dataImage).addComponent(inImage).addComponent(outImage))));

        infoTakeLabel.setFont(resourceMap.getFont("infoTakeLabel.font")); // NOI18N
        infoTakeLabel.setText(resourceMap.getString("infoTakeLabel.text")); // NOI18N
        infoTakeLabel.setName("infoTakeLabel"); // NOI18N

        takeBox.setFont(resourceMap.getFont("takeBox.font")); // NOI18N
        takeBox.setText(resourceMap.getString("takeBox.text")); // NOI18N
        takeBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        takeBox.setName("takeBox"); // NOI18N
        takeBox.setEnabled(false);
        takeBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAction(evt);
            }
        });

        javax.swing.GroupLayout tabLayout = new javax.swing.GroupLayout(this);
        setLayout(tabLayout);
        tabLayout.setHorizontalGroup(
                tabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabLayout.createSequentialGroup().addContainerGap().addGroup(tabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(descriptionPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 991, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.LEADING, tabLayout.createSequentialGroup().addGap(6, 6, 6).addComponent(statusImage).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(nameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 745, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE).addComponent(infoPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addGroup(tabLayout.createSequentialGroup().addComponent(actionPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGap(18, 18, 18).addGroup(tabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(takeBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(infoTakeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)))).addContainerGap()));
        tabLayout.setVerticalGroup(
                tabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabLayout.createSequentialGroup().addGroup(tabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addGroup(tabLayout.createSequentialGroup().addContainerGap().addGroup(tabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(nameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(infoPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabLayout.createSequentialGroup().addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(statusImage).addGap(26, 26, 26))).addComponent(descriptionPane, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(tabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(tabLayout.createSequentialGroup().addComponent(infoTakeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(18, 18, 18).addComponent(takeBox)).addComponent(actionPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));

        this.setToolTipText(tool.getName() + " (" + tool.getId() + ")");
        this.setFont(resourceMap.getFont("frame.font"));


    }

    /**
     * Evento asociado a seleccionar otra acción.
     * @param evt Evento.
     */
    private void changeAction(java.awt.event.ActionEvent evt) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                String action = (String) actionNamesPane.getSelectedItem();
                String description = tool.getActions().get(action).getDescription();
                actDescriptionTextPane.setText(description);
            }
        });

    }

    /**
     * Evento asociado a seleccionar la herramienta.
     * @param evt Evento.
     */
    private void selectAction(java.awt.event.ActionEvent evt) {
        this.context.getMainView().updateSelectedTools();
    }

    /**
     * Evento asociado a una ejecución.
     * @param evt Evento
     */
    private void executeAction(java.awt.event.ActionEvent evt) {

        if (context.status() != ClientStatus.TAKED) {
            return;
        }

        if (tool.status() != ToolStatus.TAKED) {
            return;
        }

        // 1. Obtención de la acción.
        Action action = tool.getActions().get((String) actionNamesPane.getSelectedItem());

        // 2. Muestra del diálogo de ejecución.
        if (execDialog != null) {
            execDialog.dispose();
        }
        javax.swing.JFrame mainFrame = RLF_ClientApp.getApplication().getMainFrame();
        execDialog = new ExecDialog(mainFrame, action);
        execDialog.setLocationRelativeTo(mainFrame);

        RLF_ClientApp.getApplication().show(execDialog);
        if (execDialog.getReturnStatus() != LogoutDialog.RET_OK) {
            return;
        }

        // 3. Envío al laboratorio.
        synchronized (this.context.notificationManager()) {
            if (!this.context.notificationManager().sendRequest(action)) {
                this.context.getMainView().errorWindow("Failed to execute the action, try again.");
                return;
            }

            this.actionView = new ActionView(action, this, context);
            this.tool.running(actionView);
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    actionView.setVisible(true);
                }
            });

            tool.status(ToolStatus.EXEC);
            context.getRunningActions().put(action, actionView);
        }
        changeStatus();
    }

    /**
     * Se utiliza para activar y desactivar controles dependiendo del estado de la herramienta asignada.
     */
    public void changeStatus() {
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(ToolTab.class);

        switch (tool.status()) {
            case EXEC:
                execButton.setEnabled(false);
                takeBox.setEnabled(false);
                statusImage.setIcon(resourceMap.getIcon("exec.icon"));
                statusImage.setToolTipText("This tool is running");
                break;
            case FREE:
                execButton.setEnabled(false);
                if (this.context.status() == ClientStatus.TAKED) {
                    takeBox.setEnabled(false);
                } else {
                    takeBox.setEnabled(true);
                }
                statusImage.setIcon(resourceMap.getIcon("free.icon"));
                statusImage.setToolTipText("You can take this tool");
                break;
            case TAKED:
                execButton.setEnabled(true);
                takeBox.setEnabled(false);
                statusImage.setIcon(resourceMap.getIcon("taked.icon"));
                statusImage.setToolTipText("Now you can use this tool");
                break;
            default:
                execButton.setEnabled(false);
                takeBox.setEnabled(false);
                takeBox.setSelected(false);
                statusImage.setIcon(resourceMap.getIcon("busy.icon"));
                statusImage.setToolTipText("This tool is busy");
                break;

        }

    }

    /**
     * Obtiene la herramienta asociada.
     * @return Herramienta.
     */
    public Tool getTool() {
        return this.tool;
    }

    /**
     * Indica si la herramienta está seleccionada para utilizarla.
     * @return Verdadero si está seleccionada.
     */
    public boolean isSelected() {
        return this.takeBox.isSelected();
    }
}
