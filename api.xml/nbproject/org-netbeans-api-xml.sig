#Signature file v4.1
#Version 1.30.1

CLSS public java.beans.FeatureDescriptor
cons public init()
meth public boolean isExpert()
meth public boolean isHidden()
meth public boolean isPreferred()
meth public java.lang.Object getValue(java.lang.String)
meth public java.lang.String getDisplayName()
meth public java.lang.String getName()
meth public java.lang.String getShortDescription()
meth public java.util.Enumeration<java.lang.String> attributeNames()
meth public void setDisplayName(java.lang.String)
meth public void setExpert(boolean)
meth public void setHidden(boolean)
meth public void setName(java.lang.String)
meth public void setPreferred(boolean)
meth public void setShortDescription(java.lang.String)
meth public void setValue(java.lang.String,java.lang.Object)
supr java.lang.Object
hfds classRef,displayName,expert,hidden,name,preferred,shortDescription,table

CLSS public java.lang.Object
cons public init()
meth protected java.lang.Object clone() throws java.lang.CloneNotSupportedException
meth protected void finalize() throws java.lang.Throwable
meth public boolean equals(java.lang.Object)
meth public final java.lang.Class<?> getClass()
meth public final void notify()
meth public final void notifyAll()
meth public final void wait() throws java.lang.InterruptedException
meth public final void wait(long) throws java.lang.InterruptedException
meth public final void wait(long,int) throws java.lang.InterruptedException
meth public int hashCode()
meth public java.lang.String toString()

CLSS public abstract interface org.netbeans.api.xml.cookies.CheckXMLCookie
intf org.openide.nodes.Node$Cookie
meth public abstract boolean checkXML(org.netbeans.api.xml.cookies.CookieObserver)

CLSS public final org.netbeans.api.xml.cookies.CookieMessage
cons public init(java.lang.String)
cons public init(java.lang.String,int)
cons public init(java.lang.String,int,java.lang.Object)
cons public init(java.lang.String,int,org.openide.util.Lookup)
cons public init(java.lang.String,java.lang.Object)
fld public final static int ERROR_LEVEL = 2
fld public final static int FATAL_ERROR_LEVEL = 3
fld public final static int INFORMATIONAL_LEVEL = 0
fld public final static int WARNING_LEVEL = 1
meth public final int getLevel()
meth public java.lang.Object getDetail(java.lang.Class)
meth public java.lang.String getMessage()
meth public org.openide.util.Lookup getDetails()
supr java.lang.Object
hfds details,level,message

CLSS public abstract interface org.netbeans.api.xml.cookies.CookieObserver
meth public abstract void receive(org.netbeans.api.xml.cookies.CookieMessage)

CLSS public abstract interface org.netbeans.api.xml.cookies.TransformableCookie
intf org.openide.nodes.Node$Cookie
meth public abstract void transform(javax.xml.transform.Source,javax.xml.transform.Result,org.netbeans.api.xml.cookies.CookieObserver) throws javax.xml.transform.TransformerException

CLSS public abstract interface org.netbeans.api.xml.cookies.ValidateXMLCookie
intf org.openide.nodes.Node$Cookie
meth public abstract boolean validateXML(org.netbeans.api.xml.cookies.CookieObserver)

CLSS public abstract org.netbeans.api.xml.cookies.XMLProcessorDetail
cons public init()
meth public abstract int getColumnNumber()
meth public abstract int getLineNumber()
meth public abstract java.lang.Exception getException()
meth public abstract java.lang.String getPublicId()
meth public abstract java.lang.String getSystemId()
supr java.lang.Object

CLSS public final org.netbeans.api.xml.parsers.DocumentInputSource
cons public init(javax.swing.text.Document)
meth public final void setCharacterStream(java.io.Reader)
meth public java.io.Reader getCharacterStream()
meth public java.lang.String getSystemId()
meth public java.lang.String toString()
supr org.xml.sax.InputSource
hfds class$org$openide$ErrorManager,doc

