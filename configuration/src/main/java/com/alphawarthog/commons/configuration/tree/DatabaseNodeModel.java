package com.alphawarthog.commons.configuration.tree;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationRuntimeException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.NodeAddData;
import org.apache.commons.configuration2.tree.NodeHandler;
import org.apache.commons.configuration2.tree.NodeKeyResolver;
import org.apache.commons.configuration2.tree.NodeModel;
import org.apache.commons.configuration2.tree.NodeUpdateData;
import org.apache.commons.configuration2.tree.QueryResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alphawarthog.dbutils.Transaction;
import com.alphawarthog.dbutils.TransactionManager;

public class DatabaseNodeModel implements NodeModel<DatabaseNode> {
	
	private static final DatabaseNode DEFAULT_ROOT_NODE = new DatabaseNode.Builder()
			                                                              .key("configuration")
			                                                              .build();
	
	private static final String WHERE_UUID_CLAUSE = "where uuid = ? ";
	private static final String WHERE_CONFIG_UUID_CLAUSE = "where configuration_uuid = ? ";
	private static final String AND_KEY_CLAUSE = "and key = ? ";
	
	private static final String SELECT_ROOT = "select cfg.uuid, cfg.key, null as parent_uuid, cfg.value " +
	                                          "from configuration cfg, configuration_root root " +
			                                  "where root.configuration_name = ? " +
	                                          "  and cfg.uuid = root.root_uuid ";
	
	private static final String SELECT_ATTRIBUTES = "select attr.key, attr.value " +
	                                                "from configuration_attribute attr " +
			                                        "where attr.configuration_uuid = ? ";
	
	private static final String SELECT_BY_PARENT = "select uuid, key, parent_uuid, value " +
	                                               "from configuration " +
	                                               "where parent_uuid = ? " +
			                                       "order by key, value ";
	
	private static final String SELECT_BY_UUID = "select uuid, key, parent_uuid, value " +
	                                             "from configuration " +
			                                     WHERE_UUID_CLAUSE;
	
	private static final String INSERT_CONFIG = "insert into configuration(uuid, parent_uuid, key, value) " +
	                                            "values(?, ?, ?, ?) ";
	
	private static final String INSERT_CONFIG_ROOT = "insert into configuration_root(configuration_name, root_uuid) " +
	                                                 "values(?, ?) ";
	
	private static final String INSERT_ATTRIBUTE = "insert into configuration_attribute(configuration_uuid, key, value) " +
	                                               "values(?, ?, ?) "; 
	
	private static final String DELETE_ATTRIBUTES = "delete from configuration_attribute " +
	                                                WHERE_CONFIG_UUID_CLAUSE;
	
	private static final String DELETE_ATTRIBUTE = "delete from configuration_attribute " +
	                                               WHERE_CONFIG_UUID_CLAUSE +
			                                       AND_KEY_CLAUSE;
	
	private static final String DELETE_CONFIGURATION = "delete from configuration " +
	                                                   WHERE_UUID_CLAUSE;
	
	private static final String DELETE_CONFIGURATION_ROOT = "delete from configuration_root " +
	                                                        "where root_uuid = ? ";
	
	private static final String CLEAR_CONFIGURATION_VALUE = "update configuration " +
	                                                        "set value = null " +
			                                                WHERE_UUID_CLAUSE;
	
	private static final String UPDATE_ATTRIBUTE = "update configuration_attribute " +
	                                               "set value = ? " +
			                                       WHERE_CONFIG_UUID_CLAUSE +
	                                               AND_KEY_CLAUSE;
	
	private static final String UPDATE_VALUE = "update configuration " +
	                                           "set value = ? " +
			                                   WHERE_UUID_CLAUSE;
	
	protected final Logger logger = LogManager.getLogger(getClass());
	
	private final DatabaseNodeHandler nodeHandler;
	private final TransactionManager txManager;
	private DatabaseNode root;
	
	public DatabaseNodeModel(TransactionManager txManager, DatabaseNode rootNode) {
		this(txManager, null, rootNode);
	}
	
