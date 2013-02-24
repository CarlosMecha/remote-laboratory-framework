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
public class ExecDialog extends javax.swing.JDialog {

    // Constantes:
    /** A return status code - returned if Cancel button has been pressed */
    public static final int RET_CANCEL = 0;
    /** A return status code - returned if OK button has been pressed */
    public static final int RET_OK = 1;
    // Atributos:
    /** Resultado del diálogo. */
    private int returnStatus;
    /** Lista de los parámetros. */
    private HashMap<String, JParameter> parameters;
    /** Acción asociada. */
    private Action action;
    /** Panel de componentes. */
    private javax.swing.JPanel componentsPanel;

    // Constructor:
    /**
     * Crea el diálogo de ejecución de una acción.
     * @param parent Ventana principal.
     */
    public ExecDialog(java.awt.Frame parent, Action action) {
        super(parent, true);
        this.action = action;
        this.parameters = new HashMap<String, JParameter>();
        initComponents();
        initAction();
    }

    /**
     * Añade los parámetros y los sockets propios de la acción.
     */
    private void initAction() {

        componentsPanel = new javax.swing.JPanel();
        componentsPanel.setName("propertiesPanel");

        // 1. Se crean los paneles de los parámetros.
        for (Parameter p : action.getInParameters().values()) {
            JParameter jparameter = new JParameter(p);
            parameters.put(p.getName(), jparameter);
        }
        for (Parameter p : action.getInOutParameters().values()) {
            JParameter jparameter = new JParameter(p);
            parameters.put(p.getName(), jparameter);
        }

        // 2. Se crean los paneles de los sockets.
        HashMap<String, JSocket> sockets = new HashMap<String, JSocket>();
        for (Entry<String, String> e : action.getSockets().entrySet()) {
            JSocket jsocket = new JSocket(e.getKey(), action.getTool().lab().getHost() + e.getValue());
            sockets.put(e.getKey(), jsocket);
        }

        // 3. Layout horizontal.
        javax.swing.GroupLayout propertiesPanelLayout = new javax.swing.GroupLayout(componentsPanel);
        componentsPanel.setLayout(propertiesPanelLayout);

        javax.swing.GroupLayout.ParallelGroup pg = propertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING);
        for (JParameter p : parameters.values()) {
            pg = pg.addComponent(p, javax.swing.GroupLayout.DEFAULT_SIZE, 737, Short.MAX_VALUE);
        }
        for (JSocket s : sockets.values()) {
            pg = pg.addComponent(s, javax.swing.GroupLayout.DEFAULT_SIZE, 737, Short.MAX_VALUE);
        }

        propertiesPanelLayout.setHorizontalGroup(propertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(propertiesPanelLayout.createSequentialGroup().addContainerGap().addGroup(pg).addContainerGap()));

        // 4. Layout vertical.
        javax.swing.GroupLayout.SequentialGroup sg = propertiesPanelLayout.createSequentialGroup().addContainerGap();

        for (JParameter p : parameters.values()) {
            sg = sg.addComponent(p, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);
        }
        for (JSocket s : sockets.values()) {
            sg = sg.addComponent(s, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE);
        }

        propertiesPanelLayout.setVerticalGroup(propertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(sg));

        // 5. Se añade el scrollPane.
        scrollPane.setViewportView(componentsPanel);

        // 6. Nombre de la acción.
        actionLabel.setText(action.getName());
        actionLabel.setToolTipText(action.getDescription());
        toolLabel.setText(action.getTool().getName());
        toolLabel.setToolTipText(action.getTool().getDescription());
    }

    /**
     * Acción de cerrar.
     * @param retStatus 
     */
    private void doClose(int retStatus) {
        returnStatus = retStatus;
        if (retStatus == RET_OK) {
            HashMap<String, Parameter> parms = action.getInParameters();
            for (Parameter p : parms.values()) {
                p.setValue(parameters.get(p.getName()).getValue());
            }
            parms = action.getInOutParameters();
            for (Parameter p : parms.values()) {
                p.setValue(parameters.get(p.getName()).getValue());
            }
        }
        setVisible(false);
        dispose();
    }

    /**
     * Obtiene el estado de la ventana cuando se cerró.
     * @return the return status of this dialog - one of RET_OK or RET_CANCEL.
     */
    public int getReturnStatus() {
        return returnStatus;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        infoPanel = new javax.swing.JPanel();
        actionLabel = new javax.swing.JLabel();
        toolLabel = new javax.swing.JLabel();
        buttonsPanel = new javax.swing.JPanel();
        cancelButton = new javax.swing.JButton();
        executeButton = new javax.swing.JButton();
        scrollPane = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(ExecDialog.class);
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

        javax.swing.GroupLayout infoPanelLayout = new javax.swing.GroupLayout(infoPanel);
        infoPanel.setLayout(infoPanelLayout);
        infoPanelLayout.setHorizontalGroup(
            infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, infoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(toolLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 802, Short.MAX_VALUE)
                    .addComponent(actionLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 802, Short.MAX_VALUE))
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
        );

        buttonsPanel.setName("buttonsPanel"); // NOI18N

        cancelButton.setFont(resourceMap.getFont("cancelButton.font")); // NOI18N
        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelAction(evt);
            }
        });

        executeButton.setFont(resourceMap.getFont("executeButton.font")); // NOI18N
        executeButton.setText(resourceMap.getString("executeButton.text")); // NOI18N
        executeButton.setName("executeButton"); // NOI18N
        executeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeAction(evt);
            }
        });

        javax.swing.GroupLayout buttonsPanelLayout = new javax.swing.GroupLayout(buttonsPanel);
        buttonsPanel.setLayout(buttonsPanelLayout);
        buttonsPanelLayout.setHorizontalGroup(
            buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, buttonsPanelLayout.createSequentialGroup()
                .addGap(150, 150, 150)
                .addComponent(executeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 321, Short.MAX_VALUE)
                .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(135, 135, 135))
        );
        buttonsPanelLayout.setVerticalGroup(
            buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(buttonsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(executeButton))
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
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 802, Short.MAX_VALUE)
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
    doClose(RET_CANCEL);
}//GEN-LAST:event_cancelAction

