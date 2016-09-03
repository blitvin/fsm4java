/*
 * (C) Copyright Boris Litvin 2014, 2015
 * This file is part of FSM4Java library.
 *
 *  FSM4Java is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   FSM4Java is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with FSM4Java  If not, see <http://www.gnu.org/licenses/>.
 */
package org.blitvin.statemachine;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * DOMStateMachineFactory is factory constructing instances of StateMachine
 * from description supplied by xml file. The file should conform to state_machines.xsd specification.
 * File name can be supplied explicitly in constructor argument, resetFactory method argument or, for 
 * default file name , by property org.blitvin.statemachine.DOMStateMachineFactoryImplementation.defaultXmlFileName
 * @author blitvin
 *
 */
public class DOMStateMachineFactory extends	StateMachineFactory {
	
	
	/* xml tags */
	static final String STATE_MACHINE_TAG="stateMachine";
        static final String WRAPPER_TAG="wrapper";
        static final String ELEMENT_NAME_TAG ="name";
	static final String IMPL_CLASS_TAG="class";
        static final String TYPE_TAG="type";
	static final String TRANSITION_TAG = "transition";
	static final String IS_INITIAL_STATE_TAG="isInitial";
	static final String IS_FINAL_STATE_TAG="isFinal";
	static final String ON_EVENT_TAG="event";
	static final String EVENT_TYPE_IMPL_CLASS_TAG="eventTypeClass";
	static final String DEFAULT_TRANSITION="other_events_transition";
	static final String ENABLE_ASPECT_TAG="enableAspect";
        
	protected static final String XSD_FILE = "state_machines.xsd";
	public static final String DEFAULT_XML_FILE="empty.xml";
	public static final String DEFAULT_XML_FILE_PROPERTY = "org.blitvin.statemachine.DOMStateMachineFactoryImplementation.defaultXmlFileName";
	


	protected static final Class<?>[] VALUE_OF_PARAMS = {String.class};
	/* list of parsed state machine specifications */
	protected HashMap<String,Node> stateMachineSpecs;
        protected HashMap<String,Node> wrapperSpecs;
	
	protected static final HashSet<String> standardStateTags = new HashSet<>();
	static {
		standardStateTags.add(IS_FINAL_STATE_TAG);
		standardStateTags.add(IS_INITIAL_STATE_TAG);
                standardStateTags.add(IMPL_CLASS_TAG);
		standardStateTags.add(ENABLE_ASPECT_TAG);
		standardStateTags.add(ELEMENT_NAME_TAG);
	}
	
	protected static final HashSet<String> standardTransitionTags = new HashSet<>();
	static {
		//standardTransitionTags.add(ELEMENT_NAME_TAG);
		standardTransitionTags.add(TYPE_TAG);
		standardTransitionTags.add(ON_EVENT_TAG);
	}
	/**
	 * default constructor, xml file name is deduced from property @see {@link #DEFAULT_XML_FILE_PROPERTY}.
	 * File is searched by class loader i.e. in most cases in CLASSPATH 
	 * @throws InvalidFactoryImplementation
	 */
	public DOMStateMachineFactory() throws InvalidFactoryImplementation{
		String xmlFileName =System.getProperty(DEFAULT_XML_FILE_PROPERTY,DEFAULT_XML_FILE);
		parseXml(xmlFileName);
	}
	
	/**
	 * constructor with explicit file name argument
	 * @param xmlFileName name of XML file to parse
	 * @throws InvalidFactoryImplementation
	 */
	public DOMStateMachineFactory(String xmlFileName) throws InvalidFactoryImplementation{
		parseXml(xmlFileName);
	}
	
	/**
	 * this method discards previous parsed XML file and loads new one
	 * @param xmlFileName name of new XML file to parse
	 * @throws InvalidFactoryImplementation
	 */
	public void resetFactory(String xmlFileName) throws InvalidFactoryImplementation{
		parseXml(xmlFileName);
	}
	
