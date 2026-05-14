package org.javafn.tupleGen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import org.javafn.tupleGen.Util.Generic;
import org.javafn.tupleGen.Util.MapperType;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.javafn.tupleGen.GenerateTuples.SUPPORTED_TYPES;
import static org.javafn.tupleGen.Util.DOUBLE_ARG_FIELD;
import static org.javafn.tupleGen.Util.INT_ARG_FIELD;
import static org.javafn.tupleGen.Util.LONG_ARG_FIELD;
import static org.javafn.tupleGen.Util.TYPE_MAPPERS;

public interface TupleEntry {
	int index();
	String typeName();
	TypeName type();
	default boolean isObj() { return false; }
	TypeVariableName varTypeName();
	ParameterSpec paramSpec(Modifier... modifiers);
	TypeName predicateName();
	TypeName consumerName();
	TypeName mapper(ClassName toType);
	MethodSpec genSetter(Tuple tuple);
	default MethodSpec genPredicate(final ParameterSpec field) {
		return MethodSpec.methodBuilder("matches" + (index() + 1))
				.returns(ClassName.BOOLEAN)
				.addParameter(ParameterSpec.builder(
								predicateName(),
								"fn",
								Modifier.FINAL)
						.build())
				.addModifiers(Modifier.PUBLIC)
				.addStatement("$T.requireNonNull(fn)", Objects.class)
				.addStatement("return fn.test($L)", field.name())
				.build();
	}
	default MethodSpec genConsumer(Tuple tuple, ParameterSpec field) {
		return MethodSpec.methodBuilder("peek" + (index() + 1))
				.returns(tuple.nameWithGenerics())
				.addParameter(ParameterSpec.builder(consumerName(),
								"fn",
								Modifier.FINAL)
						.build())
				.addModifiers(Modifier.PUBLIC)
				.addStatement("$T.requireNonNull(fn)", Objects.class)
				.addStatement("fn.accept($L)", field.name())
				.addStatement("return this")
				.build();
	}
	default MethodSpec genFullArgListMapper(Tuple tuple, TypeName returnType, ParameterSpec fullArgList) {
		return MethodSpec.methodBuilder("map" + (index() + 1))
				.addTypeVariable(Generic.R.varTypeName())
				.returns(returnType)
				.addParameter(fullArgList)
				.addModifiers(Modifier.PUBLIC)
				.addStatement("$T.requireNonNull(fn)", Objects.class)
				.addStatement("return $T.of($L)",
						tuple.name(),
						tuple.idx().mapToObj(j -> {
							final String varName = tuple.entryFields().get(j).name();
							if (index() == j) return "fn.apply(" + Tuple.argList(tuple.entryFields()) + ")";
							else return varName;
						}).collect(Collectors.joining(", ")))
				.build();
	}
	MethodSpec genSingleArgMapper(Tuple tuple, TypeName returnType);
	MethodSpec genSingleArgMapperTo(Tuple tuple, TypeName toType);

	static TupleEntry of(final TypeName type, final int i) {
		if (type.equals(ClassName.OBJECT)) {
			return new ObjectType(i, Generic.v(i));
		} else if (type.equals(TypeName.INT)) {
			return new IntType(i);
		} else if (type.equals(TypeName.LONG)) {
			return new LongType(i);
		} else if (type.equals(TypeName.DOUBLE)) {
			return new DoubleType(i);
		} else {
			throw new IllegalArgumentException("Unexpected type " + type);
		}
	}

	record ObjectType(int index, Generic generic) implements TupleEntry {

