/*
 * Ejecución.
 */
package org.rlf.client.view.exec;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.SwingWorker;
import org.rlf.client.RLF_ClientApp;
import org.rlf.client.ClientContext;
import org.rlf.client.data.Action;
import org.rlf.client.data.Tool.ToolStatus;
import org.rlf.client.view.ToolTab;

/**
 * Pantalla de ejecución de una acción.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class ActionView extends javax.swing.JFrame {

    // Atributos:
    /** Acción llevada a cabo. */
    private Action action;
    /** Socket de entrada. */
    private SocketChannel outSocket;
    /** Socket de salida. */
    private SocketChannel inSocket;
    /** Hilo de entrada. */
    private OutputThread thread;
    /** La acción ya está ejecutándose. */
    private boolean running;
    /** Componente padre. */
    private ToolTab toolTab;
    /** Ticket de la ejecución. */
    private String ticket;
    /** Contetxo de la aplicación. */ 
    private ClientContext context;

    // Constructor:
    /**
     * Construye la ventana de la acción.
     * @param action Acción.
     * @param toolTab Componente padre.
     * @param context Contexto de la aplicación.
     */
    public ActionView(Action action, ToolTab toolTab, ClientContext context) {
        super();
        initComponents();
        this.action = action;
        this.running = false;
        this.toolTab = toolTab;
        this.context = context;
        this.ticket = null;
        actionLabel.setText(action.getName());
        toolLabel.setText(action.getTool().getName() + " (" + action.getTool().getId() + ")");

        // Icono.
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(ActionView.class);
        String filename = resourceMap.getResourcesDir() + "icon_wait.png";
        URL url = resourceMap.getClassLoader().getResource(filename);
        Image icon = Toolkit.getDefaultToolkit().createImage(url);
        this.setIconImage(icon);

        outScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {

            public void adjustmentValueChanged(AdjustmentEvent e) {
                e.getAdjustable().setValue(e.getAdjustable().getMaximum());
            }
        });

    }
    
    /**
     * Obtiene la acción asignada a esta vista.
     * @return Acción.
     */
    public Action getAction(){
        return this.action;
    }

    /**
     * Empieza la ejecución de la acción.
     * @param ticket Ticket de la ejecución.
     * @param inSocket Socket de salida. Puede ser null si la herramienta no
     * lo requiere. El socket debe estar configurado y conectado.
     * @param outSocket Socket de salida. Puede ser null si la herramienta no
     * lo requiere. El socket debe estar configurado y conectado.
     */
    public synchronized void execute(String ticket, SocketChannel inSocket, SocketChannel outSocket) {
        this.inSocket = inSocket;
        this.outSocket = outSocket;
        this.ticket = ticket;

        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {

                // 1. Icono.
                org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(ActionView.class);
                String filename = resourceMap.getResourcesDir() + "icon_exec.png";
                URL url = resourceMap.getClassLoader().getResource(filename);
                Image icon = Toolkit.getDefaultToolkit().createImage(url);
                setIconImage(icon);

                // 2. Estado.
                statusLabel.setIcon(resourceMap.getIcon("running.icon"));
                statusLabel.setText("Running...");

                if (action.getTool().isInStream()) {
                    inText.setEnabled(true);
                    sendButton.setEnabled(true);
                }

                if (action.getTool().isOutStream()) {
                    outText.setEnabled(true);
                }

                // 3. Iniciación del hilo de la entrada.
                thread = new OutputThread();
                thread.start();

                return null;
            }
        };

        this.running = true;
        worker.execute();

    }

    /**
     * Fin de ejecución correcta. Muestra la ventana con los cambios realizados.
     * @param attrs Lista de atributos.
     */
    public synchronized void execFinish(HashMap<String, String> attrs) {

        final ResultExecBox box = new ResultExecBox(this, action, attrs, false);
        box.setLocationRelativeTo(this);

        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                RLF_ClientApp.getApplication().show(box);

                action.getTool().status(ToolStatus.TAKED);
                toolTab.changeStatus();
                close();
                return null;
            }
        };

        worker.execute();
        this.running = false;

    }

    /**
     * Fin de ejecución con errores. Muestra la ventana con los cambios realizados.
     * @param attrs Lista de atributos.
     */
    public synchronized void execError(HashMap<String, String> attrs) {

        final ResultExecBox box = new ResultExecBox(this, action, attrs, true);
        box.setLocationRelativeTo(this);

        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                RLF_ClientApp.getApplication().show(box);
                action.getTool().status(ToolStatus.TAKED);
                toolTab.changeStatus();
                close();
                return null;
            }
        };

        worker.execute();
        this.running = false;
    }

    /**
     * Fin de ejecución inesperada.
     */
    public synchronized void stop() {
        final StopExecBox box = new StopExecBox(this, action.getTool());
        box.setLocationRelativeTo(this);

        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                RLF_ClientApp.getApplication().show(box);

                action.getTool().status(ToolStatus.RESERVED);
                toolTab.changeStatus();
                close();
                return null;
            }
        };
        worker.execute();
        this.running = false;
    }

    /**
     * Se cierra la ventana sin ningún aviso.
     */
    public synchronized void forceStop() {

        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {

                action.getTool().status(ToolStatus.RESERVED);
                toolTab.changeStatus();
                close();
                return null;
            }
        };
        worker.execute();
        this.running = false;
    }

    /**
     * Cierre de la ventana y liberación de recursos.
     */
    private void close() {

        if (inSocket != null) {
            try {
                inSocket.close();
            } catch (IOException ex) {
            }
        }
        if (thread != null) {
            thread.stopOutput();
            try {
                thread.join();
            } catch (Exception ex) {
            }
        }

        action.getTool().running(null);

        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {

                setVisible(false);
                dispose();
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

        outScrollPane = new javax.swing.JScrollPane();
        outText = new javax.swing.JTextArea();
        inPanel = new javax.swing.JPanel();
        sendButton = new javax.swing.JButton();
        inText = new javax.swing.JTextField();
        statusLabel = new javax.swing.JLabel();
        actionLabel = new javax.swing.JLabel();
        toolLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.rlf.client.RLF_ClientApp.class).getContext().getResourceMap(ActionView.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeAction(evt);
            }
        });

        outScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("outScrollPane.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("outScrollPane.border.titleFont"))); // NOI18N
        outScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        outScrollPane.setToolTipText(resourceMap.getString("outScrollPane.toolTipText")); // NOI18N
        outScrollPane.setAutoscrolls(true);
        outScrollPane.setName("outScrollPane"); // NOI18N

        outText.setBackground(resourceMap.getColor("outText.background")); // NOI18N
        outText.setColumns(20);
        outText.setEditable(false);
        outText.setFont(resourceMap.getFont("outText.font")); // NOI18N
        outText.setForeground(resourceMap.getColor("outText.foreground")); // NOI18N
        outText.setLineWrap(true);
        outText.setRows(5);
        outText.setAutoscrolls(true);
        outText.setEnabled(false);
        outText.setMargin(new java.awt.Insets(4, 4, 0, 0));
        outText.setName("outText"); // NOI18N
        outScrollPane.setViewportView(outText);

        inPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("inPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("inPanel.border.titleFont"))); // NOI18N
        inPanel.setToolTipText(resourceMap.getString("inPanel.toolTipText")); // NOI18N
        inPanel.setName("inPanel"); // NOI18N

        sendButton.setFont(resourceMap.getFont("sendButton.font")); // NOI18N
        sendButton.setText(resourceMap.getString("sendButton.text")); // NOI18N
        sendButton.setEnabled(false);
        sendButton.setName("sendButton"); // NOI18N
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendInput(evt);
            }
        });

        inText.setFont(resourceMap.getFont("inText.font")); // NOI18N
        inText.setEnabled(false);
        inText.setName("inText"); // NOI18N
        inText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inTextAction(evt);
            }
        });

        javax.swing.GroupLayout inPanelLayout = new javax.swing.GroupLayout(inPanel);
        inPanel.setLayout(inPanelLayout);
        inPanelLayout.setHorizontalGroup(
            inPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, inPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(inText, javax.swing.GroupLayout.DEFAULT_SIZE, 884, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(sendButton, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        inPanelLayout.setVerticalGroup(
            inPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, inPanelLayout.createSequentialGroup()
                .addGroup(inPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sendButton, javax.swing.GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE)
                    .addComponent(inText, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        statusLabel.setFont(resourceMap.getFont("statusLabel.font")); // NOI18N
        statusLabel.setIcon(resourceMap.getIcon("statusLabel.icon")); // NOI18N
        statusLabel.setText(resourceMap.getString("statusLabel.text")); // NOI18N
        statusLabel.setToolTipText(resourceMap.getString("statusLabel.toolTipText")); // NOI18N
        statusLabel.setName("statusLabel"); // NOI18N

        actionLabel.setFont(resourceMap.getFont("actionLabel.font")); // NOI18N
        actionLabel.setText(resourceMap.getString("actionLabel.text")); // NOI18N
        actionLabel.setName("actionLabel"); // NOI18N

        toolLabel.setFont(resourceMap.getFont("toolLabel.font")); // NOI18N
        toolLabel.setText(resourceMap.getString("toolLabel.text")); // NOI18N
        toolLabel.setName("toolLabel"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(inPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(outScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1006, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(toolLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 834, Short.MAX_VALUE)
                            .addComponent(actionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 834, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(actionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(toolLabel)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 409, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(inPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void sendInput(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendInput
    if (!this.running || !this.action.getTool().isInStream()) {
        return;
    }

    String input = inText.getText().trim();
    // 1. Muestra por pantalla.
    outText.append(">" + input + "\n");
    inText.setText(null);

    // 2. Envío al socket.
    ByteBuffer buffer = ByteBuffer.allocate(input.length() + 1);
    buffer.put(input.getBytes());
    buffer.rewind();

    try {
        inSocket.write(buffer);
    } catch (IOException e) {
        input = "! (Not send)";
        outText.append(input + "\n");
    }

}//GEN-LAST:event_sendInput

private void inTextAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inTextAction
    sendInput(null);
}//GEN-LAST:event_inTextAction

private void closeAction(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeAction
    
    if (!running) return;
    
    // 1. Aviso al laboratorio.
    if (!context.notificationManager().stopRequest(ticket)){
        return;
    }
    
    // 2. Cierre de ventana.
    final SwingWorker worker = new SwingWorker() {

        @Override
        protected Object doInBackground() throws Exception {
            action.getTool().status(ToolStatus.TAKED);
            toolTab.changeStatus();
            close();
            return null;
        }
    };

    worker.execute();
    this.running = false;
}//GEN-LAST:event_closeAction
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel actionLabel;
    private javax.swing.JPanel inPanel;
    private javax.swing.JTextField inText;
    private javax.swing.JScrollPane outScrollPane;
    private javax.swing.JTextArea outText;
    private javax.swing.JButton sendButton;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JLabel toolLabel;
    // End of variables declaration//GEN-END:variables

    /**
     * Hilo de entrada.
     */
    private class OutputThread extends Thread {

        // Constantes:
        /** Tiempo de retardo. */
        public final static int DELAY = 500;
        /** Tamaño del buffer de lectura y escritura. */
        public final static int BUFFER_SIZE = 4096;
        // Atributos:
        /** Indica si ha habido una petición de parada. */
        private boolean stopped;

        // Constructor:
        /**
         * Constructor del hilo.
         */
        public OutputThread() {
            super("Output (" + action.getTool().getId() + ")");
            this.stopped = false;
        }

        /**
         * Método de ejecución principal.
         */
        @Override
        public void run() {

            Selector selector;
            SocketChannel s;
            int n = 0;
            Iterator<SelectionKey> it;
            ByteBuffer buffer = null;

            // 1. Iniciación.
            try {
                selector = Selector.open();
                outSocket.register(selector, SelectionKey.OP_READ);
            } catch (IOException e) {
                outText.append("ERROR!");
                try {
                    outSocket.close();
                } catch (IOException ex) {
                }
                return;
            }

            while (true) {

                // 2. Parada.
                synchronized (this) {
                    if (this.stopped) {
                        try {
                            outSocket.close();
                            selector.close();
                        } catch (IOException ex) {
                        }
                        break;
                    }
                }

                // 3. Lectura.
                try {
                    if (selector.select(DELAY) <= 0) {
                        continue;
                    }
                } catch (IOException e) {
                    this.stopped = true;
                    continue;
                }

                it = (selector.selectedKeys()).iterator();


                if (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();

                    // 3.1 Lectura de la salida.
                    if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {

                        s = (SocketChannel) key.channel();
                        try {
                            buffer = ByteBuffer.allocate(BUFFER_SIZE);
                            buffer.clear();
                            n = s.read(buffer);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                            it.remove();
                            continue;
                        }

                        // 3.2 Escritura en la pantalla.
                        if (n > 0) {
                            buffer.rewind();
                            byte[] b = new byte[n];
                            buffer.get(b);
                            outText.append(new String(b).trim() + "\n");
                        }

                    }

                    it.remove();
                }

            }

        }

        /**
         * Parada del hilo.
         */
        public synchronized void stopOutput() {
            this.stopped = true;
        }
    }
}