	private void parseXml(String xmlFileName) throws InvalidFactoryImplementation{
		 try {
			DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document stateMachines =dBuilder.parse(ClassLoader.getSystemResourceAsStream(xmlFileName));
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		    // load a WXS schema, represented by a Schema instance
		    Source schemaFile = new StreamSource(ClassLoader.getSystemResourceAsStream(XSD_FILE));
		    Schema schema = null;
			try {
				schema = factory.newSchema(schemaFile);
			} catch (SAXException e1) {
				throw new InvalidFactoryImplementation("can't parse xsd schema",e1);
			}
			Validator validator = schema.newValidator();
		    validator.validate(new DOMSource(stateMachines));
		    
		    stateMachines.getDocumentElement().normalize();
		    stateMachineSpecs = new HashMap<>();
                    NodeList nList = stateMachines.getElementsByTagName(STATE_MACHINE_TAG);
                    for (int idx =0 ; idx< nList.getLength(); ++idx)
                        stateMachineSpecs.put(((Element)nList.item(idx)).getAttribute(ELEMENT_NAME_TAG), nList.item(idx));
                    wrapperSpecs = new HashMap<>();
                    nList = stateMachines.getElementsByTagName(WRAPPER_TAG);
                    for (int idx =0 ; idx< nList.getLength(); ++idx)
                        wrapperSpecs.put(((Element)nList.item(idx)).getAttribute(ELEMENT_NAME_TAG), nList.item(idx));

		} catch (ParserConfigurationException e) {
			throw new InvalidFactoryImplementation("can't parse configuration", e);
		} catch (SAXException e) {
			throw new InvalidFactoryImplementation("invalid xml file", e);
		} catch (IOException e) {
			throw new InvalidFactoryImplementation("Error while reading xml or xsd file", e);
		}
	}


	
	private void fillAttributes(Node n, @SuppressWarnings("rawtypes") StateMachineBuilder builder, HashSet<String> standards){
		NamedNodeMap map = n.getAttributes();
		for(int i =0 ; i < map.getLength(); ++i) {
			if (!standards.contains(map.item(i).getNodeName())){
				builder.addProperty(map.item(i).getNodeName(), map.item(i).getNodeValue());
			}
		}
	}
	
	private Enum getEventTypeConst(Class<? extends Enum<?>> eventTypeClass, String value) throws BadStateMachineSpecification{
		Object[] mvargs =  new Object[1];
		mvargs[0] = value;
		try {
			Method vo = eventTypeClass.getMethod("valueOf", VALUE_OF_PARAMS);
			return (Enum)vo.invoke(null, mvargs);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new BadStateMachineSpecification(value +" is not a valid event type constant", e);
		}
	}
	
        private StateMachineBuilder.FSM_TYPES getFSMType(Node stateMachineNode){
            Element fsmElem = (Element)stateMachineNode;
            if (fsmElem.hasAttribute(TYPE_TAG))
                return StateMachineBuilder.FSM_TYPES.valueOf(((Element) stateMachineNode).getAttribute(TYPE_TAG));
            else
                return StateMachineBuilder.FSM_TYPES.SIMPLE; //default value in xsd
        }
        
        private void addState(Element element, StateMachineBuilder builder) throws BadStateMachineSpecification{
            // this will be refactored when something other than basic state is instroduced
            if (element.hasAttribute(ENABLE_ASPECT_TAG)) {
                int properties = StateMachineBuilder.STATE_PROPERTIES_BASIC;
                properties |= (Boolean.parseBoolean(element.getAttribute(ENABLE_ASPECT_TAG))?
                        StateMachineBuilder.STATE_PROPERTIES_ASPECT:0);
                builder.addState(element.getAttribute(ELEMENT_NAME_TAG), properties);
            } else {
                builder.addState(element.getAttribute(ELEMENT_NAME_TAG));
            }
            
            fillAttributes(element, builder, standardStateTags);
            // special treatment for predefined attributes
            Class stateClass = getClass(element, State.class);
            if (stateClass != null)
                builder.addProperty(StateMachineBuilder.STATE_CLASS_PROPERTY, stateClass);
            if (element.hasAttribute(IS_FINAL_STATE_TAG) && 
                    Boolean.parseBoolean(element.getAttribute(IS_FINAL_STATE_TAG)))
                builder.markStateAsFinal();
            if (element.hasAttribute(IS_INITIAL_STATE_TAG) && 
                    Boolean.parseBoolean(element.getAttribute(IS_INITIAL_STATE_TAG)))
                builder.markStateAsInitial();
            
            
        }
        

	/**
	 * returns instance of state machine
	 * each invocation creates machine with distinct set of states and transitions (no sharing of
	 * states among instances of machine created from the same specification)
	 * @param machineName name of state machine as specified by name attribute of StateMachine entry
	 * @return new instance of the machine constructed according to XML file's specifications
	 * @throws BadStateMachineSpecification if construction failed for any reason
	 */
	@Override
	public StateMachine<? extends Enum<?>> getStateMachine(String machineName) throws BadStateMachineSpecification {
		return getStateMachine(machineName,null);
	}
	
