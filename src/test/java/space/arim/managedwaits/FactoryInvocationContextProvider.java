package space.arim.managedwaits;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

public class FactoryInvocationContextProvider implements TestTemplateInvocationContextProvider  {

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return true;
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		return Stream.of(new LightSleepManagedWaitStrategy(), new BlockingManagedWaitStrategy())
				.map(DeadlockFreeFutureFactoryParameterResolver::new)
				.map((parameterResolver) -> new TestTemplateInvocationContext() {
					@Override
					public List<Extension> getAdditionalExtensions() {
						return List.of(parameterResolver);
					}
				});
	}

	private static class DeadlockFreeFutureFactoryParameterResolver implements ParameterResolver {

		private final ManagedWaitStrategy waitStrategy;

		DeadlockFreeFutureFactoryParameterResolver(ManagedWaitStrategy waitStrategy) {
			this.waitStrategy = waitStrategy;
		}

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
				throws ParameterResolutionException {
			Class<?> paramType = parameterContext.getParameter().getType();
			return paramType.equals(FactoryOfTheFuture.class)
					|| paramType.equals(ScheduledExecutorService.class)
					|| paramType.equals(ExecutorService.class);
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
				throws ParameterResolutionException {
			Class<?> paramType = parameterContext.getParameter().getType();
			FactoryImpl factory = extensionContext.getStore(ExtensionContext.Namespace.create(getClass()))
					.getOrComputeIfAbsent(waitStrategy, FactoryImpl::create, FactoryImpl.class);
			return paramType.equals(FactoryOfTheFuture.class) ? factory : factory.mainThreadExecutor();
		}
	}

}
