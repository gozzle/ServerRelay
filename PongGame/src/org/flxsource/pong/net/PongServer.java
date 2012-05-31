package org.flxsource.pong.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;
import org.apache.commons.daemon.DaemonInitException;


// Server to run as a daemon and do game event messaging
public class PongServer implements Daemon {

	boolean run = false;
	private Selector selector;
	private ServerSocketChannel server;

	private Map<String, Game> userMap = new HashMap<String,Game>();
	private Set<Game> games = new HashSet<Game>();
	private Client waitingClient = null;

	PongServer() {

	}


	public static void main(String[] args) {
		PongServer server = new PongServer();
		DaemonContext context = new DaemonContext() {

			@Override
			public DaemonController getController() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String[] getArguments() {
				// TODO Auto-generated method stub
				return null;
			}
		};

		try {
			server.init(context);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			server.start(); // endless loop!
			server.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		server.destroy();
	}

	@Override
	public void init(DaemonContext context) throws DaemonInitException,
	Exception {

		// set up client Listener (fail if can't bind to port)
		server = ServerSocketChannel.open();
		// set the server to non-blocking
		server.configureBlocking(false);
		try {
			// bind server to socket on port 9999
			server.socket().bind(new InetSocketAddress(9999));
		} catch (IOException e) {
			System.err.println("Couldn't open server socket channel on port " + server.socket().getLocalPort());
			e.printStackTrace();
			System.exit(-1);
		}

		selector = Selector.open();
		// register server on the selector as a connection acceptor
		server.register(selector, SelectionKey.OP_ACCEPT);

	}

	@Override
	public void start() throws Exception {
		run = true;

		System.out.println("running");
		while (run) {
			// get events from selector (will block thread)
			selector.select();

			// get keys for the selected events
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> it = keys.iterator();
			while (it.hasNext()) {
				SelectionKey key = it.next();

				// pop the key from the set
				it.remove();

				if (key.isAcceptable()) {

					// server should accept the connection
					SocketChannel clientChannel = server.accept();
					// make channel non-blocking
					clientChannel.configureBlocking(false);
					// register the client channel with the selector
					clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

//					System.out.println("received connection from " + clientChannel.socket().getInetAddress().toString());
					acceptConnection(clientChannel);
					continue;

				} else if(key.isReadable()) {
					// read from channel and write to partner client
					relay((SocketChannel)key.channel());

				} else if (key.isWritable()) {
					// not quite sure about this... don't think i want to use it (want to write in the relay method)!
				}
			}
		}
	}

	private void acceptConnection(SocketChannel channel) {

		// receive client details, and work out who it is using the authorisation system
		Client client = new Client();
		client.setUser(channel.socket().getInetAddress().toString() + ":" + channel.socket().getPort());
		client.setAddress(channel.socket().getInetAddress());
		client.setPort(channel.socket().getPort());
		client.setConnection(channel);

		if (userMap.get(client.getUser()) == null) {
			// client is not currently part of a game
			if (waitingClient == null) {
				// set the new client as the waiting client, and add to a new Game
				waitingClient = client;
				Game newGame = new Game();
				newGame.addClient(client);
				games.add(newGame);
				userMap.put(client.getUser(), newGame);

//				System.out.println("Client assigned as 'waiting client'");
			} else {
				// add the new client to the waitingClient's game
				Game game = userMap.get(waitingClient.getUser());
				game.addClient(client);
				userMap.put(client.getUser(), game);
				waitingClient = null;
				// game is ready
//				System.out.println("'game' is ready...");
			}
		} else {
			// client is already part of a game, and is reconnecting
			Game game = userMap.get(client.getUser());
			game.getClient(client).setConnected(true);
		}
	}

	private void relay(SocketChannel sourceChannel) throws IOException {
		// get client from sourceChannel
		Client client = new Client();
		client.setAddress(sourceChannel.socket().getInetAddress());
		client.setPort(sourceChannel.socket().getPort());
		client.setUser(sourceChannel.socket().getInetAddress().toString() + ":" + sourceChannel.socket().getPort());

		// read in whatever, so that there's not a buildup on the channel
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		
//		System.out.println("reading...");
		try {
			sourceChannel.read(buffer);
		} catch (IOException e) {
			// assume client has disconnected
			// get out of here!
			return;
		}
		buffer.flip();

		Game game = userMap.get(client.getUser());
		if (game != null && game.isRunning()) {
			// client is part of a full game. Otherwise ignore it

			// get partner's channel to write to
			Client partner = game.getPartner(client);
			
//			System.out.println("partner is " + partner.getUser());

			SocketChannel partnerChannel = partner.getConnection();

			// write to partner
//			System.out.println("writing...");
			try {
				partnerChannel.write(buffer);
			} catch (IOException e) {
				// assume partner has disconnected.
				// Set the client as not connected.
				partner.setConnected(false);
			}
		}

	}



	@Override
	public void stop() throws Exception {
		run = false;		
	}

	@Override
	public void destroy() {
		try {
			server.close();

			// close all channels
			selector.close();

		} catch (IOException e) {
			System.err.println("Couldn't close the listening socket on port " + server.socket().getLocalPort());
			e.printStackTrace();
		}
	}

	private class Game {

		private int id;
		private ArrayList<Client> clients = new ArrayList<Client>(2);

		public boolean isFull() {
			return clients.size() == 2;
		}

		public boolean isRunning() {
			if (!isFull()) {
				return false;
			}
			for (Client client : clients) {
				if (!client.isConnected()) {
					return false;
				}
			}
			return true;
		}

		public void addClient(Client client) {
			if (!isFull() && !clients.contains(client)) {
				clients.add(client);
//				System.out.println("adding client to game for user " + client.getUser());
			}
		}

		public ArrayList<Client> getClients() {
			return clients;
		}
		
		public Client getClient(Client client) {
			// returns the game client that matches the given client
			for (Client cl : getClients()) {
				if (cl.equals(client)) {
					return cl;
				}
			}
			
			return null;
		}

		public Client getPartner(Client queryClient) {
//			System.out.println("Getting partner to client " + queryClient.getUser());

			// check that game actually contains this client]
			boolean ok = false;
			for (Client cl : clients) {
//				System.out.println("Game contains client " + cl.getUser());
				if (cl.equals(queryClient)) {
					ok = true;
//					System.out.println("That's the one I asked about! Thanks!");
					break;
				}
			}
			if (isFull() && ok) {
//				System.out.println("all ok, getting partner now...");
				int index = clients.indexOf(queryClient);
				return clients.get((index+1)%2);
			}
			return null;
		}

		@Override
		public int hashCode() {
			// Hash on id
			return Integer.valueOf(id).hashCode();
		}
	}

	private class Client {
		// Class to hold details of connected clients
		private String username;
		private InetAddress address;
		private int port;
		private boolean connected;
		private SocketChannel connection;

		public String getUser() {
			return username;
		}

		public void setUser(String user) {
			this.username = user;
		}

		public InetAddress getAddress() {
			return address;
		}

		public void setAddress(InetAddress address) {
			this.address = address;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public SocketChannel getConnection() {
			if (isConnected()) {
				return connection;
			} else {
				return null;
			}
		}

		public void setConnection(SocketChannel channel) {
			connection = channel;
			setConnected(true);
		}
		
		public boolean isConnected() {			
			connected = connected && connection.isConnected();
			return connected;
		}
		
		public void setConnected(boolean connected) {
			this.connected = connected && 
							 (connection != null) ? connection.isConnected() : true;
		}


		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (this.getClass() != obj.getClass()) {
				return false;
			}
			Client other = (Client) obj;

			return this.username.equals(other.username)
					&& this.address.equals(other.address)
					&& this.port == other.port;
		}
	}


}
