package net.resthub.server.app;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import net.resthub.exception.QueryException;
import net.resthub.server.cache.CacheStats;
import net.resthub.server.converter.CSVConverter;
import net.resthub.server.converter.DataConverter;
import net.resthub.server.converter.JSONConverter;
import net.resthub.server.converter.XMLConverter;
import net.resthub.server.exception.ClientErrorException;
import net.resthub.server.exception.ServerErrorException;
import net.resthub.server.exporter.DataExporter;
import net.resthub.server.handler.DataHandler;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Options;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.google.common.collect.Maps;

/**
 * Data
 * @author valdo
 */
public class Data extends PagedData {
    
    private final static Map<MediaType, DataConverter> CONVERTERS = Maps.newLinkedHashMap();
    private final static List<MediaType> SUPPORTED_TYPES;
    static {
        CONVERTERS.put(MediaType.APPLICATION_JSON, new JSONConverter());
        CONVERTERS.put(MediaType.APPLICATION_XML, new XMLConverter());
        CONVERTERS.put(MediaType.TEXT_XML, new XMLConverter());
        CONVERTERS.put(MediaType.TEXT_CSV, new CSVConverter());
        CONVERTERS.put(MediaType.TEXT_PLAIN, new CSVConverter());
        SUPPORTED_TYPES = new ArrayList<>(CONVERTERS.keySet());
    }
    
    private Boolean printColumns;

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();       
        this.printColumns = getParam(Boolean.class, "cols", false);
    }
    
    @Options
    public void define() {
        addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        addHeader("Access-Control-Allow-Headers", "Content-Type");     
        StringBuilder sb = new StringBuilder();
        for (MediaType mt: SUPPORTED_TYPES) {
            sb.append(sb.length() > 0 ? "," : "").append(mt);
        }
        addHeader("Content-Type", sb.toString());
    }
    
    @Post("text")
    public void save(Representation entity) throws ResourceException, IOException {
        if (query != null) {
            getResponse().redirectTemporary(getOriginalRef());
        } else {
            String sql = entity.getText();
            if (sql != null) {
                try {
                    
                    String id = qf.createQuery(sql);
                    String oldId = super.getAttr(String.class, "queryId");
                    String url = getOriginalRef().toString();
                    
                    url = url.replaceFirst("/query/" + oldId + "/", "/query/" + id + "/");
                    
                    getResponse().redirectTemporary(url);
                } catch (QueryException ex) {
                    throw new ClientErrorException(Status.CLIENT_ERROR_BAD_REQUEST, ex.getMessage());
                }
            } else {
                throw new ClientErrorException(Status.CLIENT_ERROR_BAD_REQUEST, "Query missing?");
            }        
        }
    }
    
    @Get
    public void data() throws ResourceException {
        
        // Check media type
        MediaType preferredMediaType = getClientInfo().getPreferredMediaType(SUPPORTED_TYPES);
        if (preferredMediaType == null) {
            throw new ClientErrorException(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE, "Unsupported media types");
        }
        
        // Create query handler
        DataHandler handler = rf.createDataHandler(query, getQuery());
        handler.setPerPage(perPage);
        handler.setPage(page);
        handler.setPrintColumns(printColumns);
        
        CacheStats stats = handler.getCacheStats();
        
        // Process "If-Modified-Since"
        if (respondNotModified(stats)) {
            addExpiresHeader(stats);
            return;
        }

        // Finally, return data
        try {
            
            DataExporter dexp = qf.getExporter(handler);
            getResponse().setEntity(CONVERTERS.get(preferredMediaType).convert(handler, getHostRef(), dexp.getValue()));
            addExpiresHeader(stats);

        } catch (Exception ex) {
            if (ResourceException.class.isAssignableFrom(ex.getClass())) {
                throw (ResourceException) ex;
            }
            throw new ServerErrorException(ex);
        }
  
    }

}