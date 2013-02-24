/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rlf.client.provider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import javax.xml.ws.BindingProvider;
import org.rlf.client.data.Tool;
import org.rlf.cipher.RLF_Cipher;
import org.rlf.cipher.dummy.RLF_DummyCipher;
import org.rlf.client.ClientContext;
import org.rlf.client.ClientContext.ClientStatus;
import org.rlf.client.NotificationManager;
import org.rlf.client.data.Action;
import org.rlf.client.data.Lab;
import org.rlf.client.data.Parameter;
import org.rlf.client.data.Tool.ToolStatus;
import org.rlf.log.RLF_Log;
import org.rlf.client.ws.AuthException_Exception;
import org.rlf.client.ws.ConnectionException_Exception;
import org.rlf.client.ws.FormatException_Exception;
import org.rlf.client.ws.Provider;
import org.rlf.client.ws.ToolException_Exception;

/**
 * Controlador de los métodos de acceso al proveedor.
 * 
 * @author Carlos A. Rodríguez Mecha
 * @version 0.1
 */
public class ProviderController {

    // Estáticos:
    /** Puerto de conexión. */
    private static Provider provider = null;
    
    // Atributos:
    /** Contexto de la aplicación. */
    private ClientContext context;

    // Constructor:
    /**
     * Constructor del ocntrolador.
     * @param context Contexto de la aplicación.
     */
    public ProviderController(ClientContext context) {
        this.context = context;
    }

    // Métodos del proveedor.
    /**
     * Loguea al usuario introducido.
     * @param user Nombre de usuario.
     * @param pass Contraseña del usuario.
     * @return Verdadero si ha podido conectarse. Falso si hay un fallo de autentificación.
     * @throws ProviderException Problema al conectar con el servidor.
     */
    public boolean loginRequest(String user, String pass) throws ProviderException {

        if (this.context.status() != ClientStatus.NOTLOGGED) {
            return false;
        }

        RLF_Cipher cipher = new RLF_DummyCipher();
        String hash_pass = cipher.getHash(pass);
        String token = null;

        try {
            token = login(user, hash_pass);
        } catch (ConnectionException_Exception ce) {
            // Fallo general.
            throw new ProviderException();
        } catch (AuthException_Exception ae) {
            this.context.status(ClientStatus.NOTLOGGED);
            this.context.setAccessToken(null);
            return false;
        } catch (Exception e) {
            RLF_Log.Log().severe(e.getLocalizedMessage());
            // Fallo de conexión.
            throw new ProviderException();
        }

        this.context.status(ClientStatus.LOGGED);
        this.context.setAccessToken(token);
        return true;
    }

    /**
     * Desloguea al usuario de la aplicación.
     * @return Verdadero si ha podido desconectarse.
     * @throws ProviderException Problema al conectar con el servidor.
     */
    public boolean logoutRequest() throws ProviderException {

        if (this.context.status() == ClientStatus.NOTLOGGED) {
            return false;
        }
        boolean rsl = false;

        try {
            rsl = logout(this.context.getAccessToken());
        } catch (ConnectionException_Exception ce) {
            // Fallo general.
            throw new ProviderException();
        } catch (AuthException_Exception ae) {
            return false;
        } catch (Exception e) {
            // Fallo de conexión.
            throw new ProviderException();
        }

        this.context.status(ClientStatus.NOTLOGGED);
        this.context.setAccessToken(null);
        this.context.getTools().clear();
        return true;

    }