		@Override public String typeName() { return "Obj"; }
		@Override public TypeName type() {
			return ClassName.OBJECT;
		}
		@Override public TypeVariableName varTypeName() { return generic.varTypeName(); }
		@Override public boolean isObj() { return true; }
		@Override public ParameterSpec paramSpec(final Modifier... modifiers) {
			return ParameterSpec.builder(generic.varTypeName(), generic.fieldName(), modifiers).build();
		}
		@Override public TypeName predicateName() {
			return ParameterizedTypeName.get(ClassName.get(Predicate.class), generic.varTypeName());
		}
		@Override public TypeName consumerName() {
			return ParameterizedTypeName.get(ClassName.get(Consumer.class), generic.varTypeName());
		}
		@Override public TypeName mapper(final ClassName toType) {
			final var mapper = TYPE_MAPPERS.get(ClassName.OBJECT).get(toType);
			if (mapper == null) throw new IllegalArgumentException("Unexpected type " + toType);
			if (toType.isPrimitive()) {
				return mapper.fn();
			} else {
				return ParameterizedTypeName.get(mapper.fn(), generic.varTypeName(), toType);
			}
		}
		@Override public MethodSpec genSetter(final Tuple tuple) {
			final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("v" + (index + 1));
			final List<ParameterSpec> argNames = new ArrayList<>(tuple.entryFields());
			argNames.set(index, Generic.R.parameter());
			final TypeVariableName[] genericTypes = tuple.idx()
					.filter(j -> tuple.entries().get(j).isObj())
					.mapToObj(j -> {
						if (index == j) return Generic.R.varTypeName();
						else return tuple.entryTypes().get(j);
					})
					.toArray(TypeVariableName[]::new);
			methodBuilder
					.addTypeVariable(Generic.R.varTypeName())
					.returns(ParameterizedTypeName.get(tuple.name(), genericTypes))
					.addParameter(Generic.R.parameter(Modifier.FINAL))
					.addModifiers(Modifier.PUBLIC)
					.addStatement("return $T.of($L)", tuple.name(), Tuple.argList(argNames))
					.build();
			return methodBuilder.build();
		}
		@Override public MethodSpec genFullArgListMapper(final Tuple tuple, final TypeName returnType, final ParameterSpec fullArgList) {
			return MethodSpec.methodBuilder("map" + (index + 1))
					.addTypeVariable(Generic.R.varTypeName())
					.returns(returnType)
					.addParameter(fullArgList)
					.addModifiers(Modifier.PUBLIC)
					.addStatement("$T.requireNonNull(fn)", Objects.class)
					.addStatement("return $T.of($L)",
							tuple.name(),
							tuple.idx().mapToObj(j -> {
								final String varName = tuple.entryFields().get(j).name();
								if (index == j) return "fn.apply(" + Tuple.argList(tuple.entryFields()) + ")";
								else return varName;
							}).collect(Collectors.joining(", ")))
					.build();
		}
		@Override public MethodSpec genSingleArgMapper(final Tuple tuple, final TypeName returnType) {
			return MethodSpec.methodBuilder("map" + (index + 1))
					.addTypeVariable(Generic.R.varTypeName())
					.returns(returnType)
					.addParameter(ParameterSpec.builder(
							ParameterizedTypeName.get(
									ClassName.get(Function.class),
									varTypeName(),
									Generic.R.varTypeName()),
							"fn", Modifier.FINAL)
							.build())
					.addStatement("$T.requireNonNull(fn)", Objects.class)
					.addStatement("return $T.of($L)",
							tuple.name(),
							tuple.idx().mapToObj(j -> {
								final String varName = tuple.entryFields().get(j).name();
								if (index == j) return "fn.apply(" + varName + ")";
								else return varName;
							}).collect(Collectors.joining(", ")))
					.build();
		}
		@Override public MethodSpec genSingleArgMapperTo(final Tuple tuple, final TypeName toType) {
			return TupleEntry.genPrimitiveSingleArgMapper(tuple, index, ClassName.OBJECT, toType);
		}
	}

	record IntType(int index) implements TupleEntry {

