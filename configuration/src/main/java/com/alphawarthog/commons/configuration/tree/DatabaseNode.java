package com.alphawarthog.commons.configuration.tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.configuration2.ex.ConfigurationRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseNode {
	
	protected final Logger logger = LogManager.getLogger(getClass());
	
	public static class Builder {
		private String uuid;
		private String parentUuid;
		private String key;
		private String value;
		private Map<String, String> attributes;
		
		public Builder uuid(String uuid) {
			this.uuid = uuid;
			return this;
		}
		
		public Builder parentUuid(String parentUuid) {
			this.parentUuid = parentUuid;
			return this;
		}
		
		public Builder key(String key) {
			this.key = key;
			return this;
		}
		
		public Builder value(String value) {
			this.value = value;
			return this;
		}
		
		public Builder attributes(Map<String, String> attributes) {
			this.attributes = attributes;
			return this;
		}
		
		public DatabaseNode build() {
			return new DatabaseNode(this);
		}
	}

	private final String uuid;
	private final String parentUuid;
	private final String key;
	private final String value;
	private final SortedMap<String, String> attributes;
	private final String asString;
	private final int hashCode;
	
	private DatabaseNode(Builder builder) {
		this.uuid = builder.uuid == null ? UUID.randomUUID().toString() : UUID.fromString(builder.uuid).toString();
		this.parentUuid = builder.parentUuid == null ? null : UUID.fromString(builder.parentUuid).toString();
		this.key = Objects.requireNonNull(StringUtils.lowerCase(StringUtils.trimToNull(builder.key)), "Configuration must have a key");
		this.value = builder.value;
		
		SortedMap<String, String> sortedMap = new TreeMap<>();
		if (MapUtils.isNotEmpty(builder.attributes)) {
			for (Entry<String, String> attributeEntry : builder.attributes.entrySet()) {
				String lowerAttributeKey = Objects.requireNonNull(StringUtils.lowerCase(StringUtils.trimToNull(attributeEntry.getKey())), "Attribute key cannot be null");
				if (sortedMap.containsKey(lowerAttributeKey)) {
					throw new ConfigurationRuntimeException("Duplicate attribute key: " + lowerAttributeKey);
				}
				
				if (StringUtils.isBlank(attributeEntry.getValue())) {
					throw new ConfigurationRuntimeException("Atribute value cannot be blank");
				}
				
				sortedMap.put(lowerAttributeKey, StringUtils.trimToEmpty(attributeEntry.getValue()));
			}
		}
		
		this.attributes = Collections.unmodifiableSortedMap(sortedMap);
		
		this.asString = new ToStringBuilder(this).append("parentUuid", this.parentUuid)
				                                 .append("key", this.key)
				                                 .append("value", this.value)
				                                 .append("attributes", this.attributes)
				                                 .toString();
		this.hashCode = this.uuid.hashCode();
		logger.debug("Database node {} created", this);
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public String getParentUuid() {
		return parentUuid;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getValue() {
		return value;
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	@Override
	public String toString() {
		return asString; 
	}
	
	@Override
	public int hashCode() {
		return hashCode;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		
		if (obj != null && obj.getClass() == DatabaseNode.class) {
			DatabaseNode other = (DatabaseNode) obj;
			return getUuid().equals(other.getUuid());
		}
		
		return false;
	}

	public Builder toBuilder() {
		return new Builder().uuid(this.uuid)
				            .parentUuid(this.parentUuid)
				            .key(this.key)
				            .value(this.value)
				            .attributes(new HashMap<>(this.attributes));
	}
}
