package com.box.sdk;

import java.util.HashMap;
import java.util.Map;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * The abstract base class for all types that contain JSON data returned by the Box API. The most common implementation
 * of BoxJSONObject is {@link BoxResource.Info} and its subclasses. Changes made to a BoxJSONObject will be tracked
 * locally until the pending changes are sent back to Box in order to avoid unnecessary network requests.
 *
 */
public abstract class BoxJSONObject {
    /**
     * The JsonObject that contains any local pending changes. When getPendingChanges is called, this object will be
     * encoded to a JSON string.
     */
    private JsonObject pendingChanges;

    /**
     * A map of other BoxJSONObjects which will be lazily converted to a JsonObject once getPendingChanges is called.
     * This allows changes to be made to a child BoxJSONObject and still have those changes reflected in the JSON
     * string.
     */
    private Map<String, BoxJSONObject> lazyPendingChanges;

    /**
     * Constructs an empty BoxJSONObject.
     */
    public BoxJSONObject() {
        this.lazyPendingChanges = new HashMap<String, BoxJSONObject>();
    }

    /**
     * Constructs a BoxJSONObject by decoding it from a JSON string.
     * @param  json the JSON string to decode.
     */
    public BoxJSONObject(String json) {
        this(JsonObject.readFrom(json));
    }

    /**
     * Constructs a BoxJSONObject using an already parsed JSON object.
     * @param  jsonObject the parsed JSON object.
     */
    BoxJSONObject(JsonObject jsonObject) {
        this();

        this.update(jsonObject);
    }

    /**
     * Clears any pending changes from this JSON object.
     */
    public void clearPendingChanges() {
        this.pendingChanges = null;
        this.lazyPendingChanges.clear();
    }

    /**
     * Gets a JSON string containing any pending changes to this object that can be sent back to the Box API.
     * @return a JSON string containing the pending changes.
     */
    public String getPendingChanges() {
        JsonObject jsonObject = this.getPendingJSONObject();
        if (jsonObject == null) {
            return null;
        }

        return jsonObject.toString();
    }

    /**
     * Invoked with a JSON member whenever this object is updated or created from a JSON object.
     *
     * <p>Subclasses should override this method in order to parse any JSON members it knows about. This method is a
     * no-op by default.</p>
     *
     * @param member the JSON member to be parsed.
     */
    void parseJSONMember(JsonObject.Member member) { }

    /**
     * Adds a pending field change that needs to be sent to the API. It will be included in the JSON string the next
     * time {@link #getPendingChanges} is called.
     * @param key   the name of the field.
     * @param value the new boolean value of the field.
     */
    void addPendingChange(String key, boolean value) {
        if (this.pendingChanges == null) {
            this.pendingChanges = new JsonObject();
        }

        this.pendingChanges.set(key, value);
    }

    /**
     * Adds a pending field change that needs to be sent to the API. It will be included in the JSON string the next
     * time {@link #getPendingChanges} is called.
     * @param key   the name of the field.
     * @param value the new String value of the field.
     */
    void addPendingChange(String key, String value) {
        this.addPendingChange(key, JsonValue.valueOf(value));
    }

    /**
     * Adds a pending field change that needs to be sent to the API. It will be included in the JSON string the next
     * time {@link #getPendingChanges} is called.
     * @param key   the name of the field.
     * @param value the new BoxJSONObject value of the field.
     */
    void addPendingChange(String key, BoxJSONObject value) {
        this.lazyPendingChanges.put(key, value);
    }

    /**
     * Adds a pending field change that needs to be sent to the API. It will be included in the JSON string the next
     * time {@link #getPendingChanges} is called.
     * @param key   the name of the field.
     * @param value the JsonValue of the field.
     */
    private void addPendingChange(String key, JsonValue value) {
        if (this.pendingChanges == null) {
            this.pendingChanges = new JsonObject();
        }

        this.pendingChanges.set(key, value);
    }

    /**
     * Updates this BoxJSONObject using the information in a JSON object.
     * @param jsonObject the JSON object containing updated information.
     */
    void update(JsonObject jsonObject) {
        for (JsonObject.Member member : jsonObject) {
            if (member.getValue().isNull()) {
                continue;
            }

            this.parseJSONMember(member);
        }

        this.clearPendingChanges();
    }

    /**
     * Gets a JsonObject containing any pending changes to this object that can be sent back to the Box API.
     * @return a JsonObject containing the pending changes.
     */
    JsonObject getPendingJSONObject() {
        if (this.pendingChanges == null && !this.lazyPendingChanges.isEmpty()) {
            this.pendingChanges = new JsonObject();
        }

        for (Map.Entry<String, BoxJSONObject> entry : this.lazyPendingChanges.entrySet()) {
            this.pendingChanges.set(entry.getKey(), entry.getValue().getPendingJSONObject());
        }
        return this.pendingChanges;
    }
}