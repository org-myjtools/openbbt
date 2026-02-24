package org.myjtools.openbbt.core.test.backend;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTContextManager;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.backend.StepProviderBackend;
import org.myjtools.openbbt.core.plan.DataTable;
import org.myjtools.openbbt.core.plan.Document;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThatCode;

class TestStepProviderBackend {

	static final Config TEST_CONFIG = Config.ofMap(Map.of(
			OpenBBTConfig.RESOURCE_PATH, "src/test/resources",
			OpenBBTConfig.ENV_PATH, "target/.openbbt"
	));

	@Nested
	class TestsEnglish {
		@Test
		void testRunStepWithOneParameter() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(()->	backend.run("Step with one parameter: 5", Locale.ENGLISH, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOneParameterFailsWhenNoExpectedAdditionalData() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			DataTable dataTable = new DataTable();
			assertThatCode(() -> backend.run("Step with one parameter: 5", Locale.ENGLISH, dataTable))
				.isInstanceOf(OpenBBTException.class)
				.hasMessageContaining("Step 'stepWithOneParameter' does not expect additional data, but it was provided");
		}

		@Test
		void testRunStepWithoutParameters() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Step without parameters", Locale.ENGLISH, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithTwoParameters() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Step with two parameters: 3, 7", Locale.ENGLISH, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepNotFoundThrowsException() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Non-existent step", Locale.ENGLISH, null))
				.isInstanceOf(OpenBBTException.class)
				.hasMessageContaining("No matching step found for 'Non-existent step'");
		}

		@Test
		void testRunStepWithOnlyDataTable() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			var dataTable = new DataTable(List.of(List.of("col1", "col2"), List.of("val1", "val2")));
			assertThatCode(() -> backend.run("Step with only data table", Locale.ENGLISH, dataTable))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOnlyDocument() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			var document = Document.of("text/plain", "hello world");
			assertThatCode(() -> backend.run("Step with only document", Locale.ENGLISH, document))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOneParameterAndAssertionPassed() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Step with one parameter and assertion: 5 is greater than 2", Locale.ENGLISH, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOnlyAssertionStored() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Step with only assertion: is greater than 2", Locale.ENGLISH, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOneParameterAndDataTable() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			var dataTable = new DataTable(List.of(List.of("col1"), List.of("val1")));
			assertThatCode(() -> backend.run("Step with one parameter and data table: 5", Locale.ENGLISH, dataTable))
				.doesNotThrowAnyException();
		}

		@Test
		void testSetUp() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(backend::setUp).doesNotThrowAnyException();
		}

		@Test
		void testTearDown() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(backend::tearDown).doesNotThrowAnyException();
		}
	}


	@Nested
	class TestsSpanish {

		static final Locale ES = Locale.forLanguageTag("es");

		@Test
		void testRunStepWithOneParameter() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Paso con un parámetro: 5", ES, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOneParameterFailsWhenNoExpectedAdditionalData() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			DataTable dataTable = new DataTable();
			assertThatCode(() -> backend.run("Paso con un parámetro: 5", ES, dataTable))
				.isInstanceOf(OpenBBTException.class)
				.hasMessageContaining("Step 'stepWithOneParameter' does not expect additional data, but it was provided");
		}

		@Test
		void testRunStepWithoutParameters() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Paso sin parámetros", ES, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithTwoParameters() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Paso con dos parámetros: 3, 7", ES, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepNotFoundThrowsException() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Paso inexistente", ES, null))
				.isInstanceOf(OpenBBTException.class)
				.hasMessageContaining("No matching step found for 'Paso inexistente'");
		}

		@Test
		void testRunStepWithOnlyDataTable() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			var dataTable = new DataTable(List.of(List.of("col1", "col2"), List.of("val1", "val2")));
			assertThatCode(() -> backend.run("Paso con solo tabla de datos", ES, dataTable))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOnlyDocument() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			var document = Document.of("text/plain", "hola mundo");
			assertThatCode(() -> backend.run("Paso con solo documento", ES, document))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOneParameterAndAssertionPassed() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Paso con un parámetro y una aserción: 5 es mayor que 2", ES, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOnlyAssertionStored() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(() -> backend.run("Paso con solo aserción: es mayor que 2", ES, null))
				.doesNotThrowAnyException();
		}

		@Test
		void testRunStepWithOneParameterAndDataTable() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			var dataTable = new DataTable(List.of(List.of("col1"), List.of("val1")));
			assertThatCode(() -> backend.run("Paso con un parámetro y tabla de datos: 5", ES, dataTable))
				.doesNotThrowAnyException();
		}

		@Test
		void testSetUp() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(backend::setUp).doesNotThrowAnyException();
		}

		@Test
		void testTearDown() {
			var cm = new OpenBBTContextManager(TEST_CONFIG);
			var backend = new StepProviderBackend(cm);
			assertThatCode(backend::tearDown).doesNotThrowAnyException();
		}
	}


}
