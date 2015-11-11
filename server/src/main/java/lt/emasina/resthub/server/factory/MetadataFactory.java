/*
 * #%L
 * server
 * %%
 * Copyright (C) 2012 - 2015 valdasraps
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package lt.emasina.resthub.server.factory;

import com.google.inject.persist.Transactional;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.log4j.Log4j;
import lt.emasina.resthub.TableFactory;
import lt.emasina.resthub.factory.TableBuilder;
import org.apache.commons.beanutils.BeanUtils;
import lt.emasina.resthub.server.table.TableId;
import lt.emasina.resthub.server.table.ServerTable;
import lt.emasina.resthub.model.MdTable;
import org.json.JSONObject;

@Log4j
@Singleton
public class MetadataFactory implements MetadataFactoryIf {

    private final static String ERROR_METADATA_KEY = "Error";

    private final Map<TableId, ServerTable> tables = new ConcurrentHashMap<>();
    private final Map<TableId, ServerTable> blacklist = new ConcurrentHashMap<>();

    @Inject
    private ResourceFactory rf;

    @Inject
    private QueryFactory qf;

    @Inject
    private TableFactory tfhead;

    @Inject
    private TableBuilder tb;

    @Getter
    private boolean forceRefresh = true;

    @Transactional
    @Override
    public synchronized void refresh() throws Exception {
        TableFactory tf = tfhead;
        while (tf != null) {

            boolean doRefresh = forceRefresh || tf.isRefresh();

            if (log.isDebugEnabled()) {
                log.debug(String.format("forceRefresh = %s, doRefresh = %s", forceRefresh, doRefresh));
            }

            // Update is needed!
            if (doRefresh) {

                Set<TableId> ids = new HashSet<>();

                // Update or add tables
                for (MdTable t : tf.getTables()) {

                    TableId id = new TableId(t);
                    ServerTable st = rf.create(t, tf);

                    if (!blacklist.containsKey(id)) {

                        st.getTable().getMetadata().remove(ERROR_METADATA_KEY);

                        try {

                            // Check the table
                            tb.collectColumns(t.getConnectionName(), t.getSql(), t.getColumns());
                            tb.collectParameters(t.getSql(), t.getParameters());

                            ids.add(id);

                            if (!hasTable(id)) {

                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Adding table: %s", t));
                                }

                                tables.put(id, st);

                            } else {

                                MdTable t1 = getTable(id).getTable();
                                if (t.getUpdateTime().after(t1.getUpdateTime())) {

                                    if (log.isDebugEnabled()) {
                                        log.debug(String.format("Updating table %s", t));
                                    }

                                    tables.put(id, st);
                                    qf.removeQueries(id);
                                }
                            }

                        } catch (Exception ex) {

                            log.warn(String.format("Error while adding table %s.%s (will not be added!): %s", t.getNamespace(), t.getName(), ex.getMessage()));
                            this.blacklist.put(id, st);
                            st.getTable().getMetadata().put(ERROR_METADATA_KEY, ex.getMessage());

                        }
                    }
                }

                // Selecting tables added by this tf
                Set<TableId> exids = new HashSet<>();
                for (TableId tid: tables.keySet()) {
                    if (tf.equals(tables.get(tid).getTf())) {
                        exids.add(tid);
                    }
                }
                
                // Remove tables that does not exist anymore
                for (TableId id : exids) {
                    if (!ids.contains(id)) {

                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Removing table %s", id));
                        }

                        tables.remove(id);
                        qf.removeQueries(id);

                    }
                }
            }

            tf = tf.getNext();

        }

        forceRefresh = false;

    }

    @Override
    public Collection<ServerTable> getTables() {
        return Collections.unmodifiableCollection(tables.values());
    }

    public Collection<ServerTable> getBlacklist() {
        return Collections.unmodifiableCollection(blacklist.values());
    }

    public ServerTable getBlacklistTable(TableId id) {
        return blacklist.get(id);
    }

    public void removeBlacklistTable(TableId id) {
        blacklist.remove(id);
        forceRefresh = true;
    }

    public void clearBlacklist() {
        this.blacklist.clear();
        forceRefresh = true;
    }

    public void clearBlacklist(String namespace) {
        for (TableId id : blacklist.keySet()) {
            if (id.getNamespace().equals(namespace)) {
                blacklist.remove(id);
            }
        }
        forceRefresh = true;
    }

    @Override
    public ServerTable getTable(TableId id) {
        return tables.get(id);
    }

    @Override
    public boolean hasTable(TableId id) {
        return tables.containsKey(id);
    }

    public static JSONObject mapToJSONObject(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return new JSONObject(map);
    }

    public static void injectPrivateField(Object o, Class<?> fieldHolderClass, String fieldName, Object value) throws Exception {
        Field fResourceMd = fieldHolderClass.getDeclaredField(fieldName);
        fResourceMd.setAccessible(true);
        fResourceMd.set(o, value);
    }

    public static JSONObject beanToJSONObject(Object bean) throws Exception {
        if (bean == null) {
            return null;
        }
        JSONObject o = new JSONObject();
        Map<?, ?> describe = BeanUtils.describe(bean);
        for (Object k : describe.keySet()) {
            if (k instanceof String) {
                String ks = (String) k;
                if (!ks.equals("class")) {
                    o.putOpt(ks, describe.get(k));
                }
            }
        }
        return o;
    }

}
