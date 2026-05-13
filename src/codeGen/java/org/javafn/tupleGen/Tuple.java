package org.javafn.tupleGen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import org.javafn.tupleGen.TupleEntry.ObjectType;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.javafn.tupleGen.Util.MAP_RETURN_TYPE;

public record Tuple(
		ClassName name,
		TypeName nameWithGenerics,
		List<TupleEntry> entries,
		List<TypeVariableName> entryTypes,
		List<ParameterSpec> entryFields,
		TypeVariableName[] genericArgs
) {
	static final Map<String, String> NAME_OVERRIDES = Map.of(
			"ObjObjTuple", "PairType",
			"ObjObjObjTuple", "TrioType",
			"ObjObjObjObjTuple", "QuadType",
			"IntObjTuple", "Index");

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
		final List<TupleEntry> entries = toEntries(types);
		final String name = entries.stream()
				.map(TupleEntry::typeName)
				.collect(Collectors.joining()) + "Tuple";
		return of(entries, NAME_OVERRIDES.getOrDefault(name, name));
	}

	public static Tuple of(final List<TupleEntry> entries, final String _name) {
		final ClassName name = ClassName.get(GenerateTuples.PACKAGE_NAME, _name);
		final TypeVariableName[] genericArgs = entries.stream()
				.filter(t -> t instanceof ObjectType)
				.map(TupleEntry::varTypeName)
				.toArray(TypeVariableName[]::new);
		return new Tuple(name,
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
		idx().forEach(i -> {
			final TupleEntry entry = entries.get(i);
			final ParameterSpec field = entryFields.get(i);
			ms.add(MethodSpec.methodBuilder("matches" + (i + 1))
					.returns(ClassName.BOOLEAN)
					.addParameter(ParameterSpec.builder(
									entry.predicate(),
									"fn",
									Modifier.FINAL)
							.build())
					.addModifiers(Modifier.PUBLIC)
					.addStatement("$T.requireNonNull(fn)", Objects.class)
					.addStatement("return fn.test($L)", field.name())
					.build());
		});
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
		idx().forEach(i -> {
			final TupleEntry entry = entries.get(i);
			final ParameterSpec field = entryFields.get(i);
			ms.add(MethodSpec.methodBuilder("peek" + (i+1))
					.returns(nameWithGenerics)
					.addParameter(ParameterSpec.builder(entry.consumer(),
									"fn",
									Modifier.FINAL)
							.build())
					.addModifiers(Modifier.PUBLIC)
					.addStatement("$T.requireNonNull(fn)", Objects.class)
					.addStatement("fn.accept($L)", field.name())
					.addStatement("return this")
					.build());
		});
		return ms;
	}

	public List<MethodSpec> genMappers(final FunctionalInterfaces fi) {
		final List<MethodSpec> ms = new ArrayList<>(entries.size() + 1);
		// Full map
		ms.add(MethodSpec.methodBuilder("map")
				.addTypeVariable(MAP_RETURN_TYPE)
				.returns(MAP_RETURN_TYPE)
				.addParameter(ParameterSpec.builder(
								fi.parameterizedMapper(genericArgs, MAP_RETURN_TYPE),
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
							if (i == j) return MAP_RETURN_TYPE;
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
						mapperFunctionalType = fi.parameterizedMapper(genericArgs, MAP_RETURN_TYPE);
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

//            if (type.isPrimitive()) {
//                // ToObj Mapper
//                tupleClassBuilder.addMethod(
//                        singleArgMapperFor(index, tuple, type, typeName,
//                                MethodSpec.methodBuilder("map" + (index + 1) + "ToObj")
//                                        .addModifiers(Modifier.PUBLIC)
//                                        .returns(returnType)));
//            }
		});
		return ms;
	}
}
