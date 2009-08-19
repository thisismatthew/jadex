package jadex.commons.xml.bean;

import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.xml.AttributeInfo;
import jadex.commons.xml.BasicTypeConverter;
import jadex.commons.xml.ITypeConverter;
import jadex.commons.xml.SXML;
import jadex.commons.xml.TypeInfo;
import jadex.commons.xml.TypeInfoPathManager;
import jadex.commons.xml.TypeInfoTypeManager;
import jadex.commons.xml.reader.IObjectReaderHandler;
import jadex.commons.xml.reader.Reader;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 *  Handler for reading XML into Java beans.
 */
public class BeanObjectReaderHandler implements IObjectReaderHandler
{
	//-------- attributes --------
	
	/** The type info path manager. */
	protected TypeInfoPathManager tipmanager;
	
	/** The type info manager. */
	// For special case that an object is created via the built-in 
	// tag mechanism and there is a type info for that kind of created
	// object. Allows specifying generic type infos with interfaces.
	protected TypeInfoTypeManager titmanager;
	
	/** No type infos. */
	protected Set no_typeinfos;
	
	//-------- constructors --------
	
	/**
	 *  Create a new handler.
	 */
	public BeanObjectReaderHandler(Set typeinfos)
	{
		this.tipmanager = new TypeInfoPathManager(typeinfos);
		this.titmanager = new TypeInfoTypeManager(typeinfos);
	}
	
	//-------- methods --------

	/**
	 *  Get the most specific mapping info.
	 *  @param tag The tag.
	 *  @param fullpath The full path.
	 *  @return The most specific mapping info.
	 */
	public TypeInfo getTypeInfo(QName tag, QName[] fullpath, Map rawattributes)
	{
		return tipmanager.getTypeInfo(tag, fullpath, rawattributes);
	}
	