private void closeAction(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeAction
    doClose(RET_CANCEL);
}//GEN-LAST:event_closeAction

private void executeAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_executeAction
    doClose(RET_OK);
}//GEN-LAST:event_executeAction

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel actionLabel;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton executeButton;
    private javax.swing.JPanel infoPanel;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JLabel toolLabel;
    // End of variables declaration//GEN-END:variables

    /**
     * Panel con la información del parámetro y su valor.
     */
    private class JParameter extends javax.swing.JPanel {

        // Atributos:
        /** Etiqueta del parámetro. */
        private javax.swing.JLabel label;
        /** Campo de texto. */
        private javax.swing.JTextField text;

        // Constructor:
        /**
         * Constructor del panel.
         * @param parameter Parámetro al que representa.
         */
        public JParameter(Parameter parameter) {
            super();
            this.setName(parameter.getName());
            initComponents(parameter);
        }

        /**
         * Inicia los componentes gráficos.
         * @param parameter Parámetro al que representa.
         */
        private void initComponents(Parameter parameter) {
            this.label = new javax.swing.JLabel();
            this.text = new javax.swing.JTextField();

            this.setMaximumSize(new java.awt.Dimension(722, 100));
            this.setMinimumSize(new java.awt.Dimension(722, 40));

            this.setPreferredSize(new java.awt.Dimension(722, 40));

            org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(JParameter.class);

            this.label.setFont(resourceMap.getFont("label.font"));
            this.label.setText(parameter.getName() + " (" + parameter.getDType() + "):");
            this.label.setToolTipText(parameter.getDescription());
            this.label.setName("label");

            this.text.setFont(resourceMap.getFont("text.font"));
            this.text.setName("text");

            javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(this);
            this.setLayout(panelLayout);
            panelLayout.setHorizontalGroup(
                    panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panelLayout.createSequentialGroup().addContainerGap().addComponent(this.label, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE).addGap(18, 18, 18).addComponent(this.text, javax.swing.GroupLayout.PREFERRED_SIZE, 552, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()));
            panelLayout.setVerticalGroup(
                    panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup().addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(this.label).addComponent(this.text, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));

        }

        /**
         * Obtiene el valor del parámetro. Si no ha escrito nada el usuario
         * devuelve una cadena vacía.
         * @return Valor del parámetro introducido.
         */
        public String getValue() {
            if (this.text.getText() == null) {
                return new String();
            }
            return this.text.getText();
        }
    }

    /**
     * Panel con la información de un socket.
     */
    private class JSocket extends javax.swing.JPanel {

        // Atributos:
        /** Etiqueta del socket. */
        private javax.swing.JLabel label;

        // Constructor:
        /**
         * Constructor del panel.
         * @param id Identificador del socket.
         * @param socket Representación del socket en una cadena de texto.
         */
        public JSocket(String id, String socket) {
            super();
            this.setName(id);
            initComponents(socket);
        }

        /**
         * Inicia los componentes gráficos.
         * @param socket Representación del socket en una cadena de texto.
         */
        private void initComponents(String socket) {
            this.label = new javax.swing.JLabel();
            this.setPreferredSize(new java.awt.Dimension(737, 40));

            org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(JSocket.class);
            this.label.setFont(resourceMap.getFont("label.font"));
            this.label.setText(socket);
            this.label.setName("label"); // NOI18N

            javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(this);
            this.setLayout(panelLayout);
            panelLayout.setHorizontalGroup(
                    panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(panelLayout.createSequentialGroup().addContainerGap().addComponent(this.label, javax.swing.GroupLayout.DEFAULT_SIZE, 713, Short.MAX_VALUE).addContainerGap()));
            panelLayout.setVerticalGroup(
                    panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup().addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(this.label).addContainerGap()));


        }
    }
}
