package org.javafn.tupleGen;

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
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongUnaryOperator;
import java.util.stream.Collectors;

public class GenerateTuples {

    public static final String PACKAGE_NAME = "org.javafn.tuples";

    public static void main(String[] args) throws IOException {
        final File out = new File("build/generated/main/java");
        if (!out.exists()) {
            if (!out.mkdirs()) {
                throw new IOException("Error: Unable to create output directory " + out.getAbsolutePath());
            }
        }

        genTuple(out, Tuple.of("PairType", List.of(ClassName.OBJECT, ClassName.OBJECT)));
        genTuple(out, Tuple.of("TrioType", List.of(ClassName.OBJECT, ClassName.OBJECT, ClassName.OBJECT)));
        genTuple(out, Tuple.of("QuadType", List.of(ClassName.OBJECT, ClassName.OBJECT, ClassName.OBJECT, ClassName.OBJECT)));
        genTuple(out, Tuple.of("Index", List.of(ClassName.INT, ClassName.OBJECT)));
        genTuple(out, Tuple.of(List.of(ClassName.INT, ClassName.INT)));
        genTuple(out, Tuple.of(List.of(ClassName.INT, ClassName.OBJECT, ClassName.OBJECT)));
        genTuple(out, Tuple.of(List.of(ClassName.INT, ClassName.LONG)));
    }

    static void genTuple(final File packageDir, final Tuple tuple) throws IOException {

        final FunctionalInterfaces fi = FunctionalInterfaces.of(tuple);

        final TypeSpec.Builder tupleClassBuilder = TypeSpec.recordBuilder(tuple.name())
                .addModifiers(Modifier.PUBLIC)
                .recordConstructor(tuple.genConstructor())
                .addMethod(tuple.genStaticFactory())
                .addMethods(tuple.genPredicates(fi))
                .addMethods(tuple.genConsumers(fi))
                .addMethods(tuple.genMappers(fi))
                .addMethods(tuple.entries().stream()
                        .map(e -> e.genSetter(tuple))
                .toList());

        if (!fi.newTypes().isEmpty()) {
             tupleClassBuilder.addTypes(fi.newTypes());
        }
        if (tuple.genericArgs().length > 0) {
            tupleClassBuilder.addTypeVariables(Arrays.asList(tuple.genericArgs()));
        }

        customize(tupleClassBuilder, tuple);

        JavaFile.builder(PACKAGE_NAME, tupleClassBuilder.build())
                .build()
                .writeTo(packageDir);
    }

    static void customize(final TypeSpec.Builder tupleClassBuilder, final Tuple tuple) {
        if (tuple.entries().size() == 2 && !tuple.hasPrimitive()) {
            // Pair type
            final TypeVariableName[] genericArgs = new TypeVariableName[]{
                    TypeVariableName.get("KEY"),
                    TypeVariableName.get("VAL")};
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("from")
                    .addTypeVariables(Arrays.asList(genericArgs))
                    .addParameter(ParameterSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(Entry.class), genericArgs),
                            "e", Modifier.FINAL).build())
                    .returns(ParameterizedTypeName.get(tuple.name(), genericArgs))
                            .addStatement("return new $T<>(e.getKey(), e.getValue())", tuple.name())
                    .build());
        }
    }
}
