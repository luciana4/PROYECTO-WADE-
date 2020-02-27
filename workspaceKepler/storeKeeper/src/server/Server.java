package server;

import java.net.ServerSocket;

import org.apache.axis.client.AdminClient;
import org.apache.axis.transport.http.SimpleAxisServer;
import org.apache.axis.utils.Options;

public class Server {
	
	private void start() throws Exception {
		int webserverPort = 2000;

        ServerSocket socket = new ServerSocket(webserverPort);

        // Create and start Axis Server
        SimpleAxisServer sas = new SimpleAxisServer(); 
        sas.setServerSocket(socket);
        sas.start(true);

        // Deploy StoreKeeperService
        AdminClient ac = new AdminClient();
        Options options = new Options(new String[] { "-p", Integer.toString(webserverPort)});
        ac.process(options, getClass().getResourceAsStream("/server/storekeeper/deploy.wsdd"));

	System.out.println("-----------------------\nThe Store-Keeper web service is accessible at URL\nhttp://localhost:"+webserverPort+"/axis/services/StoreKeeperPort?wsdl\n-----------------------");
        
        StoreKeeper.getInstance().init();
       }
	

	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Server server = new Server();
		server.start();
		while(true) {
			Thread.sleep(5000);
		}
	}

}