CLSS public org.netbeans.api.xml.parsers.SAXEntityParser
cons public init(org.xml.sax.XMLReader)
cons public init(org.xml.sax.XMLReader,boolean)
intf org.xml.sax.XMLReader
meth protected boolean propagateException(org.xml.sax.SAXParseException)
meth protected org.xml.sax.InputSource wrapInputSource(org.xml.sax.InputSource)
meth public boolean getFeature(java.lang.String) throws org.xml.sax.SAXNotRecognizedException,org.xml.sax.SAXNotSupportedException
meth public java.lang.Object getProperty(java.lang.String) throws org.xml.sax.SAXNotRecognizedException,org.xml.sax.SAXNotSupportedException
meth public org.xml.sax.ContentHandler getContentHandler()
meth public org.xml.sax.DTDHandler getDTDHandler()
meth public org.xml.sax.EntityResolver getEntityResolver()
meth public org.xml.sax.ErrorHandler getErrorHandler()
meth public void parse(java.lang.String) throws java.io.IOException,org.xml.sax.SAXException
meth public void parse(org.xml.sax.InputSource) throws java.io.IOException,org.xml.sax.SAXException
meth public void setContentHandler(org.xml.sax.ContentHandler)
meth public void setDTDHandler(org.xml.sax.DTDHandler)
meth public void setEntityResolver(org.xml.sax.EntityResolver)
meth public void setErrorHandler(org.xml.sax.ErrorHandler)
meth public void setFeature(java.lang.String,boolean) throws org.xml.sax.SAXNotRecognizedException,org.xml.sax.SAXNotSupportedException
meth public void setProperty(java.lang.String,java.lang.Object) throws org.xml.sax.SAXNotRecognizedException,org.xml.sax.SAXNotSupportedException
supr java.lang.Object
hfds FAKE_PUBLIC_ID,FAKE_SYSTEM_ID,RANDOM,generalEntity,peer,used
hcls EH,ER

CLSS public abstract org.netbeans.api.xml.services.UserCatalog
cons public init()
meth public java.util.Iterator getPublicIDs()
meth public javax.xml.transform.URIResolver getURIResolver()
meth public org.xml.sax.EntityResolver getEntityResolver()
meth public static org.netbeans.api.xml.services.UserCatalog getDefault()
supr java.lang.Object
hfds class$org$netbeans$api$xml$services$UserCatalog

CLSS public org.netbeans.spi.xml.cookies.CheckXMLSupport
cons public init(org.xml.sax.InputSource)
cons public init(org.xml.sax.InputSource,int)
fld public final static int CHECK_ENTITY_MODE = 1
fld public final static int CHECK_PARAMETER_ENTITY_MODE = 2
fld public final static int DOCUMENT_MODE = 3
intf org.netbeans.api.xml.cookies.CheckXMLCookie
meth protected org.xml.sax.EntityResolver createEntityResolver()
meth protected org.xml.sax.InputSource createInputSource() throws java.io.IOException
meth protected org.xml.sax.XMLReader createParser(boolean)
meth public boolean checkXML(org.netbeans.api.xml.cookies.CookieObserver)
supr java.lang.Object

CLSS public final org.netbeans.spi.xml.cookies.DataObjectAdapters
meth public static javax.xml.transform.Source source(org.openide.loaders.DataObject)
meth public static org.xml.sax.InputSource inputSource(org.openide.loaders.DataObject)
supr java.lang.Object
hfds SAX_FEATURES_NAMESPACES,class$org$openide$cookies$EditorCookie,saxParserFactory
hcls DataObjectInputSource,DataObjectSAXSource

CLSS public org.netbeans.spi.xml.cookies.DefaultXMLProcessorDetail
cons public init(javax.xml.transform.TransformerException)
cons public init(org.xml.sax.SAXParseException)
meth public int getColumnNumber()
meth public int getLineNumber()
meth public java.lang.Exception getException()
meth public java.lang.String getPublicId()
meth public java.lang.String getSystemId()
supr org.netbeans.api.xml.cookies.XMLProcessorDetail
hfds columnNumber,exception,lineNumber,publicId,systemId

CLSS public final org.netbeans.spi.xml.cookies.TransformableSupport
cons public init(javax.xml.transform.Source)
intf org.netbeans.api.xml.cookies.TransformableCookie
meth public void transform(javax.xml.transform.Source,javax.xml.transform.Result,org.netbeans.api.xml.cookies.CookieObserver) throws javax.xml.transform.TransformerException
supr java.lang.Object
hfds source,transformerFactory
hcls ExceptionWriter,Proxy

CLSS public org.netbeans.spi.xml.cookies.ValidateXMLSupport
cons public init(org.xml.sax.InputSource)
intf org.netbeans.api.xml.cookies.ValidateXMLCookie
meth protected org.xml.sax.EntityResolver createEntityResolver()
meth protected org.xml.sax.InputSource createInputSource() throws java.io.IOException
meth protected org.xml.sax.XMLReader createParser(boolean)
meth public boolean validateXML(org.netbeans.api.xml.cookies.CookieObserver)
supr java.lang.Object