	public DatabaseNodeModel(TransactionManager txManager, String configurationName) {
		this(txManager, configurationName, DEFAULT_ROOT_NODE);
	}
	
	public DatabaseNodeModel(TransactionManager txManager, String configurationName, XMLConfiguration xmlSource) {
		this(txManager);
		
		String configName = Objects.requireNonNull(StringUtils.lowerCase(StringUtils.trimToNull(configurationName)), "Configuration name cannot be blank");
		try (Transaction tx = txManager.beginTransaction()) {
			this.root = createTree(tx, xmlSource.getNodeModel().getRootNode(), null);
			tx.executeUpdate(INSERT_CONFIG_ROOT, configName, this.root.getUuid());
		} catch (SQLException e) {
			String msg = "Unable to load configuration " + configurationName + " from XMLConfiguration " + xmlSource + ": " + e.getMessage();
			logger.error(msg, e);
			throw new ConfigurationRuntimeException(msg, e);
		}
	}
	
	private DatabaseNodeModel(TransactionManager txManager) {
		this.txManager = Objects.requireNonNull(txManager, "TransactionManager cannot be null");
		this.nodeHandler = new DatabaseNodeHandler(this);
	}
	
	public DatabaseNodeModel(TransactionManager txManager, String configurationName, DatabaseNode rootNode) {
		this(txManager);
		
		if (StringUtils.isNotBlank(configurationName)) {
			String configName = StringUtils.lowerCase(configurationName.trim());
			try (Transaction tx = txManager.beginTransaction();
			     // check if configuration name already exists
				 ResultSet rs = txManager.executeQuery(SELECT_ROOT, configName)) {
				List<DatabaseNode> nodes = getNodes(tx, rs);
				if (!nodes.isEmpty()) {
					if (rootNode != null) {
						// configuration exists, supplied root node is ignored
						logger.info("Configuration {} already exists. Supplied node {} is ignored", configName, rootNode);
					}
					
					// assign root node from database
					this.root = nodes.get(0);
				} else {
					// configuration doesn't exist, create new configuration and its root
					DatabaseNode toInsert = rootNode.getParentUuid() == null ? rootNode : new DatabaseNode.Builder()
	                        																			  .key(rootNode.getKey())
	                        																			  .value(rootNode.getValue())
	                        																			  .attributes(rootNode.getAttributes())
	                        																			  .build();
					this.root = createNode(tx, toInsert);
					tx.executeUpdate(INSERT_CONFIG_ROOT, configName, this.root.getUuid());
				}
			} catch (SQLException e) {
				String msg = "Unable to create database node model for configuration " + configurationName + " with root " + rootNode + ": " + e.getMessage();
				logger.error(msg, e);
				throw new ConfigurationRuntimeException(msg, e);
			}
		} else {
			// load root node
			this.root = getNode(rootNode.getUuid());
			if (this.root == null) {
				throw new ConfigurationRuntimeException(rootNode + " is not a valid node");
			}
		}
	}
	
	public DatabaseNodeModel duplicate() {
		return new DatabaseNodeModel(this.txManager, this.root);
	}
	
	private List<DatabaseNode> getNodes(Transaction tx, ResultSet rs) throws SQLException {
		List<DatabaseNode> result = new ArrayList<>();
		while (rs.next()) {
			String uuid = rs.getString("uuid");
			DatabaseNode.Builder builder = new DatabaseNode.Builder()
														   .uuid(uuid)
														   .key(rs.getString("key"));
			String parentUuid = rs.getString("parent_uuid");
			if (!rs.wasNull()) {
				builder = builder.value(parentUuid);
			}
		               									   
			String value = rs.getString("value");
			if (!rs.wasNull()) {
				builder = builder.value(value);
			}
			
			Map<String, String> attributes = new HashMap<>();
			try (ResultSet rs2 = tx.executeQuery(SELECT_ATTRIBUTES, uuid)) {
				while (rs2.next()) {
					attributes.put(rs2.getString("key"), rs2.getString("value"));
				}
			}
			
			result.add(builder.attributes(attributes)
					          .build());
		}
		
		return result;
	}
	
