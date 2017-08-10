package jadex.bridge.service.types.serialization;

import java.util.List;

import jadex.bridge.component.IMsgHeader;
import jadex.bridge.service.annotation.Excluded;
import jadex.commons.transformation.traverser.ITraverseProcessor;

/**
 *  Functionality for managing serialization.
 *
 */
public interface ISerializationServices
{
	/**
	 *  Encodes/serializes an object for a particular receiver.
	 *  
	 *  @param receiver The receiver.
	 *  @param cl The classloader used for encoding.
	 *  @param obj Object to be encoded.
	 *  @return Encoded object.
	 */
	public byte[] encode(IMsgHeader header, ClassLoader cl, Object obj);
	
	/**
	 *  Decodes/deserializes an object.
	 *  
	 *  @param cl The classloader used for decoding.
	 *  @param enc Encoded object.
	 *  @return Object to be encoded.
	 *  
	 */
	public Object decode(IMsgHeader header, ClassLoader cl, byte[] enc);
	
	/**
	 *  Test if an object is a remote object.
	 */
	public boolean isRemoteObject(Object target);
	
	/**
	 *  Test if an object has reference semantics. It is a reference when:
	 *  - it implements IRemotable
	 *  - it is an IService, IExternalAccess or IFuture
	 *  - if the object has used an @Reference annotation at type level
	 *  - has been explicitly set to be reference
	 */
	public boolean isLocalReference(Object object);
	
	/**
	 *  Get the clone processors.
	 *  @return The clone processors.
	 */
	public List<ITraverseProcessor> getCloneProcessors();
	
//	/**
//	 *  Gets the remote reference management.
//	 *
//	 *  @return The remote reference management.
//	 */
//	public IRemoteReferenceManagement getRemoteReferenceManagement();
//
//	/**
//	 *  Gets the remote reference module.
//	 *
//	 *  @return The remote reference module.
//	 */
//	public IRemoteReferenceModule getRemoteReferenceModule();
//
//	/**
//	 *  Sets the remote reference module.
//	 *
//	 *  @param rrm The remote reference module.
//	 */
//	public void setRemoteReferenceModule(IRemoteReferenceModule rrm);
	
//	/**
//	 *  Returns the serializer for sending.
//	 *  
//	 *  @param platform Sending platform.
//	 *  @param receiver Receiving platform.
//	 *  @return Serializer.
//	 */
//	public ISerializer getSendSerializer(IComponentIdentifier receiver);
	
//	/**
//	 *  Returns all serializers.
//	 *  
//	 *  @param platform Sending platform.
//	 *  @return Serializers.
//	 */
//	public Map<Integer, ISerializer> getSerializers();
	
//	/**
//	 *  Returns the codecs for sending.
//	 *  
//	 *  @param receiver Receiving platform.
//	 *  @return Codecs.
//	 */
//	public ICodec[] getSendCodecs(IComponentIdentifier receiver);
	
//	/**
//	 *  Returns all codecs.
//	 *  
//	 *  @return Codecs.
//	 */
//	public Map<Integer, ICodec> getCodecs();
	
//	/**
//	 *  Gets the post-processors for decoding a received message.
//	 */
//	public ITraverseProcessor[] getPostprocessors();
//	
//	/**
//	 *  Gets the pre-processors for encoding a received message.
//	 */
//	public ITraverseProcessor[] getPreprocessors();
}
