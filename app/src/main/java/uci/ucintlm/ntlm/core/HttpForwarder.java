package uci.ucintlm.ntlm.core;

import android.util.Log;

import org.apache.commons.httpclient.ConnectMethod;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.ProxyClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/* *
 * Esta es la clase principal del servidor.
 * Aquí se manejan las conexiones. 
 * (No es un Thread!!!)
 * */

public class HttpForwarder {

	private ServerSocket ssocket;
	private ExecutorService threadPool = Executors.newCachedThreadPool();
	private HttpClient delegateClient;
	private HttpClient noDelegateClient;
	static List<String> stripHeadersIn = Arrays.asList(new String[] {
			"Content-Type", "Content-Length", "Proxy-Connection" });
	static List<String> stripHeadersOut = Arrays.asList(new String[] {
			"Proxy-Authentication", "Proxy-Authorization" });
	private int inport;
	private String addr = "";
	private String user;
	private String pass;
	private String domain;
	private String bypass;
	private boolean running = true;
	private LinkedList<Socket> listaSockets = new LinkedList<Socket>();

	public HttpForwarder(String addr, int inport, String domain, String user,
			String pass, int outport, boolean onlyLocal, String bypass) throws IOException {
		this.addr = addr;
		this.inport = inport;
		this.user = user;
		this.pass = pass;
		this.domain = domain;
		this.bypass = bypass;
		if (onlyLocal) {
			this.ssocket = new ServerSocket(outport, 0,
					InetAddress.getByName("127.0.0.1"));
		} else {
			this.ssocket = new ServerSocket(outport);
		}

		MultiThreadedHttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
		manager.getParams().setDefaultMaxConnectionsPerHost(5);
		this.delegateClient = new HttpClient(manager);
		this.delegateClient.getHostConfiguration().setProxy(addr, inport);

		AuthPolicy.registerAuthScheme(AuthPolicy.NTLM, JCIFS_NTLMScheme.class);
		this.delegateClient.getState().setProxyCredentials(
				new AuthScope(AuthScope.ANY),
				new NTCredentials(user, pass, InetAddress.getLocalHost()
						.getHostName(), domain));

		this.noDelegateClient = new HttpClient(manager);
	}

	public void run() {
		try {
			while (running) {
				Socket s = this.ssocket.accept();
				listaSockets.add(s);
				this.threadPool.execute(new Handler(s));
			}
		} catch (IOException e) {
			System.out.print(e.getMessage());
		}
	}

	public void terminate() throws IOException {
        /*
        *TODO: look for doc about java.util.ConcurrentModificationException
        *this method crashes sometimes trying to access the list
        * */
        try {
            for (Socket a : listaSockets) {
                try {
                    a.close();
                } catch (Exception ex) {
                    Log.e("Error closing socket", ex.getMessage());
                }

            }

            listaSockets.clear();

        } finally {
            this.close();
            this.running = false;
        }
	}

	public void close() throws IOException {
		this.ssocket.close();
	}

	void doConnect(HttpParser parser, OutputStream os) {
		Log.i("making connection", parser.getUri());
		String[] uri = parser.getUri().split(":");
		ProxyClient client = new ProxyClient();
		client.getHostConfiguration().setHost(uri[0], Integer.parseInt(uri[1]));
		client.getHostConfiguration().setProxy(this.addr, this.inport);
		try {
			client.getState().setProxyCredentials(
					new AuthScope(AuthScope.ANY),
					new NTCredentials(this.user, this.pass, InetAddress
							.getLocalHost().getHostName(), this.domain));
		} catch (UnknownHostException e) {
		}
		Socket remoteSocket = null;
		try {
			ProxyClient.ConnectResponse response = client.connect();
			remoteSocket = response.getSocket();
			if (remoteSocket == null) {
				ConnectMethod method = response.getConnectMethod();
				throw new IOException("Socket not created: "
						+ method.getStatusLine());
			}

			os.write(response.getConnectMethod().getStatusLine().toString()
					.getBytes());

			os.write("\r\n\r\n".getBytes());
			this.threadPool.execute(new Piper(parser, remoteSocket
					.getOutputStream()));
			new Piper(remoteSocket.getInputStream(), os).run();
			parser.close();
			os.close();
		} catch (Exception e) {
		} finally {
			if (remoteSocket != null) {
				try {
					remoteSocket.close();
				} catch (Exception fe) {
				}
			}
			try {
				os.close();
			} catch (IOException e) {
			}
			try {
				parser.close();
			} catch (IOException e) {
			}
		}
	}