	private DatabaseNode createTree(Transaction tx, ImmutableNode sourceNode, DatabaseNode parentTargetNode) throws SQLException {
		Map<String, String> targetAttributes = sourceNode.getAttributes()
				                                         .entrySet()
				                                         .stream()
				                                         .filter(entry -> entry.getValue() != null)
				                                         .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
		
		DatabaseNode.Builder targetNodeBuilder = new DatabaseNode.Builder()
				                                         		 .key(sourceNode.getNodeName())
				                                         		 .value(sourceNode.getValue() == null ? null : sourceNode.getValue().toString())
				                                         		 .attributes(targetAttributes);
		if (parentTargetNode != null) {
			targetNodeBuilder.parentUuid(parentTargetNode.getUuid());
		}
		
	    DatabaseNode insertedNode = createNode(tx, targetNodeBuilder.build());
	    
	    for (ImmutableNode childSource : sourceNode.getChildren()) {
	    	createTree(tx, childSource, insertedNode);
	    }
	    
	    return insertedNode;
	}

	private DatabaseNode createNode(Transaction tx, DatabaseNode node) throws SQLException {
		tx.executeQuery(INSERT_CONFIG, node.getUuid(), node.getParentUuid(), node.getKey(), node.getValue());
		for (Entry<String, String> attribute : node.getAttributes().entrySet()) {
			tx.executeQuery(INSERT_ATTRIBUTE, node.getUuid(), attribute.getKey(), attribute.getValue());
		}
		
		return node;
	}

	public void setRootNode(DatabaseNode newRoot) {
		DatabaseNode checkedRoot = getNode(newRoot.getUuid());
		if (checkedRoot == null) {
			throw new ConfigurationRuntimeException(newRoot + " is not a valid node");
		}
		
		this.root = checkedRoot;
	}
	
	protected DatabaseNode getRootNode() {
		return this.root;
	}
	
	public NodeHandler<DatabaseNode> getNodeHandler() {
		return this.nodeHandler;
	}

	public void addProperty(String key, Iterable<?> values, NodeKeyResolver<DatabaseNode> resolver) {
		if (!IteratorUtils.isEmpty(values.iterator())) {
			NodeAddData<DatabaseNode> nodeAddData = resolver.resolveAddKey(root, StringUtils.lowerCase(key), nodeHandler);
			try (Transaction tx = txManager.beginTransaction()) {
				// add required paths first
				DatabaseNode newNode = createPath(nodeAddData.getParent(), nodeAddData.getPathNodes(), null, tx);
				if (nodeAddData.isAttribute()) {
					tx.executeUpdate(INSERT_ATTRIBUTE, newNode.getUuid(), nodeAddData.getNewNodeName(), values.iterator().next());
				} else {
					for (Object value : values) {
						createNode(tx, new DatabaseNode.Builder()
								                       .parentUuid(newNode.getUuid())
								                       .key(nodeAddData.getNewNodeName())
								                       .value(value.toString())
								                       .build());
					}
				}
			} catch (SQLException e) {
				String msg = "Unable to add property at key " + key + ": " + e.getMessage();
				logger.error(msg, e);
				throw new ConfigurationRuntimeException(msg, e);
			}
		}
	}
	
	protected List<DatabaseNode> getChildren(DatabaseNode parentNode) {
		try (Transaction tx = txManager.beginTransaction();
			 ResultSet rs = tx.executeQuery(SELECT_BY_PARENT, parentNode.getUuid())) {
			return getNodes(tx, rs);
		} catch (SQLException e) {
			String msg = "Unable to get children of node " + parentNode + ": " + e.getMessage();
			logger.error(msg, e);
			throw new ConfigurationRuntimeException(msg, e);
		}
	}