CLSS public abstract org.openide.nodes.Node
cons protected init(org.openide.nodes.Children)
cons protected init(org.openide.nodes.Children,org.openide.util.Lookup)
fld public final static java.lang.String PROP_COOKIE = "cookie"
fld public final static java.lang.String PROP_DISPLAY_NAME = "displayName"
fld public final static java.lang.String PROP_ICON = "icon"
fld public final static java.lang.String PROP_LEAF = "leaf"
fld public final static java.lang.String PROP_NAME = "name"
fld public final static java.lang.String PROP_OPENED_ICON = "openedIcon"
fld public final static java.lang.String PROP_PARENT_NODE = "parentNode"
fld public final static java.lang.String PROP_PROPERTY_SETS = "propertySets"
fld public final static java.lang.String PROP_SHORT_DESCRIPTION = "shortDescription"
fld public final static org.openide.nodes.Node EMPTY
innr public abstract interface static Cookie
innr public abstract interface static Handle
innr public abstract static IndexedProperty
innr public abstract static Property
innr public abstract static PropertySet
intf org.openide.util.HelpCtx$Provider
intf org.openide.util.Lookup$Provider
meth protected final boolean hasPropertyChangeListener()
meth protected final void fireCookieChange()
meth protected final void fireDisplayNameChange(java.lang.String,java.lang.String)
meth protected final void fireIconChange()
meth protected final void fireNameChange(java.lang.String,java.lang.String)
meth protected final void fireNodeDestroyed()
meth protected final void fireOpenedIconChange()
meth protected final void firePropertyChange(java.lang.String,java.lang.Object,java.lang.Object)
meth protected final void firePropertySetsChange(org.openide.nodes.Node$PropertySet[],org.openide.nodes.Node$PropertySet[])
meth protected final void fireShortDescriptionChange(java.lang.String,java.lang.String)
meth protected final void setChildren(org.openide.nodes.Children)
meth protected java.lang.Object clone() throws java.lang.CloneNotSupportedException
meth public <%0 extends org.openide.nodes.Node$Cookie> {%%0} getCookie(java.lang.Class<{%%0}>)
meth public abstract boolean canCopy()
meth public abstract boolean canCut()
meth public abstract boolean canDestroy()
meth public abstract boolean canRename()
meth public abstract boolean hasCustomizer()
meth public abstract java.awt.Component getCustomizer()
meth public abstract java.awt.Image getIcon(int)
meth public abstract java.awt.Image getOpenedIcon(int)
meth public abstract java.awt.datatransfer.Transferable clipboardCopy() throws java.io.IOException
meth public abstract java.awt.datatransfer.Transferable clipboardCut() throws java.io.IOException
meth public abstract java.awt.datatransfer.Transferable drag() throws java.io.IOException
meth public abstract org.openide.nodes.Node cloneNode()
meth public abstract org.openide.nodes.Node$Handle getHandle()
meth public abstract org.openide.nodes.Node$PropertySet[] getPropertySets()
meth public abstract org.openide.util.HelpCtx getHelpCtx()
meth public abstract org.openide.util.datatransfer.NewType[] getNewTypes()
meth public abstract org.openide.util.datatransfer.PasteType getDropType(java.awt.datatransfer.Transferable,int,int)
meth public abstract org.openide.util.datatransfer.PasteType[] getPasteTypes(java.awt.datatransfer.Transferable)
meth public boolean equals(java.lang.Object)
meth public final boolean isLeaf()
meth public final javax.swing.JPopupMenu getContextMenu()
meth public final org.openide.nodes.Children getChildren()
meth public final org.openide.nodes.Node getParentNode()
meth public final org.openide.util.Lookup getLookup()
meth public final void addNodeListener(org.openide.nodes.NodeListener)
meth public final void addPropertyChangeListener(java.beans.PropertyChangeListener)
meth public final void removeNodeListener(org.openide.nodes.NodeListener)
meth public final void removePropertyChangeListener(java.beans.PropertyChangeListener)
meth public int hashCode()
meth public java.lang.String getHtmlDisplayName()
meth public java.lang.String toString()
meth public javax.swing.Action getPreferredAction()
meth public javax.swing.Action[] getActions(boolean)
meth public org.openide.util.actions.SystemAction getDefaultAction()
 anno 0 java.lang.Deprecated()
meth public org.openide.util.actions.SystemAction[] getActions()
 anno 0 java.lang.Deprecated()
meth public org.openide.util.actions.SystemAction[] getContextActions()
 anno 0 java.lang.Deprecated()
meth public void destroy() throws java.io.IOException
meth public void setDisplayName(java.lang.String)
meth public void setHidden(boolean)
 anno 0 java.lang.Deprecated()
meth public void setName(java.lang.String)
meth public void setShortDescription(java.lang.String)
supr java.beans.FeatureDescriptor
hfds INIT_LOCK,LOCK,TEMPL_COOKIE,err,hierarchy,listeners,lookups,parent,warnedBadProperties
hcls LookupEventList

