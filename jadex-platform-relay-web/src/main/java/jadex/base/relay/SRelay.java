package jadex.base.relay;


/**
 *  Constants for relay service.
 */
public class SRelay
{
	//-------- constants --------
	
	/** Default relay address. */
	public static final String	DEFAULT_ADDRESS	= "relay-http://jadex.informatik.uni-hamburg.de/relay/";
	
	/** The default message type (followed by arbitrary message content from some sender). */
	public static final byte	MSGTYPE_DEFAULT	= 1;
	
	/** The ping message type (just the type byte and no content). */
	public static final byte	MSGTYPE_PING	= 2;
	
	/** The add awareness info message type for a new platform (awareness info content, usually a component id). */
	public static final byte	MSGTYPE_AWAADD	= 3;
	
	/** The remove awareness info message type for a disconnected platform (awareness info content, usually a component id). */
	public static final byte	MSGTYPE_AWAREMOVE	= 4;
}
