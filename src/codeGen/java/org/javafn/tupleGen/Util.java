package org.javafn.tupleGen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;

import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleFunction;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongFunction;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import static org.javafn.tupleGen.GenerateTuples.PACKAGE_NAME;

public class Util {
	static final TypeVariableName MAP_RETURN_TYPE = TypeVariableName.get("R");
	static final TypeVariableName INT_TYPE = TypeVariableName.get(TypeName.INT.toString());
	static final TypeVariableName LONG_TYPE = TypeVariableName.get(TypeName.LONG.toString());
	static final TypeVariableName DOUBLE_TYPE = TypeVariableName.get(TypeName.DOUBLE.toString());

	static final ParameterSpec.Builder MAP_RETURN_FIELD = ParameterSpec.builder(MAP_RETURN_TYPE, "r");
	static final ParameterSpec.Builder INT_ARG_FIELD = ParameterSpec.builder(INT_TYPE, "r");
	static final ParameterSpec.Builder LONG_ARG_FIELD = ParameterSpec.builder(LONG_TYPE, "r");
	static final ParameterSpec.Builder DOUBLE_ARG_FIELD = ParameterSpec.builder(DOUBLE_TYPE, "r");

	record MapperType(ClassName fn, String applyName) {}

	static final Map<TypeName, Map<TypeName, MapperType>> TYPE_MAPPERS = Map.of(
			ClassName.OBJECT, Map.of(
					ClassName.OBJECT, new MapperType(ClassName.get(Function.class), "apply"),
					ClassName.INT, new MapperType(ClassName.get(ToIntFunction.class), "applyAsInt"),
					ClassName.LONG, new MapperType(ClassName.get(ToLongFunction.class), "applyAsLong"),
					ClassName.DOUBLE, new MapperType(ClassName.get(ToDoubleFunction.class), "applyAsDouble")
			),
			TypeName.INT, Map.of(
					ClassName.OBJECT, new MapperType(ClassName.get(IntFunction.class), "apply"),
					ClassName.INT, new MapperType(ClassName.get(IntUnaryOperator.class), "applyAsInt"),
					ClassName.LONG, new MapperType(ClassName.get(IntToLongFunction.class), "applyAsLong"),
					ClassName.DOUBLE, new MapperType(ClassName.get(IntToDoubleFunction.class), "applyAsDouble")
			),
			TypeName.LONG, Map.of(
					ClassName.OBJECT, new MapperType(ClassName.get(LongFunction.class), "apply"),
					ClassName.INT, new MapperType(ClassName.get(LongToIntFunction.class), "applyAsInt"),
					ClassName.LONG, new MapperType(ClassName.get(LongUnaryOperator.class), "applyAsLong"),
					ClassName.DOUBLE, new MapperType(ClassName.get(LongToDoubleFunction.class), "applyAsDouble")
			),
			TypeName.DOUBLE, Map.of(
					ClassName.OBJECT, new MapperType(ClassName.get(DoubleFunction.class), "apply"),
					ClassName.INT, new MapperType(ClassName.get(DoubleToIntFunction.class), "applyAsInt"),
					ClassName.LONG, new MapperType(ClassName.get(DoubleToLongFunction.class), "applyAsLong"),
					ClassName.DOUBLE, new MapperType(ClassName.get(DoubleUnaryOperator.class), "applyAsDouble")
			)
	);

	static String capitalize(final String name) {
		return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
	}

	static ClassName genNestedClassName(final String tupleSimpleName, final String suffix) {
		return ClassName.get(PACKAGE_NAME,
				tupleSimpleName,
				tupleSimpleName + suffix);
	}
}