		@Override public String typeName() { return "Int"; }
		@Override public TypeName type() { return TypeName.INT; }
		@Override public TypeVariableName varTypeName() { return Util.INT_TYPE; }
		@Override public ParameterSpec paramSpec(final Modifier... modifiers) {
			return ParameterSpec.builder(TypeName.INT, "v" + (index + 1), modifiers).build();
		}
		@Override public TypeName predicateName() { return TypeName.get(IntPredicate.class); }
		@Override public TypeName consumerName() { return TypeName.get(IntConsumer.class); }
		@Override public TypeName mapper(final ClassName toType) {
			final MapperType mapper = TYPE_MAPPERS.get(ClassName.INT).get(toType);
			if (mapper == null) throw new IllegalArgumentException("Unexpected type " + toType);
			if (toType.isPrimitive()) {
				return mapper.fn();
			} else {
				return ParameterizedTypeName.get(mapper.fn(), toType);
			}
		}
		@Override public MethodSpec genSetter(final Tuple tuple) {
			return genPrimitiveSetter(tuple, index, INT_ARG_FIELD);
		}
		@Override public MethodSpec genSingleArgMapper(final Tuple tuple, final TypeName returnType) {
			return TupleEntry.genPrimitiveMapper(tuple, index, IntUnaryOperator.class, returnType, "applyAsInt");
		}
		@Override public MethodSpec genSingleArgMapperTo(final Tuple tuple, final TypeName toType) {
			return TupleEntry.genPrimitiveSingleArgMapper(tuple, index, TypeName.INT, toType);
		}
	}

	record LongType(int index) implements TupleEntry {
		@Override public String typeName() { return "Long"; }
		@Override public TypeName type() { return TypeName.LONG; }
		@Override public TypeVariableName varTypeName() { return TypeVariableName.get(TypeName.LONG.toString()); }
		@Override public ParameterSpec paramSpec(final Modifier... modifiers) {
			return ParameterSpec.builder(TypeName.LONG, "v" + (index + 1), modifiers).build();
		}
		@Override public TypeName predicateName() { return TypeName.get(LongPredicate.class); }
		@Override public TypeName consumerName() { return TypeName.get(LongConsumer.class); }
		@Override public TypeName mapper(final ClassName toType) {
			final MapperType mapper = TYPE_MAPPERS.get(ClassName.LONG).get(toType);
			if (mapper == null) throw new IllegalArgumentException("Unexpected type " + toType);
			if (toType.isPrimitive()) {
				return mapper.fn();
			} else {
				return ParameterizedTypeName.get(mapper.fn(), toType);
			}
		}
		@Override public MethodSpec genSetter(final Tuple tuple) {
			return genPrimitiveSetter(tuple, index, LONG_ARG_FIELD);
		}

		@Override public MethodSpec genSingleArgMapper(final Tuple tuple, final TypeName returnType) {
			return TupleEntry.genPrimitiveMapper(tuple, index, LongUnaryOperator.class, returnType, "applyAsLong");
		}
		@Override public MethodSpec genSingleArgMapperTo(final Tuple tuple, final TypeName toType) {
			return TupleEntry.genPrimitiveSingleArgMapper(tuple, index, ClassName.LONG, toType);
		}
	}

	record DoubleType(int index) implements TupleEntry {
		@Override public String typeName() { return "Double"; }
		@Override public TypeName type() { return TypeName.DOUBLE; }
		@Override public TypeVariableName varTypeName() { return TypeVariableName.get(TypeName.DOUBLE.toString()); }
		@Override public ParameterSpec paramSpec(final Modifier... modifiers) {
			return ParameterSpec.builder(TypeName.DOUBLE, "v" + (index + 1), modifiers).build();
		}
		@Override public TypeName predicateName() { return TypeName.get(DoublePredicate.class); }
		@Override public TypeName consumerName() { return TypeName.get(DoubleConsumer.class); }
		@Override public TypeName mapper(final ClassName toType) {
			final MapperType mapper = TYPE_MAPPERS.get(ClassName.DOUBLE).get(toType);
			if (mapper == null) throw new IllegalArgumentException("Unexpected type " + toType);
			if (toType.isPrimitive()) {
				return mapper.fn();
			} else {
				return ParameterizedTypeName.get(mapper.fn(), toType);
			}
		}
		@Override public MethodSpec genSetter(final Tuple tuple) {
			return genPrimitiveSetter(tuple, index, DOUBLE_ARG_FIELD);
		}
		@Override public MethodSpec genSingleArgMapper(final Tuple tuple, final TypeName returnType) {
			return TupleEntry.genPrimitiveMapper(tuple, index, DoubleUnaryOperator.class, returnType, "applyAsDouble");
		}
		@Override public MethodSpec genSingleArgMapperTo(final Tuple tuple, final TypeName toType) {
			return TupleEntry.genPrimitiveSingleArgMapper(tuple, index, ClassName.DOUBLE, toType);
		}
	}

