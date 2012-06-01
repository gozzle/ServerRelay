/**
 * 
 */
package org.flxsource.pong.net;

import java.nio.ByteBuffer;

/**
 * @author Administrator
 *
 */
public interface ByteSerializable {
	
	public void writeTo(ByteBuffer buffer);
	public void readFrom(ByteBuffer buffer);

}
