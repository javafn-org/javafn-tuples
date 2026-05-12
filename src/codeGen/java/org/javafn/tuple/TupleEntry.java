package org.javafn.tuple;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public interface TupleEntry {
	int index();
	String typeName();
	TypeName type();
	default boolean isObj() { return false; }
	TypeVariableName varTypeName();
	ParameterSpec paramSpec();
	TypeName predicate();
	TypeName consumer();
	TypeName mapper(ClassName toType);
	MethodSpec genSetter(Tuple tuple);

	static TupleEntry of(final TypeName type, final int i) {
		if (type.equals(ClassName.OBJECT)) {
			return new ObjectType(i, TypeVariableName.get("V" + i));
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


	record ObjectType(int index, TypeVariableName varTypeName) implements TupleEntry {

		static final TypeVariableName Z_TYPE = TypeVariableName.get("Z");
		static final ParameterSpec Z_FIELD = ParameterSpec.builder(Z_TYPE, "z").build();

		@Override public String typeName() { return "Obj"; }
		@Override public TypeName type() {
			return ParameterizedTypeName.get(ClassName.get(Consumer.class), varTypeName);
		}
		@Override public boolean isObj() { return true; }
		@Override public ParameterSpec paramSpec() {
			return ParameterSpec.builder(varTypeName, "v" + (index +1), Modifier.FINAL).build();
		}
		@Override public TypeName predicate() {
			return ParameterizedTypeName.get(ClassName.get(Predicate.class), varTypeName);
		}
		@Override public TypeName consumer() {
			return ParameterizedTypeName.get(ClassName.get(Consumer.class), varTypeName);
		}
		@Override public TypeName mapper(final ClassName toType) {
			if (toType.isPrimitive()) {
				if (toType.equals(TypeName.INT)) {
					return ParameterizedTypeName.get(ClassName.get(ToIntFunction.class), varTypeName);
				} else if (toType.equals(TypeName.LONG)) {
					return ParameterizedTypeName.get(ClassName.get(ToLongFunction.class), varTypeName);
				} else if (toType.equals(TypeName.DOUBLE)) {
					return ParameterizedTypeName.get(ClassName.get(ToDoubleFunction.class), varTypeName);
				} else {
					throw new IllegalArgumentException("Unexpected type " + toType);
				}
			} else {
				return ParameterizedTypeName.get(ClassName.get(Function.class), varTypeName, toType);
			}
		}
		@Override public MethodSpec genSetter(final Tuple tuple) {
			final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("v" + (index + 1));
			final List<ParameterSpec> argNames = new ArrayList<>(tuple.entryFields());
			argNames.set(index, Z_FIELD);
			final TypeVariableName[] genericTypes = tuple.idx()
					.filter(j -> tuple.entries().get(j).isObj())
					.mapToObj(j -> {
						if (index == j) return Z_TYPE;
						else return tuple.entryTypes().get(j);
					})
					.toArray(TypeVariableName[]::new);
			methodBuilder
					.addTypeVariable(Z_TYPE)
					.returns(ParameterizedTypeName.get(tuple.name(), genericTypes))
					.addParameter(ParameterSpec.builder(Z_TYPE, "z").addModifiers(Modifier.FINAL).build())
					.addModifiers(Modifier.PUBLIC)
					.addStatement("return $T.of($L)", tuple.name(), Tuple.argList(argNames))
					.build();
			return methodBuilder.build();
		}
	}

	record IntType(int index) implements TupleEntry {
		static final TypeVariableName INT_TYPE = TypeVariableName.get(TypeName.INT.toString());
		static final ParameterSpec ARG_FIELD = ParameterSpec.builder(INT_TYPE, "z").build();

		@Override public String typeName() { return "Int"; }
		@Override public TypeName type() { return TypeName.INT; }
		@Override public TypeVariableName varTypeName() { return INT_TYPE; }
		@Override public ParameterSpec paramSpec() {
			return ParameterSpec.builder(TypeName.INT, "v" + index, Modifier.FINAL).build();
		}
		@Override public TypeName predicate() { return TypeName.get(IntPredicate.class); }
		@Override public TypeName consumer() { return TypeName.get(IntConsumer.class); }
		@Override public TypeName mapper(final ClassName toType) {
			if (toType.isPrimitive()) {
				if (toType.equals(TypeName.INT)) {
					return TypeName.get(IntUnaryOperator.class);
				} else if (toType.equals(TypeName.LONG)) {
					return TypeName.get(IntToLongFunction.class);
				} else if (toType.equals(TypeName.DOUBLE)) {
					return TypeName.get(IntToDoubleFunction.class);
				} else {
					throw new IllegalArgumentException("Unexpected type " + toType);
				}
			} else {
				return ParameterizedTypeName.get(ClassName.get(IntFunction.class), toType);
			}
		}
		@Override public MethodSpec genSetter(final Tuple tuple) {
			final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("v" + (index + 1));
			final List<ParameterSpec> argNames = new ArrayList<>(tuple.entryFields());
			argNames.set(index, ARG_FIELD);
			methodBuilder
					.returns(ParameterizedTypeName.get(tuple.name(), tuple.genericArgs()))
					.addParameter(ParameterSpec.builder(INT_TYPE, "z").addModifiers(Modifier.FINAL).build())
					.addModifiers(Modifier.PUBLIC)
					.addStatement("return $T.of($L)", tuple.name(), Tuple.argList(argNames))
					.build();
			return methodBuilder.build();
		}
	}

	record LongType(int index) implements TupleEntry {
		static final TypeVariableName LONG_TYPE = TypeVariableName.get(TypeName.INT.toString());
		static final ParameterSpec ARG_FIELD = ParameterSpec.builder(LONG_TYPE, "z").build();
		@Override public String typeName() { return "Long"; }
		@Override public TypeName type() { return TypeName.LONG; }
		@Override public TypeVariableName varTypeName() { return TypeVariableName.get(TypeName.LONG.toString()); }
		@Override public ParameterSpec paramSpec() {
			return ParameterSpec.builder(TypeName.LONG, "v" + index, Modifier.FINAL).build();
		}
		@Override public TypeName predicate() { return TypeName.get(LongPredicate.class); }
		@Override public TypeName consumer() { return TypeName.get(LongConsumer.class); }
		@Override public TypeName mapper(final ClassName toType) {
			if (toType.isPrimitive()) {
				if (toType.equals(TypeName.INT)) {
					return TypeName.get(LongToIntFunction.class);
				} else if (toType.equals(TypeName.LONG)) {
					return TypeName.get(LongUnaryOperator.class);
				} else if (toType.equals(TypeName.DOUBLE)) {
					return TypeName.get(LongToDoubleFunction.class);
				} else {
					throw new IllegalArgumentException("Unexpected type " + toType);
				}
			} else {
				return ParameterizedTypeName.get(ClassName.get(LongFunction.class), toType);
			}
		}
		@Override public MethodSpec genSetter(final Tuple tuple) {
			final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("v" + (index + 1));
			final List<ParameterSpec> argNames = new ArrayList<>(tuple.entryFields());
			argNames.set(index, ARG_FIELD);
			methodBuilder
					.returns(ParameterizedTypeName.get(tuple.name(), tuple.genericArgs()))
					.addParameter(ParameterSpec.builder(LONG_TYPE, "z").addModifiers(Modifier.FINAL).build())
					.addModifiers(Modifier.PUBLIC)
					.addStatement("return $T.of($L)", tuple.name(), Tuple.argList(argNames))
					.build();
			return methodBuilder.build();
		}
	}

	record DoubleType(int index) implements TupleEntry {
		static final TypeVariableName DOUBLE_TYPE = TypeVariableName.get(TypeName.INT.toString());
		static final ParameterSpec ARG_FIELD = ParameterSpec.builder(DOUBLE_TYPE, "z").build();
		@Override public String typeName() { return "Double"; }
		@Override public TypeName type() { return TypeName.DOUBLE; }
		@Override public TypeVariableName varTypeName() { return TypeVariableName.get(TypeName.DOUBLE.toString()); }
		@Override public ParameterSpec paramSpec() {
			return ParameterSpec.builder(TypeName.DOUBLE, "v" + index, Modifier.FINAL).build();
		}
		@Override public TypeName predicate() { return TypeName.get(LongPredicate.class); }
		@Override public TypeName consumer() { return TypeName.get(LongConsumer.class); }
		@Override public TypeName mapper(final ClassName toType) {
			if (toType.isPrimitive()) {
				if (toType.equals(TypeName.INT)) {
					return TypeName.get(DoubleToIntFunction.class);
				} else if (toType.equals(TypeName.LONG)) {
					return TypeName.get(DoubleToLongFunction.class);
				} else if (toType.equals(TypeName.DOUBLE)) {
					return TypeName.get(DoubleUnaryOperator.class);
				} else {
					throw new IllegalArgumentException("Unexpected type " + toType);
				}
			} else {
				return ParameterizedTypeName.get(ClassName.get(LongFunction.class), toType);
			}
		}
		@Override public MethodSpec genSetter(final Tuple tuple) {
			final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("v" + (index + 1));
			final List<ParameterSpec> argNames = new ArrayList<>(tuple.entryFields());
			argNames.set(index, ARG_FIELD);
			methodBuilder
					.returns(ParameterizedTypeName.get(tuple.name(), tuple.genericArgs()))
					.addParameter(ParameterSpec.builder(DOUBLE_TYPE, "z").addModifiers(Modifier.FINAL).build())
					.addModifiers(Modifier.PUBLIC)
					.addStatement("return $T.of($L)", tuple.name(), Tuple.argList(argNames))
					.build();
			return methodBuilder.build();
		}
	}
}
