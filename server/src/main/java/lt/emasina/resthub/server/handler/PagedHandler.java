package lt.emasina.resthub.server.handler;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lt.emasina.resthub.server.query.Query;

import org.restlet.data.Form;
import org.restlet.resource.ResourceException;

import lt.emasina.resthub.server.cache.CcBase;
import lt.emasina.resthub.server.exporter.Exporter;

public abstract class PagedHandler<C extends CcBase<?>, E extends Exporter<C>> extends Handler<C,E> {
	
    @Getter
    @Setter
    private Integer perPage;
    
    @Getter
    @Setter
    private Integer page;
    
    public PagedHandler(Query query, Form form) throws ResourceException {
    	super(query, form);
    }
    
    @Override
    protected List getIdParts() {
        List parts = new ArrayList();
        parts.add(perPage);
        parts.add(page);
        return parts;
    }
    
}
