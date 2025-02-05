package com.sigmundgranaas.forgero.core.model.match.builders;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Handles the extraction of certain types of values from JsonElements.
 */
public class ElementParser {

	/**
	 * Extracts a JsonObject from the given JsonElement if the JsonElement is a JsonObject with a specified type.
	 *
	 * @param element The JsonElement to extract the JsonObject from.
	 * @param type    The type to match in the JsonObject.
	 * @return An Optional<JsonObject> that contains the JsonObject if it exists and matches the type.
	 */
	public static Optional<JsonObject> fromIdentifiedElement(JsonElement element, String type) {
		if (element.isJsonObject()) {
			var object = element.getAsJsonObject();
			return Optional.of(object).filter(jsonObject -> jsonObject.has("type") && jsonObject.get("type").getAsString().equals(type));
		}
		return Optional.empty();
	}

	/**
	 * Extracts a String from the given JsonElement if the JsonElement is a JsonPrimitive that contains a string.
	 *
	 * @param element The JsonElement to extract the String from.
	 * @return An Optional<String> that contains the String if it exists.
	 */
	public static Optional<String> fromString(JsonElement element) {
		if (element.isJsonPrimitive()) {
			return Optional.of(element.getAsJsonPrimitive().getAsString());
		}
		return Optional.empty();
	}
}