	private DatabaseNode createPath(DatabaseNode parentNode, List<String> paths, String newNodeName, Transaction tx) throws SQLException {
		DatabaseNode newNode = parentNode;
		// build all paths first
		if (CollectionUtils.isNotEmpty(paths)) {
			for (String path : paths) {
				newNode = new DatabaseNode.Builder()
				                          .parentUuid(newNode.getUuid())
				                          .key(path)
				                          .build();
				createNode(tx, newNode);
			}
		}
		
		if (StringUtils.isNotBlank(newNodeName)) {
			newNode = new DatabaseNode.Builder()
					                  .parentUuid(newNode.getUuid())
					                  .key(newNodeName)
					                  .build();
			createNode(tx, newNode);
		}
		
		return newNode;
	}

	public void addNodes(String key, Collection<? extends DatabaseNode> nodes, NodeKeyResolver<DatabaseNode> resolver) {
		if (CollectionUtils.isNotEmpty(nodes)) {
			NodeAddData<DatabaseNode> nodeAddData = resolver.resolveAddKey(root, StringUtils.lowerCase(key), nodeHandler);
			if (nodeAddData.isAttribute()) {
				throw new ConfigurationRuntimeException("Nodes cannot be added to an attribute, key " + key + " resolves to an attribute");
			}
			
			try (Transaction tx = txManager.beginTransaction()) {
				DatabaseNode newParent = createPath(nodeAddData.getParent(), nodeAddData.getPathNodes(), nodeAddData.getNewNodeName(), tx);
				for (DatabaseNode node : nodes) {
					createNode(tx, new DatabaseNode.Builder()
							                       .uuid(node.getUuid())
			                                       .parentUuid(newParent.getUuid())
			                                       .key(node.getKey())
			                                       .value(node.getValue())
			                                       .attributes(node.getAttributes())
			                                       .build());		                 
				}
			} catch (SQLException e) {
				String msg = "Unable to add nodes " + nodes + " at key " + key + ": " + e.getMessage();
				logger.error(msg, e);
				throw new ConfigurationRuntimeException(msg, e);
			}
		}
	}

	public void setProperty(String key, Object value, NodeKeyResolver<DatabaseNode> resolver) {
		NodeUpdateData<DatabaseNode> nodeUpdateData = resolver.resolveUpdateKey(root, StringUtils.lowerCase(key), value, nodeHandler);
		try (Transaction tx = txManager.beginTransaction()) {
			// delete items
			deleteProperty(tx, nodeUpdateData);
			
			// update items
			updateProperty(tx, nodeUpdateData);
			
			// add items
			addProperty(nodeUpdateData.getKey(), nodeUpdateData.getNewValues(), resolver);
		} catch (SQLException e) {
			String msg = "Unable to set property " + value + " at key " + key + ": " + e.getMessage();
			logger.error(msg, e);
			throw new ConfigurationRuntimeException(msg, e);
		}
	}

	private void updateProperty(Transaction tx, NodeUpdateData<DatabaseNode> nodeUpdateData) throws SQLException {
		for (Entry<QueryResult<DatabaseNode>, Object> toUpdate : nodeUpdateData.getChangedValues().entrySet()) {
			QueryResult<DatabaseNode> updateKey = toUpdate.getKey();
			Object updateValue = toUpdate.getValue();
			if (updateKey.isAttributeResult()) {
				if (updateValue == null) {
					tx.executeQuery(DELETE_ATTRIBUTE, updateKey.getNode().getUuid(), updateKey.getAttributeName());
				} else {
					tx.executeQuery(UPDATE_ATTRIBUTE, updateValue.toString(), updateKey.getNode().getUuid(), updateKey.getAttributeName());
				}
			} else {
				if (updateValue == null) {
					tx.executeQuery(CLEAR_CONFIGURATION_VALUE, updateKey.getNode().getUuid());
				} else {
					tx.executeQuery(UPDATE_VALUE, updateValue.toString(), updateKey.getNode().getUuid());
				}
			}
		}
	}

	private void deleteProperty(Transaction tx, NodeUpdateData<DatabaseNode> nodeUpdateData) throws SQLException {
		for (QueryResult<DatabaseNode> toRemove : nodeUpdateData.getRemovedNodes()) {
			if (toRemove.isAttributeResult()) {
				tx.executeUpdate(DELETE_ATTRIBUTE, toRemove.getNode().getUuid(), toRemove.getAttributeName());
			} else {
				deleteNode(toRemove.getNode(), tx);
			}
		}
	}

