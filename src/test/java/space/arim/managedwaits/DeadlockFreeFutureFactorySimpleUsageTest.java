package space.arim.managedwaits;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.concurrent.TimeUnit;

@ExtendWith(FactoryInvocationContextProvider.class)
public class DeadlockFreeFutureFactorySimpleUsageTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@TestTemplate
	public void chain1(FactoryOfTheFuture factory) {
		factory.runAsync(() -> logger.trace("Chain 1: simple async task"))
				.orTimeout(1L, TimeUnit.SECONDS).join();
		logger.trace("Finished Chain 1");
	}

	@TestTemplate
	public void chain2(FactoryOfTheFuture factory) {
		factory.runSync(() -> logger.trace("Chain 2: main thread task"))
				.orTimeout(1L, TimeUnit.SECONDS).join();
		logger.trace("Finished Chain 2");
	}

	@TestTemplate
	public void chain3(FactoryOfTheFuture factory) {
		factory.runSync(() -> logger.debug("Chain 3.1: main thread task"))
				.thenRunAsync(() -> logger.debug("Chain 3.2: back to async"))
				.thenRunSync(() -> logger.debug("Chain 3.3: back to main thread"))
				.orTimeout(1L, TimeUnit.SECONDS).join();
		logger.debug("Finished Chain 3");
	}

}
