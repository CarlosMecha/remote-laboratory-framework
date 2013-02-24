/*
 * Herramienta.
 */
package org.rlf.client.data;

import java.util.HashMap;
import org.rlf.client.view.ToolTab;
import org.rlf.client.view.exec.ActionView;

/**
 * Representación de los datos de una herramienta.
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class Tool {
 
    // Enum:
    /** Estados de la herramienta. */
    public enum ToolStatus {
      /** Herramienta reservada por otro usuario. */
      RESERVED,
      /** Herramienta libre. */
      FREE,
      /** Herramienta reservada por el usuario. */
      TAKED,
      /** Herramienta en ejecución. */
      EXEC
    };
    
    // Atributos:
    /** Identificador. */
    private String id;
    /** Nombre. */
    private String name;
    /** Descripción. */
    private String description;
    /** Entrada. */
    private boolean inStream;
    /** Salida. */
    private boolean outStream;
    /** Herramienta de datos. */
    private boolean dataTool;
    /** Parámetros. */
    private HashMap<String, Parameter> parameters;
    /** Acciones. */
    private HashMap<String, Action> actions;
    /** Indica el estado de la herramienta. */
    private ToolStatus status;
    /** Laboratorio al que pertenece la herramienta. */
    private Lab lab;
    /** Instancia de ejecución. */
    private ActionView running;
    /** Componente gráfico que representa la herramienta. */
    private ToolTab component;
    
    // Constructor:
    /**
     * Crea una nueva herramienta.
     * @param id Identificador.
     * @param name Nombre.
     * @param description Descripción.
     * @param inStream Indica si tiene flujo de entrada.
     * @param outStream Indica si tiene flujo de salida.
     * @param dataTool Indica si es una herramienta de datos.
     */
    public Tool(String id, String name, String description, boolean inStream, boolean outStream, boolean dataTool) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.inStream = inStream;
        this.outStream = outStream;
        this.dataTool = dataTool;
        this.parameters = new HashMap<String, Parameter>();
        this.actions = new HashMap<String, Action>();
        this.status = ToolStatus.FREE;
        this.lab = null;
    }
    
    
    // Métodos getters:

    /**
     * Obtiene el laboratorio que contiene la herramienta.
     * @return El laboratorio. Puede ser null si no se ha asignado aún.
     */
    public Lab lab() {
        return lab;
    }

    /**
     * Asigna el laboratorio a la herramienta.
     * @param lab Laboratorio.
     */
    public void lab(Lab lab) {
        this.lab = lab;
    }
    
    /**
     * Obtiene la instancia de ejecución. Puede estar en espera o ejecutando.
     * @return Componente de ejecución. Puede ser null.
     */
    public ActionView running(){
        return this.running;
    }
    
    /**
     * Establece la instancia de ejecución.
     * @param view Componente.
     */
    public void running(ActionView view){
        this.running = view;
    }
    
    /**
     * Obtiene la instancia del componente gráfico que representa la herramienta.
     * @return Componente.
     */
    public ToolTab component(){
        return this.component;
    }
    
    /**
     * Establece el componente gráfico.
     * @param tab Componente.
     */
    public void component(ToolTab tab){
        this.component = tab;
    }
    
    /**
     * Indica el estado de la herramienta.
     * @return Estado.
     */
    public ToolStatus status(){
        return this.status;
    }
    
    /**
     * Modifica el estado de la herramienta.
     * @param status Nuevo estado.
     */
    public void status(ToolStatus status){
        this.status = status;
    }
    
    /**
     * Conjunto de acciones que puede realizar la herramienta.
     * @return Acciones.
     */
    public HashMap<String, Action> getActions() {
        return actions;
    }

    /**
     * Indica si es una herramienta de datos.
     * @return Verdadero si lo es.
     */
    public boolean isDataTool() {
        return dataTool;
    }

    /**
     * Obtiene la descripción de la herramienta.
     * @return Descripción.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Identificador de la herramienta.
     * @return Identificador.
     */
    public String getId() {
        return id;
    }

    /**
     * Indica si tiene flujo de entrada.
     * @return Verdadero si lo tiene.
     */
    public boolean isInStream() {
        return inStream;
    }

    /**
     * Nombre de la herramienta.
     * @return Nombre.
     */
    public String getName() {
        return name;
    }

    /**
     * Indica si tiene flujo de salida.
     * @return Verdadero si tiene.
     */
    public boolean isOutStream() {
        return outStream;
    }

    /**
     * Obtiene la lista de parámetros de la herramienta.
     * @return Parámetros.
     */
    public HashMap<String, Parameter> getParameters() {
        return parameters;
    }
    
    /**
     * Añade un nuevo parámetro a la herramienta.
     * @param parameter Nuevo parámetro.
     */
    public void addParameter(Parameter parameter){
        this.parameters.put(parameter.getName(), parameter);
    }
    
    /**
     * Añade una nueva acción a la herramienta.
     * @param action Nueva acción.
     */
    public void addAction(Action action){
        this.actions.put(action.getName(), action);
    }
    
    
}
