package jadex.base.service.message.transport.codecs;

import jadex.commons.transformation.binaryserializer.BinarySerializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 *  The Jadex Binary codec. Codec supports parallel
 *  calls of multiple concurrent clients (no method
 *  synchronization necessary).
 *  
 *  Converts object -> byte[] and byte[] -> object.
 */
public class JadexBinaryCodec implements ICodec
{
	//-------- constants --------
	
	/** The JadexBinary codec id. */
	public static final byte CODEC_ID = 6;

	/** The debug flag. */
	protected boolean DEBUG = false;
	
	//-------- methods --------
	
	/**
	 *  Encode an object.
	 *  @param obj The object.
	 *  @throws IOException
	 */
	public Object encode(Object val, ClassLoader classloader)
	{
		byte[] ret = BinarySerializer.objectToByteArray(val, null, null, classloader);
		if(DEBUG)
			System.out.println("encode message: "+(new String(ret)));
		return ret;
	}

	/**
	 *  Decode an object.
	 *  @return The decoded object.
	 *  @throws IOException
	 */
	public Object decode(Object bytes, ClassLoader classloader)
	{
		Object ret = bytes instanceof byte[]
			? BinarySerializer.objectFromByteArray((byte[])bytes, null, null, classloader)
			: BinarySerializer.objectFromByteArrayInputStream((ByteArrayInputStream)bytes, null, null, classloader);
		if(DEBUG)
			System.out.println("decode message: "+(new String((byte[])bytes)));
		return ret;
	}
}