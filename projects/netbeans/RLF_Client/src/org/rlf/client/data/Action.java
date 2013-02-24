/*
 * Acción.
 */
package org.rlf.client.data;

import java.util.HashMap;

/**
 * Acción concreta de una herramienta.
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class Action {
    
    // Atributos.
    /** Herramienta. */
    private Tool tool;
    /** Nombre de la acción. */
    private String name;
    /** Descripción. */
    private String description;
    /** Descripción de los sockets de la acción. */
    private HashMap<String, String> sockets;
    /** Lista de parámetros asociados de entrada. */
    private HashMap<String, Parameter> inParameters;
    /** Lista de parámetros asociados de salida. */
    private HashMap<String, Parameter> outParameters;
    /** Lista de parámetros asociados de entrada y salida. */
    private HashMap<String, Parameter> inOutParameters;
    
    
    // Constructor:
    /**
     * Constructor de la acción.
     * @param tool Herramienta.
     * @param name Nombre de la acción.
     * @param description Descripción.
     */
    public Action(Tool tool, String name, String description) {
        this.tool = tool;
        this.name = name;
        this.description = description;
        this.inParameters = new HashMap<String, Parameter>();
        this.outParameters = new HashMap<String, Parameter>();
        this.inOutParameters = new HashMap<String, Parameter>();
        this.sockets = new HashMap<String, String>();
    }
    
    
    // Métodos getters:
    /**
     * Descripción de la acción.
     * @return Descripción.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Lista de parámetros de entrada y salida.
     * @return Lista.
     */
    public HashMap<String, Parameter> getInOutParameters() {
        return inOutParameters;
    }

    /**
     * Lista de parámetros de entrada.
     * @return Parámetros.
     */
    public HashMap<String, Parameter> getInParameters() {
        return inParameters;
    }

    /**
     * Nombre de la acción.
     * @return Nombre.
     */
    public String getName() {
        return name;
    }

    /**
     * Parámetros de salida.
     * @return Lista.
     */
    public HashMap<String, Parameter> getOutParameters() {
        return outParameters;
    }

    /**
     * Descripción de los sockets a utilizar.
     * @return Sockets.
     */
    public HashMap<String, String> getSockets() {
        return sockets;
    }

    /**
     * Herramienta.
     * @return Herramienta.
     */
    public Tool getTool() {
        return tool;
    }
    
    /**
     * Añade un socket a la acción.
     * @param port Puerto asociado al socket.
     * @param protocol Protocolo.
     * @param type Indica si es de datos, de video o de gráficas.
     * @param mode Indica si es de lectura, escritura o ambos.
     */
    public void addSocket(String port, String protocol, String type, String mode){
        sockets.put(port, ":" + port + " (" + protocol + " - " + type + "[" + mode + "])");
    }
    
    /**
     * Añade un parámetro de entrada.
     * @param parameter Parámetro.
     */
    public void addInParameter(Parameter parameter){
        inParameters.put(parameter.getName(), parameter);
    }
    
    /**
     * Añade un parámetro de salida.
     * @param parameter Parámetro.
     */
    public void addOutParameter(Parameter parameter){
        outParameters.put(parameter.getName(), parameter);
    }
    
    /**
     * Añade un parámetro de entrada y salida.
     * @param parameter Parámetro.
     */
    public void addInOutParameter(Parameter parameter){
        inOutParameters.put(parameter.getName(), parameter);
    }
    
    
}
