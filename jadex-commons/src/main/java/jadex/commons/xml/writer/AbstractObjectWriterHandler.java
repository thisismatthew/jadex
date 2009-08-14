package jadex.commons.xml.writer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jadex.commons.SReflect;
import jadex.commons.xml.AbstractInfo;
import jadex.commons.xml.AttributeInfo;
import jadex.commons.xml.ITypeConverter;
import jadex.commons.xml.SubobjectInfo;
import jadex.commons.xml.TypeInfo;

/**
 *  Abstract base class for an object writer handler. Is object type agnostic and
 *  uses several abstract methods that have to be overridden by concrete handlers.
 */
public abstract class AbstractObjectWriterHandler implements IObjectWriterHandler
{
	//-------- attributes --------
	
	/** Control flag for generating container tags. */
	protected boolean gentypetags = true;
	
	/** The type mappings. */
	protected Map typeinfos;

	//-------- constructors --------
	
	/**
	 *  Create a new writer handler.
	 */
	public AbstractObjectWriterHandler(Set typeinfos)
	{
		this.typeinfos = createTypeInfos(typeinfos);
	}
	
	//-------- methods --------
	
	/**
	 *  Create type infos for each tag sorted by specificity.
	 *  @param linkinfos The mapping infos.
	 *  @return Map of mapping infos.
	 */
	protected Map createTypeInfos(Set typeinfos)
	{
		Map ret = new HashMap();
		
		for(Iterator it=typeinfos.iterator(); it.hasNext(); )
		{
			TypeInfo mapinfo = (TypeInfo)it.next();
			TreeSet maps = (TreeSet)ret.get(mapinfo.getTypeInfo());
			if(maps==null)
			{
				maps = new TreeSet(new AbstractInfo.SpecificityComparator());
				ret.put(mapinfo.getTypeInfo(), maps);
			}
			maps.add(mapinfo);
		}
		
		return ret;
	}
	
	/**
	 *  Get the most specific mapping info.
	 *  @param tag The tag.
	 *  @param fullpath The full path.
	 *  @return The most specific mapping info.
	 */
	public TypeInfo getTypeInfo(Object object, String[] fullpath, Object context)//, Map rawattributes)
	{
		Object type = getObjectType(object, context);
//		System.out.println("type is: "+type);
		TypeInfo ret = findTypeInfo((Set)typeinfos.get(type), fullpath);
		return ret;
	}
	
	/**
	 * 
	 */
	protected TypeInfo findTypeInfo(Set typeinfos, String[] fullpath)
	{
		TypeInfo ret = null;
		if(typeinfos!=null)
		{
			for(Iterator it=typeinfos.iterator(); ret==null && it.hasNext(); )
			{
				TypeInfo ti = (TypeInfo)it.next();
				String[] tmp = ti.getXMLPathElementsWithoutElement();
				boolean ok = tmp==null || tmp.length<=fullpath.length;;
				if(tmp!=null)
				{
					for(int i=1; i<=tmp.length && ok; i++)
					{
						ok = tmp[tmp.length-i].equals(fullpath[fullpath.length-i]);
					}
				}
				if(ok)
					ret = ti;
//				if(fullpath.endsWith(tmp.getXMLPathWithoutElement())) // && (tmp.getFilter()==null || tmp.getFilter().filter(rawattributes)))
			}
		}
		return ret;
	}
	
	/**
	 *  Get the object type
	 *  @param object The object.
	 *  @return The object type.
	 */
	public abstract Object getObjectType(Object object, Object context);
		
	/**
	 *  Get the tag name for an object.
	 */
	public abstract String getTagName(Object object, Object context);
	
