package eu.wdaqua.qanary.process;

import com.fasterxml.jackson.annotation.JsonValue;
import eu.wdaqua.qanary.exceptions.InvalidProcessDefinitionException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * The components a question answering process executes and their order, i.e.,
 * the {@code componentlist} of a JSON request starting a process.
 * <p>
 * A process definition is a JSON array whose elements are executed one after
 * another. An element is either the name of a component or a parallel block: a
 * JSON object whose properties are named streams. All streams of a parallel
 * block run at the same time, the components of a stream run one after another,
 * and the element following the block starts when all streams have finished.
 *
 * <pre>
 * ["a", "b"]                                    a, then b
 * ["a", {"p1": ["b"], "x": ["c", "d"]}, "e"]    a, then b parallel to (c, then d), then e
 * </pre>
 *
 * A stream is a process definition itself, so it may contain parallel blocks as
 * well. The names of the streams are chosen by the caller; they identify the
 * streams if anything goes wrong and therefore have to be unique within a
 * process definition.
 *
 * @param steps the elements, executed one after another
 */
@JsonDeserialize(using = QanaryProcessDefinitionDeserializer.class)
public record QanaryProcessDefinition(List<Step> steps) {

	/**
	 * the process definition that executes no component
	 */
	public static final QanaryProcessDefinition EMPTY = new QanaryProcessDefinition(List.of());

	/**
	 * an element of a process definition
	 */
	public sealed interface Step permits ComponentStep, ParallelStep {
	}

	/**
	 * executes the component with the given name
	 *
	 * @param componentName the name the component is registered with
	 */
	public record ComponentStep(String componentName) implements Step {

		public ComponentStep {
			Objects.requireNonNull(componentName, "componentName");
		}
	}

	/**
	 * executes all streams at the same time; finished when all streams have
	 * finished
	 *
	 * @param streams at least one stream
	 */
	public record ParallelStep(List<NamedStream> streams) implements Step {

		public ParallelStep {
			streams = List.copyOf(streams);
			if (streams.isEmpty()) {
				throw new InvalidProcessDefinitionException(
						"a parallel block ({}) needs at least one stream, e.g., {\"p1\": [\"a\"], \"p2\": [\"b\"]}");
			}
		}
	}

	/**
	 * a part of a parallel block: its steps are executed one after another, at
	 * the same time as the other streams of the block
	 *
	 * @param name  identifies the stream (e.g., in error reports)
	 * @param steps at least one step
	 */
	public record NamedStream(String name, List<Step> steps) {

		public NamedStream {
			if (name == null || name.isBlank()) {
				throw new InvalidProcessDefinitionException(
						"the name of a parallel stream must not be empty, as it identifies the stream");
			}
			steps = List.copyOf(steps);
			if (steps.isEmpty()) {
				throw new InvalidProcessDefinitionException(
						"the parallel stream \"" + name + "\" contains no component");
			}
		}
	}

	/**
	 * @throws InvalidProcessDefinitionException if a stream name is used more than
	 *                                           once
	 */
	public QanaryProcessDefinition {
		steps = List.copyOf(steps);
		Set<String> streamNames = new HashSet<>();
		checkUniqueStreamNames(steps, streamNames);
	}

	private static void checkUniqueStreamNames(List<Step> steps, Set<String> streamNames) {
		for (Step step : steps) {
			if (step instanceof ParallelStep parallel) {
				for (NamedStream stream : parallel.streams()) {
					if (!streamNames.add(stream.name())) {
						throw duplicateStreamName(stream.name());
					}
					checkUniqueStreamNames(stream.steps(), streamNames);
				}
			}
		}
	}

	static InvalidProcessDefinitionException duplicateStreamName(String name) {
		return new InvalidProcessDefinitionException("the stream name \"" + name + "\" is used more than once; "
				+ "stream names identify the parallel streams and have to be unique within the componentlist");
	}

	/**
	 * a process definition executing the given components one after another (the
	 * component list of the form-based requests)
	 *
	 * @param componentNames the component names, null for none
	 */
	public static QanaryProcessDefinition sequential(List<String> componentNames) {
		if (componentNames == null) {
			return EMPTY;
		}
		return new QanaryProcessDefinition(componentNames.stream().<Step>map(ComponentStep::new).toList());
	}

