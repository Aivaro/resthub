package net.resthub.server.test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import junit.framework.TestCase;
import net.resthub.ConnectionFactory;
import net.resthub.server.factory.MetadataFactoryIf;
import net.resthub.server.factory.ResourceFactory;
import net.resthub.server.query.QueryId;
import net.resthub.server.parser.check.CheckSelectParser;
import net.resthub.server.parser.check.SubSelectDef;
import net.resthub.server.parser.update.UpdateSelectParser;
import net.resthub.server.test.factory.ConnectionFactoryTest;
import net.resthub.server.test.factory.MetadataFactoryTest;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.select.Select;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

/**
 * SelectParserTestSuite
 * @author valdo
 */
public abstract class AbstractParserTest extends TestCase {
    
    protected final Injector injector;
    protected final ResourceFactory rf;
    protected final MetadataFactoryIf mf;

    public AbstractParserTest() throws SchedulerException {
        
        final SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        final Scheduler scheduler = schedulerFactory.getScheduler();
        
        this.injector = Guice.createInjector(new AbstractModule() {
            
            @Override
            protected void configure() {
                install(new FactoryModuleBuilder().build(ResourceFactory.class));
                bind(MetadataFactoryIf.class).to(MetadataFactoryTest.class);
                bind(CCJSqlParserManager.class).toInstance(new CCJSqlParserManager());
                bind(ConnectionFactory.class).toInstance(new ConnectionFactoryTest());
                bind(Scheduler.class).toInstance(scheduler);
            }
        });
        this.rf = injector.getInstance(ResourceFactory.class);
        this.mf = injector.getInstance(MetadataFactoryIf.class);
    }
    
    protected SubSelectDef getSubSelectDef(String sql) {
        return getCheckParser(sql).getSelectDef();
    }
    
    protected CheckSelectParser getCheckParser(String sql) {
        CheckSelectParser checkParser = rf.createSelectParser((SubSelectDef) null);
        QueryId qid = rf.create(sql);
        qid.getSelect().getSelectBody().accept(checkParser);
        return checkParser;
    }
    
    protected UpdateSelectParser getUpdateParser(String sql) {
        UpdateSelectParser updParser = new UpdateSelectParser(getCheckParser(sql));
        QueryId qid = rf.create(sql);
        qid.getSelect().getSelectBody().accept(updParser);
        return updParser;
    }
    
    protected Select getUpdateSelect(String sql) {
        UpdateSelectParser updParser = new UpdateSelectParser(getCheckParser(sql));
        QueryId qid = rf.create(sql);
        Select select = qid.getSelect();
        select.getSelectBody().accept(updParser);
        return select;
    }
    
}
