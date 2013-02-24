/*
 * Parada del laboratorio
 */

package org.rlf.client.view.exec;

import org.rlf.client.data.Tool;

/**
 * Ventana que indica que el laboratorio se ha parado.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class StopExecBox extends javax.swing.JDialog {

    // Constructor.
    /**
     * Crea la ventana.
     */
    public StopExecBox(java.awt.Frame parent, Tool tool) {
        super(parent, true);
        initComponents();
        this.toolLabel.setText(tool.getName() + " (" + tool.getId() + ")");
    }

    /**
     * Inicia los componentes gráficos.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        textLabel = new javax.swing.JLabel();
        closeButton = new javax.swing.JButton();
        toolLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(StopExecBox.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        setResizable(false);

        textLabel.setFont(resourceMap.getFont("textLabel.font")); // NOI18N
        textLabel.setIcon(resourceMap.getIcon("textLabel.icon")); // NOI18N
        textLabel.setText(resourceMap.getString("textLabel.text")); // NOI18N
        textLabel.setName("textLabel"); // NOI18N

        closeButton.setFont(resourceMap.getFont("closeButton.font")); // NOI18N
        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeAction(evt);
            }
        });

        toolLabel.setFont(resourceMap.getFont("toolLabel.font")); // NOI18N
        toolLabel.setText(resourceMap.getString("toolLabel.text")); // NOI18N
        toolLabel.setName("toolLabel"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(139, 139, 139)
                .addComponent(closeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(144, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(toolLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 359, Short.MAX_VALUE)
                    .addComponent(textLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 359, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(toolLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(textLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(closeButton)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Acción de cerrar la ventana.
     * @param evt Evento.
     */
private void closeAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeAction
    dispose();
}//GEN-LAST:event_closeAction

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JLabel textLabel;
    private javax.swing.JLabel toolLabel;
    // End of variables declaration//GEN-END:variables
}
