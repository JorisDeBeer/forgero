package com.sigmundgranaas.forgero.model;

import java.util.ArrayList;
import java.util.List;

public class CompositeModelTemplate implements ModelTemplate {
    private final List<ModelTemplate> models;

    public CompositeModelTemplate() {
        this.models = new ArrayList<>();
    }

    public void add(ModelTemplate template) {
        models.add(template);
    }

    public List<ModelTemplate> getModels() {
        return models;
    }

    @Override
    public <T> T convert(Converter<T, ModelTemplate> converter) {
        return converter.convert(this);
    }
}
