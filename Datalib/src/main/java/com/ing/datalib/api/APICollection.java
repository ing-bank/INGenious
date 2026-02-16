package com.ing.datalib.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a collection of API requests organized in folders.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class APICollection implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private List<APIRequest> requests;
    private List<APICollection> folders;
    private List<KeyValuePair> variables;
    private AuthConfig defaultAuth;
    private List<KeyValuePair> defaultHeaders;
    private long createdAt;
    private long updatedAt;
    private String parentId;

    public APICollection() {
        this.id = UUID.randomUUID().toString();
        this.requests = new ArrayList<>();
        this.folders = new ArrayList<>();
        this.variables = new ArrayList<>();
        this.defaultHeaders = new ArrayList<>();
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }

    public APICollection(String name) {
        this();
        this.name = name;
    }

    public APICollection(String name, String description) {
        this(name);
        this.description = description;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public List<APIRequest> getRequests() {
        return requests;
    }

    public void setRequests(List<APIRequest> requests) {
        this.requests = requests;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public List<APICollection> getFolders() {
        return folders;
    }

    public void setFolders(List<APICollection> folders) {
        this.folders = folders;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public List<KeyValuePair> getVariables() {
        return variables;
    }

    public void setVariables(List<KeyValuePair> variables) {
        this.variables = variables;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public AuthConfig getDefaultAuth() {
        return defaultAuth;
    }

    public void setDefaultAuth(AuthConfig defaultAuth) {
        this.defaultAuth = defaultAuth;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public List<KeyValuePair> getDefaultHeaders() {
        return defaultHeaders;
    }

    public void setDefaultHeaders(List<KeyValuePair> defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    // Collection management methods
    public void addRequest(APIRequest request) {
        if (requests == null) {
            requests = new ArrayList<>();
        }
        requests.add(request);
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void removeRequest(APIRequest request) {
        if (requests != null) {
            requests.remove(request);
            this.updatedAt = Instant.now().toEpochMilli();
        }
    }

    public void removeRequestById(String requestId) {
        if (requests != null) {
            requests.removeIf(r -> r.getId().equals(requestId));
            this.updatedAt = Instant.now().toEpochMilli();
        }
    }

    public APIRequest findRequestById(String requestId) {
        if (requests != null) {
            for (APIRequest r : requests) {
                if (r.getId().equals(requestId)) {
                    return r;
                }
            }
        }
        // Search in subfolders
        if (folders != null) {
            for (APICollection folder : folders) {
                APIRequest found = folder.findRequestById(requestId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public void addFolder(APICollection folder) {
        if (folders == null) {
            folders = new ArrayList<>();
        }
        folder.setParentId(this.id);
        folders.add(folder);
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void removeFolder(APICollection folder) {
        if (folders != null) {
            folders.remove(folder);
            this.updatedAt = Instant.now().toEpochMilli();
        }
    }

    public void removeFolderById(String folderId) {
        if (folders != null) {
            folders.removeIf(f -> f.getId().equals(folderId));
            this.updatedAt = Instant.now().toEpochMilli();
        }
    }

    public APICollection findFolderById(String folderId) {
        if (folders != null) {
            for (APICollection folder : folders) {
                if (folder.getId().equals(folderId)) {
                    return folder;
                }
                // Search recursively
                APICollection found = folder.findFolderById(folderId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public void addVariable(String key, String value) {
        if (variables == null) {
            variables = new ArrayList<>();
        }
        variables.add(new KeyValuePair(key, value));
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public String getVariable(String key) {
        if (variables != null) {
            for (KeyValuePair kvp : variables) {
                if (kvp.getKey().equals(key) && kvp.isEnabled()) {
                    return kvp.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Returns the total count of requests including nested folders.
     */
    public int getTotalRequestCount() {
        int count = requests != null ? requests.size() : 0;
        if (folders != null) {
            for (APICollection folder : folders) {
                count += folder.getTotalRequestCount();
            }
        }
        return count;
    }

    /**
     * Returns all requests including from nested folders.
     */
    public List<APIRequest> getAllRequests() {
        List<APIRequest> all = new ArrayList<>();
        if (requests != null) {
            all.addAll(requests);
        }
        if (folders != null) {
            for (APICollection folder : folders) {
                all.addAll(folder.getAllRequests());
            }
        }
        return all;
    }

    @Override
    public String toString() {
        return name != null ? name : "Collection";
    }
}
