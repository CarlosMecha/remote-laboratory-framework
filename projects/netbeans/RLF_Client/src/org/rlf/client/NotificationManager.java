/*
 * Notificador.
 */
package org.rlf.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import org.rlf.client.data.Action;
import org.rlf.client.data.Lab;
import org.rlf.client.data.Parameter;
import org.rlf.client.data.Tool;
import org.rlf.client.view.exec.ActionView;
import org.rlf.net.RLF_NetHelper;
import org.rlf.net.message.RLF_NetMessage;
import org.rlf.net.message.RLF_NetMessageID;

/**
 * Hilo de que comunica las notificaciones de los laboratorios a la aplicación.
 *
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class NotificationManager extends Thread {

    // Constantes:
    /** Tiempo de muestreo. */
    public final static int DELAY = 500;
    // Atributos:
    /** Contexto de la aplicación. */
    private ClientContext context;
    /** Lista de laboratorios conectados. */
    private HashMap<String, Lab> labs;
    /** Lista con las acciones en ejecución. */
    private HashMap<String, ActionView> actions;
    /** Petición de parada. */
    private boolean stopped;

    // Constructor:
    /**
     * Construye el notificador.
     * @param context Contexto.
     * @param labs Lista de laboratorios conectados.
     */
    public NotificationManager(ClientContext context, HashMap<String, Lab> labs) {
        super("NotificationManager");
        this.context = context;
        this.labs = labs;
        this.stopped = false;
        this.actions = new HashMap<String, ActionView>();
    }

    // Métodos varios.
    /**
     * Parada general del hilo. Desconecta los laboratorios.
     */
    public synchronized void stopManager() {
        this.stopped = true;
    }

    /**
     * Envía una solicitud de ejecución al laboratorio en concreto. (Sincronizado
     * en la llamada.)
     * @param action Acción a ejecutar.
     * @return Verdadero si ha podido conseguirlo.
     */
    public boolean sendRequest(Action action) {

        Lab lab = action.getTool().lab();
        if (lab == null || !lab.isConnected()) {
            return false;
        }

        RLF_NetHelper net = new RLF_NetHelper();
        RLF_NetMessage msg, reply;
        HashMap<String, String> attrs = new HashMap<String, String>();

        // 1. Construcción del mensaje.
        attrs.put("tool", action.getTool().getId());
        attrs.put("action", action.getName());
        JsonObject object = new JsonObject();
        for (Parameter p : action.getInParameters().values()) {
            String value = p.getValue();
            if (value == null) {
                value = new String();
            }
            object.addProperty(p.getName(), value);
        }
        for (Parameter p : action.getInOutParameters().values()) {
            String value = p.getValue();
            if (value == null) {
                value = new String();
            }
            object.addProperty(p.getName(), value);
        }
        attrs.put("parameters", new Gson().toJson(object));
        msg = new RLF_NetMessage(RLF_NetMessageID.EXEC, attrs);

        // 2. Envío.
        if (!net.sendMessage(msg, lab.getRequestSocket())) {
            return false;
        }

        // 3. Recepción de confirmación.
        reply = net.reciveMessage(lab.getRequestSocket());
        if (reply == null || reply.getId() != RLF_NetMessageID.OK) {
            return false;
        }

        return true;

    }

    /**
     * Envía una señal de parada a la acción ejecutada actualmente. No puede realizarse
     * con una acción que está en espera o que ya se haya acabado.
     * @param ticket Ticket de ejecución.
     * @return Verdadero si se ha confirmado la acción de parada.
     */
    public synchronized boolean stopRequest(String ticket) {

        if (!actions.containsKey(ticket)) {
            return false;
        }

        Action action = actions.get(ticket).getAction();

        Lab lab = action.getTool().lab();
        if (lab == null || !lab.isConnected()) {
            return false;
        }

        // 1. Envío de petición.
        RLF_NetHelper net = new RLF_NetHelper();
        RLF_NetMessage msg, reply;
        HashMap<String, String> attrs = new HashMap<String, String>();
        attrs.put("ticket", ticket);
        msg = new RLF_NetMessage(RLF_NetMessageID.EXEC_FINISH, attrs);

        if (!net.sendMessage(msg, lab.getRequestSocket())) {
            return false;
        }

        // 3. Recepción de confirmación.
        reply = net.reciveMessage(lab.getRequestSocket());
        if (reply == null || reply.getId() != RLF_NetMessageID.OK) {
            return false;
        }

        actions.remove(ticket);

        return true;

    }

    /**
     * Ejecución.
     */
    @Override
    public void run() {

        SocketChannel socket;
        Selector selector;
        RLF_NetHelper net = new RLF_NetHelper();

        try {
            Thread.sleep(DELAY);
            selector = Selector.open();
        } catch (Exception e) {
            // Error general.
            return;
        }

        // 1. Conexión de cada laboratorio.
        for (Lab lab : labs.values()) {
            if (lab.connect(context)) {
                socket = lab.getNotificationSocket();
                try {
                    socket.register(selector, SelectionKey.OP_READ, lab.getName());
                } catch (ClosedChannelException ex) {
                    continue;
                }
            }
        }

        while (true) {

            synchronized (this) {
                if (this.stopped) {
                    try {
                        selector.close();
                    } catch (IOException ex) {
                    }

                    // 1. Desconexión de las herramientas.
                    for (Tool t : context.getTools().values()) {
                        if (t.running() != null) {
                            t.running().forceStop();
                        } else {
                            t.status(Tool.ToolStatus.RESERVED);
                            t.component().changeStatus();
                        }
                        t.lab(null);
                    }

                    // 2. Desconexión de cada laboratorio.
                    for (Lab lab : labs.values()) {
                        lab.disconnect();
                    }

                    break;
                }
            }

            // 3. Selección.
            try {
                if (selector.select(DELAY) <= 0) {
                    continue;
                }
            } catch (IOException e) {
                this.stopped = true;
                continue;
            }

            Iterator<SelectionKey> it = (selector.selectedKeys()).iterator();

            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();

                // 3.1 Se leen las peticiones.
                if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                    socket = (SocketChannel) key.channel();
                    Lab lab = labs.get((String) key.attachment());

                    RLF_NetMessage msg = net.reciveMessage(socket);
                    if (msg == null) {
                        if (!stopped) {
                            stopLab(lab);
                        }
                        key.cancel();
                        it.remove();
                        continue;
                    }
                    
                    synchronized (this) {
                        // 3.2 Se tratan.
                        switch (msg.getId()) {

                            case STOPLAB:
                                stopLab(lab);
                                break;
                            case TIMEOUT:
                                timeout();
                                break;
                            case EXEC:
                                exec(msg.getAttributes());
                                break;
                            case EXEC_ERROR:
                                exec_finish(msg.getAttributes(), true);
                                break;
                            case EXEC_FINISH:
                                exec_finish(msg.getAttributes(), false);
                                break;
                            default:
                                break;

                        }

                    }
                }
                it.remove();
            }


        }
    }

    /**
     * Parada del laboratorio. Avisa a todas las ejecuciones que estén activas.
     * @param lab Laboratorio.
     */
    private void stopLab(Lab lab) {

        if (this.stopped) {
            return;
        }

        // 1. Notificación.
        for (Tool t : context.getTools().values()) {
            if (t.lab() != null && t.lab().equals(lab)) {
                if (t.running() != null) {
                    t.running().stop();
                } else {
                    t.status(Tool.ToolStatus.RESERVED);
                    t.component().changeStatus();
                }
                t.lab(null);
            }
        }

        // 2. Desconexión.
        lab.disconnect();
        labs.remove(lab.getName());

        // 3. Si todos los laboratorios se han desconectado.
        if (labs.isEmpty()) {
            this.stopped = true;
            context.getMainView().notMoreTools();
        }

    }

    /**
     * Envía la señal de que el usuario ha excedido el tiempo máximo.
     */
    private void timeout() {
        if (stopped) {
            return;
        }

        this.stopped = true;
        context.getMainView().timeout();

    }

    /**
     * La ejecución ha comenzado, por lo que se pueden obtener los puertos de 
     * escucha y envío.
     * @param attributes Mensaje enviado.
     */
    private void exec(HashMap<String, String> attributes) {

        // 1. Herramienta.
        Tool tool = this.context.getTools().get(attributes.get("tool"));
        ActionView view = tool.running();
        if (view == null) {
            return;
        }
        String ticket = attributes.get("ticket");

        // 2. Socket de entrada.
        SocketChannel inSocket = null;
        if (tool.isInStream()) {
            int portIn = Integer.parseInt(attributes.get("portIn"));
            try {
                SocketAddress addr = new InetSocketAddress(tool.lab().getHost(),
                        portIn);
                inSocket = SocketChannel.open(addr);
                inSocket.configureBlocking(true);
            } catch (IOException e) {
                view.stop();
                return;
            }
        }

        // 3. Socket de salida.
        SocketChannel outSocket = null;
        if (tool.isOutStream()) {
            try {
                int portOut = Integer.parseInt(attributes.get("portOut"));
                SocketAddress addr = new InetSocketAddress(tool.lab().getHost(),
                        portOut);
                outSocket = SocketChannel.open(addr);
                outSocket.configureBlocking(false);
            } catch (Exception e) {
                view.stop();
                try {
                    inSocket.close();
                } catch (Exception ex) {
                }
                return;
            }
        }

        view.execute(ticket, inSocket, outSocket);
        actions.put(ticket, view);

    }

    /**
     * Fin de la ejecución de una acción.
     * @param attributes Atributos enviados.
     * @param error Indica si la ejecución ha tenido un error.
     */
    private void exec_finish(HashMap<String, String> attributes, boolean error) {

        // 1. Herramienta.
        ActionView view;
        if (attributes.get("tool") == null) {
            view = actions.get(attributes.get("ticket"));
        } else {
            Tool tool = this.context.getTools().get(attributes.get("tool"));
            view = tool.running();

        }

        if (view == null) {
            return;
        }

        if (attributes == null || attributes.isEmpty()) {
            view.stop();
            return;
        }

        String ticket = attributes.remove("ticket");

        if (!error) {
            view.execFinish(attributes);
        } else {
            view.execError(attributes);
        }

        actions.remove(ticket);

    }
}
