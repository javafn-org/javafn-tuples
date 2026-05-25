package org.javafn.tupleGen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import org.javafn.tupleGen.TupleEntry.ObjectType;
import org.javafn.tupleGen.Util.Generic;
import org.javafn.utils.Data;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.function.Predicate.not;
import static org.javafn.tupleGen.Util.ITERATORS;
import static org.javafn.tupleGen.Util.STREAMERS;
import static org.javafn.tupleGen.Util.getQualifier;

public record Tuple(
		TupleType type,
		ClassName name,
		TypeName nameWithGenerics,
		List<TupleEntry> entries,
		List<TypeVariableName> entryTypes,
		List<ParameterSpec> entryFields,
		TypeVariableName[] genericArgs,
		boolean typesAreHomogeneous
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
			"ObjPair", "Pair",
			"ObjTrio", "Trio",
			"ObjQuad", "Quad",
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
		final boolean typesAreHomogeneous;
		{
			final String nameTmp;
			final String firstType = entries.get(0).typeName();
			if (entries.stream().map(TupleEntry::typeName).allMatch(firstType::equals)) {
				typesAreHomogeneous = true;
				nameTmp = entries.get(0).typeName() + type;
			} else {
				typesAreHomogeneous = false;
				nameTmp = entries.stream()
						.map(TupleEntry::typeName)
						.collect(Collectors.joining()) + type;
			}

			name = ClassName.get(GenerateTuples.PACKAGE_NAME, NAME_OVERRIDES.getOrDefault(nameTmp, nameTmp));
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
				genericArgs, typesAreHomogeneous);
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

	public List<MethodSpec> genStaticPredicates(final FunctionalInterfaces fi) {
		final List<MethodSpec> ms = new ArrayList<>(entries.size() + 1);
		// Full element list matches
		ms.add(MethodSpec.methodBuilder("matches")
				.addTypeVariables(Arrays.asList(genericArgs))
				.returns(ParameterizedTypeName.get(ClassName.get(Predicate.class), nameWithGenerics))
				.addParameter(ParameterSpec.builder(
								fi.parameterizedPredicate(genericArgs), "fn", Modifier.FINAL)
						.build())
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addStatement("return tuple -> tuple.matches(fn)")
				.build());
		// Single argument matches
		idx().forEach(i -> ms.add(entries.get(i).genStaticPredicate(this)));
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

	public List<MethodSpec> genStaticConsumers(final FunctionalInterfaces fi) {
		final List<MethodSpec> ms = new ArrayList<>(entries.size() + 1);
		// Full element list peekers
		ms.add(MethodSpec.methodBuilder("peek")
				.addTypeVariables(Arrays.asList(genericArgs))
				.returns(ParameterizedTypeName.get(ClassName.get(Consumer.class), nameWithGenerics))
				.addParameter(ParameterSpec.builder(
								fi.parameterizedConsumer(genericArgs), "fn", Modifier.FINAL)
						.build())
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addStatement("return tuple -> tuple.peek(fn)")
				.build());
		ms.add(MethodSpec.methodBuilder("consume" + getQualifier(this))
				.addTypeVariables(Arrays.asList(genericArgs))
				.returns(ParameterizedTypeName.get(ClassName.get(Consumer.class), nameWithGenerics))
				.addParameter(ParameterSpec.builder(
								fi.parameterizedConsumer(genericArgs),
								"fn",
								Modifier.FINAL)
						.build())
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addStatement("return tuple -> tuple.consume(fn)")
				.build());
		// Single argument peekers
		idx().forEach(i -> ms.add(entries.get(i).genStaticConsumer(this)));
		return ms;
	}
	public List<MethodSpec> genStaticMappers(final FunctionalInterfaces fi) {
		final List<MethodSpec> ms = new ArrayList<>();
		// Full element list mappers
		ms.add(MethodSpec.methodBuilder("map" + getQualifier(this))
				.addTypeVariables(Data.append(Arrays.asList(genericArgs), Generic.R.varTypeName()))
				.returns(ParameterizedTypeName.get(
						ClassName.get(Function.class),
						nameWithGenerics,
						Generic.R.varTypeName()))
				.addParameter(ParameterSpec.builder(
						fi.parameterizedMapper(genericArgs, Generic.R.varTypeName()),
						"fn", Modifier.FINAL)
						.build())
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addStatement("return tuple -> tuple.map(fn)")
				.build());
		// Single argument mappers
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
			ms.add(entry.genStaticFullArgMapper(this, returnType, fullMapperArg));

			// Single arg list
			ms.add(entry.genStaticSingleArgMapper(this, returnType));

			if (i < 2) {
				GenerateTuples.SUPPORTED_TYPES.keySet().stream()
						.filter(not(t -> entry.type().equals(t)))
						.forEach(toType -> ms.add(entry.genStaticSingleArgMapperTo(this, toType)));
			}
		});
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

            if (i < 2) {
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

	public List<MethodSpec> genChunks() {
        if (!typesAreHomogeneous) {
            return List.of();
        }
		final TypeName types = entries.get(0).type();
		final List<MethodSpec> chunkMethods = new ArrayList<>(2);
		{
			final MethodSpec.Builder builder = MethodSpec.methodBuilder("chunks")
					.addModifiers(Modifier.STATIC, Modifier.PUBLIC);
			if (ClassName.OBJECT.equals(types)) {
				builder.addTypeVariable(Generic.A.varTypeName())
						.returns(ParameterizedTypeName.get(ClassName.get(Stream.class),
								ParameterizedTypeName.get(name,
										idx().mapToObj(i -> Generic.A.varTypeName()).toArray(TypeVariableName[]::new))))
						.addParameter(TypeVariableName.get(Generic.A.name() + "[]"), "a", Modifier.FINAL)
						.addParameter(Generic.A.varTypeName(), "pad", Modifier.FINAL);
			} else {
				builder.returns(ParameterizedTypeName.get(ClassName.get(Stream.class), name))
						.addParameter(TypeVariableName.get(types + "[]"), "a", Modifier.FINAL)
						.addParameter(types, "pad", Modifier.FINAL);
			}

			final int numel = type.numel();

			builder.addStatement("final int len = a.length")
					.addStatement("final int nChunks = len / $L", numel)
					.addStatement("final int nChunked = nChunks * $L", numel)
					.addStatement("final int rem = len % $L", numel)
					.addStatement("""
							return Stream.concat(
									IntStream.range(0, nChunks).map(i -> i * $L).mapToObj(i -> $T.of($L)),
									rem == 0 ? Stream.empty() : Stream.of($T.of($L)))
							""", numel, name,
					idx().mapToObj(i -> i == 0 ? "a[i]" : "a[i + %d]".formatted(i)).collect(Collectors.joining(", ")),
					name,
					idx().mapToObj(i -> {
						if (i == 0) {
							return "a[nChunked]";
						} else if (numel == i + 1) {
							return "pad";
						} else {
							return "rem == %d ? a[nChunked + %d] : pad".formatted(i + 1, i);
						}
					}).collect(Collectors.joining(", ")));

			chunkMethods.add(builder.build());
		}
		{
			final MethodSpec.Builder builder = MethodSpec.methodBuilder("chunks")
					.addModifiers(Modifier.STATIC, Modifier.PUBLIC);
			if (ClassName.OBJECT.equals(types)) {
				builder.addTypeVariable(Generic.A.varTypeName())
						.returns(ParameterizedTypeName.get(ClassName.get(Stream.class),
								ParameterizedTypeName.get(name,
										idx().mapToObj(i -> Generic.A.varTypeName()).toArray(TypeVariableName[]::new))))
						.addParameter(TypeVariableName.get(Generic.A.name() + "[]"), "a", Modifier.FINAL);
			} else {
				builder.returns(ParameterizedTypeName.get(ClassName.get(Stream.class), name))
						.addParameter(TypeVariableName.get(types + "[]"), "a", Modifier.FINAL);
			}

			final int numel = type.numel();

			builder.addStatement("final int len = a.length")
					.beginControlFlow("if (len % $L != 0)", numel)
					.addStatement("throw new IllegalArgumentException($S)",
							"The number of elements must be divisible by %d; use the pad argument if you want the final item to contain padding".formatted(numel))
					.endControlFlow()
					.addStatement("return IntStream.range(0, len / $L).map(i -> i * $L).mapToObj(i -> $T.of($L))",
							numel, numel, name,
							idx().mapToObj(i -> i == 0 ? "a[i]" : "a[i + %d]".formatted(i)).collect(Collectors.joining(", ")));

			chunkMethods.add(builder.build());
		}

		return chunkMethods;
    }

	public Optional<MethodSpec> genWindows() {
		if (!typesAreHomogeneous) {
			return Optional.empty();
		}

		final TypeName types = entries.get(0).type();
		final MethodSpec.Builder builder = MethodSpec.methodBuilder("windows")
				.addModifiers(Modifier.STATIC, Modifier.PUBLIC);
		if (ClassName.OBJECT.equals(types)) {
			builder.addTypeVariable(Generic.A.varTypeName())
					.returns(ParameterizedTypeName.get(ClassName.get(Stream.class),
							ParameterizedTypeName.get(name,
									idx().mapToObj(i -> Generic.A.varTypeName()).toArray(TypeVariableName[]::new))))
					.addParameter(TypeVariableName.get(Generic.A.name() + "[]"), "a", Modifier.FINAL);
		} else {
			builder.returns(ParameterizedTypeName.get(ClassName.get(Stream.class), name))
					.addParameter(TypeVariableName.get(types + "[]"), "a", Modifier.FINAL);
		}

		final int numel = type.numel();

		builder.addStatement("return IntStream.range(0, a.length - $L).mapToObj(i -> $T.of($L))",
				numel - 1, name, idx()
						.mapToObj(i -> i == 0 ? "a[i]" : "a[i + %d]".formatted(i))
						.collect(Collectors.joining(", ")));

		return Optional.of(builder.build());
	}

	public Optional<MethodSpec> genPartition() {
		if (type.numel() > 2 || !typesAreHomogeneous) {
			return Optional.empty();
		}

		final TypeName types = entries.get(0).type();
		final ClassName streamer = STREAMERS.get(types);

		final MethodSpec.Builder builder = MethodSpec.methodBuilder("partition")
				.addModifiers(Modifier.STATIC, Modifier.PUBLIC);
		final String builderGenericArgs;
		if (ClassName.OBJECT.equals(types)) {
			builder.addTypeVariable(Generic.A.varTypeName())
					.returns(
							ParameterizedTypeName.get(ClassName.get(GenerateTuples.PACKAGE_NAME, "Pair"),
								ParameterizedTypeName.get(ClassName.get(Stream.class), Generic.A.varTypeName()),
								ParameterizedTypeName.get(ClassName.get(Stream.class), Generic.A.varTypeName())))
					.addParameter(ParameterizedTypeName.get(streamer, Generic.A.varTypeName()),
							"stream", Modifier.FINAL)
					.addParameter(ParameterizedTypeName.get(ClassName.get(Predicate.class), Generic.A.varTypeName()),
							"fn", Modifier.FINAL);
			builderGenericArgs = "<A>";
		} else {
			builder.returns(
					ParameterizedTypeName.get(ClassName.get(GenerateTuples.PACKAGE_NAME, "Pair"),
							streamer, streamer))
					.addParameter(streamer, "stream", Modifier.FINAL)
					.addParameter(entries.get(0).predicateName(), "fn", Modifier.FINAL);
			builderGenericArgs = "";
		}

		builder.addStatement("final $T.Builder$L trueStream = $T.builder()", streamer, builderGenericArgs, streamer)
				.addStatement("final $T.Builder$L falseStream = $T.builder()", streamer, builderGenericArgs, streamer)
				.addStatement("""
						stream.forEach(a -> {
							if (fn.test(a)) trueStream.add(a);
							else falseStream.add(a);
						})
						""")
				.addStatement("return Pair.of(trueStream.build(), falseStream.build())");
		return Optional.of(builder.build());
	}

	public Optional<MethodSpec> genStream() {
		if (!typesAreHomogeneous) {
			return Optional.empty();
		}

		final TypeName types = entries.get(0).type();
		final ClassName streamer = STREAMERS.get(types);

		final MethodSpec.Builder builder = MethodSpec.methodBuilder("stream")
				.addModifiers(Modifier.STATIC, Modifier.PUBLIC);
		if (ClassName.OBJECT.equals(types)) {
			builder.addTypeVariable(Generic.A.varTypeName())
					.returns(ParameterizedTypeName.get(streamer, Generic.A.varTypeName()))
					.addParameter(ParameterSpec.builder(
							ParameterizedTypeName.get(name, idx().mapToObj(i -> Generic.A.varTypeName()).toArray(TypeVariableName[]::new)),
							"tuple", Modifier.FINAL).build());
		} else {
			builder.returns(streamer)
					.addParameter(name, "tuple", Modifier.FINAL);
		}

		builder.addStatement("return $T.of($L)", streamer,
				idx().mapToObj(i -> "tuple.v%d()".formatted(i + 1)).collect(Collectors.joining(", ")));
		return Optional.of(builder.build());
	}
}
