package org.flxsource.pong.net;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class Packet<T extends Serializable> {
	
	private static final int MAX_USER_LENGTH = 100;
	private static int MAX_OBJECT_SIZE = 1024;
	
	private static Charset charset = Charset.forName("UTF-8");
	
	private String username;
	private T object;
	
	private Packet() {
		// creates empty packet for filling
	}
	
	private Packet(String username, T object) {
		this.username = username;
		this.object = object;
	}
	
	/**
	 * Returns ByteBuffer ready to be written to a SocketChannel (or other channel),
	 * prepending the username to a serialized object of type T. It is up to the user
	 * to ensure that the serialization methods implemented by the class T is efficient
	 * to avoid sending too much data over the network. An OversizedObjectException
	 * will be thrown if the serialized object data is larger than MAX_OBJECT_SIZE. 
	 * Maximum username length is 100 characters.
	 * 
	 * @param username
	 * @param object
	 * @return
	 */
	public static <T extends Serializable> ByteBuffer write(String username, T object) {		
		// create username buffer
		ByteBuffer uNameBuff = ByteBuffer.allocate(MAX_USER_LENGTH);
		uNameBuff.put(charset.encode(username));
		
		// delegate creation of object buffer to protected method
		ByteBuffer objBuff = toByteBuffer(object);
		
		//join the two
		ByteBuffer buffer = ByteBuffer.allocate(MAX_USER_LENGTH + MAX_OBJECT_SIZE);
		buffer.put(uNameBuff).put(objBuff);
		
		// make it ready to write out
		buffer.flip();
		
		return buffer;
	}
	
	public static <T extends Serializable> Packet<T> read(ByteBuffer buffer) {
		// flips (if position != 0), then reads the buffer into a String then a T
		if (buffer.position() != 0) {
			buffer.flip();
		}
		
		// Get username string
		ByteBuffer uNameBuff = ByteBuffer.allocate(MAX_USER_LENGTH);
		while (uNameBuff.hasRemaining()) {
			uNameBuff.put(buffer.get());
		}
		uNameBuff.flip();
		String username = charset.decode(uNameBuff).toString();
		
		// compact the buffer to read the object out
		buffer.compact();
		
		// delegate object reading
		T object = fromByteBuffer(buffer);
		
		Packet<T> packet = new Packet<>(username, object);
		return packet;
	}
	
	public static void setMaxObjectSize(int size) {
		MAX_OBJECT_SIZE = size;
	}
	
	public static int getMaxObjectSize() {
		return MAX_OBJECT_SIZE;
	}
	
	public static int getMaxUsernameLength() {
		return MAX_USER_LENGTH;
	}
	
	public String getUsername() {
		return this.username;
	}
	
	public T getObject() {
		return this.object;
	}
	
	protected static <T extends Serializable> ByteBuffer toByteBuffer(T object) {
		// T is serializable, use java serialization to create buffer
		ByteBuffer buffer = ByteBuffer.allocate(MAX_OBJECT_SIZE);
		
		ObjectOutputStream.

		
		return null;
	}
	
	protected static <T extends Serializable> T fromByteBuffer(ByteBuffer buffer) {
		// fill packet with data from buffer
		return null;
	}

}
