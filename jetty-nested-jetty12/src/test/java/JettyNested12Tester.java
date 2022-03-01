import org.eclipse.jetty.nested.JettyNestedHandler;
import org.eclipse.jetty.server.Server;

public class JettyNested12Tester
{
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8080);
        server.setHandler(new JettyNestedHandler());
        server.start();
        server.join();
    }
}
