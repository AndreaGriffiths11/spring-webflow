package org.springframework.faces.webflow;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jakarta.faces.application.ApplicationFactory;
import jakarta.faces.context.FacesContextFactory;
import jakarta.faces.event.PhaseEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PhaseListener;
import jakarta.faces.lifecycle.LifecycleFactory;
import jakarta.faces.render.RenderKitFactory;
import org.apache.myfaces.test.base.junit.AbstractJsfTestCase;
import org.apache.myfaces.test.mock.MockApplicationFactory;
import org.apache.myfaces.test.mock.MockFacesContext;
import org.apache.myfaces.test.mock.MockFacesContextFactory;
import org.apache.myfaces.test.mock.MockRenderKitFactory;
import org.apache.myfaces.test.mock.lifecycle.MockLifecycle;
import org.apache.myfaces.test.mock.lifecycle.MockLifecycleFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class JsfUtilsTests extends AbstractJsfTestCase {

	public JsfUtilsTests() {
		super();
	}

	@Test
	public void testBeforeListenersCalledInForwardOrder() {
		List<OrderVerifyingPhaseListener> list = new ArrayList<>();
		MockLifecycle lifecycle = new MockLifecycle();
		PhaseListener listener1 = new OrderVerifyingPhaseListener(null, list);
		lifecycle.addPhaseListener(listener1);
		PhaseListener listener2 = new OrderVerifyingPhaseListener(null, list);
		lifecycle.addPhaseListener(listener2);
		PhaseListener listener3 = new OrderVerifyingPhaseListener(null, list);
		lifecycle.addPhaseListener(listener3);
		JsfUtils.notifyBeforeListeners(PhaseId.ANY_PHASE, lifecycle, new MockFacesContext());
		assertEquals(listener1, list.getFirst());
		assertEquals(listener2, list.get(1));
		assertEquals(listener3, list.get(2));
	}

	@Test
	public void testAfterListenersCalledInReverseOrder() {
		List<OrderVerifyingPhaseListener> list = new ArrayList<>();
		MockLifecycle lifecycle = new MockLifecycle();
		PhaseListener listener1 = new OrderVerifyingPhaseListener(list, null);
		lifecycle.addPhaseListener(listener1);
		PhaseListener listener2 = new OrderVerifyingPhaseListener(list, null);
		lifecycle.addPhaseListener(listener2);
		PhaseListener listener3 = new OrderVerifyingPhaseListener(list, null);
		lifecycle.addPhaseListener(listener3);
		JsfUtils.notifyAfterListeners(PhaseId.ANY_PHASE, lifecycle, new MockFacesContext());
		assertEquals(listener3, list.getFirst());
		assertEquals(listener2, list.get(1));
		assertEquals(listener1, list.get(2));
	}

	@Test
	public void testGetFactory() {
		// Not testing all but at least test the mocked factories
		assertTrue(JsfUtils.findFactory(ApplicationFactory.class) instanceof MockApplicationFactory);
		assertTrue(JsfUtils.findFactory(FacesContextFactory.class) instanceof MockFacesContextFactory);
		assertTrue(JsfUtils.findFactory(LifecycleFactory.class) instanceof MockLifecycleFactory);
		assertTrue(JsfUtils.findFactory(RenderKitFactory.class) instanceof MockRenderKitFactory);
	}

	@Test
	public void testGetUnknowFactory() {
		try {
			JsfUtils.findFactory(InputStream.class);
			fail("Did not throw");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	private static class OrderVerifyingPhaseListener implements PhaseListener {

		private final List<OrderVerifyingPhaseListener> afterPhaseList;
		private final List<OrderVerifyingPhaseListener> beforePhaseList;

		public OrderVerifyingPhaseListener(List<OrderVerifyingPhaseListener> afterPhaseList,
				List<OrderVerifyingPhaseListener> beforePhaseList) {
			this.afterPhaseList = afterPhaseList;
			this.beforePhaseList = beforePhaseList;
		}

		public void afterPhase(PhaseEvent event) {
			this.afterPhaseList.add(this);
		}

		public void beforePhase(PhaseEvent event) {
			this.beforePhaseList.add(this);
		}

		public PhaseId getPhaseId() {
			return PhaseId.ANY_PHASE;
		}

	}

}
