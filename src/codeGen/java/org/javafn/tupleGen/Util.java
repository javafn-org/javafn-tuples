package org.javafn.tupleGen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;

import java.util.Locale;

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

	static String capitalize(final String name) {
		return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
	}

	static ClassName genNestedClassName(final String tupleSimpleName, final String suffix) {
		return ClassName.get(PACKAGE_NAME,
				tupleSimpleName,
				tupleSimpleName + suffix);
	}
}
