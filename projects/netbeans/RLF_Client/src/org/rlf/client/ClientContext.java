/*
 * Contexto de la aplicación.
 */
package org.rlf.client;

import java.util.HashMap;
import org.rlf.client.data.Action;
import org.rlf.client.data.Tool;
import org.rlf.client.view.ClientView;
import org.rlf.client.view.exec.ActionView;

/**
 * Contiene las variables de autentificación y el estado de la aplicación.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class ClientContext {
    
    // Enum:
    /** Estado de la aplicación. */
    public enum ClientStatus {
        /** No logueado. */
        NOTLOGGED,
        /** Logueado. */
        LOGGED,
        /** Con herramientas adquiridas. */
        TAKED;
    };
    
    // Atributos:
    /** Token de acceso del usuario. */
    private String accessToken;
    /** Token de uso del usuario. */
    private String useToken;
    /** Indica el estado de la aplicación. */
    private ClientStatus status;
    /** Lista de herramientas permitidas al usuario. */
    private HashMap<String, Tool> tools;
    /** Vista principal de la aplicación. */
    private ClientView mainView;
    /** Tiempo de uso del usuario. */
    private int useTime;
    /** Hilo de notificaciones principal. */
    private NotificationManager notificationManager;
    /** Lista con las acciones en ejecución actuales. */
    private HashMap<Action, ActionView> runningActions;
    
    /** Instancia del contexto. */
    private static ClientContext instance = null;
    
    // Constructor:
    /**
     * Constructor del contexto.
     */
    private ClientContext(){
        this.accessToken = null;
        this.useToken = null;
        this.status = ClientStatus.NOTLOGGED;
        this.runningActions = new HashMap<Action, ActionView>();
    }
    
    /**
     * Obtiene la instancia del contexto.
     * @return Contexto.
     */
    public static ClientContext Instance(){
        if (instance == null){
            instance = new ClientContext();
        }
        return instance;
    }
    
    // Métodos getters y setters:
    /**
     * Obtiene el tiempo restante que le queda al usuario.
     * @return Tiempo en minutos.
     */
    public int getUseTime() {
        return useTime;
    }

    /**
     * Establece un nuevo tiempo.
     * @param useTime Tiempo en minutos.
     */
    public void setUseTime(int useTime) {
        this.useTime = useTime;
    }
    
    
    /**
     * Obtiene la lista de herramientas en la aplicación.
     * @return Lista de herramientas.
     */
    public HashMap<String, Tool> getTools() {
        return tools;
    }
    
    /**
     * Obtiene la lista de ejecuciones.
     * @return Lista.
     */
    public HashMap<Action, ActionView> getRunningActions(){
        return this.runningActions;
    }

    /**
     * Establece la lista de herramientas que el usuario puede utilizar.
     * @param tools Herramientas.
     */
    public void setTools(HashMap<String, Tool> tools) {
        this.tools = tools;
    }
    
    /**
     * Obtiene el token de acceso.
     * @return Token de acceso. Puede ser null.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Establece el token de acceso del usuario.
     * @param accessToken Token de acceso.
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Indica el estado de la aplicación.
     * @return Estado.
     */
    public ClientStatus status() {
        return status;
    }

    /**
     * Establece el estado de la aplicación.
     * @param status Nuevo estado.
     */
    public void status(ClientStatus status) {
        this.status = status;
    }

    /**
     * Obtiene el token de uso del usuario para ejecutar acciones.
     * @return Token de uso.
     */
    public String getUseToken() {
        return useToken;
    }

    /**
     * Establece el nuevo token de uso del usuario para ejecutar acciones.
     * @param useToken Token de uso.
     */
    public void setUseToken(String useToken) {
        this.useToken = useToken;
    }

    /**
     * Obtiene la ventana principal de la aplicación.
     * @return Ventana.
     */
    public ClientView getMainView() {
        return mainView;
    }

    /**
     * Establece la ventana principal.
     * @param mainView Ventana.
     */
    public void setMainView(ClientView mainView) {
        this.mainView = mainView;
    }
    
    /**
     * Establece el hilo gestor de notificaciones.
     * @param notificationManager Gestor.
     */
    public void notificationManager(NotificationManager notificationManager){
        this.notificationManager = notificationManager;
    }
    
    /**
     * Obtiene el hilo gestor de notificaciones.
     * @return Gestor.
     */
    public NotificationManager notificationManager(){
        return this.notificationManager;
    }
    
    
    
}