	public Object clearTree(String key, NodeKeyResolver<DatabaseNode> resolver) {
		List<ImmutableNode> result = new ArrayList<>();
		try (Transaction tx = txManager.beginTransaction()) {
			for (DatabaseNode nodeToClear : resolver.resolveNodeKey(root, StringUtils.lowerCase(key), nodeHandler)) {
				result.add(deleteNode(nodeToClear, tx));
			}
		} catch (SQLException e) {
			String msg = "Unable to clear tree at " + key + ": " + e.getMessage();
			logger.error(msg, e);
			throw new ConfigurationRuntimeException(msg, e);
		}
		
		return result;
	}
	
	private ImmutableNode deleteNode(DatabaseNode node, Transaction tx) throws SQLException {
		ImmutableNode result = getInMemoryRepresentation(node); 
		
		String uuid = node.getUuid();
		
		// delete attributes first
		tx.executeUpdate(DELETE_ATTRIBUTES, uuid);
		
		// delete children one by one
		for (DatabaseNode childNode : getChildren(node)) {
			deleteNode(childNode, tx);
		}
		
		// delete root if the node is a root node
		tx.executeUpdate(DELETE_CONFIGURATION_ROOT, uuid);
		
		// delete this node
		tx.executeUpdate(DELETE_CONFIGURATION, uuid);
		
		return result;
	}

	public void clearProperty(String key, NodeKeyResolver<DatabaseNode> resolver) {
		List<QueryResult<DatabaseNode>> toClearList = resolver.resolveKey(root, StringUtils.lowerCase(key), nodeHandler);
		if (CollectionUtils.isNotEmpty(toClearList)) {
			try (Transaction tx = txManager.beginTransaction()) {
				for (QueryResult<DatabaseNode> toClear : toClearList) {
					if (toClear.isAttributeResult()) {
						tx.executeUpdate(DELETE_ATTRIBUTE, toClear.getNode().getUuid(), toClear.getAttributeName());
					} else {
						tx.executeUpdate(CLEAR_CONFIGURATION_VALUE, toClear.getNode().getUuid());
					}
				}
			} catch (SQLException e) {
				String msg = "Unable to clear property for key " + key + ": " + e.getMessage();
				logger.error(msg, e);
				throw new ConfigurationRuntimeException(msg, e);
			}
		}
	}

	public void clear(NodeKeyResolver<DatabaseNode> resolver) {
		try (Transaction tx = txManager.beginTransaction()) {
			deleteNode(root, tx);
		} catch (SQLException e) {
			String msg = "Unable to clear tree at root " + root + ": " + e.getMessage();
			logger.error(msg, e);
			throw new ConfigurationRuntimeException(msg, e);
		}
	}
	
	private ImmutableNode getInMemoryRepresentation(DatabaseNode node) {
		ImmutableNode.Builder builder = new ImmutableNode.Builder()
														 .name(node.getKey())
														 .value(node.getValue());

		for (String attributeKey : nodeHandler.getAttributes(node)) {
			builder.addAttribute(attributeKey, nodeHandler.getAttributeValue(node, attributeKey));
		}

		for (DatabaseNode childNode : nodeHandler.getChildren(node)) {
			builder.addChild(getInMemoryRepresentation(childNode));
		}

		return builder.create();
	}

	public ImmutableNode getInMemoryRepresentation() {
		return getInMemoryRepresentation(root);
	}

	protected DatabaseNode getNode(String uuid) {
		try (Transaction tx = txManager.beginTransaction();
			 ResultSet rs = txManager.executeQuery(SELECT_BY_UUID, uuid)) {
			List<DatabaseNode> nodes = getNodes(tx, rs);
			if (nodes.isEmpty()) {
				return null;
			}
			
			return nodes.get(0);
		} catch (SQLException e) {
			String msg = "Unable to get node " + uuid + ": " + e.getMessage();
			logger.error(msg, e);
			throw new ConfigurationRuntimeException(msg, e);
		}
	}
}
