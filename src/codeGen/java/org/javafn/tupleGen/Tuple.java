package org.javafn.tupleGen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.MethodSpec.Builder;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import org.javafn.tupleGen.TupleEntry.ObjectType;
import org.javafn.tupleGen.Util.Generic;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.function.Predicate.not;
import static org.javafn.tupleGen.Util.ITERATORS;
import static org.javafn.tupleGen.Util.STREAMERS;

public record Tuple(
		TupleType type,
		ClassName name,
		TypeName nameWithGenerics,
		List<TupleEntry> entries,
		List<TypeVariableName> entryTypes,
		List<ParameterSpec> entryFields,
		TypeVariableName[] genericArgs
) {
	enum TupleType {
		Pair,
		Trio,
		Quad
		;
		public static TupleType get(final int i) {
			return switch (i) {
				case 2 -> Pair;
				case 3 -> Trio;
				case 4 -> Quad;
				default -> throw new IllegalArgumentException("Invalid element count " + i);
			};
		}
		public int numel() {
			return switch (this) {
				case Pair -> 2;
				case Trio -> 3;
				case Quad -> 4;
			};
		}
	}
	static final Map<String, String> NAME_OVERRIDES = Map.of(
			"ObjObjPair", "Pair",
			"ObjObjObjTrio", "Trio",
			"ObjObjObjObjQuad", "Quad",
			"IntObjPair", "Index");

	static List<TupleEntry> toEntries(final List<TypeName> types) {
		return IntStream.range(0, types.size())
				.mapToObj(i -> TupleEntry.of(types.get(i), i))
				.toList();
	}

	static String argList(final List<ParameterSpec> varFields) {
		return varFields.stream()
				.map(ParameterSpec::name)
				.collect(Collectors.joining(", "));
	}

	public static Tuple of(final List<TypeName> types) {
		final TupleType type = TupleType.get(types.size());
		final List<TupleEntry> entries = toEntries(types);
		final ClassName name;
		{
			String tmp = entries.stream()
					.map(TupleEntry::typeName)
					.collect(Collectors.joining()) + type;
			tmp = NAME_OVERRIDES.getOrDefault(tmp, tmp);
			name = ClassName.get(GenerateTuples.PACKAGE_NAME, tmp);
		}
		final TypeVariableName[] genericArgs = entries.stream()
				.filter(t -> t instanceof ObjectType)
				.map(TupleEntry::varTypeName)
				.toArray(TypeVariableName[]::new);
		return new Tuple(type, name,
				genericArgs.length == 0 ? name : ParameterizedTypeName.get(name, genericArgs),
				entries,
				entries.stream().map(TupleEntry::varTypeName).toList(),
				entries.stream().map(TupleEntry::paramSpec).toList(),
				genericArgs);
	}

	public boolean isFullyPrimitive() {
		return genericArgs.length == 0;
	}

	public boolean isFullyNonPrimitive() {
		return entries.size() == genericArgs.length;
	}

	public IntStream idx() {
		return IntStream.range(0, entries.size());
	}

	public MethodSpec genConstructor() {
		return MethodSpec.constructorBuilder()
				.addParameters(entryFields)
				.build();
	}

	public MethodSpec genStaticFactory() {
		return MethodSpec.methodBuilder("of")
				.returns(nameWithGenerics)
				.addTypeVariables(Arrays.asList(genericArgs))
				.addParameters(entryFields)
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addStatement("return new $L$L($L)",
						name.simpleName(),
						isFullyPrimitive() ? "" : "<>",
						argList(entryFields))
				.build();
	}

	public List<MethodSpec> genPredicates(final FunctionalInterfaces fi) {
		final List<MethodSpec> ms = new ArrayList<>(entries.size() + 1);
		// Full element list matches
		ms.add(MethodSpec.methodBuilder("matches")
				.returns(ClassName.BOOLEAN)
				.addParameter(ParameterSpec.builder(
						fi.parameterizedPredicate(genericArgs), "fn", Modifier.FINAL)
						.build())
				.addModifiers(Modifier.PUBLIC)
				.addStatement("$T.requireNonNull(fn)", Objects.class)
				.addStatement("return fn.test($L)", argList(entryFields))
				.build());
		// Single argument matches
		idx().forEach(i -> ms.add(entries.get(i).genPredicate(entryFields.get(i))));
		return ms;
	}

	public List<MethodSpec> genConsumers(final FunctionalInterfaces fi) {

		final List<MethodSpec> ms = new ArrayList<>(entries.size() + 2);
		// Full peek
		ms.add(MethodSpec.methodBuilder("peek")
				.returns(nameWithGenerics)
				.addParameter(ParameterSpec.builder(
								fi.parameterizedConsumer(genericArgs),
								"fn",
								Modifier.FINAL)
						.build())
				.addModifiers(Modifier.PUBLIC)
				.addStatement("$T.requireNonNull(fn)", Objects.class)
				.addStatement("fn.accept($L)", argList(entryFields))
				.addStatement("return this")
				.build());
		// Full consume is almost the same, so let's include it here
		ms.add(MethodSpec.methodBuilder("consume")
				.addParameter(ParameterSpec.builder(
								fi.parameterizedConsumer(genericArgs),
								"fn",
								Modifier.FINAL)
						.build())
				.addModifiers(Modifier.PUBLIC)
				.addStatement("$T.requireNonNull(fn)", Objects.class)
				.addStatement("fn.accept($L)", argList(entryFields))
				.build());
		// Single arg peek
		idx().forEach(i -> ms.add(entries.get(i).genConsumer(this, entryFields.get(i))));
		return ms;
	}

	public List<MethodSpec> genMappers(final FunctionalInterfaces fi) {
		final List<MethodSpec> ms = new ArrayList<>(entries.size() + 1);
		// Full map
		ms.add(MethodSpec.methodBuilder("map")
				.addTypeVariable(Generic.R.varTypeName())
				.returns(Generic.R.varTypeName())
				.addParameter(ParameterSpec.builder(
								fi.parameterizedMapper(genericArgs, Generic.R.varTypeName()),
								"fn",
								Modifier.FINAL)
						.build())
				.addModifiers(Modifier.PUBLIC)
				.addStatement("$T.requireNonNull(fn)", Objects.class)
				.addStatement("return fn.apply($L)", argList(entryFields))
				.build());
		// Single element map, full args and single args
		idx().forEach(i -> {
			final TupleEntry entry = entries.get(i);

			final TypeName returnType;
			if (nameWithGenerics instanceof ParameterizedTypeName pType) {
				final TypeVariableName[] genericArgs = idx()
						.filter(j -> entries.get(j).isObj())
						.mapToObj(j -> {
							if (i == j) return Generic.R.varTypeName();
							else return entryTypes.get(j);
						})
						.toArray(TypeVariableName[]::new);
				returnType = ParameterizedTypeName.get(pType.rawType(), genericArgs);
			} else {
				returnType = nameWithGenerics;
			}
			// Full arg list
			final ParameterSpec fullMapperArg;
			{
				final TypeName mapperFunctionalType;
				if (isFullyPrimitive()) {
					mapperFunctionalType = fi.primitiveTypeOperators().get(entry.type());
				} else {
					if (entry.isObj()) {
						mapperFunctionalType = fi.parameterizedMapper(genericArgs, Generic.R.varTypeName());
					} else {
						mapperFunctionalType = ParameterizedTypeName.get(
								fi.primitiveTypeOperators().get(entry.type()),
								genericArgs);
					}
				}
				fullMapperArg = ParameterSpec
						.builder(mapperFunctionalType, "fn", Modifier.FINAL)
						.build();
			}
			ms.add(entry.genFullArgListMapper(this, returnType, fullMapperArg));

			// Single arg
			ms.add(entry.genSingleArgMapper(this, returnType));

            if (type == TupleType.Pair) {
				GenerateTuples.SUPPORTED_TYPES.keySet().stream()
						.filter(not(t -> entry.type().equals(t)))
						.forEach(toType -> ms.add(entry.genSingleArgMapperTo(this, toType)));
            }
		});
		return ms;
	}

	public MethodSpec genZipper() {

		final List<TypeVariableName> genericTypeArgs = entries.stream()
				.filter(TupleEntry::isObj)
				.mapToInt(TupleEntry::index)
				.mapToObj(Generic::arbitrary)
				.map(Generic::varTypeName).toList();
		final TypeName nameWithGenerics = genericTypeArgs.isEmpty()
				? name
				: ParameterizedTypeName.get(name, genericTypeArgs.toArray(TypeVariableName[]::new));

		final List<ParameterSpec> params = new ArrayList<>(type.numel());
		for (final TupleEntry e : entries) {
			final ClassName streamer = STREAMERS.get(e.type());
			final Generic g = Generic.arbitrary(e.index());
			final TypeName argType;
			if (e.isObj()) {
				argType = ParameterizedTypeName.get(streamer, g.varTypeName());
			} else {
				argType = streamer;
			}
			params.add(ParameterSpec.builder(argType, "stream" + g.name(), Modifier.FINAL).build());
		}
		
		final TypeName returns = ParameterizedTypeName.get(
				ClassName.get(Stream.class),
				nameWithGenerics);

		final MethodSpec.Builder builder = MethodSpec.methodBuilder("zip")
				.addTypeVariables(genericTypeArgs)
				.returns(returns)
				.addParameters(params)
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
		for (final TupleEntry e : entries) {
			final TypeName iterClass;
			final Generic g = Generic.arbitrary(e.index());
			if (e.isObj()) {
				iterClass = ParameterizedTypeName.get(ClassName.get(Iterator.class), g.varTypeName());
			} else {
				iterClass = ITERATORS.get(e.type());
			}
			builder.addStatement("final $T iter$L = $T.requireNonNull($L).iterator()",
					iterClass,
					g.name(),
					Objects.class,
					"stream" + g.name());
		}

		final TypeSpec wrappedIter = TypeSpec.anonymousClassBuilder("")
				.addSuperinterface(ParameterizedTypeName.get(ClassName.get(Iterator.class), nameWithGenerics))
				.addMethod(MethodSpec.methodBuilder("hasNext")
						.returns(TypeName.BOOLEAN)
						.addAnnotation(Override.class)
						.addModifiers(Modifier.PUBLIC)
						.addStatement("return $L",
								entries.stream()
										.map(e -> "iter" + Generic.arbitrary(e.index()).name() + ".hasNext()")
										.collect(Collectors.joining(" && ")))

						.build())
				.addMethod(MethodSpec.methodBuilder("next")
						.returns(nameWithGenerics)
						.addAnnotation(Override.class)
						.addModifiers(Modifier.PUBLIC)
						.addStatement("return $T.of($L)",
								name(),
								entries.stream()
										.map(e -> "iter" + Generic.arbitrary(e.index()).name() + ".next()")
										.collect(Collectors.joining(", ")))
						.build())
				.build();

		builder.addStatement("return $T.stream($T.spliteratorUnknownSize($L, $T.ORDERED), false)",
				StreamSupport.class,
				Spliterators.class,
				wrappedIter,
				Spliterator.class);
		return builder.build();
	}

}