	private static MethodSpec genPrimitiveSetter(Tuple tuple, int index, ParameterSpec.Builder _argField) {
		final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("v" + (index + 1));
		final List<ParameterSpec> argNames = new ArrayList<>(tuple.entryFields());
		final ParameterSpec argField = _argField.addModifiers(Modifier.FINAL).build();
		argNames.set(index, argField);
		final TypeName returnType = tuple.isFullyPrimitive() ? tuple.name() : tuple.nameWithGenerics();
		methodBuilder
				.returns(returnType)
				.addParameter(argField)
				.addModifiers(Modifier.PUBLIC)
				.addStatement("return $T.of($L)", tuple.name(), Tuple.argList(argNames))
				.build();
		return methodBuilder.build();
	}

	private static MethodSpec genPrimitiveMapper(
			final Tuple tuple,
			final int index,
			final Class<?> operator,
			final TypeName returnType,
			final String applyFnName) {
		return MethodSpec.methodBuilder("map" + (index + 1))
				.returns(returnType)
				.addParameter(ParameterSpec.builder(
								ClassName.get(operator),
								"fn",
								Modifier.FINAL)
						.build())
				.addStatement("$T.requireNonNull(fn)", Objects.class)
				.addStatement("return $T.of($L)",
						tuple.name(),
						tuple.idx().mapToObj(j -> {
							final String varName = tuple.entryFields().get(j).name();
							if (index == j) return "fn." + applyFnName + "(" + varName + ")";
							else return varName;
						}).collect(Collectors.joining(", ")))
				.build();
	}

	private static MethodSpec genPrimitiveSingleArgMapper(
			final Tuple tuple, final int index, final TypeName fromType, final TypeName toType) {
		if (fromType.equals(toType)) throw new IllegalArgumentException();

		final Tuple newTuple;
		{
			final ArrayList<TypeName> newTupleTypes = new ArrayList<>(tuple.entries().stream().map(TupleEntry::type).toList());
			newTupleTypes.set(index, toType);
			newTuple = Tuple.of(newTupleTypes);
		}
		final MapperType mapper = TYPE_MAPPERS.get(fromType).get(toType);
		if (mapper == null) throw new IllegalArgumentException("Unexpected type " + toType);

		final TypeName paramType;
		final List<TypeVariableName> newGenericTypes;
		if (ClassName.OBJECT.equals(fromType)) {
			newGenericTypes = List.of();
			paramType = ParameterizedTypeName.get(mapper.fn(), tuple.entries().get(index).varTypeName());
		} else if (ClassName.OBJECT.equals(toType)) {
			newGenericTypes = List.of(newTuple.entries().get(index).varTypeName());
			paramType = ParameterizedTypeName.get(mapper.fn(), newGenericTypes.toArray(TypeName[]::new));
		} else {
			newGenericTypes = List.of();
			paramType = mapper.fn();
		}

		return MethodSpec.methodBuilder("map" + (index + 1) + "To" + SUPPORTED_TYPES.get(toType))
				.addTypeVariables(newGenericTypes)
				.returns(newTuple.nameWithGenerics())
				.addParameter(ParameterSpec.builder(paramType, "fn", Modifier.FINAL).build())
				.addStatement("$T.requireNonNull(fn)", Objects.class)
				.addStatement("return $T.of($L)",
						newTuple.name(),
						tuple.idx().mapToObj(j -> {
							final String varName = tuple.entryFields().get(j).name();
							if (index == j) return "fn.%s(%s)".formatted(mapper.applyName(), varName);
							else return varName;
						}).collect(Collectors.joining(", ")))
				.build();
	}
}