    /**
     * Obtiene todas las herramientas que el usuario puede utilizar.
     * @return Verdadero si ha podido completar la operación.
     * @throws ProviderException Problema al conectar con el servidor.
     */
    public boolean toolsRequest() throws ProviderException {

        if (this.context.status() != ClientStatus.LOGGED) {
            return false;
        }
        String json = null;

        try {
            json = describeTools(this.context.getAccessToken());
        } catch (ConnectionException_Exception ce) {
            // Fallo general.
            throw new ProviderException();
        } catch (AuthException_Exception ae) {
            return false;
        } catch (Exception e) {
            // Fallo de conexión.
            throw new ProviderException();
        }

        HashMap<String, Tool> tools = new HashMap<String, Tool>();
        JsonParser parser = new JsonParser();
        JsonArray array = null;

        try {
            array = parser.parse(json).getAsJsonArray();
        } catch (Exception e) {
            return false;
        }

        // 1. Se obtiene cada herramienta.
        for (JsonElement element : array) {
            try {
                JsonObject jtool = element.getAsJsonObject();
                String id, name, description;
                boolean data, in, out;
                id = jtool.getAsJsonPrimitive("id").getAsString();
                name = jtool.getAsJsonPrimitive("name").getAsString();
                description = jtool.getAsJsonPrimitive("description").getAsString();
                data = (jtool.getAsJsonPrimitive("data").getAsString().compareToIgnoreCase("true") == 0);
                in = (jtool.getAsJsonPrimitive("in_stream").getAsString().compareToIgnoreCase("true") == 0);
                out = (jtool.getAsJsonPrimitive("out_stream").getAsString().compareToIgnoreCase("true") == 0);
                Tool tool = new Tool(id, name, description, in, out, data);

                // 2. Parámetros.
                JsonArray jparameters = jtool.getAsJsonArray("parameters");
                if (jparameters != null) {
                    for (JsonElement elem : jparameters) {
                        JsonObject jparameter = elem.getAsJsonObject();
                        String pname, pdescription, pdtype;
                        pname = jparameter.get("name").getAsString();
                        pdescription = jparameter.get("description").getAsString();
                        if (jparameter.get("dfl") != null) {
                            pdescription += "\n (dfl: " + jparameter.get("dfl").getAsString() + ")";
                        }
                        if (jparameter.get("max") != null) {
                            pdescription += "\n (max: " + jparameter.get("max").getAsString() + ")";
                        }
                        if (jparameter.get("min") != null) {
                            pdescription += "\n (min: " + jparameter.get("min").getAsString() + ")";
                        }
                        pdtype = jparameter.get("data-type").getAsString();
                        Parameter parameter = new Parameter(pname, pdescription, pdtype);
                        tool.addParameter(parameter);

                    }

                }

                // 3. Acciones.
                JsonArray jactions = jtool.getAsJsonArray("actions");
                for (JsonElement elem : jactions) {
                    JsonObject jaction = elem.getAsJsonObject();
                    String aname, adescription;
                    aname = jaction.get("name").getAsString();
                    if (aname.compareToIgnoreCase("resetter") == 0) {
                        continue;
                    }
                    adescription = jaction.get("description").getAsString();
                    Action action = new Action(tool, aname, adescription);
                    tool.addAction(action);

                    // 3.1 Sockets.
                    JsonArray jsockets = jaction.getAsJsonArray("sockets");
                    if (jsockets != null) {
                        for (JsonElement e : jsockets) {
                            JsonObject jsocket = e.getAsJsonObject();
                            String sport, sprotocol, stype, smode;
                            sport = jsocket.get("port").getAsString();
                            sprotocol = jsocket.get("protocol").getAsString();
                            stype = jsocket.get("type").getAsString();
                            smode = jsocket.get("mode").getAsString();
                            action.addSocket(sport, sprotocol, stype, smode);

                        }
                    }

                    // 3.2 Parámetros.
                    JsonArray jactparameters = jaction.getAsJsonArray("parameters");
                    if (jactparameters != null) {
                        for (JsonElement e : jactparameters) {
                            JsonObject jactparameter = e.getAsJsonObject();
                            String apname, aptype;
                            apname = jactparameter.get("name").getAsString();
                            aptype = jactparameter.get("type").getAsString();
                            Parameter par = tool.getParameters().get(apname);
                            if (par != null) {
                                if (aptype.compareToIgnoreCase("in") == 0) {
                                    action.addInParameter(par);
                                } else if (aptype.compareToIgnoreCase("out") == 0) {
                                    action.addOutParameter(par);
                                } else {
                                    action.addInOutParameter(par);
                                }
                            }
                        }
                    }
                }

                tools.put(tool.getId(), tool);

            } catch (Exception e) {
                continue;
            }
        }

        this.context.setTools(tools);

        return true;


    }

