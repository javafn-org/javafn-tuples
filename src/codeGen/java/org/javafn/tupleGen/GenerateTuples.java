package org.javafn.tupleGen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import org.javafn.tupleGen.Tuple.TupleType;
import org.javafn.utils.Data;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;

public class GenerateTuples {

    public static final String PACKAGE_NAME = "org.javafn.tuples";

    public static final Map<TypeName, String> SUPPORTED_TYPES = Map.of(
            ClassName.OBJECT, "Obj",
            ClassName.INT, "Int",
            ClassName.LONG, "Long",
            ClassName.DOUBLE, "Double");

    record TupleFunctionalInterfaces(Tuple tuple, FunctionalInterfaces fi) {}

    public static void main(String[] args) throws IOException {
        final File out = new File("build/generated/main/java");
        if (!out.exists()) {
            if (!out.mkdirs()) {
                throw new IOException("Error: Unable to create output directory " + out.getAbsolutePath());
            }
        }

        final List<List<TypeName>> tuples2 = SUPPORTED_TYPES.keySet().stream()
                .map(List::of)
                .flatMap(l -> SUPPORTED_TYPES.keySet().stream()
                        .map(r -> Data.append(l, r)))
                .toList();
        final List<List<TypeName>> tuples3 = tuples2.stream()
                .map(l -> Data.append(l, ClassName.OBJECT))
                .toList();
        final List<List<TypeName>> tuples4 = tuples2.stream()
                .map(l -> Data.append(l, List.of(ClassName.OBJECT, ClassName.OBJECT)))
                .toList();

        final List<TupleFunctionalInterfaces> pairs = tuples2.stream()
                .map(Tuple::of)
                .map(t -> new TupleFunctionalInterfaces(t, FunctionalInterfaces.of(t)))
                .toList();
        final List<TupleFunctionalInterfaces> trios = tuples3.stream()
                .map(Tuple::of)
                .map(t -> new TupleFunctionalInterfaces(t, FunctionalInterfaces.of(t)))
                .toList();
        final List<TupleFunctionalInterfaces> quads = tuples4.stream()
                .map(Tuple::of)
                .map(t -> new TupleFunctionalInterfaces(t, FunctionalInterfaces.of(t)))
                .toList();

        Stream.of(pairs, trios, quads)
                .flatMap(List::stream)
                .forEach(tuple -> {
	                try {
		                genTuple(out, tuple);
	                } catch (IOException e) {
		                throw new RuntimeException(e);
	                }
                });

        genHelper(out, TupleType.Pair, pairs);
        genHelper(out, TupleType.Trio, trios);
        genHelper(out, TupleType.Quad, quads);
    }

    static void genTuple(final File packageDir, final TupleFunctionalInterfaces tfi) throws IOException {
        final Tuple tuple = tfi.tuple;
        final FunctionalInterfaces fi = tfi.fi;

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

    static void genHelper(final File packageDir, final TupleType type, List<TupleFunctionalInterfaces> tfis) throws IOException {

        final TypeSpec.Builder tupleHelperBuilder = TypeSpec.classBuilder(type.name() + "s")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .addStatement("throw new $T(\"This is a static helper class and should never be instantiated.\")", IllegalStateException.class)
                        .build());
        for (final TupleFunctionalInterfaces tfi : tfis) {
            final Tuple tuple = tfi.tuple;
            final FunctionalInterfaces fi = tfi.fi;
            tupleHelperBuilder.addMethod(tuple.genZipper())
                    .addMethods(tuple.genStaticPredicates(fi))
                    .addMethods(tuple.genStaticConsumers(fi))
                    .addMethods(tuple.genStaticMappers(fi));
        }
//                .addMethod()
//                .addMethods(tuple.genPredicates(fi))
//                .addMethods(tuple.genConsumers(fi))
//                .addMethods(tuple.genMappers(fi))
//                .addMethods(tuple.entries().stream()
//                        .map(e -> e.genSetter(tuple))
//                        .toList());
        JavaFile.builder(PACKAGE_NAME, tupleHelperBuilder.build())
                .build()
                .writeTo(packageDir);
    }

    static void customize(final TypeSpec.Builder tupleClassBuilder, final Tuple tuple) {
        if (TupleType.Pair.name().equals(tuple.name().simpleName())) {
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
