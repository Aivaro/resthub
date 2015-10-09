package net.resthub.server.app;

import net.resthub.server.exception.ServerErrorException;
import net.resthub.server.table.ServerTable;
import net.resthub.server.table.TableId;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Options;

public class Tables extends ServerBaseResource {

    private String namespace = null;
    
    @Override
    public void doInit() {
        super.doInit();
        this.namespace = super.getAttr(String.class, "tableNs");
    }
    
    @Options
    public void define() {
        addHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        addHeader("Access-Control-Allow-Headers", "Content-Type");
        addHeader("Content-Type", "application/json");
    }

    @Get
    public void describe() {
        try {
            JSONObject ret = new JSONObject();
            for (ServerTable tmd : mf.getTables()) {
                TableId id = tmd.getId();
                if (namespace != null) {
                    if (namespace.equals(id.getNamespace())) {
                        ret.put(id.getName(), tmd.getReference("table", getHostRef()));
                    }
                } else {
                    if (!ret.has(id.getNamespace())) {
                        ret.put(id.getNamespace(), new JSONObject());
                    }
                    JSONObject nso = (JSONObject) ret.get(id.getNamespace());
                    nso.put(id.getName(), tmd.getReference("table", getHostRef()));
                }
            }
            getResponse().setEntity(new JsonRepresentation(ret));
            
        } catch (JSONException ex) {
            throw new ServerErrorException(ex);
        }
    }
    
}