    /**
     * Actualiza los estados de las herramientas que el usuario puede utilizar.
     * @return Verdadero si ha podido completar la operación.
     * @throws ProviderException Problema al conectar con el servidor.
     */
    public boolean updateRequest() throws ProviderException {

        if (this.context.status() != ClientStatus.LOGGED) {
            return false;
        }
        String json = null;

        try {
            json = getStatus(this.context.getAccessToken());
        } catch (ConnectionException_Exception ce) {
            // Fallo general.
            throw new ProviderException();
        } catch (AuthException_Exception ae) {
            return false;
        } catch (Exception e) {
            // Fallo de conexión.
            throw new ProviderException();
        }

        HashMap<String, Tool> tools = this.context.getTools();
        JsonParser parser = new JsonParser();
        JsonArray array = null;

        try {
            array = parser.parse(json).getAsJsonArray();
        } catch (Exception e) {
            throw new ProviderException();
        }

        // 1. Se obtiene cada herramienta.
        for (JsonElement element : array) {
            try {
                JsonObject jtool = element.getAsJsonObject();
                String id = jtool.get("id").getAsString();
                boolean status = (jtool.get("status").getAsString().compareToIgnoreCase("ONLINE") == 0);
                Tool t = tools.get(id);
                if (t != null) {
                    t.status(status ? ToolStatus.FREE : ToolStatus.RESERVED);
                }

            } catch (Exception e) {
                continue;
            }
        }

        return true;

    }

    /**
     * Obtiene las herramientas seleccionadas por el usuario.
     * @param list Lista de identificadores de herramientas.
     * @return Verdadero si ha podido completar la operación. Falso si alguna 
     * de las herramientas no está disponible o ha habido un fallo general.
     * @throws ProviderException Problema al conectar con el servidor.
     */
    public boolean takeRequest(LinkedList<String> list) throws ProviderException {
        
        if (this.context.status() != ClientStatus.LOGGED) {
            return false;
        }
        
        String json = null;
        
        // 1. Creación de la lista.
        JsonArray array = new JsonArray();
        JsonObject reply = new JsonObject();
        JsonParser parser = new JsonParser();
        for (String i : list) {
            array.add(new JsonPrimitive(Integer.parseInt(i)));
        }

        // 2. Petición al proveedor.
        try {
            json = takeTools(this.context.getAccessToken(), new Gson().toJson(array));
        } catch (ConnectionException_Exception ce) {
            // Fallo general.
            throw new ProviderException();
        } catch (AuthException_Exception ae) {
            return false;
        } catch (ToolException_Exception te) {
            return false;
        } catch (Exception e) {
            // Fallo de conexión.
            throw new ProviderException();
        }
        
        // 3. Creación del objeto de respuesta.
        try {
            reply = parser.parse(json).getAsJsonObject();
        } catch (Exception e) {
            throw new ProviderException();
        }
        
        // 4. Obtención del token de uso y del tiempo.
        this.context.setUseToken(reply.get("token").getAsString());
        this.context.setUseTime(reply.get("timeout").getAsInt());
        
        // 5. Obtención de los laboratorios.
        JsonArray jlabs = reply.get("labs").getAsJsonArray();
        HashMap<String, Tool> tools = this.context.getTools();
        HashMap<String, Lab> labs = new HashMap<String, Lab>();
        String lname, lhost;
        int prequest, pnotification;
        for (JsonElement element : jlabs){
            JsonObject jlab = element.getAsJsonObject();
            lname = jlab.get("name").getAsString();
            lhost = jlab.get("host").getAsString();
            prequest = jlab.get("request").getAsInt();
            pnotification = jlab.get("notification").getAsInt();
            Lab lab = new Lab(lname, lhost, prequest, pnotification);
            labs.put(lab.getName(), lab);
            for (JsonElement elem : jlab.get("tools").getAsJsonArray()){
                Tool t = tools.get(elem.getAsString());
                if (t == null) continue;
                t.lab(lab);
                t.status(ToolStatus.TAKED);
            }
            
        }
        
        // 6. Iniciación del hilo de notificaciones.
        NotificationManager manager = new NotificationManager(context, labs);
        context.notificationManager(manager);
        manager.start();
        
        return true;
    }
    
    
    // Métodos remotos:
    /**
     * Obtiene los parámetros de configuración del proveedor del fichero de configuración correspondiente.
     * Sólo se realizará la primera vez que se invoque.
     */
    private static void obtainConf(){
        
        if (provider != null) return;
        
        Properties prop = new Properties();
	FileInputStream in;
        String provider_url = null;

        try {
            in = new FileInputStream(System.getProperty("user.dir", "")
                                + File.separator + "res" + File.separator + "client.conf");
            prop.load(in);
            in.close();
            
            provider_url = prop.getProperty("provider_url");
            org.rlf.client.ws.Provider_Service providerService = new org.rlf.client.ws.Provider_Service();
            provider = providerService.getProviderPort();
            
            
        } catch (Exception e) {
            // Fallo general. Cierre de la aplicación.
            System.exit(-1);
        }
            BindingProvider bindingProvider = (BindingProvider) provider;
            bindingProvider.getRequestContext().put(
            BindingProvider.ENDPOINT_ADDRESS_PROPERTY, provider_url);
            
        
    
        
        if (provider == null){
            // Fallo general. Cierre de la aplicación.
            System.exit(-1);
        }
        
    }
    
