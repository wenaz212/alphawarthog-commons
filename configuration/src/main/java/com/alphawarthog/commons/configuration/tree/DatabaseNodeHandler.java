package com.alphawarthog.commons.configuration.tree;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.tree.NodeHandler;
import org.apache.commons.configuration2.tree.NodeMatcher;
import org.apache.commons.lang3.StringUtils;

public class DatabaseNodeHandler implements NodeHandler<DatabaseNode> {
	
	private final DatabaseNodeModel nodeModel;

	protected DatabaseNodeHandler(DatabaseNodeModel databaseNodeModel) {
		this.nodeModel = Objects.requireNonNull(databaseNodeModel, "DatabaseNodeModel cannot be null");
	}

	public String nodeName(DatabaseNode node) {
		return node.getKey();
	}

	public Object getValue(DatabaseNode node) {
		return node.getValue();
	}

	public DatabaseNode getParent(DatabaseNode node) {
		return node.getParentUuid() == null ? null : nodeModel.getNode(node.getParentUuid());
	}

	public List<DatabaseNode> getChildren(DatabaseNode node) {
		return nodeModel.getChildren(node);
	}

	public List<DatabaseNode> getChildren(DatabaseNode node, String name) {
		return getChildren(node).stream()
				                .filter(n -> n.getKey().equalsIgnoreCase(name))
				                .collect(Collectors.toList());
	}

	public <C> List<DatabaseNode> getMatchingChildren(DatabaseNode node, NodeMatcher<C> matcher, C criterion) {
		return getChildren(node).stream()
				                .filter(n -> matcher.matches(n, this, criterion))
				                .collect(Collectors.toList());
	}

	public DatabaseNode getChild(DatabaseNode node, int index) {
		return getChildren(node).get(index);
	}

	public int indexOfChild(DatabaseNode parent, DatabaseNode child) {
		return getChildren(parent).indexOf(child);
	}

	public int getChildrenCount(DatabaseNode node, String name) {
		return (int) getChildren(node).stream()
				                      .filter(child -> child.getKey().equalsIgnoreCase(name))
				                      .count();
	}

	public <C> int getMatchingChildrenCount(DatabaseNode node, NodeMatcher<C> matcher, C criterion) {
		return getMatchingChildren(node, matcher, criterion).size();
	}

	public Set<String> getAttributes(DatabaseNode node) {
		return node.getAttributes().keySet();
	}

	public boolean hasAttributes(DatabaseNode node) {
		return !node.getAttributes().isEmpty();
	}

	public Object getAttributeValue(DatabaseNode node, String name) {
		return node.getAttributes().get(StringUtils.lowerCase(name));
	}

	public boolean isDefined(DatabaseNode node) {
		return StringUtils.isNotBlank(node.getValue()) || !getChildren(node).isEmpty() || !getAttributes(node).isEmpty();
	}

	public DatabaseNode getRootNode() {
		return nodeModel.getRootNode();
	}
}
