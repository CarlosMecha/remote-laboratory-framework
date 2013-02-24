/*
 * Laboratorio en el cliente.
 */
package org.rlf.client.data;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import org.rlf.client.ClientContext;
import org.rlf.net.RLF_NetHelper;
import org.rlf.net.message.RLF_NetMessage;
import org.rlf.net.message.RLF_NetMessageID;

/**
 * Contiene la información de un laboratorio.
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class Lab {

    // Atributos:
    /** Nombre. */
    private String name;
    /** Host del laboratorio. */
    private String host;
    /** Puerto de peticiones. */
    private int requestPort;
    /** Puerto de notificaciones. */
    private int notificationPort;
    /** Indica si el cliente está conectado con el laboratorio. */
    private boolean connected;
    /** Socket de peticiones. */
    private SocketChannel requestSocket;
    /** Socket de notificaciones. */
    private SocketChannel notificationSocket;

    // Constructor:
    /**
     * Constructor del laboratorio.
     * @param name Nombre del laboratorio.
     * @param host Host del laboratorio.
     * @param requestP Puerto de petición.
     * @param notificationP Puerto de notificación.
     */
    public Lab(String name, String host, int requestP, int notificationP) {
        this.name = name;
        this.host = host;
        this.requestPort = requestP;
        this.notificationPort = notificationP;
        this.connected = false;
        this.requestSocket = null;
        this.notificationSocket = null;
    }

    // Métodos getters:
    /**
     * Indica si se ha conectado el laboratorio.
     * @return Verdadero si se ha conectado.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Obtiene la dirección del host del laboratorio.
     * @return Dirección del host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Nombre del laboratorio.
     * @return Nombre.
     */
    public String getName() {
        return name;
    }

    /**
     * Socket de notificaciones.
     * @return Socket conectado y configurado. Puede ser null si aún no se ha conectado.
     */
    public SocketChannel getNotificationSocket() {
        return notificationSocket;
    }

    /**
     * Socket de peticiones.
     * @return Socket conectado y configurado. Puede ser null si aún no se ha conectado.
     */
    public SocketChannel getRequestSocket() {
        return requestSocket;
    }

    // Métodos varios.
    /**
     * Conecta y autentifica con el laboratorio.
     * @param context Contexto de la aplicación.
     * @return Verdadero si ha podido. Falso en caso contrario.
     */
    public boolean connect(ClientContext context) {

        if (connected) {
            return false;
        }

        SocketAddress notificationA = new InetSocketAddress(host,
                this.notificationPort);
        SocketAddress requestA = new InetSocketAddress(host,
                this.requestPort);
        HashMap<String, String> attr = new HashMap<String, String>();
        attr.put("token", context.getUseToken());
        RLF_NetMessage msg = new RLF_NetMessage(RLF_NetMessageID.AUTH, attr);
        RLF_NetMessage reply;
        RLF_NetHelper net = new RLF_NetHelper();

        // 1. Se activa el socket de notificación.
        try {
            notificationSocket = SocketChannel.open(notificationA);
            notificationSocket.configureBlocking(true);
            if (!net.sendMessage(msg, notificationSocket)) {
                throw new Exception();
            }
            if ((reply = net.reciveMessage(notificationSocket)) == null) {
                throw new Exception();
            }
            if (reply.getId() != RLF_NetMessageID.OK) {
                throw new Exception();
            }
            notificationSocket.configureBlocking(false);
        } catch (Exception e) {
            try {
                notificationSocket.close();
            } catch (Exception ex) {
            }
            notificationSocket = null;
            requestSocket = null;
            connected = false;
            return false;
        }

        // 1. Se activa el socket de petición.
        try {
            requestSocket = SocketChannel.open(requestA);
            requestSocket.configureBlocking(true);
            if (!net.sendMessage(msg, requestSocket)) {
                throw new Exception();
            }
            
            if ((reply = net.reciveMessage(requestSocket)) == null) {
                throw new Exception();
            }
            if (reply.getId() != RLF_NetMessageID.OK) {
                throw new Exception();
            }

        } catch (Exception e) {
            try {
                notificationSocket.close();
                requestSocket.close();
            } catch (Exception ex) {
            }
            notificationSocket = null;
            requestSocket = null;
            connected = false;
            return false;
        }
        
        connected = true;
        return true;

    }
    
    /**
     * Desconecta del laboratorio.
     */
    public void disconnect(){
        if (!this.connected) return;
        
        try{
            notificationSocket.close();
        } catch (Exception e){
            
        }
        
        try{
            requestSocket.close();
        } catch (Exception e){
            
        }
    }
}
