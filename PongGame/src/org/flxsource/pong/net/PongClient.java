package org.flxsource.pong.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Set;

public class PongClient {
	
	// Class will connect to server and then send and receive messages
	
	private SocketChannel connection;
	private Selector selector;
	
	int id;
	
	PongClient() {
		init();
	}
	
	public void init() {
		id = (int) (Math.random()*100);
		
		try {
			connection = SocketChannel.open();
			connection.configureBlocking(false);
			connection.connect(new InetSocketAddress(InetAddress.getLocalHost(), 9999));
		} catch (IOException e) {
			System.err.println("Unable to connect socket to port 9999 on the localhost");
			e.printStackTrace();
		}
		
		try {
			selector = Selector.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			connection.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		} catch (ClosedChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// connection should be set up by here
	}
	
	public void run() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		while (true) {
			try {
				while (selector.select(500) > 0) {

					Set<SelectionKey> keys = selector.selectedKeys();
					for (SelectionKey key : keys) {
						keys.remove(key);

						SocketChannel channel = (SocketChannel)key.channel();

						if (key.isConnectable()) {
							// connection has been accepted
							System.out.println("Connection accepted");
							if (channel.isConnectionPending()) {
								channel.finishConnect();
								// write out connection packet
								ConnectionPacket packet = new ConnectionPacket();
								packet.setUsername(Integer.toString(id));
								buffer.clear();
								packet.writeTo(buffer);
								channel.write(buffer);
							}
							continue;

						} else if (key.isReadable()) {
							System.out.println("reading...");
							
							buffer.clear();
							channel.read(buffer);
							buffer.flip();
							StringPacket packet = new StringPacket();
							packet.readFrom(buffer);
							System.out.println(packet.toString());
							
						} else if (key.isWritable()) {
							System.out.println("writing...");
							
							StringPacket packet = new StringPacket();
							packet.setUsername(Integer.toString(id));
							packet.setMessage("Hello from client " + id + "!");
							
							buffer.clear();
							packet.writeTo(buffer);
							channel.write(buffer);
							
							Thread.sleep(1000);
						}
					}
				}
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
	
	public static void main(String[] args) {
		
		PongClient client = new PongClient();
		
		client.run();
	}

}
