/*
 * Copyright 2004-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.faces.model;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIData;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.event.ActionListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.faces.webflow.FlowActionListener;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Custom {@link ActionListener} that inspects the {@link UIComponent} that signaled the current {@link ActionEvent} to
 * determine whether it is a child of any iterator type of component (such as {@link UIData}) that uses a
 * {@link SelectionAware} data model implementation. If a containing SelectionAware model is found, the row containing
 * the event-signaling component instance will be selected. This enables convenient access to the selected model state
 * at any time through EL expressions such as #{model.selectedRow.id} without having to rely on the whether or not the
 * current row index is pointing to the desired row as it would need to be to use an expression such as
 * #{model.rowData.id}
 *
 * @author Jeremy Grelle
 */
public class SelectionTrackingActionListener implements ActionListener {

	private static final Method NO_MATCH =
			ClassUtils.getMethodIfAvailable(System.class, "currentTimeMillis", (Class<?>[]) null);

	private static final Log logger = LogFactory.getLog(FlowActionListener.class);

	private final ActionListener delegate;

	private final Map<Class<?>, Method> valueMethodCache = new ConcurrentHashMap<>(256);


	public SelectionTrackingActionListener(ActionListener delegate) {
		this.delegate = delegate;
	}

	public void processAction(ActionEvent event) throws AbortProcessingException {
		trackSelection(event.getComponent());
		this.delegate.processAction(event);
	}

	private void trackSelection(UIComponent component) {
		// Find parent component with a SelectionAware model if it exists
		UIComponent currentComponent = component;
		while (currentComponent.getParent() != null && !(currentComponent.getParent() instanceof UIViewRoot)) {
			UIComponent parent = currentComponent.getParent();
			Method valueAccessor = getValueMethod(parent.getClass());
			if (valueAccessor != null) {
				Object value = ReflectionUtils.invokeMethod(valueAccessor, parent);
				if (value instanceof SelectionAware<?> aware) {
					aware.setCurrentRowSelected(true);
					if (logger.isDebugEnabled()) {
						logger.debug("Row selection has been set on the current SelectionAware data model.");
					}
					break;
				}
			}
			currentComponent = currentComponent.getParent();
		}
	}

	private Method getValueMethod(Class<?> parentClass) {
		Method method = this.valueMethodCache.get(parentClass);
		if (method == null) {
			method = ReflectionUtils.findMethod(parentClass, "getValue");
			this.valueMethodCache.put(parentClass, (method != null) ? method : NO_MATCH);
		}
		return (method != NO_MATCH) ? method : null;
	}

}