	/**
	 *  Get the most specific mapping info.
	 *  @param tag The tag.
	 *  @param fullpath The full path.
	 *  @return The most specific mapping info.
	 */
	public TypeInfo getTypeInfo(Object object, QName[] fullpath, Object context)
	{
		Object type = getObjectType(object, context);
		if(no_typeinfos!=null && no_typeinfos.contains(type))
			return null;
			
		TypeInfo ret = titmanager.getTypeInfo(type, fullpath);
		// Hack! due to HashMap.Entry is not visible as class
		if(ret==null)
		{
			if(type instanceof Class)
			{
				// Class name not necessary no more
//				Class clazz = (Class)type;
//				type = SReflect.getClassName(clazz);
//				ret = findTypeInfo((Set)typeinfos.get(type), fullpath);
//				if(ret==null)
//				{
				
				// Try if interface or supertype is registered
				List tocheck = new ArrayList();
				tocheck.add(type);
				
				for(int i=0; i<tocheck.size() && ret==null; i++)
				{
					Class clazz = (Class)tocheck.get(i);
					Set tis = titmanager.getTypeInfosByType(clazz);
					ret = titmanager.findTypeInfo(tis, fullpath);
					if(ret==null)
					{
						Class[] interfaces = clazz.getInterfaces();
						for(int j=0; j<interfaces.length; j++)
							tocheck.add(interfaces[j]);
						clazz = clazz.getSuperclass();
						if(clazz!=null)
							tocheck.add(clazz);
					}
				}
				
				// Special case array
				// Requires Object[].class being registered 
				if(ret==null && ((Class)type).isArray())
				{
					ret = titmanager.findTypeInfo(titmanager.getTypeInfosByType(Object[].class), fullpath);
				}
				
				// Add concrete class for same info if it is used
				if(ret!=null)
				{
					TypeInfo ti = new TypeInfo(ret.getSupertype(), ret.getXMLPath(), 
						type, ret.getCommentInfo(), ret.getContentInfo(), 
						ret.getDeclaredAttributeInfos(), ret.getPostProcessor(), ret.getFilter(), 
						ret.getDeclaredSubobjectInfos(), ret.getNamespace());
					
					titmanager.addTypeInfo(ti);
				}
				else
				{
					if(no_typeinfos==null)
						no_typeinfos = new HashSet();
					no_typeinfos.add(type);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Create an object for the current tag.
	 *  @param type The object type to create.
	 *  @param root Flag, if object should be root object.
	 *  @param context The context.
	 *  @return The created object (or null for none).
	 */
	public Object createObject(Object type, boolean root, Object context, Map rawattributes, ClassLoader classloader) throws Exception
	{
		Object ret = null;
		
		if(type instanceof QName)
		{
//			System.out.println("here: "+typeinfo);
			QName tag = (QName)type;
			String pck = tag.getNamespaceURI().substring(SXML.PROTOCOL_TYPEINFO.length());
			String clazzname = pck+"."+tag.getLocalPart();
			
			// Special case array
			int length = -1;
			int idx = clazzname.indexOf("__");
			if(idx!=-1)
			{
				length = Integer.parseInt(clazzname.substring(idx+2));
				clazzname = clazzname.substring(0, idx);
			}
			
			Class clazz = SReflect.classForName0(clazzname, classloader);
			
			if(clazz!=null)
			{
				if(length!=-1)
				{
					ret = Array.newInstance(clazz, length);
				}
				else if(!BasicTypeConverter.isBuiltInType(clazz))
				{
					// Must have empty constructor.
					ret = clazz.newInstance();
				}
			}
		}
		else if(type instanceof TypeInfo)
		{
			Object ti =  ((TypeInfo)type).getTypeInfo();
			if(ti instanceof Class && ((Class)ti).isInterface())
			{
				type = ((TypeInfo)type).getXMLTag();
			}
			else
			{
				type = ti;
			}
		}	
		
		if(type instanceof Class)
		{
			Class clazz = (Class)type;
			if(!BasicTypeConverter.isBuiltInType(clazz))
			{
				// Must have empty constructor.
				ret = clazz.newInstance();
			}
		}
		else if(type instanceof IBeanObjectCreator)
		{
			ret = ((IBeanObjectCreator)type).createObject(context, rawattributes, classloader);
		}
		
		return ret;
	}
	
	/**
	 *  Get the object type
	 *  @param object The object.
	 *  @return The object type.
	 */
	public Object getObjectType(Object object, Object context)
	{
		return object.getClass();
	}
	
	/**
	 *  Convert an object to another type of object.
	 */
	public Object convertContentObject(Object object, QName tag, Object context, ClassLoader classloader)
	{
		Object ret = object;
		if(tag.getNamespaceURI().startsWith(SXML.PROTOCOL_TYPEINFO))
		{
			String clazzname = tag.getNamespaceURI().substring(SXML.PROTOCOL_TYPEINFO.length())+"."+tag.getLocalPart();
			Class clazz = SReflect.classForName0(clazzname, classloader);
			if(clazz!=null)
			{
				if(!BasicTypeConverter.isBuiltInType(clazz))
					throw new RuntimeException("No converter known for: "+clazz);
				ret = BasicTypeConverter.getBasicConverter(clazz).convertObject(object, null, classloader, context);
			}
		}
		return ret;
	}

	
	/**
	 *  Handle the attribute of an object.
	 *  @param object The object.
	 *  @param attrname The attribute name.
	 *  @param attrval The attribute value.
	 *  @param attrinfo The attribute info.
	 *  @param context The context.
	 */
	public void handleAttributeValue(Object object, QName xmlattrname, List attrpath, String attrval, 
		Object attrinfo, Object context, ClassLoader classloader, Object root) throws Exception
	{
		// Hack!
		if(attrval!=null)
			setAttributeValue(attrinfo, xmlattrname, object, attrval, root, classloader);
		else if(attrinfo instanceof BeanAttributeInfo && ((BeanAttributeInfo)attrinfo).getDefaultValue()!=null)
			setAttributeValue(attrinfo, xmlattrname, object, ((BeanAttributeInfo)attrinfo).getDefaultValue(), root, classloader);
	}
		
	// Hack!!!
	protected Map arraycounter;
	/**
	 *  Link an object to its parent.
	 *  @param object The object.
	 *  @param parent The parent object.
	 *  @param linkinfo The link info.
	 *  @param tagname The current tagname (for name guessing).
	 *  @param context The context.
	 */
	public void linkObject(Object object, Object parent, Object linkinfo, 
		QName[] pathname, Object context, ClassLoader classloader, Object root) throws Exception
	{
		QName tag = pathname[pathname.length-1];
		
		// Add object to its parent.
		boolean	linked	= false;
		
		if(linkinfo!=null)
		{
			setAttributeValue(linkinfo, tag, parent, object, root, classloader);
			linked = true;
		}
		
		// Special case array
		
		if(parent.getClass().isArray())
		{
			Integer cnt = null;
			if(arraycounter==null)
			{
				arraycounter = new WeakHashMap();
			}
			else
			{
				cnt = (Integer)arraycounter.get(parent);
			}
			if(cnt==null)
				cnt = new Integer(0);
			
			Array.set(parent, cnt.intValue(), object);
			
			arraycounter.put(parent, new Integer(cnt.intValue()+1));
			
			linked = true;
		}
		else
		{
			List classes	= new LinkedList();
			classes.add(object.getClass());
			
			String[] plunames = new String[pathname.length];
			String[] sinnames = new String[pathname.length];
			for(int i=0; i<pathname.length; i++)
			{
				String name = pathname[i].getLocalPart().substring(0, 1).toUpperCase()+pathname[i].getLocalPart().substring(1);
				plunames[i] = name;
				sinnames[i] = SUtil.getSingular(name);
			}
			
			// Try name guessing via class/superclass/interface names of object to add
			while(!linked && !classes.isEmpty())
			{
				Class clazz = (Class)classes.remove(0);
				
				for(int i=0; i<plunames.length && !linked; i++)
				{
					linked = internalLinkObjects(clazz, "set"+plunames[i], object, parent, root, classloader);
					if(!linked)
					{
						linked = internalLinkObjects(clazz, "add"+sinnames[i], object, parent, root, classloader);
						if(!linked && sinnames[i].equals(plunames[i]))
							linked = internalLinkObjects(clazz, "add"+plunames[i], object, parent, root, classloader);
					}
				}
				
				if(!linked && !BasicTypeConverter.isBuiltInType(clazz))
				{
					String name = SReflect.getInnerClassName(clazz);
					linked = internalLinkObjects(clazz, "set"+name, object, parent, root, classloader);
					if(!linked)
					{
						linked = internalLinkObjects(clazz, "add"+name, object, parent, root, classloader);
						if(!linked)
						{
							String sinname = SUtil.getSingular(name);
							if(!name.equals(sinname))
								linked = internalLinkObjects(clazz, "add"+sinname, object, parent, root, classloader);
						}
					}
				}
				
				if(!linked)
				{
					if(clazz.getSuperclass()!=null)
						classes.add(clazz.getSuperclass());
					Class[]	ifs	= clazz.getInterfaces();
					for(int i=0; i<ifs.length; i++)
					{
						classes.add(ifs[i]);
					}
				}
			}
		}
		
		if(!linked)
			throw new RuntimeException("Could not link: "+object+" "+parent);
	}
	
	//-------- helper methods --------
	
	/**
	 *  Set an attribute value.
	 *  Similar to handleAttributValue but allows objects as attribute values (for linking).
	 *  @param attrinfo The attribute info.
	 *  @param xmlattrname The xml attribute name.
	 *  @param object The object.
	 *  @param attrval The attribute value.
	 *  @param root The root object.
	 *  @param classloader The classloader.
	 */
	protected void setAttributeValue(Object attrinfo, QName xmlattrname, Object object, 
		Object attrval, Object root, ClassLoader classloader)
	{
		boolean set = false;
		
		// Write to a map.
		if(attrinfo instanceof BeanAttributeInfo)
		{	
			BeanAttributeInfo bai = (BeanAttributeInfo)attrinfo;
			
			if(bai.getMapName()!=null)
			{
				String mapname = bai.getMapName().length()==0? bai.getMapName(): bai.getMapName().substring(0,1).toUpperCase()+bai.getMapName().substring(1);
//				String jattrname = bai.getAttributeName()!=null? bai.getAttributeName(): xmlattrname;
				
				Object key = null;
				if(bai.getReadMapKeyMethod()!=null)
				{
					Method m = bai.getReadMapKeyMethod();
					try
					{
						key = m.invoke(attrval, new Object[0]);
					}
					catch(Exception e)
					{
					}
				}
				else
				{
					key =  bai.getAttributeName()!=null? bai.getAttributeName(): xmlattrname;
				}
				
				if(bai.getReadMethod()!=null)
				{
					Method m = bai.getReadMethod();
					Class[] ps = m.getParameterTypes();
					Object arg = convertAttributeValue(attrval, ps[1], bai.getConverterRead(), root, classloader);
					
					try
					{
						m.invoke(object, new Object[]{key, arg});
						set = true;
					}
					catch(Exception e)
					{
					}
				}
				else
				{
					String[] prefixes = new String[]{"put", "set", "add"};
					for(int i=0; i<prefixes.length && !set; i++)
					{
						Method[] ms = SReflect.getMethods(object.getClass(), prefixes[i]+mapname);
						for(int j=0; j<ms.length && !set; j++)
						{
							Class[] ps = ms[j].getParameterTypes();
							if(ps.length==2)
							{
								Object arg = convertAttributeValue(attrval, ps[1], bai.getConverterRead(), root, classloader);
								
								try
								{
									ms[j].invoke(object, new Object[]{key, arg});
									set = true;
								}
								catch(Exception e)
								{
								}
							}
						}
					}
				}
			}
			else if(bai.getWriteMethod()!=null)
			{
				Method m = bai.getWriteMethod();
				Class[] ps = m.getParameterTypes();
				if(ps.length==1)
				{
					Object arg = convertAttributeValue(attrval, ps[0], bai.getConverterRead(), root, classloader);
					
					try
					{
						m.invoke(object, new Object[]{arg});
						set = true;
					}
					catch(Exception e)
					{
					}
				}
				else
				{
					throw new RuntimeException("Read method should have one parameter: "+bai+" "+m);
				}
			}
		}
	
		if(!set && attrinfo instanceof AttributeInfo)
		{
			AttributeInfo ai = (AttributeInfo)attrinfo;
			
			String postfix = ai.getAttributeIdentifier()!=null? ((String)ai.getAttributeIdentifier())
				.substring(0,1).toUpperCase()+((String)ai.getAttributeIdentifier()).substring(1)
				: xmlattrname.getLocalPart().substring(0,1).toUpperCase()+xmlattrname.getLocalPart().substring(1);
				
			set = setDirectValue(new String[]{"set", "add"}, postfix, attrval, object, root, classloader, 
				ai instanceof BeanAttributeInfo? ((BeanAttributeInfo)ai).getConverterRead(): null);
		
			if(!set)
			{
				String oldpostfix = postfix;
				postfix = SUtil.getSingular(postfix);
				if(!postfix.equals(oldpostfix))
				{
					// First try add, as set might also be there and used for a non-multi attribute.
					set = setDirectValue(new String[]{"set", "add"}, postfix, attrval, object, root, classloader, 
						ai instanceof BeanAttributeInfo? ((BeanAttributeInfo)ai).getConverterRead(): null);
				}
			}
		}
		else if(!set)
		{
			// Write as normal bean attribute.
			
			String postfix = attrinfo instanceof String? ((String)attrinfo).substring(0,1).toUpperCase()+((String)attrinfo).substring(1)
				: xmlattrname.getLocalPart().substring(0,1).toUpperCase()+xmlattrname.getLocalPart().substring(1);
			
			set = setDirectValue(new String[]{"set", "add"}, postfix, attrval, object, root, classloader, null);
		
			if(!set)
			{
				String oldpostfix = postfix;
				postfix = SUtil.getSingular(postfix);
				if(!postfix.equals(oldpostfix))
				{
					set = setDirectValue(new String[]{"set", "add"}, postfix, attrval, object, root, classloader, null);
				}
			}
		}
		
		if(!set)
			throw new RuntimeException("Failure in setting attribute: "+xmlattrname+" on object: "+object);
	}
	
	/**
	 *  Convert an attribute value by using a converter.
	 *  @param attrval The attribute value.
	 *  @param targetcalss The target class.
	 *  @param converter The converter.
	 *  @param root The root.
	 *  @param classloader The classloader.
	 */
	protected Object convertAttributeValue(Object attrval, Class targetclass, ITypeConverter converter, Object root, ClassLoader classloader)
	{
		Object ret = attrval;

//		if(converter!=null && !converter.acceptsInputType(attrval.getClass()))
//			System.out.println("hererrrrr: "+attrval+" "+converter);
		
		if(converter!=null)// && converter.acceptsInputType(attrval.getClass()))
		{
			ret = converter.convertObject(attrval, root, classloader, null);
		}
		else if(!String.class.isAssignableFrom(targetclass))
		{
			ITypeConverter conv = BasicTypeConverter.getBasicConverter(targetclass);
			if(conv!=null)// && conv.acceptsInputType(attrval.getClass()))
				ret = conv.convertObject(attrval, root, classloader, null);
		}
	
		return ret;
	}
	
	/**
	 *  Set a value directly on a Java bean.
	 *  @param prefixes The method prefixes.
	 *  @param postfix The mothod postfix.
	 *  @param attrval The attribute value.
	 *  @param object The object.
	 *  @param root The root.
	 *  @param classloader The classloader.
	 *  @param converter The converter.
	 */
	protected boolean setDirectValue(String[] prefixes, String postfix, Object attrval, Object object, Object root, ClassLoader classloader, ITypeConverter converter)
	{
		boolean set = false;
		
		for(int i=0; i<prefixes.length && !set; i++)
		{
			try
			{
			Method[] ms = SReflect.getMethods(object.getClass(), prefixes[i]+postfix);
			
			for(int j=0; j<ms.length && !set; j++)
			{
				Class[] ps = ms[j].getParameterTypes();
				if(ps.length==1)
				{
					Object arg = convertAttributeValue(attrval, ps[0], converter, root, classloader);
					
					try
					{
						ms[j].invoke(object, new Object[]{arg});
						set = true;
					}
					catch(Exception e)
					{
					}
				}
			}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		return set;
	}
	
	/**
	 *  Internal link objects method.
	 *  @param clazz The clazz.
	 *  @param name The name.
	 *  @param object The object.
	 *  @param parent The parent.
	 *  @param root The root.
	 *  @param classloader classloader.
	 */
	protected boolean internalLinkObjects(Class clazz, String name, Object object, Object parent, Object root, ClassLoader classloader) throws Exception
	{
		boolean ret = false;
			
		Method[] ms = SReflect.getMethods(parent.getClass(), name);
		for(int i=0; !ret && i<ms.length; i++)
		{
			Class[] ps = ms[i].getParameterTypes();
			if(ps.length==1)
			{
				if(ps[0].isAssignableFrom(clazz))
				{
					try
					{
						ms[i].invoke(parent, new Object[]{object});
						ret	= true;
					}
					catch(Exception e)
					{
					}
				}
				else if(object instanceof String)
				{
					ITypeConverter converter = BasicTypeConverter.getBasicConverter(ps[0]);
					if(converter != null)
					{
						try
						{
							object = converter.convertObject((String)object, root, classloader, null);
							ms[i].invoke(parent, new Object[]{object});
							ret	= true;
						}
						catch(Exception e)
						{
						}
					}
				}
			}
		}
		
		return ret;
	}
}