    /**
     * Se loguea al usuario.
     * @param user Nombre del usuario.
     * @param hashPass Hash del password.
     * @return Token de acceso.
     * @throws ConnectionException_Exception Fallo de conexión de RLF.
     * @throws AuthException_Exception Fallo en la autentificación.
     */
    private static String login(java.lang.String user, java.lang.String hashPass) throws ConnectionException_Exception, AuthException_Exception {
        obtainConf();
        return provider.login(user, hashPass);
    }
    
    

    /**
     * Se desloguea al usuario. Si está utilizando alguna acción se desconectará.
     * @param auth Token de uso.
     * @return Verdadero si ha podido completar la operación.
     * @throws AuthException_Exception Fallo en la autentificación.
     * @throws ConnectionException_Exception Fallo de conexión de RLF.
     */
    private static Boolean logout(java.lang.String auth) throws AuthException_Exception, ConnectionException_Exception {
        return provider.logout(auth);
    }

    /**
     * Obtiene el permiso de uso de las herramientas introducidas.
     * @param auth Token de acceso.
     * @param array Array en formato Json con los identificadores de las herramientas.
     * @return Objeto con el token de uso y la localización de los laboratorios.
     * @throws FormatException_Exception Formato incorrecto en el array.
     * @throws AuthException_Exception Fallo de autentificación.
     * @throws ToolException_Exception Alguna de las herramientas seleccionadas está siendo usada.
     * @throws ConnectionException_Exception Fallo de conexión de RLF.
     */
    private static String takeTools(java.lang.String auth, java.lang.String array) throws FormatException_Exception, AuthException_Exception, ToolException_Exception, ConnectionException_Exception {
        return provider.takeTools(auth, array);
    }

    /**
     * Obtiene el estado actual de las herramientas a las que puede acceder el usuario.
     * @param auth Token de acceso.
     * @return Array en formato Json con el identificador, nombre y estado de cada herramienta.
     * @throws ConnectionException_Exception Fallo de conexión de RLF.
     * @throws AuthException_Exception Fallo de autentificación.
     */
    private static String getStatus(java.lang.String auth) throws ConnectionException_Exception, AuthException_Exception {
        return provider.getStatus(auth);
    }

    /**
     * Obtiene la descripción de todas las herramientas a las que el usuario puede acceder.
     * @param auth Token de acceso.
     * @return Array en formato Json que contiene las diferentes herramientas.
     * @throws AuthException_Exception Fallo de autentificación.
     * @throws ConnectionException_Exception Fallo de conexión de RLF.
     */
    private static String describeTools(java.lang.String auth) throws AuthException_Exception, ConnectionException_Exception {
        return provider.describeTools(auth);
    }

}