        private Class<?> getClass(Element theNode, Class template)
			throws BadStateMachineSpecification {
		Class<?> retVal = null;
		String className = null;
		if (theNode.hasAttribute(IMPL_CLASS_TAG))
			className = theNode.getAttribute(IMPL_CLASS_TAG);
		else
			return null;
		
		try {
			retVal = Class.forName(className);
			if (!template.isAssignableFrom(retVal))
				throw new BadStateMachineSpecification(className + " is not inherited from "+template.getCanonicalName() );
		} catch (ClassNotFoundException e) {
			throw new BadStateMachineSpecification("class not found", e);
		}


			return retVal;
	}

    @Override
    public StateMachine<? extends Enum<?>> getStateMachine(String name, HashMap<Object, Object> fsmProperties) throws BadStateMachineSpecification {
        //handle wrappers
        StateMachineBuilder<? extends Enum<?>> builder = getBuilder(name);
        if (fsmProperties != null)
            builder.addFSMProperties(fsmProperties);
        return builder.build();
    }

    @Override
    public StateMachineBuilder<? extends Enum<?>> getBuilder(String name) throws BadStateMachineSpecification{
        Node stateMachineNode = stateMachineSpecs.get(name);
        
        if (stateMachineNode == null)
            throw new BadStateMachineSpecification("Unknown state machine :"+name);
        
                
        Element stateMachineElem = (Element)stateMachineNode;
        
        /* event type class represents enum type of state machine alphabet*/
	Class eventTypeClass = null;
	try {
            eventTypeClass = Class.forName(stateMachineElem.getAttribute(EVENT_TYPE_IMPL_CLASS_TAG));
	}catch (ClassNotFoundException e) {
            throw new BadStateMachineSpecification("Event type class "+ EVENT_TYPE_IMPL_CLASS_TAG+ " not found", e);
	}
	/* event type expected to be enum */
	if (!eventTypeClass.isEnum())
            throw new BadStateMachineSpecification("Expecting enum class name in attribute "+EVENT_TYPE_IMPL_CLASS_TAG);
		
		
        StateMachineBuilder builder = new StateMachineBuilder(getFSMType(stateMachineNode), eventTypeClass);
        
        NodeList stateNodes =stateMachineElem.getChildNodes();
	for(int i = 0 ; i < stateNodes.getLength(); ++i) {
            if (stateNodes.item(i).getNodeType() != Node.ELEMENT_NODE)
		continue;
            Element stateElem = (Element) stateNodes.item(i);
            addState(stateElem, builder);
            NodeList transitionNodes = stateElem.getChildNodes();
            /* now construct transitions for current state */
            for(int j = 0 ; j < transitionNodes.getLength() ; ++j) {
                Node transitionNode = transitionNodes.item(j);
		if ( transitionNode.getNodeType()== Node.ELEMENT_NODE) {
                    Element transitionElem = (Element)transitionNode;
                    if (transitionElem.hasAttribute(TYPE_TAG)) {
                        StateMachineBuilder.TRANSITION_TYPE type = 
                                StateMachineBuilder.TRANSITION_TYPE.valueOf(transitionElem.getAttribute(TYPE_TAG));
                        if (transitionNode.getNodeName().equals(DEFAULT_TRANSITION)) {
                            builder.addDefaultTransition(type);
                        } else  {
                            builder.addTransition(getEventTypeConst(eventTypeClass, 
						transitionElem.getAttribute(ON_EVENT_TAG)),type);
                        }
                        
                    } else {
                        if (transitionNode.getNodeName().equals(DEFAULT_TRANSITION)) {
                            builder.addDefaultTransition();
                        } else  {
                            builder.addTransition(getEventTypeConst(eventTypeClass, 
						transitionElem.getAttribute(ON_EVENT_TAG)));
                        }    
                    }
                    fillAttributes(transitionNode, builder, standardTransitionTags);
                }
            }
        }
                        
        return builder;
    }

    @Override
    public Set<String> getNamesOfProvidedFSMs() {
        HashSet<String> retVal = new HashSet<>(stateMachineSpecs.keySet());
        retVal.addAll(wrapperSpecs.keySet());
        return retVal;
    }

   
}