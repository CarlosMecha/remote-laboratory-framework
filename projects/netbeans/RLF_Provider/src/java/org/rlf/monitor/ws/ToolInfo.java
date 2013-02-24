/*
 * Estado de una herramienta.
 */
package org.rlf.monitor.ws;

/**
 * Información de una herramienta básico para un monitor.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class ToolInfo {

    // Enum:
    /** Estado de las herramientas. */
    public enum ToolStatus {

        /** La herramienta está en línea y disponible. */
        ONLINE,
        /** La herramienta está actualmente en uso por algún usuario. */
        INUSE,
        /** La herramienta está desconectada. */
        OFFLINE
    };
    // Atributos:
    /** Nombre de la herramienta. */
    private String name;
    /** Identificador. */
    private String id;
    /** Estado. */
    private ToolStatus status;

    // Constructor:
    /**
     * Constructor por defecto.
     */
    public ToolInfo(){
        
    }
    
    /**
     * Constructor de la información.
     * @param id Identificador de la herramienta.
     * @param name Nombre de la herramienta.
     * @param status Estado actual.
     */
    public ToolInfo(String id, String name, ToolStatus status) {
        this.name = name;
        this.id = id;
        this.status = status;
    }

    // Métodos getters:
    /**
     * Obtiene el identificador de la herramienta.
     * @return Identificador único.
     */
    public String getId() {
        return id;
    }

    /** 
     * Obtiene el nombre de la herramienta.
     * @return Nombre.
     */
    public String getName() {
        return name;
    }

    /**
     * Obtiene el estado actual de la herramienta.
     * @return Estado.
     */
    public ToolStatus getStatus() {
        return status;
    }
    
    // Métodos setters:
    /**
     * Incluye el identificador de la herramienta.
     * @param id Identificador.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Modifica el nombre de la herramienta.
     * @param name Nombre.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Cambia el estado de la herramienta.
     * @param status Estado.
     */
    public void setStatus(ToolStatus status) {
        this.status = status;
    }
    
}
