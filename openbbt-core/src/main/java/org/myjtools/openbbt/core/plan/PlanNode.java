package org.myjtools.openbbt.core.plan;


import lombok.*;
import java.util.*;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@Getter @Setter @NoArgsConstructor @EqualsAndHashCode @ToString
public class PlanNode {

	private PlanNodeID nodeID;
	private NodeType nodeType;
	private String name;
	private String language;
	private String identifier;
	private String source;
	private String keyword;
	private DataTable dataTable;
	private Document document;
	private String description;
	private Set<String> tags;
	private SortedMap<String,String> properties;
	private String display;


	public PlanNode(NodeType type) {
		this.nodeType = type;
	}


	public PlanNode addProperties(Map<String, String> properties) {
		this.properties().putAll(properties);
		return this;
	}


	public PlanNode addProperty(String key, String value) {
		this.properties().put(key,value);
		return this;
	}



	public PlanNode addTag(String tag) {
		this.tags().add(tag);
		return this;
	}


	public PlanNode addTags(Collection<String> tags) {
		this.tags().addAll(tags);
		return this;
	}


	public boolean hasProperty(String property, String value) {
		return value.equals(this.properties().get(property));
	}


	public boolean hasTag(String tag) {
		return tags().contains(tag);
	}





	public SortedMap<String, String> properties() {
		if (properties == null) {
			return new TreeMap<>();
		}
		return properties;
	}


	public Set<String> tags() {
		if (tags == null) {
			return new HashSet<>();
		}
		return tags;
	}

}
