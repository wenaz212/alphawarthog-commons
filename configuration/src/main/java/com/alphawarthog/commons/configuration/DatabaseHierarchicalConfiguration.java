package com.alphawarthog.commons.configuration;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration2.AbstractHierarchicalConfiguration;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationRuntimeException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.InMemoryNodeModel;
import org.apache.commons.configuration2.tree.NodeHandler;
import org.apache.commons.configuration2.tree.NodeModel;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.alphawarthog.commons.configuration.tree.DatabaseNode;
import com.alphawarthog.commons.configuration.tree.DatabaseNodeModel;
import com.alphawarthog.sql.TransactionManager;

public class DatabaseHierarchicalConfiguration extends AbstractHierarchicalConfiguration<DatabaseNode> {
	
	public class InMemoryHierarchicalConfiguration extends BaseHierarchicalConfiguration {
		
		private InMemoryHierarchicalConfiguration(NodeModel<ImmutableNode> model) {
			super(model);
		}
	}
	
	private final boolean supportUpdates;
	
	protected final String supportUpdatesErrorMessage = "This instance of " + getClass().getSimpleName() + " does not support updates";
	
	public DatabaseHierarchicalConfiguration(TransactionManager txManager, String configurationName, XMLConfiguration sourceConfig) {
		this(txManager, configurationName, sourceConfig, false);
	}
	
	public DatabaseHierarchicalConfiguration(TransactionManager txManager, String configurationName, DatabaseNode rootNode) {
		this(txManager, configurationName, rootNode, false);
	}
	
	public DatabaseHierarchicalConfiguration(TransactionManager txManager, String configurationName, DatabaseNode rootNode, boolean supportUpdates) {
		this(new DatabaseNodeModel(txManager, configurationName, rootNode), supportUpdates);
	}
	
	private DatabaseHierarchicalConfiguration(NodeModel<DatabaseNode> model, boolean supportUpdates) {
		super(model);
		this.supportUpdates = supportUpdates;
	}

	public DatabaseHierarchicalConfiguration(TransactionManager txManager, String configurationName, XMLConfiguration sourceConfig, boolean supportUpdates) {
		this(new DatabaseNodeModel(txManager, configurationName, sourceConfig), supportUpdates);
	}

	@Override
	public HierarchicalConfiguration<DatabaseNode> configurationAt(String key, boolean supportUpdates) {
		List<HierarchicalConfiguration<DatabaseNode>> result = configurationsAt(key, supportUpdates);
		if (CollectionUtils.isEmpty(result)) {
			return null;
		}
		
		if (result.size() > 1) {
			throw new IllegalArgumentException(key + " returns more than 1 result");
		}
		
		return result.get(0);
	}

	@Override
	public HierarchicalConfiguration<DatabaseNode> configurationAt(String key) {
		return configurationAt(key, false);
	}

	@Override
	public List<HierarchicalConfiguration<DatabaseNode>> configurationsAt(String key) {
		return configurationsAt(key, false);
	}
	
	private List<DatabaseNode> databaseNodesAt(String key) {
		NodeHandler<DatabaseNode> handler = getNodeModel().getNodeHandler();
		return resolveNodeKey(handler.getRootNode(), key, handler);
	}

	@Override
	public List<HierarchicalConfiguration<DatabaseNode>> configurationsAt(String key, boolean supportUpdates) {
		return databaseNodesAt(key).stream()
				                   .map(node -> {
				                       NodeModel<DatabaseNode> newModel = cloneNodeModel();
				                       newModel.setRootNode(node);
				                       return new DatabaseHierarchicalConfiguration(newModel, supportUpdates);
				                   }) 
				                   .collect(Collectors.toList());
	}

	@Override
	public List<HierarchicalConfiguration<DatabaseNode>> childConfigurationsAt(String key) {
		return childConfigurationsAt(key, false);
	}
	
	private List<DatabaseNode> childrenDatabaseNodesAt(String key) {
		List<DatabaseNode> parentNodes = databaseNodesAt(key);
		if (parentNodes.isEmpty()) {
			throw new IllegalArgumentException(key + " is not a valid node key");
		} 
		
		if (parentNodes.size() > 1) {
			throw new IllegalArgumentException(key + " returns more than 1 node");
		}
		
		return getModel().getNodeHandler().getChildren(parentNodes.get(0));
	}

	@Override
	public List<HierarchicalConfiguration<DatabaseNode>> childConfigurationsAt(String key, boolean supportUpdates) {
		return childrenDatabaseNodesAt(key).stream()
				      					   .map(childNode -> {
				      						   NodeModel<DatabaseNode> newModel = cloneNodeModel();
				      						   newModel.setRootNode(childNode);
				      						   return new DatabaseHierarchicalConfiguration(newModel, supportUpdates);
				      					   })
				      					   .collect(Collectors.toList());
	}

	@Override
	public ImmutableHierarchicalConfiguration immutableConfigurationAt(String key, boolean supportUpdates) {
		return toInMemoryConfiguration().immutableConfigurationAt(key, supportUpdates);
	}
	
	private InMemoryHierarchicalConfiguration toInMemoryConfiguration() {
		ImmutableNode inMemoryNode = getModel().getInMemoryRepresentation();
		InMemoryNodeModel inMemoryModel = new InMemoryNodeModel(inMemoryNode);
		return new InMemoryHierarchicalConfiguration(inMemoryModel);
	}

	@Override
	public ImmutableHierarchicalConfiguration immutableConfigurationAt(String key) {
		return toInMemoryConfiguration().immutableConfigurationAt(key);
	}

	@Override
	public List<ImmutableHierarchicalConfiguration> immutableConfigurationsAt(String key) {
		return toInMemoryConfiguration().immutableChildConfigurationsAt(key);
	}
	
	@Override
	public List<ImmutableHierarchicalConfiguration> immutableChildConfigurationsAt(String key) {
		return toInMemoryConfiguration().immutableChildConfigurationsAt(key);
	}

	@Override
	protected NodeModel<DatabaseNode> cloneNodeModel() {
		return ((DatabaseNodeModel) getNodeModel()).duplicate();
	}
	
	@Override
	protected void addPropertyInternal(final String key, final Object value) {
		checkSupportUpdates();
		super.addPropertyInternal(key, value);
	}

	private void checkSupportUpdates() {
		if (!this.supportUpdates) {
			throw new ConfigurationRuntimeException(supportUpdatesErrorMessage);
		}
	}

	@Override
	protected void setPropertyInternal(final String key, final Object value) {
		checkSupportUpdates();
		super.setPropertyInternal(key, value);
	}

	@Override
	protected void clearPropertyDirect(String key) {
		checkSupportUpdates();
		super.clearPropertyDirect(key);
	}

	@Override
	protected void clearInternal() {
		checkSupportUpdates();
		super.clearInternal();
	}

	@Override
	protected void addNodesInternal(final String key, final Collection<? extends DatabaseNode> nodes) {
		checkSupportUpdates();
		super.addNodesInternal(key, nodes);
	}

	@Override
	protected Object clearTreeInternal(final String key) {
		checkSupportUpdates();
		return super.clearTreeInternal(key);
	}
	
	@Override
	public String toString() {
		return toString(getNodeModel().getNodeHandler().getRootNode());
	}

	private String toString(DatabaseNode node) {
		return new ToStringBuilder(node).append("node", node)
				                        .append("childrenNodes", getNodeModel().getNodeHandler()
			                                      							   .getChildren(node)
			                                      							   .stream()
			                                      							   .map(DatabaseNode::toString)
			                                      							   .collect(Collectors.toList()))
				                        .toString();
	}
}
