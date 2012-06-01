package org.flxsource.pong.net;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class StringPacket extends PongPacket {

	private String message;
	
	private static final int MAX_MESSAGE_SIZE = 500;
	private static final Charset charset = Charset.forName("UTF-8");
	private ByteBuffer msgBuff = ByteBuffer.allocate(MAX_MESSAGE_SIZE);
	
	public StringPacket() {
		
	}
	
	public StringPacket(String username, String message) {
		super(username);
		setMessage(message);
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	@Override
	protected void write(ByteBuffer buffer) {
		msgBuff.clear();
		msgBuff.put(charset.encode(message));
		buffer.put(msgBuff);

	}

	@Override
	protected void read(ByteBuffer buffer) {
		msgBuff.clear();
		buffer.get(msgBuff.array());
		message = charset.decode(msgBuff).toString();
	}

}
