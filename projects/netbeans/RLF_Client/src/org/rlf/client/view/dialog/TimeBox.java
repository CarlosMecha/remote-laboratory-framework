/*
 * Alerta de tiempo.
 */

package org.rlf.client.view.dialog;

/**
 * Muestra un mensaje en la pantalla por el tiempo de uso. Puede mostrar una alerta
 * de 5 minutos y de un minuto.
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class TimeBox extends javax.swing.JDialog {

    /**
     * Aviso de tiempo.
     * @param parent Frame.
     * @param less1min Indica si le queda menos de un minuto.
     */
    public TimeBox(java.awt.Frame parent, boolean less1min) {
        super(parent, false);
        initComponents();
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(TimeBox.class);
        if (less1min) {
            this.messageLabel.setText(resourceMap.getString("less1min.text"));
        } else {
            this.messageLabel.setText(resourceMap.getString("minutes.text"));
        }
    }
    
    /**
     * Aviso de timeout.
     * @param parent Frame.
     * @param less1min Indica si le queda menos de un minuto.
     */
    public TimeBox(java.awt.Frame parent) {
        super(parent, false);
        initComponents();
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(TimeBox.class);
        this.messageLabel.setText(resourceMap.getString("timeout.text"));
        
    }

    /**
     * Inicia los componentes gráficos.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        messageLabel = new javax.swing.JLabel();
        okButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(TimeBox.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        setResizable(false);

        messageLabel.setFont(resourceMap.getFont("messageLabel.font")); // NOI18N
        messageLabel.setIcon(resourceMap.getIcon("messageLabel.icon")); // NOI18N
        messageLabel.setText(resourceMap.getString("messageLabel.text")); // NOI18N
        messageLabel.setName("messageLabel"); // NOI18N

        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okAction(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(messageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(144, 144, 144)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(messageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                .addComponent(okButton)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void okAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okAction
    dispose();
}//GEN-LAST:event_okAction

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel messageLabel;
    private javax.swing.JButton okButton;
    // End of variables declaration//GEN-END:variables
}
