package net.resthub.server.app;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import net.resthub.server.exception.ClientErrorException;
import net.resthub.util.ResourceInjector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Message;
import org.restlet.Request;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import com.google.inject.Inject;
import net.resthub.server.ServerAppConfig;

/**
 * BaseResource class
 *
 * @author valdo
 */
public abstract class BaseResource extends ServerResource {
    
    @Inject
    private static volatile ResourceInjector memberInjector;
    
    @Inject
    protected ServerAppConfig cfg;
    
    private final AtomicBoolean injected = new AtomicBoolean(false);

    @Override
    protected void doInit() throws ResourceException {
        if (injected.compareAndSet(false, true)) {
            memberInjector.injectMembers(this);
        }
    }
    
    protected boolean isAttr(String name) {
        return getRequest().getAttributes().containsKey(name);
    }

    protected String getAttr(String name) throws ResourceException {
        return getAttr(String.class, name);
    }

    protected <T> T getAttr(Class<T> clazz, String name) throws ResourceException {
        String str = (String) getRequest().getAttributes().get(name);
        return convertValue(clazz, str);
    }

    protected <T> T getParam(Class<T> clazz, String name, T defaultValue) throws ResourceException {
        T v = getParam(clazz, name);
        return v == null ? defaultValue : v;
    }
    
    protected <T> T getParam(Class<T> clazz, String name) throws ResourceException {
        String str = (String) getQuery().getFirstValue(name);
        return convertValue(clazz, str);
    }
    
    protected <T> T getHeaderValue(Class<T> clazz, String name) throws ResourceException {
        return getHeaderValue(getRequest(), clazz, name);
    }

    protected void injectResourceData(Object o) {
        memberInjector.injectMembers(o);
    }

    public URL getReference(Object... pathElement) {
        return cfg.getReference(getHostRef(), null, pathElement);
    }
    
    public void addHeader(String name, String value) {
        BaseResource.addHeader(getResponse(), name, value);
    }

    /**
     * Static fields and methods
     */
    
    private static final String UTF8 = "UTF-8";
    private static final SimpleDateFormat[] DATE_FORMATS = {
        new SimpleDateFormat("EEE,dd MMM yyyy HH:mm:ss zzz"), 
        new SimpleDateFormat("EEEEEE,dd-MMM-yy HH:mm:ss zzz"),
        new SimpleDateFormat("EEE MMM  dd HH:mm:ss yyyy"),
        new SimpleDateFormat("y-M-d"),
        new SimpleDateFormat("y-M-d H:m:s")
    };
    
    @SuppressWarnings("unchecked")
    public static <T> T getHeaderValue(Request request, Class<T> clazz, String name) throws ResourceException {
        Series<Parameter> headers = (Series<Parameter>) request.getAttributes().get("org.restlet.http.headers");
        return convertValue(clazz, headers.getValues(name));
    }
    
    @SuppressWarnings("unchecked")
	public static <T> T convertValue(Class<T> clazz, String str) throws ResourceException {
        if (str != null) {
            
            /*
            try {
                str = URLDecoder.decode(str, UTF8);
            } catch (UnsupportedEncodingException ex) {
                throw new ClientErrorException(Status.CLIENT_ERROR_BAD_REQUEST, ex);
            }
            */

            if (clazz.isEnum()) {

                Integer i = convertValue(Integer.class, str);
                if (i != null) {
                    T[] values = clazz.getEnumConstants();
                    if (i >= 0 && i < values.length) {
                        return values[i];
                    }
                }

            } else {

                if (clazz.equals(String.class)) {
                    return (T) str;
                }

                if (!str.isEmpty()) {

                    if (Number.class.isAssignableFrom(clazz)) {
                        str = str.replace(",", "");
                    }
                    
                    if (clazz.equals(Integer.class)) {
                        return (T) Integer.valueOf(str);
                    }

                    if (clazz.equals(Long.class)) {
                        return (T) Long.valueOf(str.replace(",", ""));
                    }

                    if (clazz.equals(Float.class)) {
                        return (T) Float.valueOf(str);
                    }

                    if (clazz.equals(Double.class)) {
                        return (T) Double.valueOf(str);
                    }

                    if (clazz.equals(BigDecimal.class)) {
                        return (T) new BigDecimal(str);
                    }

                    if (clazz.equals(BigInteger.class)) {
                        return (T) new BigInteger(str);
                    }
                    
                    if (clazz.equals(Boolean.class)) {
                        return (T) Boolean.valueOf(str);
                    }

                    try {

                        if (clazz.equals(JSONObject.class)) {
                            return (T) new JSONObject(str);
                        }

                        if (clazz.equals(JSONArray.class)) {
                            return (T) new JSONArray(str);
                        }

                    } catch (JSONException ex) {
                        throw new ClientErrorException(Status.CLIENT_ERROR_BAD_REQUEST, ex);
                    }

                    if (clazz.equals(Date.class) || clazz.equals(Timestamp.class)) {
                        
                        Date d = null;
                        ParseException parseEx = null;
                        for (SimpleDateFormat formatString : DATE_FORMATS) {
                            try {
                                 d = formatString.parse(str);
                            } catch (ParseException ex) {
                                parseEx = ex;
                            }   
                        }
                        
                        if (d == null) {
                            throw new ClientErrorException(Status.CLIENT_ERROR_BAD_REQUEST, parseEx);
                        }
                        
                        if (clazz.equals(Timestamp.class)) {
                            return (T) new Timestamp(d.getTime());
                        } else {
                            return (T) d;
                        }
                    }

                }
            }
        }
        return (T) null;
    }
    
    @SuppressWarnings("unchecked")
	public static void addHeader(Message message, String name, String value) {
        Series<Header> headers = (Series<Header>)
        message.getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        if (headers == null) {
            headers = new Series<>(Header.class);
            message.getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, headers);
        }
        headers.add(new Header(name, value));
    }
    
    @SuppressWarnings("unchecked")
	public static void removeHeader(Message message, String name, String value) {
        Series<Header> headers = (Series<Header>)
        message.getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        if (headers != null) {
            Set<Header> headersToRemove = new HashSet<>();
            for(Header header: headers) {
                if (header.getName().equals(name) && header.getValue().equals(value)){
                    headersToRemove.add(header);
                }
            }
            headers.removeAll(headersToRemove);
        }
    }
    
}
