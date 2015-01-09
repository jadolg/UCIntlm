package uci.ucintlm.service;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

import uci.ucintlm.ntlm.core.HttpForwarder;

public class ServerTask extends AsyncTask {
	/*
	 * this starts the server as an AsyncTask
	 * no type arguments required
	 * AsyncTask replaces Threading mechanisms in Android
	 * */	
	private String user="",pass="",domain="uci.cu",server="10.0.0.1", bypass;
	private int inport = 8080,outport = 8080;
	private HttpForwarder forwardingServer;
	
	
	
public ServerTask(String user, String pass, String domain, String server,
			int inport, int outport,String bypass) {
		super();
		this.user = user;
		this.pass = pass;
		this.domain = domain;
		this.server = server;
		this.inport = inport;
		this.outport = outport;
		this.bypass = bypass;
	}

	
	public void stop(){
		try {
			forwardingServer.terminate();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Object doInBackground(Object... arg0) {
		try {
			Log.i(getClass().getName(), "Server thread "+user+" "+domain+" "+server+" "+inport+" "+outport);
			forwardingServer = new HttpForwarder(server, inport, domain,user, pass,outport, true,bypass);
			forwardingServer.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
