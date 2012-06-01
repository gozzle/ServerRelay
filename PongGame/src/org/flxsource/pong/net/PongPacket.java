package org.flxsource.pong.net;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

abstract public class PongPacket implements ByteSerializable {
	
	private String username;
	
	private static final int MAX_USER_LENGTH = 100;
	private static final Charset charset = Charset.forName("UTF-8");
	private ByteBuffer uNameBuff = ByteBuffer.allocate(MAX_USER_LENGTH);
	
	public PongPacket() {
		
	}
	
	public PongPacket(String username) {
		setUsername(username);
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getUsername() {
		return this.username;
	}

	@Override
	public final void writeTo(ByteBuffer buffer) {
		// write username, then call write
		uNameBuff.clear();
		uNameBuff.put(charset.encode(username));
		buffer.put(uNameBuff);
		
		// write out subclass's data to the buffer
		write(buffer);
	}

	@Override
	public final void readFrom(ByteBuffer buffer) {
		// read username, then call read
		uNameBuff.clear();
		buffer.get(uNameBuff.array());
		username = charset.decode(uNameBuff).toString();
		
		// read in subclass's data
		read(buffer);

	}
	
	abstract protected void write(ByteBuffer buffer);
	abstract protected void read(ByteBuffer buffer);

}
