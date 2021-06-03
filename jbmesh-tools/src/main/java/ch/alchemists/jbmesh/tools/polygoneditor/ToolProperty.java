// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.tools.polygoneditor;

import com.jme3.scene.Node;
import com.simsilica.lemur.*;
import com.simsilica.lemur.core.VersionedReference;

abstract class ToolProperty<T> {
    public interface ValueChangedCallback<T> {
        void onValueChanged(T value);
    }


    private final String name;
    private T value;
    private final ValueChangedCallback<T> callback;



    public ToolProperty(String name, T defaultValue, ValueChangedCallback<T> callback) {
        this.name = name;
        this.value = defaultValue;
        this.callback = callback;
    }


    public String getName() {
        return name;
    }


    public void setValue(T value) {
        this.value = value;
        callback.onValueChanged(value);
    }

    public T getValue() {
        return value;
    }


    public abstract Node getGuiElement();

    public void update(float tpf) {}



    static class BooleanProperty extends ToolProperty<Boolean> {
        public BooleanProperty(String name, boolean defaultValue, ValueChangedCallback<Boolean> callback) {
            super(name, defaultValue, callback);
        }

        @Override
        public Node getGuiElement() {
            Checkbox checkbox = new Checkbox(getName());
            checkbox.getModel().setChecked(getValue());

            checkbox.addClickCommands(src -> {
                setValue(checkbox.getModel().isChecked());
            });

            return checkbox;
        }
    }


    static class FloatProperty extends ToolProperty<Float> {
        private final float min;
        private final float max;

        private Label label;
        private VersionedReference<Double> ref;

        public FloatProperty(String name, float defaultValue, float min, float max, ValueChangedCallback<Float> callback) {
            super(name, defaultValue, callback);
            this.min = min;
            this.max = max;
        }

        @Override
        public Node getGuiElement() {
            Container container = new Container();
            label = new Label( getText(getValue()) );
            container.addChild(label);

            Slider slider = new Slider(Axis.X);
            container.addChild(slider);
            slider.getModel().setMinimum(min);
            slider.getModel().setMaximum(max);
            slider.getModel().setValue(getValue());
            ref = slider.getModel().createReference();

            return container;
        }

        @Override
        public void update(float tpf) {
            if(ref.update()) {
                float val = (float)(double)ref.get();
                setValue(val);
                label.setText( getText(val) );
            }
        }

        private String getText(float val) {
            return String.format("%s: %.2f", getName(), val);
        }
    }
}
