
package com.ing.datalib.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "name",
    "version",
    "attributes",
    "tags",
    "_meta",
    "data"
})

/**
 *
 * 
 */
public class ProjectInfo {

    @JsonProperty("name")
    private String name;
    @JsonProperty("id")
    private String id;
    @JsonProperty("_meta")
    private MetaList meta = new MetaList();
    @JsonProperty("attributes")
    private Attributes attributes = new Attributes();
    @JsonProperty("tags")
    private Tags tags = new Tags();
    @JsonProperty("data")
    private Data data = new Data();
    
    @JsonProperty("version")
    private String version; 
            
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }
    
    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
    }
    
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("attributes")
    public Attributes getAttributes() {
        return attributes;
    }

    @JsonProperty("attributes")
    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes.addAll(attributes);
    }

    @JsonProperty("tags")
    public Tags getTags() {
        return tags;
    }

    @JsonProperty("tags")
    public void setTags(List<Tag> tags) {
        this.tags.addAll(tags);
    }

    public void setTags(Tags tags) {
        this.tags = tags;
    }

    @JsonProperty("data")
    public Data getData() {
        return data;
    }

    public void addData(DataItem datum) {
        if (Objects.nonNull(this.data)) {
            this.data.add(datum);
        }
    }

    @JsonProperty("data")
    public void setData(Data data) {
        this.data = data;
    }

    @JsonIgnore
    public void setData(List<DataItem> data) {
        this.data.addAll(data);
    }

    @JsonProperty("_meta")
    public MetaList getMeta() {
        return meta;
    }

    @JsonIgnore
    public void setMeta(List<Meta> meta) {
        this.meta.addAll(meta);
    }

    @JsonIgnore
    public void addMeta(Meta meta) {
        if (Objects.nonNull(this.meta)) {
            this.meta.add(meta);
        }
    }

    @JsonProperty("_meta")
    public void setMeta(MetaList meta) {
        this.meta = meta;
    }

    @JsonIgnore
    public List<Tag> getAllTags(Tags t) {
        if (t != null) {
            return Stream.concat(t.stream(), getAllTags())
                    .collect(Collectors.toCollection(Tags::new));
        } else {
            return getAllTags().collect(Collectors.toCollection(Tags::new));
        }
    }

    @JsonIgnore
    private Stream<Tag> getAllTags() {
        return Stream.concat(data.stream().flatMap(item -> item.getTags().stream()),
                Stream.concat(tags.stream(), getMetaTags()));
    }

    @JsonIgnore
    private Stream<Tag> getMetaTags() {
        return meta.stream().filter(Meta::isTag).map(Meta::toTag);
    }

    @JsonIgnore
    public static ProjectInfo create(String name) {
        ProjectInfo p = new ProjectInfo();
        p.setName(name);
        p.setId(name);
        p.setAttributes(new ArrayList<>());
        p.setTags(new ArrayList<>());
        p.setMeta(Meta.def());
        p.setData(new ArrayList<>());
        p.setVersion("1.3");
        return p;
    }

    @JsonIgnore
    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(this);
    }

    @JsonIgnore
    public Optional<Meta> findMeta(String type, String name) {
        Meta m = Meta.create(type, name);
        return meta.stream().filter(m::equals).findFirst();
    }

    @JsonIgnore
    public Optional<Meta> findScenario(String name) {
        return findMeta(Meta.Attributes.scenario.name(), name);
    }

    public Meta findScenarioOrCreate(String name) {
        Optional<Meta> find = findScenario(name);
        if (find.isPresent()) {
            return find.get();
        } else {
            Meta m = Meta.createScenario(name);
            addMeta(m);
            return m;
        }
    }

    @JsonIgnore
    public Stream<Meta> findScenarios() {
        return meta.stream().filter(this::isScn);
    }

    @JsonIgnore
    public void removeAll(Tag tag) {
        getMeta().remove(tag);
        getTags().remove(tag);
        getData().stream().forEach(tc -> {
            tc.getTags().removeTag(tag);
        });
        findScenarios().forEach(scn -> {
            scn.getTags().removeTag(tag);
        });
    }

    @JsonIgnore
    private boolean isScn(Meta m) {
        return Meta.Attributes.scenario.name().equals(m.getType());
    }

}
