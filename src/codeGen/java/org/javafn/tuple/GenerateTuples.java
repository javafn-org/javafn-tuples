package org.javafn.tuple;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GenerateTuples {

    public static final String PACKAGE_NAME = "org.javafn.tuples";

    record Tuple(
            int numElements,
            ClassName name,
            ClassName predicate,
            ClassName consumer,
            ClassName mapper
    ) {
        public IntStream idx() { return IntStream.range(1, numElements + 1); }
        public ParameterizedTypeName parameterizedMapper(final List<Var> vars, TypeVariableName retType) {
            final int nArgs = vars.size();
            final TypeName[] args = new TypeName[nArgs + 1];
            for (int i = 0; i < vars.size(); i++) {
                args[i] = vars.get(i).type;
            }
            args[nArgs] = retType;
            return ParameterizedTypeName.get(mapper, args);
        }
    }

    record Var(TypeVariableName type, ParameterSpec field) { }

    public static void main(String[] args) throws IOException {
        final File out = new File("build/generated/main/java");
        if (!out.exists()) {
            if (!out.mkdirs()) {
                throw new IOException("Error: Unable to create output directory " + out.getAbsolutePath());
            }
        }

        final var pair = new Tuple(2,
                ClassName.get(PACKAGE_NAME, "PairType"),
                ClassName.get(BiPredicate.class),
                ClassName.get(BiConsumer.class),
                ClassName.get(BiFunction.class));
        genTuple(out, pair);

    }

    static void genTuple(final File packageDir, final Tuple tuple) throws IOException {
        final List<Var> vars = tuple.idx()
                .mapToObj(i -> {
                    final var type = TypeVariableName.get("V" + i);
                    final var field = ParameterSpec.builder(type, "v" + i).build();
                    return new Var(type, field);
                })
                .toList();
        final TypeVariableName zType = TypeVariableName.get("Z");

        final TypeSpec tupleClass = TypeSpec.recordBuilder(tuple.name)
                .addTypeVariables(vars.stream().map(Var::type).toList())
                .recordConstructor(MethodSpec.constructorBuilder()
                        .addParameters(vars.stream().map(Var::field).toList())
                        .build())
                .addMethod(MethodSpec.methodBuilder("of")
                        .returns(TypeVariableName.get(tuple.name.simpleName()))
                        .addTypeVariables(vars.stream().map(Var::type).toList())
                        .addParameters(vars.stream().map(Var::field).toList())
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addStatement("return new $L<>($L)", tuple.name.simpleName(),
                                vars.stream().map(Var::field).map(ParameterSpec::name).collect(Collectors.joining(", ")))
                        .build())
                .addMethod(MethodSpec.methodBuilder("map")
                        .addTypeVariable(zType)
                        .returns(zType)
                        .addParameter(ParameterSpec.builder(
                                tuple.parameterizedMapper(vars, zType), "fn", Modifier.FINAL)
                                .build())
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("return fn.apply($L)", vars.stream()
                                .map(Var::field).map(ParameterSpec::name)
                                .collect(Collectors.joining(", ")))
                        .build())
                .build();
        JavaFile.builder(PACKAGE_NAME, tupleClass).build().writeTo(packageDir);
    }
}
