/*
 * Parámetro.
 */
package org.rlf.client.data;

/**
 * Corresponde a un parámetro de una herramienta.
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class Parameter {
    
    // Atributos:
    /** Nombre del parámetro. */
    private String name;
    /** Descripción del parámetro. */
    private String description;
    /** Valor del parámetro. */
    private String value;
    /** Tipo de datos. */
    private String dtype;

    // Constructor:
    /**
     * Constructor del parámetro.
     * @param name Nombre.
     * @param description Descripción.
     * @param dtype Tipo de datos.
     */
    public Parameter(String name, String description, String dtype) {
        this.name = name;
        this.description = description;
        this.dtype = dtype;
    }

    // Métodos getters y setters:
    /**
     * Obtiene la descripción del parámetro.
     * @return Descripción.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Obtiene el nombre del parámetro.
     * @return Nombre.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Obtiene el tipo de datos del parámetro.
     * @return Tipo.
     */
    public String getDType(){
        return dtype;
    }

    /**
     * Obtiene el valor del parámetro.
     * @return Valor.
     */
    public String getValue() {
        return value;
    }

    /**
     * Modifica el valor del parámetro.
     * @param value Valor.
     */
    public void setValue(String value) {
        this.value = value;
    }
    
}
