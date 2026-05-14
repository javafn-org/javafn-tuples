package org.javafn.tupleGen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;

import javax.lang.model.element.Modifier;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.PrimitiveIterator.OfDouble;
import java.util.PrimitiveIterator.OfInt;
import java.util.PrimitiveIterator.OfLong;
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
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.javafn.tupleGen.GenerateTuples.PACKAGE_NAME;

public class Util {

	public enum Generic {
		A, B, C, D, R, V1, V2, V3, V4;
		public static Generic arbitrary(final int i) {
			return switch (i) {
				case 0 -> A; case 1 -> B; case 2 -> C; case 3 -> D;
				default -> throw new IllegalArgumentException();
			};
		}
		public static Generic v(final int i) {
			return switch (i) {
				case 0 -> V1; case 1 -> V2; case 2 -> V3; case 3 -> V4;
				default -> throw new IllegalArgumentException();
			};
		}
		private final String fieldName = name().toLowerCase(Locale.ROOT);
		private final TypeVariableName typeVariableName = TypeVariableName.get(name());
		private final ParameterSpec.Builder field = ParameterSpec.builder(typeVariableName, fieldName);
		public String fieldName() { return fieldName; }
		public TypeVariableName varTypeName() { return typeVariableName; }
		public ParameterSpec parameter(final Modifier... modifiers) { return field.addModifiers(modifiers).build(); }
	}

	static final TypeVariableName INT_TYPE = TypeVariableName.get(TypeName.INT.toString());
	static final TypeVariableName LONG_TYPE = TypeVariableName.get(TypeName.LONG.toString());
	static final TypeVariableName DOUBLE_TYPE = TypeVariableName.get(TypeName.DOUBLE.toString());

	static final ParameterSpec.Builder INT_ARG_FIELD = ParameterSpec.builder(INT_TYPE, "r");
	static final ParameterSpec.Builder LONG_ARG_FIELD = ParameterSpec.builder(LONG_TYPE, "r");
	static final ParameterSpec.Builder DOUBLE_ARG_FIELD = ParameterSpec.builder(DOUBLE_TYPE, "r");

	record MapperType(ClassName fn, String applyName) {}

	static final Map<TypeName, Map<TypeName, MapperType>> TYPE_MAPPERS = Map.of(
			ClassName.OBJECT, Map.of(
					ClassName.OBJECT, new MapperType(ClassName.get(Function.class), "apply"),
					TypeName.INT, new MapperType(ClassName.get(ToIntFunction.class), "applyAsInt"),
					TypeName.LONG, new MapperType(ClassName.get(ToLongFunction.class), "applyAsLong"),
					TypeName.DOUBLE, new MapperType(ClassName.get(ToDoubleFunction.class), "applyAsDouble")
			),
			TypeName.INT, Map.of(
					ClassName.OBJECT, new MapperType(ClassName.get(IntFunction.class), "apply"),
					TypeName.INT, new MapperType(ClassName.get(IntUnaryOperator.class), "applyAsInt"),
					TypeName.LONG, new MapperType(ClassName.get(IntToLongFunction.class), "applyAsLong"),
					TypeName.DOUBLE, new MapperType(ClassName.get(IntToDoubleFunction.class), "applyAsDouble")
			),
			TypeName.LONG, Map.of(
					ClassName.OBJECT, new MapperType(ClassName.get(LongFunction.class), "apply"),
					TypeName.INT, new MapperType(ClassName.get(LongToIntFunction.class), "applyAsInt"),
					TypeName.LONG, new MapperType(ClassName.get(LongUnaryOperator.class), "applyAsLong"),
					TypeName.DOUBLE, new MapperType(ClassName.get(LongToDoubleFunction.class), "applyAsDouble")
			),
			TypeName.DOUBLE, Map.of(
					ClassName.OBJECT, new MapperType(ClassName.get(DoubleFunction.class), "apply"),
					TypeName.INT, new MapperType(ClassName.get(DoubleToIntFunction.class), "applyAsInt"),
					TypeName.LONG, new MapperType(ClassName.get(DoubleToLongFunction.class), "applyAsLong"),
					TypeName.DOUBLE, new MapperType(ClassName.get(DoubleUnaryOperator.class), "applyAsDouble")
			)
	);

	static final Map<TypeName, ClassName> STREAMERS = Map.of(
			ClassName.OBJECT, ClassName.get(Stream.class),
			ClassName.INT, ClassName.get(IntStream.class),
			ClassName.LONG, ClassName.get(LongStream.class),
			ClassName.DOUBLE, ClassName.get(DoubleStream.class));
	static final Map<TypeName, ClassName> ITERATORS = Map.of(
			ClassName.OBJECT, ClassName.get(Iterator.class),
			ClassName.INT, ClassName.get(OfInt.class),
			ClassName.LONG, ClassName.get(OfLong.class),
			ClassName.DOUBLE, ClassName.get(OfDouble.class));

	static String capitalize(final String name) {
		return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
	}

	static ClassName genNestedClassName(final String tupleSimpleName, final String suffix) {
		return ClassName.get(PACKAGE_NAME,
				tupleSimpleName,
				tupleSimpleName + suffix);
	}
}
