package io.ocfl.core.extension;

import io.ocfl.api.model.ValidationCode;
import io.ocfl.core.storage.common.Storage;

/**
 * Validation context for OCFL extensions.
 */
public interface ValidationContext {

    /**
     * @return the path to the object root within the storage
     */
    String getObjectRootPath();

    /**
     * @return the path to the extension's directory within the object
     */
    String getExtensionPath();

    /**
     * @return the storage implementation
     */
    Storage getStorage();

    /**
     * Adds a validation issue.
     *
     * @param code the validation code
     * @param message the error message
     * @param args message arguments
     */
    void addIssue(ValidationCode code, String message, Object... args);

}
