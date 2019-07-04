/*
 * Copyright 2019 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.logic;

import androidx.annotation.NonNull;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryController;

public class AuditEvent {

    public enum AuditEventType {
        // Beginning of the form
        BEGINNING_OF_FORM("beginning of form", false, false),
        // Create a question
        QUESTION("question"),
        // Create a group
        GROUP("group questions"),
        // Prompt to add a new repeat
        PROMPT_NEW_REPEAT("add repeat"),
        // Repeat group
        REPEAT("repeat", false, false),
        // Show the "end of form" view
        END_OF_FORM("end screen"),
        // Start filling in the form
        FORM_START("form start"),
        // Exit the form
        FORM_EXIT("form exit"),
        // Resume filling in the form after previously exiting
        FORM_RESUME("form resume"),
        // Save the form
        FORM_SAVE("form save"),
        // Finalize the form
        FORM_FINALIZE("form finalize"),
        // Jump to a question
        HIERARCHY("jump"),
        // Error in save
        SAVE_ERROR("save error"),
        // Error in finalize
        FINALIZE_ERROR("finalize error"),
        // Constraint or missing answer error on save
        CONSTRAINT_ERROR("constraint error"),
        // Delete a repeat group
        DELETE_REPEAT("delete repeat"),

        // Google Play Services are not available
        GOOGLE_PLAY_SERVICES_NOT_AVAILABLE("google play services not available", true, true),
        // Location permissions are granted
        LOCATION_PERMISSIONS_GRANTED("location permissions granted", true, true),
        // Location permissions are not granted
        LOCATION_PERMISSIONS_NOT_GRANTED("location permissions not granted", true, true),
        // Location tracking option is enabled
        LOCATION_TRACKING_ENABLED("location tracking enabled", true, true),
        // Location tracking option is disabled
        LOCATION_TRACKING_DISABLED("location tracking disabled", true, true),
        // Location providers are enabled
        LOCATION_PROVIDERS_ENABLED("location providers enabled", true, true),
        // Location providers are disabled
        LOCATION_PROVIDERS_DISABLED("location providers disabled", true, true),
        // Unknown event type
        UNKNOWN_EVENT_TYPE("Unknown AuditEvent Type");

        private final String value;
        private final boolean isLogged;
        private final boolean isLocationRelated;

        AuditEventType(String value, boolean isLogged, boolean isLocationRelated) {
            this.value = value;

            this.isLogged = isLogged;
            this.isLocationRelated = isLocationRelated;
        }

        AuditEventType(String value) {
            this(value, true, false);
        }

        public String getValue() {
            return value;
        }

        public boolean isLogged() {
            return isLogged;
        }

        public boolean isLocationRelated() {
            return isLocationRelated;
        }
    }

    private final long start;
    private AuditEventType auditEventType;
    private String latitude;
    private String longitude;
    private String accuracy;
    @NonNull private String oldValue;
    @NonNull private String newValue = "";
    private long end;
    private boolean endTimeSet;
    private boolean isTrackingLocationsEnabled;
    private boolean isTrackingChangesEnabled;
    private FormIndex formIndex;

    /*
     * Create a new event
     */
    public AuditEvent(long start, AuditEventType auditEventType) {
        this(start, auditEventType, false, false, null, null);
    }

    public AuditEvent(long start, AuditEventType auditEventType,  boolean isTrackingLocationsEnabled, boolean isTrackingChangesEnabled) {
        this(start, auditEventType, isTrackingLocationsEnabled, isTrackingChangesEnabled, null, null);
    }

    public AuditEvent(long start, AuditEventType auditEventType, boolean isTrackingLocationsEnabled,
                      boolean isTrackingChangesEnabled, FormIndex formIndex, String oldValue) {
        this.start = start;
        this.auditEventType = auditEventType;
        this.isTrackingLocationsEnabled = isTrackingLocationsEnabled;
        this.isTrackingChangesEnabled = isTrackingChangesEnabled;
        this.formIndex = formIndex;
        this.oldValue = oldValue == null ? "" : oldValue;
    }

    /*
     * Return true if this is a view type event
     *  Hierarchy Jump
     *  Question
     *  Prompt for repeat
     */
    public boolean isIntervalAuditEventType() {
        return auditEventType == AuditEventType.HIERARCHY
                || auditEventType == AuditEventType.QUESTION
                || auditEventType == AuditEventType.GROUP
                || auditEventType == AuditEventType.END_OF_FORM
                || auditEventType == AuditEventType.PROMPT_NEW_REPEAT;
    }

    /*
     * Mark the end of an interval event
     */
    public void setEnd(long endTime) {
        this.end = endTime;
        this.endTimeSet = true;
    }

    public boolean isEndTimeSet() {
        return endTimeSet;
    }

    public AuditEventType getAuditEventType() {
        return auditEventType;
    }

    public FormIndex getFormIndex() {
        return formIndex;
    }

    public boolean hasNewAnswer() {
        return !oldValue.equals(newValue);
    }

    public boolean isLocationAlreadySet() {
        return latitude != null && !latitude.isEmpty()
                && longitude != null && !longitude.isEmpty()
                && accuracy != null && !accuracy.isEmpty();
    }

    public void setLocationCoordinates(String latitude, String longitude, String accuracy) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }

    public void recordValueChange(String newValue) {
        this.newValue = newValue != null ? newValue : "";

        // Clear values if they are equal
        if (this.oldValue.equals(this.newValue)) {
            this.oldValue = "";
            this.newValue = "";
            return;
        }

        if (oldValue.contains(",") || oldValue.contains("\n")) {
            oldValue = getEscapedValueForCsv(oldValue);
        }

        if (this.newValue.contains(",") || this.newValue.contains("\n")) {
            this.newValue = getEscapedValueForCsv(this.newValue);
        }
    }

    /*
     * convert the event into a record to write to the CSV file
     */
    @NonNull
    public String toString() {
        String node = formIndex == null || formIndex.getReference() == null ? "" : formIndex.getReference().toString();
        if (auditEventType == AuditEvent.AuditEventType.QUESTION || auditEventType == AuditEvent.AuditEventType.GROUP) {
            int idx = node.lastIndexOf('[');
            if (idx > 0) {
                node = node.substring(0, idx);
            }
        }

        String event;
        if (isTrackingLocationsEnabled && isTrackingChangesEnabled) {
            event = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s", auditEventType.getValue(), node, start, end != 0 ? end : "", latitude, longitude, accuracy, oldValue, newValue);
        } else if (isTrackingLocationsEnabled) {
            event = String.format("%s,%s,%s,%s,%s,%s,%s", auditEventType.getValue(), node, start, end != 0 ? end : "", latitude, longitude, accuracy);
        } else if (isTrackingChangesEnabled) {
            event = String.format("%s,%s,%s,%s,%s,%s", auditEventType.getValue(), node, start, end != 0 ? end : "", oldValue, newValue);
        } else {
            event = String.format("%s,%s,%s,%s", auditEventType.getValue(), node, start, end != 0 ? end : "");
        }

        return event;
    }

    // Get event type based on a Form Controller event
    public static AuditEventType getAuditEventTypeFromFecType(int fcEvent) {
        AuditEventType auditEventType;
        switch (fcEvent) {
            case FormEntryController.EVENT_BEGINNING_OF_FORM:
                auditEventType = AuditEventType.BEGINNING_OF_FORM;
                break;
            case FormEntryController.EVENT_GROUP:
                auditEventType = AuditEventType.GROUP;
                break;
            case FormEntryController.EVENT_REPEAT:
                auditEventType = AuditEventType.REPEAT;
                break;
            case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                auditEventType = AuditEventType.PROMPT_NEW_REPEAT;
                break;
            case FormEntryController.EVENT_END_OF_FORM:
                auditEventType = AuditEventType.END_OF_FORM;
                break;
            default:
                auditEventType = AuditEventType.UNKNOWN_EVENT_TYPE;
        }
        return auditEventType;
    }

    /**
     * Escapes quotes and then wraps in quotes for output to CSV.
     */
    private String getEscapedValueForCsv(String value) {
        if (value.contains("\"")) {
            value = value.replaceAll("\"", "\"\"");
        }

        return "\"" + value + "\"";
    }
}
