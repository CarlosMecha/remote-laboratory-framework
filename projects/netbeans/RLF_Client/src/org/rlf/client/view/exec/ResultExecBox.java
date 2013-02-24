/*
 * Ventana de ejecución.
 */
package org.rlf.client.view.exec;

import java.util.HashMap;
import java.util.Map.Entry;
import org.rlf.client.data.Action;
import org.rlf.client.data.Parameter;

/**
 * Diálogo de ejecución.
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class ResultExecBox extends javax.swing.JDialog {

    // Atributos:
    /** Acción asociada. */
    private Action action;
    /** Panel de componentes. */
    private javax.swing.JPanel componentsPanel;

    // Constructor:
    /**
     * Crea el diálogo de ejecución de una acción.
     * @param parent Ventana principal.
     * @param action Acción a la que corresponde la ejecución.
     * @param 
     */
    public ResultExecBox(java.awt.Frame parent, Action action, HashMap<String, String> rst, boolean error) {
        super(parent, true);
        this.action = action;
        initComponents();
        initAction(rst, error);
    }

    /**
     * Añade los parámetros devueltos de la ejecución.
     * @param rst Lista de cambios.
     * @param error Indica si ha habido error de ejecución.
     */
    private void initAction(HashMap<String, String> rst, boolean error) {

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(ResultExecBox.class);
        actionLabel.setText(action.getName());
        toolLabel.setText(action.getTool().getName() + " (" + action.getTool().getId() + ")");
        
        
        if (error) {
            resultLabel.setText(resourceMap.getString("error.text"));
            resultLabel.setIcon(resourceMap.getIcon("error.icon"));
        }

        componentsPanel = new javax.swing.JPanel();
        componentsPanel.setName("propertiesPanel");
        HashMap<String, JChange> changes = new HashMap<String, JChange>();

        // 1. Se crean los paneles de los cambios.
        String status = rst.get("status");
        if (status != null) {
            
        }
        for (Entry<String, String> e : rst.entrySet()){
            if (e.getKey().contains("exception")) {
                changes.put(e.getKey(), new JChange(e.getKey(), e.getValue(), "Exception ocurred"));
            } else if (e.getKey().compareToIgnoreCase("status") == 0){
                changes.put("status", new JChange("status", status, "Exit status"));
            } else {
                Parameter p = action.getOutParameters().get(e.getKey());
                if (p == null) {
                    p = action.getInOutParameters().get(e.getKey());
                }
                if (p == null) {
                    continue;
                }
                changes.put(e.getKey(), new JChange(e.getKey(), e.getValue(), p.getDescription()));
            }
        }

        // 2. Layout horizontal.
        javax.swing.GroupLayout propertiesPanelLayout = new javax.swing.GroupLayout(componentsPanel);
        componentsPanel.setLayout(propertiesPanelLayout);

        javax.swing.GroupLayout.ParallelGroup pg = propertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING);
        for (JChange c : changes.values()) {
            pg = pg.addComponent(c, javax.swing.GroupLayout.DEFAULT_SIZE, 737, Short.MAX_VALUE);
        }

        propertiesPanelLayout.setHorizontalGroup(propertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(propertiesPanelLayout.createSequentialGroup().addContainerGap().addGroup(pg).addContainerGap()));

        // 3. Layout vertical.
        javax.swing.GroupLayout.SequentialGroup sg = propertiesPanelLayout.createSequentialGroup().addContainerGap();

        for (JChange c : changes.values()) {
            sg = sg.addComponent(c, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE);
        }

        propertiesPanelLayout.setVerticalGroup(propertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(sg));

        // 4. Se añade el scrollPane.
        scrollPane.setViewportView(componentsPanel);

        // 6. Nombre de la acción.
        actionLabel.setText(action.getName());
        actionLabel.setToolTipText(action.getDescription());
        toolLabel.setText(action.getTool().getName());
        toolLabel.setToolTipText(action.getTool().getDescription());
    }

    /** 
     * Inicializa los componentes gráficos básicos.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        infoPanel = new javax.swing.JPanel();
        actionLabel = new javax.swing.JLabel();
        toolLabel = new javax.swing.JLabel();
        resultLabel = new javax.swing.JLabel();
        buttonsPanel = new javax.swing.JPanel();
        closeButton = new javax.swing.JButton();
        scrollPane = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(ResultExecBox.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeAction(evt);
            }
        });

        infoPanel.setName("infoPanel"); // NOI18N

        actionLabel.setFont(resourceMap.getFont("actionLabel.font")); // NOI18N
        actionLabel.setText(resourceMap.getString("actionLabel.text")); // NOI18N
        actionLabel.setName("actionLabel"); // NOI18N

        toolLabel.setFont(resourceMap.getFont("toolLabel.font")); // NOI18N
        toolLabel.setText(resourceMap.getString("toolLabel.text")); // NOI18N
        toolLabel.setName("toolLabel"); // NOI18N

        resultLabel.setFont(resourceMap.getFont("resultLabel.font")); // NOI18N
        resultLabel.setIcon(resourceMap.getIcon("resultLabel.icon")); // NOI18N
        resultLabel.setText(resourceMap.getString("resultLabel.text")); // NOI18N
        resultLabel.setName("resultLabel"); // NOI18N

        javax.swing.GroupLayout infoPanelLayout = new javax.swing.GroupLayout(infoPanel);
        infoPanel.setLayout(infoPanelLayout);
        infoPanelLayout.setHorizontalGroup(
            infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(actionLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(toolLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 648, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 22, Short.MAX_VALUE)
                .addComponent(resultLabel)
                .addContainerGap())
        );
        infoPanelLayout.setVerticalGroup(
            infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(actionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(toolLabel)
                .addContainerGap(28, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, infoPanelLayout.createSequentialGroup()
                .addContainerGap(28, Short.MAX_VALUE)
                .addComponent(resultLabel)
                .addGap(25, 25, 25))
        );

        buttonsPanel.setName("buttonsPanel"); // NOI18N

        closeButton.setFont(resourceMap.getFont("closeButton.font")); // NOI18N
        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelAction(evt);
            }
        });

        javax.swing.GroupLayout buttonsPanelLayout = new javax.swing.GroupLayout(buttonsPanel);
        buttonsPanel.setLayout(buttonsPanelLayout);
        buttonsPanelLayout.setHorizontalGroup(
            buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, buttonsPanelLayout.createSequentialGroup()
                .addContainerGap(372, Short.MAX_VALUE)
                .addComponent(closeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(334, 334, 334))
        );
        buttonsPanelLayout.setVerticalGroup(
            buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(buttonsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(closeButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        scrollPane.setName("scrollPane"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(buttonsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(infoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 792, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(infoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(buttonsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void cancelAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelAction
    setVisible(false);
    dispose();
}//GEN-LAST:event_cancelAction

private void closeAction(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeAction
    setVisible(false);
    dispose();
}//GEN-LAST:event_closeAction
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel actionLabel;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton closeButton;
    private javax.swing.JPanel infoPanel;
    private javax.swing.JLabel resultLabel;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JLabel toolLabel;
    // End of variables declaration//GEN-END:variables

 
    /**
     * Panel con la información de un cambio.
     */
    private class JChange extends javax.swing.JPanel {

        // Atributos:
        /** Etiqueta del socket. */
        private javax.swing.JLabel label;

        // Constructor:
        /**
         * Constructor del panel.
         * @param id Identificador del cambio
         * @param description Representación del cambio.
         * @param toolTipText Texto de ayuda. Puede ser null.
         */
        public JChange(String id, String description, String toolTipText) {
            super();
            this.setName(id);
            initComponents(id, description, toolTipText);
        }

        /**
         * Inicia los componentes gráficos.
         * @param id Identificador del cambio
         * @param description Representación del cambio.
         * @param toolTipText Texto de ayuda. Puede ser null.
         */
        private void initComponents(String id, String description, String toolTipText) {
            this.label = new javax.swing.JLabel();
            this.setPreferredSize(new java.awt.Dimension(737, 40));

            org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(JChange.class);
            this.label.setFont(resourceMap.getFont("label.font"));
            this.label.setText(id + ": " + description);
            this.label.setToolTipText(toolTipText);
            this.label.setName("label");

            javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(this);
            this.setLayout(panelLayout);
            panelLayout.setHorizontalGroup(
                    panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panelLayout.createSequentialGroup().addContainerGap().addComponent(this.label, javax.swing.GroupLayout.DEFAULT_SIZE, 713, Short.MAX_VALUE).addContainerGap()));
            panelLayout.setVerticalGroup(
                    panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup().addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(this.label).addContainerGap()));


        }
    }
}
