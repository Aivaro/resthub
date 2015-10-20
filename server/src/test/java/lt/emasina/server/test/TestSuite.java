package lt.emasina.server.test;

import lt.emasina.server.test.support.TestConnectionFactory;
import lombok.extern.log4j.Log4j;
import lt.emasina.resthub.factory.XmlResourceTableFactory;
import lt.emasina.resthub.server.ServerApp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.restlet.Component;
import org.restlet.data.Protocol;

/**
 *
 * @author valdo
 */
@Log4j
@RunWith(JUnit4.class)
public class TestSuite {
    
    public static final String HOST = "http://localhost:8112";
    private static final String XML_RESOURCE = "/lt/emasina/server/xml/tables.xml";
    
    @Test
    public void runTests() throws Exception {
        
        String url = System.getenv("TEST_DATABASE_URL");
        if (url == null) url = "192.168.54.1:1521/cerndev";
        
        ServerApp app = new ServerApp(new TestConnectionFactory(url), new XmlResourceTableFactory(XML_RESOURCE));
        Component comp = new Component();
        comp.getServers().add(Protocol.HTTP, 8112);
        comp.getDefaultHost().attach(app);
        comp.start();
        
        new ServerRunnerWorker().setupQueries();
        new JavaClientWorker().setupQueries();
        
        comp.stop();

    }
    
}