	class Handler implements Runnable {

		Socket localSocket;
		//ByteBuffer buffer = ByteBuffer.allocate(8192);

		public Handler(Socket localSocket) {
			this.localSocket = localSocket;
		}

		private boolean matches(String url, String bypass) {
	        LinkedList<StringBuilder> patterns = new LinkedList<StringBuilder>();

	        for (String i : bypass.split(",")) {
	            StringBuilder s = new StringBuilder(i);
	            if (i.length() > 0) {
	                while (s.charAt(0) == ' ') {
	                    s.delete(0, 1);
	                }
	                if (s.charAt(0) == '*') {
	                    s.insert(0, ' ');
	                }
	                patterns.add(s);
	            }
	        }

	        for (StringBuilder i : patterns) {
	            Pattern p = Pattern.compile(i.toString());
	            if (p.matcher(url).find()) {
	            	Log.i(getClass().getName(), "url matches bypass "+url);
	                return true; 
	            }
	        }
	        Log.i(getClass().getName(), "url does not matches bypass "+url);
	        return false;
	    }

		public void run() {
			try {
				HttpParser parser = new HttpParser(
						this.localSocket.getInputStream());
				HttpMethod method = null;
				try {
					while (!parser.parse()) {
					}
				} catch (IOException e) {

					parser.close();
					return;
				}
				
//				HttpClient client = (bypass != null) && matches(parser.getUri().toString(), bypass) ? HttpForwarder.this.noDelegateClient : HttpForwarder.this.delegateClient;
				
				HttpClient client = HttpForwarder.this.delegateClient;
				
				if (parser.getMethod().equals("GET")) {
					Log.i(getClass().getName(),"GET "+parser.getUri());
					method = new GetMethod();
				} else if (parser.getMethod().equals("POST")) {
					Log.i(getClass().getName(),"POST "+parser.getUri());
					method = new PostMethod();
				} else if (parser.getMethod().equals("HEAD")) {
					Log.i(getClass().getName(),"HEAD "+parser.getUri());
					method = new HeadMethod();
				} else {
					if (parser.getMethod().equals("CONNECT")) {
						Log.i(getClass().getName(),"CONNECT "+parser.getUri());
						HttpForwarder.this.doConnect(parser,
								this.localSocket.getOutputStream());
						return;
					}
					throw new Exception("Unknown method: " + parser.getMethod());
				}
				if ((method instanceof EntityEnclosingMethod)) {
					EntityEnclosingMethod method2 = (EntityEnclosingMethod) method;
					method2.setRequestEntity(new StreamingRequestEntity(parser));
				}

				method.setURI(new URI(parser.getUri(), true));
				method.setFollowRedirects(false);
				method.getParams().setCookiePolicy("ignoreCookies");

				for (int i = 0; i < parser.getHeaders().length; i++) {
					Header h = parser.getHeaders()[i];

					if (HttpForwarder.stripHeadersIn.contains(h.getName())) {
						continue;
					}
					method.addRequestHeader(h);
				}

				client.executeMethod(method);
				this.localSocket.shutdownInput();
				OutputStream os = this.localSocket.getOutputStream();
				os.write(method.getStatusLine().toString().getBytes());
				os.write("\r\n".getBytes());

				Header[] headers = method.getResponseHeaders();
				for (int i = 0; i < headers.length; i++) {
					if (HttpForwarder.stripHeadersOut.contains(headers[i])) {
						continue;
					}
					os.write(headers[i].toExternalForm().getBytes());

				}

				InputStream is = method.getResponseBodyAsStream();
				if (is != null) {
					os.write("\r\n".getBytes());
					new Piper(is, os).run();
				}

				method.releaseConnection();
				this.localSocket.close();
			} catch (Exception e) {
			}
		}
	}
}
