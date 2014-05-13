package net.resthub.server.app;

import javax.inject.Inject;

import net.resthub.server.exporter.CountExporter;
import net.resthub.server.factory.ResourceFactory;
import net.resthub.server.handler.CountHandler;
import net.resthub.server.query.Query;

import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Options;
import org.restlet.resource.ResourceException;

/**
 * Count
 * @author valdo
 */
public class Count extends ServerBaseResource {

    @Inject
    private ResourceFactory rf;
    
    private CountHandler handler;

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        Query q = getQueryMd(true);
        this.handler = rf.createCountHandler(q, getQuery());
    }
    
    @Options
    public void define() {
        addHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        addHeader("Access-Control-Allow-Headers", "Content-Type");
        addHeader("Content-Type", "text/plain");
    }

    @Get
    public void count() throws ResourceException {
        CountExporter de = qf.getExporter(handler);
        getResponse().setEntity(new StringRepresentation(de.getValue().getValue().toString()));     
    }
}