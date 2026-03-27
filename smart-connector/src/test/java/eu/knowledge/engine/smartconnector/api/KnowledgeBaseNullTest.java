package eu.knowledge.engine.smartconnector.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.junit.jupiter.api.Test;

import eu.knowledge.engine.smartconnector.impl.SmartConnectorBuilder;

/**
 * Giving null values for id, name or description is incorrect and this unit
 * tests makes sure we throw a null pointer exception when constructing the SC.
 */
class KnowledgeBaseNullTest {

	private MyKnowledgeBase kb = new MyKnowledgeBase("kb1");

	private static class MyKnowledgeBase implements KnowledgeBase {

		private String id;
		private String name;
		private String desc;
		private boolean idIsNull = false;
		private boolean nameIsNull = false;
		private boolean descIsNull = false;

		public MyKnowledgeBase(String aName) {
			this.id = "https://www.example.org/" + aName;
			this.name = aName;
			this.desc = aName + " description";
		}

		@Override
		public URI getKnowledgeBaseId() {
			if (idIsNull)
				return null;
			else
				return URI.create(this.id);
		}

		@Override
		public String getKnowledgeBaseName() {
			if (nameIsNull)
				return null;
			else
				return this.name;
		}

		@Override
		public String getKnowledgeBaseDescription() {
			if (descIsNull)
				return null;
			else
				return this.desc;
		}

		@Override
		public void smartConnectorReady(SmartConnector aSC) {
		}

		@Override
		public void smartConnectorConnectionLost(SmartConnector aSC) {
		}

		@Override
		public void smartConnectorConnectionRestored(SmartConnector aSC) {
		}

		@Override
		public void smartConnectorStopped(SmartConnector aSC) {
		}

		public void setNull(boolean anIdIsNull, boolean aNameIsNull, boolean aDescIsNull) {
			this.idIsNull = anIdIsNull;
			this.nameIsNull = aNameIsNull;
			this.descIsNull = aDescIsNull;
		}
	}

	@Test
	void testNullId() {
		kb.setNull(true, false, false);
		assertThrows(NullPointerException.class, () -> {
			SmartConnectorBuilder.newSmartConnector(kb).create();
		});
	}

	@Test
	void testNullName() {
		kb.setNull(false, true, false);
		assertThrows(NullPointerException.class, () -> {
			SmartConnectorBuilder.newSmartConnector(kb).create();
		});
	}

	@Test
	void testNullDesc() {
		kb.setNull(false, false, true);
		assertThrows(NullPointerException.class, () -> {
			SmartConnectorBuilder.newSmartConnector(kb).create();
		});
	}

}
