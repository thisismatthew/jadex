package jadex.xml.writer;

import jadex.commons.SReflect;
import jadex.xml.AccessInfo;
import jadex.xml.AttributeInfo;
import jadex.xml.IContext;
import jadex.xml.IObjectStringConverter;
import jadex.xml.SubobjectInfo;
import jadex.xml.TypeInfo;
import jadex.xml.TypeInfoTypeManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;

/**
 *  Abstract base class for an object writer handler. Is object type agnostic and
 *  uses several abstract methods that have to be overridden by concrete handlers.
 */
public abstract class AbstractObjectWriterHandler implements IObjectWriterHandler
{
	//-------- attributes --------
	
	/** Control flag for generating container tags. */
	protected boolean gentypetags;
	
	/** The flattening flag for tags, i.e. generate always new containing tags or use one. */
	protected boolean flattening = true;
	
	/** The type info manager. */
	protected TypeInfoTypeManager titmanager;
	
	//-------- constructors --------
	
	/**
	 *  Create a new writer handler.
	 */
	public AbstractObjectWriterHandler(Set typeinfos)
	{
		this(true, typeinfos);
	}
	
	/**
	 *  Create a new writer handler.
	 */
	public AbstractObjectWriterHandler(boolean gentypetags, Set typeinfos)
	{
		this.gentypetags = gentypetags;
		this.titmanager = new TypeInfoTypeManager(typeinfos);
	}
	
	//-------- methods --------
	
	/**
	 *  Get the most specific mapping info.
	 *  @param type The type.
	 *  @param fullpath The full path.
	 *  @return The most specific mapping info.
	 */
	public TypeInfo getTypeInfo(Object object, QName[] fullpath, IContext context)
	{
		Object type = getObjectType(object, context);
		return titmanager.getTypeInfo(type, fullpath);
	}
	
	/**
	 *  Get the object type
	 *  @param object The object.
	 *  @return The object type.
	 */
	public abstract Object getObjectType(Object object, IContext context);
	
