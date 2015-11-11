package org.codefx.maven.plugin.jdeps.result;

import com.google.common.collect.Sets;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codefx.maven.plugin.jdeps.dependency.Violation;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingInt;

/**
 * A {@link ResultOutputStrategy} that uses the Mojos facilities to report violations, i.e. the logger and exceptions.
 */
public class MojoResultOutputStrategy implements ResultOutputStrategy {

	private static final String MESSAGE_IGNORE_DEPENDENCIES =
			"\nJDeps reported {0} dependencies on JDK-internal APIs that are configured to be ignored.";
	private static final String MESSAGE_INFORM_DEPENDENCIES =
			"\nJDeps reported {0} dependencies on JDK-internal APIs that are configured to be logged:\n{1}";
	private static final String MESSAGE_WARN_DEPENDENCIES =
			"\nJDeps reported {0} dependencies on JDK-internal APIs that are configured to be warned about:\n{1}";
	private static final String MESSAGE_FAIL_DEPENDENCIES =
			"\nJDeps reported {0} dependencies on JDK-internal APIs that are configured to fail the build:\n{1}";
	private static final String MESSAGE_NO_DEPENDENCIES =
			"\nJDeps reported no dependencies on JDK-internal APIs.";

	private final Log log;

	/**
	 * Creates a new output strategy.
	 *
	 * @param log the log to use for reporting violations
	 */
	public MojoResultOutputStrategy(Log log) {
		this.log = Objects.requireNonNull(log, "The argument 'log' must not be null.");
	}

	@Override
	public void output(Result result) throws MojoFailureException {
		int violationsCount = logNumberOfViolationsToIgnore(result);
		violationsCount += logViolationsToInform(result);
		violationsCount += logViolationsToWarn(result);
		violationsCount += throwExceptionForViolationsToFail(result);

		// note that the previous line might have thrown an exception in which case this is never executed
		if (violationsCount == 0)
			logZeroDependencies(log);
	}

	private int logNumberOfViolationsToIgnore(Result result) {
		return logViolations(result.violationsToIgnore(), MESSAGE_IGNORE_DEPENDENCIES, log::info);
	}

	private int logViolationsToInform(Result result) {
		return logViolations(result.violationsToInform(), MESSAGE_INFORM_DEPENDENCIES, log::info);
	}

	private int logViolationsToWarn(Result result) {
		return logViolations(result.violationsToWarn(), MESSAGE_WARN_DEPENDENCIES, log::warn);
	}

	private int throwExceptionForViolationsToFail(Result result) throws MojoFailureException {
		return countAndIfViolationsExist(
				result.violationsToFail(),
				(count, message) -> {
					throw new MojoFailureException(format(MESSAGE_FAIL_DEPENDENCIES, count, message));
				});
	}

	private int logViolations(Stream<Violation> violations, String messageFormat, Consumer<String> log) {
		return countAndIfViolationsExist(
				violations,
				(count, message) -> log.accept(format(messageFormat, count, message)));
	}

	private <E extends Exception> int countAndIfViolationsExist(
			Stream<Violation> violations, HandleViolations<E> handleViolations) throws E {
		Pair<Integer, String> countAndMessage = violations
				.collect(
						PairCollector.pairing(
								summingInt(violation -> violation.getInternalDependencies().size()),
								mapping(Violation::toMultiLineString, joining("\n"))));
		if (countAndMessage.first == 0)
			return 0;

		handleViolations.handle(countAndMessage.first, countAndMessage.second);
		return countAndMessage.first;
	}

	private void logZeroDependencies(Log log) {
		log.info(MESSAGE_NO_DEPENDENCIES);
	}

	@FunctionalInterface
	private interface HandleViolations<E extends Exception> {

		void handle(int count, String message) throws E;

	}

	/*
	 * TODO: The following should be polished, tested and placed somewhere else.
	 */

	private static class Pair<A, B> {

		final A first;
		final B second;

		Pair(A first, B second) {
			this.first = first;
			this.second = second;
		}
	}

	/**
	 * Uses two collectors on a stream of pairs to create a pair of collection results.
	 *
	 * @param <T> the type of input elements to the reduction operation
	 * @param <A> the result type of the first reduction operation
	 * @param <B> the result type of the second reduction operation
	 * @param <CA> the mutable accumulation type of the first reduction operation
	 * @param <CB> the mutable accumulation type of the second reduction operation
	 */
	private static class PairCollector<T, A, B, CA, CB> implements Collector<T, Pair<CA, CB>, Pair<A, B>> {

		private final Collector<T, CA, A> firstCollector;
		private final Collector<T, CB, B> secondCollector;

		private PairCollector(Collector<T, CA, A> firstCollector, Collector<T, CB, B> secondCollector) {
			this.firstCollector = firstCollector;
			this.secondCollector = secondCollector;
		}

		public static <T, A, B, CA, CB> Collector<T, ?, Pair<A, B>> pairing(
				Collector<T, CA, A> firstCollector, Collector<T, CB, B> secondCollector) {
			return new PairCollector<>(firstCollector, secondCollector);
		}

		@Override
		public Supplier<Pair<CA, CB>> supplier() {
			return () -> new Pair<>(firstCollector.supplier().get(), secondCollector.supplier().get());
		}

		@Override
		public BiConsumer<Pair<CA, CB>, T> accumulator() {
			return (containers, newValue) -> {
				firstCollector.accumulator().accept(containers.first, newValue);
				secondCollector.accumulator().accept(containers.second, newValue);
			};
		}

		@Override
		public BinaryOperator<Pair<CA, CB>> combiner() {
			return (containers, otherContainers) -> {
				CA firstNewContainer = firstCollector.combiner().apply(containers.first, otherContainers.first);
				CB secondNewContainer = secondCollector.combiner().apply(containers.second, otherContainers.second);
				return new Pair<>(firstNewContainer, secondNewContainer);
			};
		}

		@Override
		public Function<Pair<CA, CB>, Pair<A, B>> finisher() {
			return containers -> {
				A firstResult = firstCollector.finisher().apply(containers.first);
				B secondResult = secondCollector.finisher().apply(containers.second);
				return new Pair<>(firstResult, secondResult);
			};
		}

		@Override
		public Set<Characteristics> characteristics() {
			return Sets.intersection(firstCollector.characteristics(), secondCollector.characteristics());
		}

	}

}
