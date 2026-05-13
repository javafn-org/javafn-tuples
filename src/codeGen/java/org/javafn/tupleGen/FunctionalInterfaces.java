package org.javafn.tupleGen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public record FunctionalInterfaces(
		ClassName fullPredicate,
		ClassName fullConsumer,
		ClassName fullMapper,
		Map<TypeName, ClassName> primitiveTypeOperators,
		List<TypeSpec> newTypes
) {

	public static FunctionalInterfaces of(final Tuple tuple) {
		final List<TupleEntry> entries = tuple.entries();
		final int nTypes = entries.size();
		if (nTypes < 2 || nTypes > 4) {
			throw new IllegalStateException("Only tuples of length 2, 3, or 4 are supported");
		}
		if (nTypes == 2 && !tuple.hasPrimitive()) {
			// This is a pair; let's reuse the Bi* interfaces
			return new FunctionalInterfaces(
					ClassName.get(BiPredicate.class), ClassName.get(BiConsumer.class),
					ClassName.get(BiFunction.class), Map.of(), List.of());
		}
		final List<TypeSpec> newTypes = new ArrayList<>();

		final ClassName predicate = Util.genNestedClassName(tuple.name().simpleName(), "Predicate");
		final ClassName consumer = Util.genNestedClassName(tuple.name().simpleName(), "Consumer");
		final ClassName mapper = Util.genNestedClassName(tuple.name().simpleName(), "Function");

		newTypes.add(TypeSpec.interfaceBuilder(predicate)
				.addAnnotation(FunctionalInterface.class)
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addTypeVariables(Arrays.asList(tuple.genericArgs()))
				.addMethod(MethodSpec.methodBuilder("test")
						.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
						.returns(TypeName.BOOLEAN)
						.addParameters(tuple.entryFields())
						.build())
				.build());
		newTypes.add(TypeSpec.interfaceBuilder(consumer)
				.addAnnotation(FunctionalInterface.class)
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addTypeVariables(Arrays.asList(tuple.genericArgs()))
				.addMethod(MethodSpec.methodBuilder("accept")
						.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
						.addParameters(tuple.entryFields())
						.build())
				.build());

		final List<TypeVariableName> mapperGenericArgs = new ArrayList<>(tuple.genericArgs().length + 1);
		mapperGenericArgs.addAll(List.of(tuple.genericArgs()));
		mapperGenericArgs.add(Util.MAP_RETURN_TYPE);
		newTypes.add(TypeSpec.interfaceBuilder(mapper)
				.addAnnotation(FunctionalInterface.class)
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addTypeVariables(mapperGenericArgs)
				.addMethod(MethodSpec.methodBuilder("apply")
						.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
						.addParameters(tuple.entryFields())
						.returns(Util.MAP_RETURN_TYPE)
						.build())
				.build());

		final Map<TypeName, ClassName> operators = new HashMap<>();
		for (final TupleEntry entry : entries) {
			if (!(entry.isObj() || operators.containsKey(entry.type()))) {
				final ClassName operator = Util.genNestedClassName(tuple.name().simpleName(),
						"To" + Util.capitalize(entry.typeName()) + "Operator");
				newTypes.add(TypeSpec.interfaceBuilder(operator)
						.addAnnotation(FunctionalInterface.class)
						.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
						.addTypeVariables(Arrays.asList(tuple.genericArgs()))
						.addMethod(MethodSpec.methodBuilder("apply")
								.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
								.addParameters(tuple.entryFields())
								.returns(entry.varTypeName())
								.build())
						.build());
				operators.put(entry.type(), operator);
			}
		}

		return new FunctionalInterfaces(predicate, consumer, mapper, operators, newTypes);
	}
	public TypeName parameterizedPredicate(final TypeVariableName[] genericArgs) {
		if (genericArgs.length == 0) {
			return fullPredicate;
		} else {
			return ParameterizedTypeName.get(fullPredicate, genericArgs);
		}
	}

	public TypeName parameterizedConsumer(final TypeVariableName[] genericArgs) {
		if (genericArgs.length == 0) {
			return fullConsumer;
		} else {
			return ParameterizedTypeName.get(fullConsumer, genericArgs);
		}
	}

	public TypeName parameterizedMapper(final TypeVariableName[] genericArgs) {
		if (genericArgs.length == 0) {
			return fullMapper;
		} else {
			return ParameterizedTypeName.get(fullMapper, genericArgs);
		}
	}

	public TypeName parameterizedMapper(final TypeVariableName[] genericArgs, final TypeVariableName retType) {
		final int nArgs = genericArgs.length;
		final TypeName[] args = new TypeName[nArgs + 1];
		System.arraycopy(genericArgs, 0, args, 0, nArgs);
		args[nArgs] = retType;
		return ParameterizedTypeName.get(fullMapper, args);
	}
}