	/**
	 * create a process definition from its JSON structure
	 *
	 * @param json a list (JSON array) whose elements are component names (strings)
	 *             and parallel blocks (maps from the stream names to such lists);
	 *             null for an empty process definition
	 * @throws InvalidProcessDefinitionException if the structure is not a valid
	 *                                           process definition
	 */
	public static QanaryProcessDefinition fromJson(Object json) {
		if (json == null) {
			return EMPTY;
		}
		return new QanaryProcessDefinition(stepsFromJson(json, "the componentlist"));
	}

	private static List<Step> stepsFromJson(Object json, String what) {
		if (!(json instanceof List<?> elements)) {
			throw new InvalidProcessDefinitionException(what
					+ " has to be an array of component names and parallel blocks, but it is " + describe(json));
		}
		List<Step> steps = new ArrayList<>();
		for (Object element : elements) {
			if (element instanceof String componentName) {
				steps.add(new ComponentStep(componentName));
			} else if (element instanceof Map<?, ?> block) {
				steps.add(parallelStepFromJson(block));
			} else {
				throw new InvalidProcessDefinitionException(what + " contains " + describe(element)
						+ "; allowed are component names (strings) and parallel blocks (objects)");
			}
		}
		return steps;
	}

	private static ParallelStep parallelStepFromJson(Map<?, ?> block) {
		List<NamedStream> streams = new ArrayList<>();
		for (Map.Entry<?, ?> stream : block.entrySet()) {
			String name = String.valueOf(stream.getKey());
			streams.add(new NamedStream(name, stepsFromJson(stream.getValue(), "the parallel stream \"" + name + "\"")));
		}
		return new ParallelStep(streams);
	}

	private static String describe(Object json) {
		if (json == null) {
			return "null";
		} else if (json instanceof String string) {
			return "the string \"" + string + "\"";
		} else if (json instanceof Map) {
			return "an object";
		} else if (json instanceof List) {
			return "an array";
		} else {
			return "the value " + json;
		}
	}

	/**
	 * the JSON structure of this process definition (see
	 * {@link #fromJson(Object)})
	 */
	@JsonValue
	public List<Object> toJson() {
		return toJson(this.steps);
	}

	private static List<Object> toJson(List<Step> steps) {
		List<Object> json = new ArrayList<>();
		for (Step step : steps) {
			if (step instanceof ComponentStep component) {
				json.add(component.componentName());
			} else { // the only other kind of step
				ParallelStep parallel = (ParallelStep) step;
				Map<String, Object> block = new LinkedHashMap<>();
				for (NamedStream stream : parallel.streams()) {
					block.put(stream.name(), toJson(stream.steps()));
				}
				json.add(block);
			}
		}
		return json;
	}

	/**
	 * @return true if no component is executed
	 */
	public boolean isEmpty() {
		return this.steps.isEmpty();
	}

	/**
	 * @return true if the process definition contains no parallel block, i.e.,
	 *         all components are executed one after another
	 */
	public boolean isSequential() {
		return this.steps.stream().allMatch(ComponentStep.class::isInstance);
	}

	/**
	 * @return the names of all components in the order they are listed (with
	 *         repetitions), also those of the parallel streams
	 */
	public List<String> componentNames() {
		List<String> names = new ArrayList<>();
		collectComponentNames(this.steps, names);
		return names;
	}

	private static void collectComponentNames(List<Step> steps, List<String> names) {
		for (Step step : steps) {
			if (step instanceof ComponentStep component) {
				names.add(component.componentName());
			} else { // the only other kind of step
				ParallelStep parallel = (ParallelStep) step;
				for (NamedStream stream : parallel.streams()) {
					collectComponentNames(stream.steps(), names);
				}
			}
		}
	}

	/**
	 * e.g., {@code [a, {p1=[b], x=[c, d]}, e]}; a sequential process definition is
	 * shown like a list of component names
	 */
	@Override
	public String toString() {
		return toJson().toString();
	}
}
