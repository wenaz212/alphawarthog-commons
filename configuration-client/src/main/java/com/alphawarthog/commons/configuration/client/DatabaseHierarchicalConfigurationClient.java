package com.alphawarthog.commons.configuration.client;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.ConfigurationDecoder;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.ex.ConfigurationRuntimeException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alphawarthog.commons.configuration.DatabaseHierarchicalConfiguration;
import com.alphawarthog.commons.configuration.tree.DatabaseNode;
import com.alphawarthog.sql.TransactionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DatabaseHierarchicalConfigurationClient {
	
	private static final Logger CONSOLE_LOGGER = LogManager.getLogger(DatabaseHierarchicalConfigurationClient.class);
	private static final BinaryOperator<String> MERGE_EXCEPTION = (u, v) -> { throw new ConfigurationRuntimeException("Duplicated nodes"); };
	
	private static final String NULL_STRING = "xsi:nil=\"true\"";
	private static final String MISSING_PROPERTY_KEY = "Property key must be supplied";
	private static final String URL = "url";
	private static final String CONFIGURATION_NAME = "configurationName";
	private static final String ROOT_NODE_NAME = "rootNodeName";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String READONLY = "readonly";
	private static final String USE_XPATH_EXPR = "useXPathExpression";
	
	protected final Logger logger = LogManager.getLogger(getClass());
	
	private final DatabaseHierarchicalConfiguration config;
	private final boolean supportUpdates;
	private final TransactionManager txManager;
	private final ObjectMapper mapper;
	
	public DatabaseHierarchicalConfigurationClient(Properties props) {
		String url = getInitPropertyKey(props, URL);
		String username = getInitPropertyKey(props, USERNAME);
		String password = getInitPropertyKey(props, PASSWORD);
		txManager = StringUtils.isBlank(username) ? new TransactionManager(url) : new TransactionManager(url, username, password);
		
		String rootNodeName = getInitPropertyKey(props, ROOT_NODE_NAME);
		boolean readonly = Boolean.parseBoolean(getInitPropertyKey(props, READONLY, Boolean.FALSE.toString()));
		DatabaseNode rootNode = new DatabaseNode.Builder()
				                                .key(rootNodeName)
				                                .build();
		this.supportUpdates = !readonly;
		String name = getInitPropertyKey(props, CONFIGURATION_NAME);
		this.config = new DatabaseHierarchicalConfiguration(txManager, name, rootNode, supportUpdates);
		
		boolean useXPath = Boolean.parseBoolean(getInitPropertyKey(props, USE_XPATH_EXPR, Boolean.TRUE.toString()));
		if (useXPath) {
			this.config.setExpressionEngine(new XPathExpressionEngine());
		}
		
		this.mapper = new ObjectMapper();
	}
	
	private String getInitPropertyKey(Properties props, String suffix) {
		return props.getProperty(getClass().getSimpleName() + "." + suffix);
	}
	
	private String getInitPropertyKey(Properties props, String suffix, String defaultValue) {
		return props.getProperty(getClass().getSimpleName() + "." + suffix, defaultValue);
	}
	
	public String addNode(String key, DatabaseNode node) {
		return addNodes(key, Arrays.asList(node)).get(0);
	}
	
	public List<String> addNodes(String key, List<DatabaseNode> nodes) {
		config.addNodes(key, nodes);
		
		// now try to find the added nodes
		return config.childConfigurationsAt(key, supportUpdates)
				     .stream()
				     .map(this::getRootNode)
				     .filter(nodes::contains)
				     .map(this::getNodeKey)
				     .collect(Collectors.toList());
	}
	
	public Map<DatabaseNode, String> getChildrenNodes(String key) {
		return config.childConfigurationsAt(key, supportUpdates)
				     .stream()
				     .map(this::getRootNode)
				     .collect(Collectors.toMap(Function.identity(), this::getNodeKey, MERGE_EXCEPTION, LinkedHashMap::new));
	}
	
	public String clearTree(String key) {
		String toClear = config.childConfigurationsAt(key, supportUpdates).toString();
		config.clearTree(key);
		return toClear;
	}
	
	public DatabaseNode getNode(String key) {
		Map<DatabaseNode, String> nodes = getNodes(key);
		if (nodes.isEmpty()) {
			return null;
		}
		
		if (nodes.size() > 1) {
			throw new IllegalArgumentException(key + " returns more than 1 node");
		}
		
		return nodes.keySet().iterator().next();
	} 
	
	public Map<DatabaseNode, String> getNodes(String key) {
		return config.configurationsAt(key, supportUpdates)
				     .stream()
				     .map(this::getRootNode)
				     .collect(Collectors.toMap(Function.identity(), this::getNodeKey, MERGE_EXCEPTION, LinkedHashMap::new));
	}
	
	private DatabaseNode getRootNode(HierarchicalConfiguration<DatabaseNode> cfg) {
		return cfg.getNodeModel().getNodeHandler().getRootNode();
	}
	
	private String getNodeKey(DatabaseNode node) {
		return config.nodeKey(node, new HashMap<>(), config.getNodeModel().getNodeHandler());
	}
	
	public Object addProperty(String key, Object value) {
		config.addProperty(key, value);
		return config.getProperty(key);
	}
	
	public ImmutableNode clear() {
		ImmutableNode toClear = config.getNodeModel().getInMemoryRepresentation();
		config.clear();
		return toClear;
	}
	
	public Object clearProperty(String key) {
		Object toClear = config.getProperty(key);
		config.clearProperty(key);
		return toClear;
	}
	
	public Object setProperty(String key, Object value) {
		Object oldValue = config.getProperty(key);
		config.setProperty(key, value);
		return oldValue;
	}
	
	public boolean containsKey(String key) {
		return config.containsKey(key);
	}
	
	public <T> T get(Class<T> cls, String key) {
		return config.get(cls, key);
	}
	
	public <T> T get(Class<T> cls, String key, T defaultValue) {
		return config.get(cls, key, defaultValue);
	}
	
	public Object getArray(Class<?> cls, String key) {
		return config.getArray(cls, key);
	}
	
	public BigDecimal getBigDecimal(String key) {
		return config.getBigDecimal(key);
	}
	
	public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
		return config.getBigDecimal(key, defaultValue);
	}
	
	public BigInteger getBigInteger(String key) {
		return config.getBigInteger(key);
	}
	
	public BigInteger getBigInteger(String key, BigInteger defaultValue) {
		return config.getBigInteger(key, defaultValue);
	}
	
	public boolean getBoolean(String key) {
		return config.getBoolean(key);
	}
	
	public boolean getBoolean(String key, boolean defaultValue) {
		return config.getBoolean(key, defaultValue);
	}
	
	public Boolean getBoolean(String key, Boolean defaultValue) {
		return config.getBoolean(key, defaultValue);
	}
	
	public byte getByte(String key) {
		return config.getByte(key);
	}
	
	public byte getByte(String key, byte defaultValue) {
		return config.getByte(key, defaultValue);
	}
	
	public Byte getByte(String key, Byte defaultValue) {
		return config.getByte(key, defaultValue);
	}
	
	public <T> Collection<T> getCollection(Class<T> cls, String key, Collection<T> target) {
		return config.getCollection(cls, key, target);
	}
	
	public <T> Collection<T> getCollection(Class<T> cls, String key, Collection<T> target, Collection<T> defaultValue) {
		return config.getCollection(cls, key, target, defaultValue);
	}
	
	public double getDouble(String key) {
		return config.getDouble(key);
	}
	
	public double getDouble(String key, double defaultValue) {
		return config.getDouble(key, defaultValue);
	}
	
	public Double getDouble(String key, Double defaultValue) {
		return config.getDouble(key, defaultValue);
	}
	
	public String getEncodedString(String key) {
		return config.getEncodedString(key);
	}
	
	public String getEncodedString(String key, ConfigurationDecoder decoder) {
		return config.getEncodedString(key, decoder);
	}
	
	public float getFloat(String key) {
		return config.getFloat(key);
	}
	
	public float getFloat(String key, float defaultValue) {
		return config.getFloat(key, defaultValue);
	}
	
	public Float getFloat(String key, Float defaultValue) {
		return config.getFloat(key, defaultValue);
	}
	
	public int getInt(String key) {
		return config.getInt(key);
	}
	
	public int getInt(String key, int defaultValue) {
		return config.getInt(key, defaultValue);
	}
	
	public int getInteger(String key, Integer defaultValue) {
		return config.getInteger(key, defaultValue);
	}
	
	public Iterator<String> getKeys() {
		return config.getKeys();
	}
	
	public Iterator<String> getKeys(String prefix) {
		return config.getKeys(prefix);
	}
	
	public <T> List<T> getList(Class<T> cls, String key) {
		return config.getList(cls, key);
	}
	
	public <T> List<T> getList(Class<T> cls, String key, List<T> defaultValue) {
		return config.getList(cls, key, defaultValue);
	}
	
	public List<Object> getList(String key) {
		return config.getList(key);
	}
	
	public List<Object> getList(String key, List<?> defaultValue) {
		return config.getList(key, defaultValue);
	}
	
	public long getLong(String key) {
		return config.getLong(key);
	}
	
	public long getLong(String key, long defaultValue) {
		return config.getLong(key, defaultValue);
	}
	
	public Properties getProperties(String key) {
		return config.getProperties(key);
	}
	
	public Object getProperty(String key) {
		return config.getProperty(key);
	}
	
	public short getShort(String key) {
		return config.getShort(key);
	}
	
	public short getShort(String key, short defaultValue) {
		return config.getShort(key, defaultValue);
	}
	
	public Short getShort(String key, Short defaultValue) {
		return config.getShort(key, defaultValue);
	}
	
	public String getString(String key) {
		return config.getString(key);
	}
	
	public String getString(String key, String defaultValue) {
		return config.getString(key, defaultValue);
	}
	
	public String[] getStringArray(String key) {
		return config.getStringArray(key);
	}
	
	public boolean isEmpty() {
		return config.isEmpty();
	}
	
	public int size() {
		return config.size();
	}
	
	private String executeCommand(String command, String... params) {
		switch (StringUtils.lowerCase(command)) {
		case "load": return loadConfigurationFromFile(params);
		case "addnode": 
		case "addnodes": return addNodes(params);
		case "addproperty": return addProperty(params);
		case "clear": return clearAndReturnString();
		case "clearproperty": return clearProperty(params);
		case "cleartree": return clearTree(params);
		case "containskey": return containsKey(params);
		case "getarray": // return list
		case "getlist":	return getList(params);
		case "getchildrennodes": return getChildrenNodes(params);
		case "getkeys": return getKeysAsString();
		case "getnode": return getNode(params);
		case "getnodes": return getNodes(params);
		case "getproperties": return getProperties(params);
		case "getproperty": return getProperty(params);
		case "getstring": return getString(params);
		case "getstringarray": return getStringArray(params);	
		case "isempty": return Boolean.toString(isEmpty());
		case "setproperty": return setProperty(params);
		case "size": return Integer.toString(size());
		default: throw new ConfigurationRuntimeException("Command " + command + " is not supported");             
		}
	}
	
	private String addNodes(String[] params) {
		checkParamsLength(params, 1, "Parent node key must be supplied");
		List<DatabaseNode> nodesToInsert = Arrays.stream(params, 1, params.length)
												 .map(filePath -> {
													 try {
														 return mapper.readValue(new File(filePath), DatabaseNode.class);
													 } catch (IOException e) {
														 String msg = "Unable to read JSON string from " + filePath + ": " + e.getMessage();
														 logger.error(msg, e);
														 throw new ConfigurationRuntimeException(msg, e);
													 }
												 })
												 .collect(Collectors.toList());
		List<String> insertedKeys = addNodes(params[0], nodesToInsert);
		return insertedKeys.size() == 1 ? insertedKeys.get(0) : insertedKeys.toString();
	}

	private String getNode(String[] params) {
		checkParamsLength(params, 1, "Node key must be supplied");
		return nullOrString(getNode(params[0]));
	}

	private String setProperty(String[] params) {
		checkParamsLength(params, 2, "Property to be set and its key must be supplied");
		return params[0] + " set. Old value = " + nullOrString(setProperty(params[0], params[1]));
	}

	private String getStringArray(String[] params) {
		checkParamsLength(params, 1, MISSING_PROPERTY_KEY);
		return Arrays.asList(getStringArray(params[0])).toString();
	}

	private void checkParamsLength(String[] params, int minLength, String message) {
		if (params.length < minLength) {
			throw new ConfigurationRuntimeException(message);
		}
	}
	
	private String getString(String[] params) {
		checkParamsLength(params, 1, MISSING_PROPERTY_KEY);
		return nullOrString(getString(params[0]));
	}

	private String getProperty(String[] params) {
		checkParamsLength(params, 1, MISSING_PROPERTY_KEY);
		return nullOrString(getProperty(params[0]));
	}

	private String getProperties(String[] params) {
		checkParamsLength(params, 1, MISSING_PROPERTY_KEY);
		return getProperties(params[0]).toString();
	}

	private String getNodes(String[] params) {
		checkParamsLength(params, 1, "Node key must be supplied");
		return getNodes(params[0]).toString();
	}

	private String getKeysAsString() {
		List<String> result = new ArrayList<>();
		Iterator<String> keys = this.getKeys();
		while (keys.hasNext()) {
			result.add(keys.next());
		}
		
		return result.toString();
	}

	private String getChildrenNodes(String[] params) {
		checkParamsLength(params, 1, "Parent node key must be supplied");
		return getChildrenNodes(params[0]).toString();
	}

	private String getList(String[] params) {
		checkParamsLength(params, 1, MISSING_PROPERTY_KEY);
		return getList(params[0]).stream()
				                 .map(o -> o == null ? null : o.toString())
				                 .collect(Collectors.toList())
				                 .toString();
	}

	private String containsKey(String[] params) {
		checkParamsLength(params, 1, MISSING_PROPERTY_KEY);
		return Boolean.toString(this.containsKey(params[0]));
	}

	private String clearTree(String[] params) {
		checkParamsLength(params, 1, "Root key of tree to be cleared must be supplied");
		return "Tree starting from node " + nullOrString(clearTree(params[0])) + " cleared";
	}

	private String clearProperty(String[] params) {
		checkParamsLength(params, 1, MISSING_PROPERTY_KEY);
		return params[0] + " cleared. Old value = " + nullOrString(clearProperty(params[0])); 
	}

	private String clearAndReturnString() {
		return "Tree starting from node " + nullOrString(clear()) + " cleared";
	}

	private String addProperty(String[] params) {
		checkParamsLength(params, 2, "Property to be added and its key must be supplied");
		return nullOrString(addProperty(params[0], params[1])) + " added to " + params[0];
	}
	
	private String nullOrString(Object o) {
		return o == null ? NULL_STRING : o.toString(); 
	}

	private String loadConfigurationFromFile(String[] params) {
		checkParamsLength(params, 2, "New configuration name and source configuration XML file must be supplied");
		File sourceFile = new File(params[1]);
		XMLConfiguration sourceConfig = new XMLConfiguration();
		try {
			sourceConfig.read(new FileReader(sourceFile));
		} catch (IOException | ConfigurationException e) {
			String msg = "Unable to load XML configuration from " + sourceFile.getPath() + ": " + e.getMessage();
			logger.error(msg, e);
			throw new ConfigurationRuntimeException(msg, e);
		}
		
		String configurationName = StringUtils.lowerCase(params[0]);
		new DatabaseHierarchicalConfiguration(this.txManager, configurationName, sourceConfig, supportUpdates);
		return "Configuration " + configurationName + " loaded from " + sourceFile.getPath();
	}

	public static void main(String[] args) throws IOException {
		 File propertiesFile = new File(args[0]);
		 Properties props = new Properties();
		 props.load(new FileReader(propertiesFile));
		 DatabaseHierarchicalConfigurationClient client = new DatabaseHierarchicalConfigurationClient(props);
		 String command = args[1];
		 String output = client.executeCommand(command, args.length > 2 ? Arrays.copyOfRange(args, 2, args.length) : new String[] {});
		 CONSOLE_LOGGER.info(output);
	}
}