	/**
	 *  Get write info for an object.
	 */
	public WriteObjectInfo getObjectWriteInfo(Object object, TypeInfo typeinfo, IContext context)
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
				Object property = getProperty(info);//==null? AttributeInfo.COMMENT: getProperty(info);
				if(property!=null)
				{
					doneprops.add(getPropertyName(property));
					if(!(info instanceof AttributeInfo && ((AttributeInfo)info).isIgnoreWrite()))
					{
						Object value = getValue(object, property, context, info);
						if(value!=null)
						{
							value = convertValue(info, value, context);
							wi.setComment(value.toString());
						}
					}
				}
			}
			
			// Content
			
			info = typeinfo.getContentInfo();
			if(info!=null)
			{
				Object property = getProperty(info);//==null? AttributeInfo.CONTENT: getProperty(info);
				if(property!=null)
				{
					doneprops.add(getPropertyName(property));
					if(!(info instanceof AttributeInfo && ((AttributeInfo)info).isIgnoreWrite()))
					{
						Object value = getValue(object, property, context, info);
						if(value!=null)
						{
							value = convertValue(info, value, context);
							wi.setContent(value.toString());
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
							Object value = getValue(object, property, context, info);	
							
							if(value!=null)
							{
								// When attribute is a IDREF then 
								// a) find type info for current value
								// b) find id attribute
								// c) find id attribute value
								AttributeInfo attrinfo = (AttributeInfo)info;
								if(AttributeInfo.IDREF.equals(attrinfo.getId()))
								{
									Set tis = titmanager.getTypeInfosByType(value.getClass());
									if(tis==null || tis.size()!=1)
										throw new RuntimeException("Could not determine type info for idref object: "+value);
									TypeInfo ti = (TypeInfo)tis.iterator().next();
									
									AttributeInfo idinfo = null;
									if(ti.getAttributeInfos()!=null)
									{
										for(Iterator it2 = ti.getAttributeInfos().iterator(); idinfo==null && it2.hasNext(); )
										{
											AttributeInfo tmp = (AttributeInfo)it2.next();
											if(AttributeInfo.ID.equals(tmp.getId()))
												idinfo = tmp;
										}
									}
									if(idinfo==null && (ti.getCommentInfo() instanceof AttributeInfo) 
										&& AttributeInfo.ID.equals(((AttributeInfo)ti.getCommentInfo()).getId()))
									{
										idinfo = ((AttributeInfo)ti.getCommentInfo());
									}
									if(idinfo==null && (ti.getContentInfo() instanceof AttributeInfo) 
										&& AttributeInfo.ID.equals(((AttributeInfo)ti.getContentInfo()).getId()))
									{
										idinfo = ((AttributeInfo)ti.getContentInfo());
									}
									
									if(idinfo==null)
										throw new RuntimeException("Could not determine id attribute of type info: "+ti);
									
									Object prop = getProperty(idinfo);
									value = getValue(value, prop, context, idinfo);
									
//									System.out.println("Found id value: "+value);
								}
								
								Object defval = getDefaultValue(info);
								if(!value.equals(defval))
								{
									value = convertValue(info, value, context);
									
									// Do we want sometimes to write default values?
									Object xmlattrname = null;
									if(info instanceof AttributeInfo)
										xmlattrname = ((AttributeInfo)info).getXMLAttributeName();
									if(xmlattrname==null)
										xmlattrname = getPropertyName(property);
									
									wi.addAttribute(xmlattrname, value.toString());
								}
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
					SubobjectInfo soinfo = (SubobjectInfo)it.next();
					info = soinfo.getAccessInfo();
					TypeInfo sotypeinfo = soinfo.getTypeInfo();
					Object property = getProperty(info);
					if(property!=null)
					{
						doneprops.add(getPropertyName(property));
						if(!(info instanceof AccessInfo && ((AccessInfo)info).isIgnoreWrite()))
						{	
							String propname = getPropertyName(property);
							Object value = getValue(object, property, context, info);
							if(value!=null)
							{
//								String xmlsoname = soinfo.getXMLPath()!=null? soinfo.getXMLPath(): getPropertyName(property);
								QName[] xmlpath = soinfo.getXMLPathElements();
								if(xmlpath==null)
									xmlpath = new QName[]{QName.valueOf(getPropertyName(property))};
								
								// Fetch elements directly if it is a multi subobject
								if(soinfo.isMulti())
								{
									Iterator it2 = SReflect.getIterator(value);
									while(it2.hasNext())
									{
										Object val = it2.next();
										
										if(isTypeCompatible(val, sotypeinfo, context))
										{
											QName[] path = createPath(xmlpath, val, context);
											// todo: extract flattening info from subobject info
											wi.addSubobject(path, val, flattening);
										}
									}
								}
								else
								{
									if(isTypeCompatible(value, sotypeinfo, context))
									{
										QName[] path = createPath(xmlpath, value, context);
										// todo: extract flattening info from subobject info
										wi.addSubobject(path, value, flattening);
									}
								}
							}
						}
					}
				}
			}
		}
			
		// Get properties from type inspection.
		
		Collection props = getProperties(object, context, typeinfo!=null? typeinfo.isIncludeFields(): false);
		if(props!=null)
		{
			for(Iterator it=props.iterator(); it.hasNext(); )
			{
				Object property = it.next();
				String propname = getPropertyName(property);

				if(!doneprops.contains(propname))
				{
					doneprops.add(propname);
					Object value = getValue(object, property, context, null);
	
					if(value!=null)
					{
						// Make to an attribute when
						// a) it is a basic type
						// b) it can be decoded to the right object type
						boolean prefertags = typeinfo!=null && typeinfo.getMappingInfo()!=null && typeinfo.getMappingInfo().isPreferTags();
						if(!prefertags && isBasicType(property, value) && isDecodableToSameType(property, value, context))
						{
							if(!value.equals(getDefaultValue(property)))
								wi.addAttribute(propname, value.toString());
						}
						else
						{
							// todo: remove
							// Hack special case array, todo: support generically via typeinfo???
							QName[] xmlpath = new QName[]{QName.valueOf(propname)};
							QName[] path = createPath(xmlpath, value, context);
							// todo: use some default for flattening
							wi.addSubobject(path, value, flattening);
						}
					}
				}
			}
		}
		
		// Special case that no info about object was found.
		// Hack?!
		if(typeinfo==null && wi.getAttributes()==null && wi.getSubobjects()==null && wi.getContent()==null && doneprops.size()==0)
		{
			// todo: use prewriter
//			System.out.println("Special case for content: "+object);
			wi.setContent(object.toString());
		}
		
//		System.out.println("wi: "+object+" "+wi.getContent()+" "+wi.getSubobjects());
		
		return wi;
	}
	
	/**
	 *  Create a qname path.
	 */
	protected QName[] createPath(QName[] xmlpath, Object value, IContext context)
	{
		QName[] ret = xmlpath;
		if(gentypetags)
		{
			ret = new QName[xmlpath.length+1];
			System.arraycopy(xmlpath, 0, ret, 0, xmlpath.length);
			QName tag = getTagName(value, context);
			ret[ret.length-1] = tag;
		}
		return ret;
	}
	
	/**
	 *  Convert a value before writing.
	 */
	protected Object convertValue(Object info, Object value, IContext context)
	{
		Object ret = value;
		if(info instanceof AttributeInfo)
		{
			IObjectStringConverter conv = ((AttributeInfo)info).getConverter();
			if(conv!=null)
			{
				ret = conv.convertObject(value, context);
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
			ret = ((AttributeInfo)property).getAccessInfo().getDefaultValue();
		}
		return ret;
	}
	
	/**
	 *  Get a value from an object.
	 */
	protected abstract Object getValue(Object object, Object attr, IContext context, Object info);
	
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
	protected abstract Collection getProperties(Object object, IContext context, boolean includefields);

	/**
	 *  Test is a value is a basic type (and can be mapped to an attribute).
	 */
	protected abstract boolean isBasicType(Object property, Object value);
	
	/**
	 *  Test if a value is compatible with the defined typeinfo.
	 */
	protected abstract boolean isTypeCompatible(Object object, TypeInfo info, IContext context);
	
	/**
	 *  Test if a value is decodable to the same type.
	 */
	protected abstract boolean isDecodableToSameType(Object property, Object value, IContext context); 
}
