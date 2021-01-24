package space.arim.managedwaits;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(FactoryInvocationContextProvider.class)
public class DeadlockFreeFutureFactoryComplexUsageTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private void sleepOneSecond() {
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException ex) {
			fail(ex);
		}
	}

	@TestTemplate
	public void chain4(FactoryOfTheFuture factory) {
		logger.debug("Chain 4 initiating");
		factory.runSync(() -> {
			logger.debug("Chain 4: chain starting on main thread wanders async then resynchronises");

			CentralisedFuture<?> chain4Future = factory.runAsync(() -> {
				logger.debug("Chain 4.1: start");
				sleepOneSecond();
				logger.debug("Chain 4.1: end");

			}).thenRunSync(() -> {
				logger.debug("Chain 4.2");
			});
			logger.debug("Chain 4.3: Awaiting completion of 4.1/4.2...");
			chain4Future.orTimeout(5L, TimeUnit.SECONDS).join();
			logger.info("Chain 4.3: Awaited completion");

		}).orTimeout(10L, TimeUnit.SECONDS).join();
		logger.debug("Finished Chain 4");
	}

	@TestTemplate
	public void chain5(FactoryOfTheFuture factory, ExecutorService mainThreadExecutor)
			throws InterruptedException, ExecutionException, TimeoutException {
		logger.debug("Chain 5 initiating");

		CentralisedFuture<?> mainFuture = factory.runAsync(() -> {
			logger.trace("Chain 5.1: start");
			sleepOneSecond();
			logger.info("Chain 5.1: end");
		}).thenRunSync(() -> {
			logger.info("Chain 5.2");
		});

		logger.debug("Chain 5: Dispatching 5.3 OuterCleanup");
		mainThreadExecutor.submit(() -> {

			logger.info("Chain 5.3: Dispatching 5.4 InnerCleanup");
			CentralisedFuture<?> auxiliaryFuture = factory.runAsync(() -> {
				logger.debug("Chain 5.4: Starting to wait on 5.1/5.2");
				mainFuture.orTimeout(6L, TimeUnit.SECONDS).join();
				logger.debug("Chain 5.4: Succeeded waiting on 5.1/5.2");
			});

			auxiliaryFuture.orTimeout(8L, TimeUnit.SECONDS).join();
			logger.info("Chain 5.3: Succeeded waiting on 5.4 InnerCleanup");

		}).get(10L, TimeUnit.SECONDS);
		logger.debug("Finished Chain 5");
	}

}