CLSS public abstract interface static org.openide.nodes.Node$Cookie
 outer org.openide.nodes.Node

CLSS public final org.openide.util.HelpCtx
cons public init(java.lang.Class<?>)
 anno 0 java.lang.Deprecated()
cons public init(java.lang.String)
cons public init(java.net.URL)
 anno 0 java.lang.Deprecated()
fld public final static org.openide.util.HelpCtx DEFAULT_HELP
innr public abstract interface static Displayer
innr public abstract interface static Provider
meth public boolean display()
meth public boolean equals(java.lang.Object)
meth public int hashCode()
meth public java.lang.String getHelpID()
meth public java.lang.String toString()
meth public java.net.URL getHelp()
meth public static org.openide.util.HelpCtx findHelp(java.awt.Component)
meth public static org.openide.util.HelpCtx findHelp(java.lang.Object)
meth public static void setHelpIDString(javax.swing.JComponent,java.lang.String)
supr java.lang.Object
hfds err,helpCtx,helpID

CLSS public abstract interface static org.openide.util.HelpCtx$Provider
 outer org.openide.util.HelpCtx
meth public abstract org.openide.util.HelpCtx getHelpCtx()

CLSS public abstract org.openide.util.Lookup
cons public init()
fld public final static org.openide.util.Lookup EMPTY
innr public abstract interface static Provider
innr public abstract static Item
innr public abstract static Result
innr public final static Template
meth public <%0 extends java.lang.Object> java.util.Collection<? extends {%%0}> lookupAll(java.lang.Class<{%%0}>)
meth public <%0 extends java.lang.Object> org.openide.util.Lookup$Item<{%%0}> lookupItem(org.openide.util.Lookup$Template<{%%0}>)
meth public <%0 extends java.lang.Object> org.openide.util.Lookup$Result<{%%0}> lookupResult(java.lang.Class<{%%0}>)
meth public abstract <%0 extends java.lang.Object> org.openide.util.Lookup$Result<{%%0}> lookup(org.openide.util.Lookup$Template<{%%0}>)
meth public abstract <%0 extends java.lang.Object> {%%0} lookup(java.lang.Class<{%%0}>)
meth public static org.openide.util.Lookup getDefault()
supr java.lang.Object
hfds defaultLookup
hcls DefLookup,Empty

CLSS public abstract interface static org.openide.util.Lookup$Provider
 outer org.openide.util.Lookup
meth public abstract org.openide.util.Lookup getLookup()

CLSS public org.xml.sax.InputSource
cons public init()
cons public init(java.io.InputStream)
cons public init(java.io.Reader)
cons public init(java.lang.String)
meth public java.io.InputStream getByteStream()
meth public java.io.Reader getCharacterStream()
meth public java.lang.String getEncoding()
meth public java.lang.String getPublicId()
meth public java.lang.String getSystemId()
meth public void setByteStream(java.io.InputStream)
meth public void setCharacterStream(java.io.Reader)
meth public void setEncoding(java.lang.String)
meth public void setPublicId(java.lang.String)
meth public void setSystemId(java.lang.String)
supr java.lang.Object
hfds byteStream,characterStream,encoding,publicId,systemId

CLSS public abstract interface org.xml.sax.XMLReader
meth public abstract boolean getFeature(java.lang.String) throws org.xml.sax.SAXNotRecognizedException,org.xml.sax.SAXNotSupportedException
meth public abstract java.lang.Object getProperty(java.lang.String) throws org.xml.sax.SAXNotRecognizedException,org.xml.sax.SAXNotSupportedException
meth public abstract org.xml.sax.ContentHandler getContentHandler()
meth public abstract org.xml.sax.DTDHandler getDTDHandler()
meth public abstract org.xml.sax.EntityResolver getEntityResolver()
meth public abstract org.xml.sax.ErrorHandler getErrorHandler()
meth public abstract void parse(java.lang.String) throws java.io.IOException,org.xml.sax.SAXException
meth public abstract void parse(org.xml.sax.InputSource) throws java.io.IOException,org.xml.sax.SAXException
meth public abstract void setContentHandler(org.xml.sax.ContentHandler)
meth public abstract void setDTDHandler(org.xml.sax.DTDHandler)
meth public abstract void setEntityResolver(org.xml.sax.EntityResolver)
meth public abstract void setErrorHandler(org.xml.sax.ErrorHandler)
meth public abstract void setFeature(java.lang.String,boolean) throws org.xml.sax.SAXNotRecognizedException,org.xml.sax.SAXNotSupportedException
meth public abstract void setProperty(java.lang.String,java.lang.Object) throws org.xml.sax.SAXNotRecognizedException,org.xml.sax.SAXNotSupportedException