	/**
	 *  Get write info for an object.
	 */
	public WriteObjectInfo getObjectWriteInfo(Object object, TypeInfo typeinfo, Object context, ClassLoader classloader)
	{
		// todo: conversion value to string
		
		WriteObjectInfo wi = new WriteObjectInfo();
		HashSet doneprops = new HashSet();
		
		if(typeinfo!=null)
		{
			// Comment
			
			Object info = typeinfo.getCommentInfo();
			if(info!=null)
			{
				Object property = getProperty(info);
				if(property!=null)
				{
					doneprops.add(getPropertyName(property));
					if(!(info instanceof AttributeInfo && ((AttributeInfo)info).isIgnoreWrite()))
					{
						try
						{
							Object value = getValue(object, property, context, info);
							if(value!=null)
							{
								value = convertValue(info, value, classloader, context);
								wi.setComment(value.toString());
							}
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
					}
				}
			}
			
			// Content
			
			info = typeinfo.getContentInfo();
			if(info!=null)
			{
				Object property = getProperty(info);
				if(property!=null)
				{
					doneprops.add(getPropertyName(property));
					if(!(info instanceof AttributeInfo && ((AttributeInfo)info).isIgnoreWrite()))
					{
						try
						{
							Object value = getValue(object, property, context, info);
							if(value!=null)
							{
								value = convertValue(info, value, classloader, context);
								wi.setContent(value.toString());
							}
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
					}
				}
			}
			
			// Attributes
			
			Collection attrinfos = typeinfo.getAttributeInfos();
			if(attrinfos!=null)
			{
				for(Iterator it=attrinfos.iterator(); it.hasNext(); )
				{
					info = it.next();
					Object property = getProperty(info);
					if(property!=null)
					{
						doneprops.add(getPropertyName(property));
						if(!(info instanceof AttributeInfo && ((AttributeInfo)info).isIgnoreWrite()))
						{	
							try
							{
								Object value = getValue(object, property, context, info);
								if(value!=null)
								{
									Object defval = getDefaultValue(info);
									
									if(!value.equals(defval))
									{
										String xmlattrname = null;
										if(info instanceof AttributeInfo)
											xmlattrname = ((AttributeInfo)info).getXMLAttributeName();
										if(xmlattrname==null)
											xmlattrname = getPropertyName(property);
										
										value = convertValue(info, value, classloader, context);
										wi.addAttribute(xmlattrname, value.toString());
									}
								}
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						}
					}
				}
			}
			
			// Subobjects 
			
			Collection subobsinfos = typeinfo.getSubobjectInfos();
			if(subobsinfos!=null)
			{
				for(Iterator it=subobsinfos.iterator(); it.hasNext(); )
				{
					try
					{
						SubobjectInfo soinfo = (SubobjectInfo)it.next();
						info = soinfo.getLinkInfo();
						TypeInfo sotypeinfo = soinfo.getTypeInfo();
						Object property = getProperty(info);
						if(property!=null)
						{
							doneprops.add(getPropertyName(property));
							if(!(info instanceof AttributeInfo && ((AttributeInfo)info).isIgnoreWrite()))
							{	
								String propname = getPropertyName(property);
								Object value = getValue(object, property, context, info);
								if(value!=null)
								{
//									String xmlsoname = soinfo.getXMLPath()!=null? soinfo.getXMLPath(): getPropertyName(property);
									String[] xmlpath = soinfo.getXMLPathElements();
									if(xmlpath==null)
										xmlpath = new String[]{getPropertyName(property)};
									
//									if(soinfo.isMulti() || value.getClass().isArray()
//										|| (property.equals(AttributeInfo.THIS) && SReflect.isIterable(value)))
									// Fetch elements directly if it is a multi subobject
									if(soinfo.isMulti())
									{
										Iterator it2 = SReflect.getIterator(value);
										while(it2.hasNext())
										{
											Object val = it2.next();
											
											if(isTypeCompatible(val, sotypeinfo, context))
											{
												String[] tmp = xmlpath;
												if(gentypetags)
												{
													tmp = new String[xmlpath.length+1];
													System.arraycopy(xmlpath, 0, tmp, 0, xmlpath.length);
													tmp[tmp.length-1] = getTagName(val, context);
												}
//												String pathname = gentypetags? xmlsoname+"/"+getTagName(val, context): xmlsoname;
												wi.addSubobject(tmp, val);
											}
										}
									}
									else
									{
										if(isTypeCompatible(value, sotypeinfo, context))
										{
//											String pathname = gentypetags? xmlsoname+"/"+getTagName(value, context): xmlsoname;
											String[] tmp = xmlpath;
											if(gentypetags)
											{
												tmp = new String[xmlpath.length+1];
												System.arraycopy(xmlpath, 0, tmp, 0, xmlpath.length);
												tmp[tmp.length-1] = getTagName(value, context);
											}
											wi.addSubobject(tmp, value);
										}
									}
								}
							}
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
			
		Collection props = getProperties(object, context);
		if(props!=null)
		{
			for(Iterator it=props.iterator(); it.hasNext(); )
			{
				Object property = it.next();
				String propname = getPropertyName(property);

				if(!doneprops.contains(propname))
				{
					doneprops.add(propname);
					try
					{
						Object value = getValue(object, property, context, null);
		
						if(value!=null)
						{
							if(isBasicType(property, value))
							{
								if(!value.equals(getDefaultValue(property)))
									wi.addAttribute(propname, value.toString());
							}
							else
							{
								// todo: remove
								// Hack special case array, todo: support generically via typeinfo???
								if(value.getClass().isArray())
								{
									Iterator it2 = SReflect.getIterator(value);
									if(it2.hasNext())
									{
										while(it2.hasNext())
										{
											Object val = it2.next();
											String[] xmlpath = new String[]{propname};
											String[] tmp = xmlpath;
											if(gentypetags)
											{
												tmp = new String[xmlpath.length+1];
												System.arraycopy(xmlpath, 0, tmp, 0, xmlpath.length);
												tmp[tmp.length-1] = getTagName(val, context);
											}
											wi.addSubobject(tmp, val);
										}
									}
								}
								else
								{
									String[] xmlpath = new String[]{propname};
									String[] tmp = xmlpath;
									if(gentypetags)
									{
										tmp = new String[xmlpath.length+1];
										System.arraycopy(xmlpath, 0, tmp, 0, xmlpath.length);
										tmp[tmp.length-1] = getTagName(value, context);
									}
									wi.addSubobject(tmp, value);
								}
							}
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		
		// Special case that no info about object was found.
		// Hack?!
		if(wi.getAttributes()==null && wi.getSubobjects()==null && wi.getContent()==null)
		{
			// todo: use prewriter
			wi.setContent(object.toString());
		}
		
//		System.out.println("wi: "+object+" "+wi.getContent()+" "+wi.getSubobjects());
		
		return wi;
	}
	
	/**
	 *  Convert a value before writing.
	 */
	protected Object convertValue(Object info, Object value, ClassLoader classloader, Object context)
	{
		Object ret = value;
		if(info instanceof AttributeInfo)
		{
			ITypeConverter conv = ((AttributeInfo)info).getConverterWrite();
			if(conv!=null)
			{
				ret = conv.convertObject(value, null, classloader, context);
			}
		}
		return ret;
	}
	
	/**
	 *  Get the default value.
	 */
	protected Object getDefaultValue(Object property)
	{
		Object ret = null;
		if(property instanceof AttributeInfo)
		{
			ret = ((AttributeInfo)property).getDefaultValue();
		}
		return ret;
	}
	
	/**
	 *  Get a value from an object.
	 */
	protected abstract Object getValue(Object object, Object attr, Object context, Object info);
	
	/**
	 *  Get the property.
	 */
	protected abstract Object getProperty(Object info);
	
	/**
	 *  Get the name of a property.
	 */
	protected abstract String getPropertyName(Object property);

	/**
	 *  Get the properties of an object. 
	 */
	protected abstract Collection getProperties(Object object, Object context);

	/**
	 *  Test is a value is a basic type (and can be mapped to an attribute).
	 */
	protected abstract boolean isBasicType(Object property, Object value);
	
	/**
	 *  Test if a value is compatible with the defined typeinfo.
	 */
	protected abstract boolean isTypeCompatible(Object object, TypeInfo info, Object context);
}
