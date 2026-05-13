package org.javafn.tupleGen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import org.javafn.utils.Data;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GenerateTuples {

    public static final String PACKAGE_NAME = "org.javafn.tuples";

    public static final List<TypeName> DISTINCT_TYPES = List.of(
            ClassName.OBJECT, ClassName.INT, ClassName.LONG, ClassName.DOUBLE);

    public static void main(String[] args) throws IOException {
        final File out = new File("build/generated/main/java");
        if (!out.exists()) {
            if (!out.mkdirs()) {
                throw new IOException("Error: Unable to create output directory " + out.getAbsolutePath());
            }
        }

        final List<List<TypeName>> tuples2 = DISTINCT_TYPES.stream()
                .map(List::of)
                .flatMap(l -> DISTINCT_TYPES.stream()
                        .map(r -> Data.append(l, r)))
                .toList();
        final List<List<TypeName>> tuples3 = tuples2.stream()
                .map(l -> Data.append(l, ClassName.OBJECT))
                .toList();
        final List<List<TypeName>> tuples4 = tuples2.stream()
                .map(l -> Data.append(l, List.of(ClassName.OBJECT, ClassName.OBJECT)))
                .toList();

        Stream.of(tuples2, tuples3, tuples4)
                .flatMap(List::stream)
                .map(Tuple::of)
                .forEach(tuple -> {
	                try {
		                genTuple(out, tuple);
	                } catch (IOException e) {
		                throw new RuntimeException(e);
	                }
                });
    }

    static void genTuple(final File packageDir, final Tuple tuple) throws IOException {

        final FunctionalInterfaces fi = FunctionalInterfaces.of(tuple);

        final TypeSpec.Builder tupleClassBuilder = TypeSpec.recordBuilder(tuple.name())
                .addModifiers(Modifier.PUBLIC)
                .recordConstructor(tuple.genConstructor())
                .addTypes(fi.newTypes())
                .addTypeVariables(Arrays.asList(tuple.genericArgs()))
                .addMethod(tuple.genStaticFactory())
                .addMethods(tuple.genPredicates(fi))
                .addMethods(tuple.genConsumers(fi))
                .addMethods(tuple.genMappers(fi))
                .addMethods(tuple.entries().stream()
                        .map(e -> e.genSetter(tuple))
                .toList());

        customize(tupleClassBuilder, tuple);

        JavaFile.builder(PACKAGE_NAME, tupleClassBuilder.build())
                .build()
                .writeTo(packageDir);
    }

    static void customize(final TypeSpec.Builder tupleClassBuilder, final Tuple tuple) {
        if ("PairType".equals(tuple.name().simpleName())) {
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
                            .addStatement("$T.requireNonNull(e)", Objects.class)
                            .addStatement("return new $T<>(e.getKey(), e.getValue())", tuple.name())
                    .build());
        }
    }
}
