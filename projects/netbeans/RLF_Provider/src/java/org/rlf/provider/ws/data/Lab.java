/*
 * Laboratorio
 */
package org.rlf.provider.ws.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.LinkedList;

/**
 * Representación de los datos de un laboratorio almacenado en la base de datos.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class Lab {
    
    // Atributos:
    /** Nombre del laboratorio. */
    private String name;
    /** Máquina. */
    private String host;
    /** Puerto de recepción de peticiones. */
    private int port;
    /** Puerto del cliente. */
    private int clientPort;
    /** Puerto de notificaciones. */
    private int notificationPort;
    /** Lista con las herramientas a su cargo. */
    private LinkedList<Integer> tools;
    
    // Constructor:

    /**
     * Constructor del laboratorio.
     * 
     * @param name Nombre.
     * @param host Máquina.
     * @param port Puerto.
     * @param clientPort Puerto de conexión con el cliente.
     * @param notificationPort Puerto de notificaciones.
     */
    public Lab(String name, String host, int port, int clientPort, int notificationPort) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.clientPort = clientPort;
        this.notificationPort = notificationPort;
        this.tools = new LinkedList<Integer>();
    }

    /**
     * Puerto de conexión con el cliente.
     * 
     * @return Número de puerto.
     */
    public int getClientPort() {
        return clientPort;
    }

    /**
     * Máquina del laboratorio.
     * @return Dirección.
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
     * Puerto de notificaciones.
     * @return Número de puerto.
     */
    public int getNotificationPort() {
        return notificationPort;
    }

    /**
     * Puerto del laboratorio.
     * @return Número de puerto.
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Lista con las herramientas pertenecientes al laboratorio.
     * @return Lista de identificadores.
     */
    public LinkedList<Integer> getTools(){
        return tools;
    }
    
    /**
     * Añade una herramienta al laboratorio.
     * @param tool Identificador.
     */
    public void addTool(int id){
        tools.add(id);
    }
    
    /**
     * Indica si el laboratorio contiene una herramienta dada.
     * @param tool Identificador.
     * @return Verdadero si la contiene.
     */
    public boolean containsTool(int id){
        return tools.contains(id);
    }
    
    /**
     * Convierte la representación del laboratorio en Json.
     * @return Json con la información.
     */
    public JsonObject toJson(){
        
        JsonObject json = new JsonObject();
        JsonArray ids = new JsonArray();
        json.addProperty("name", name);
        json.addProperty("host", host);
        json.addProperty("request", clientPort);
        json.addProperty("notification", notificationPort);
        
        for (Integer id : tools){
            ids.add(new JsonPrimitive(id.toString()));
        }
        json.add("tools", ids);
        
        return json;
    }
    
}
