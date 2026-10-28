package org.javafn.tupleGen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import org.javafn.tupleGen.Tuple.TupleType;
import org.javafn.tupleGen.Util.Generic;
import org.javafn.utils.Data;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
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
            tuple.genChunks().forEach(tupleHelperBuilder::addMethod);
            tuple.genWindows().ifPresent(tupleHelperBuilder::addMethod);
            tuple.genPartition().ifPresent(tupleHelperBuilder::addMethod);
            tuple.genStream().ifPresent(tupleHelperBuilder::addMethod);
        }

        customizeHelper(tupleHelperBuilder, type);

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
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(ParameterSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(Entry.class), genericArgs),
                            "e", Modifier.FINAL).build())
                    .returns(ParameterizedTypeName.get(tuple.name(), genericArgs))
                    .addStatement("$T.requireNonNull(e)", Objects.class)
                    .addStatement("return new $T<>(e.getKey(), e.getValue())", tuple.name())
                    .build());
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("toMap")
                    .addTypeVariables(Arrays.asList(genericArgs))
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ParameterizedTypeName.get(ClassName.get(Collector.class),
                            ParameterizedTypeName.get(tuple.name(),
                                    Arrays.stream(genericArgs)
                                            .map(n -> TypeVariableName.get("? extends " + n.name()))
                                            .toArray(TypeVariableName[]::new)),
                            TypeVariableName.get("?"),
                            ParameterizedTypeName.get(ClassName.get(Map.class), genericArgs)))
                    .addStatement("return $T.toMap(Pair::v1, Pair::v2)", Collectors.class)
                    .build());
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("toTrio")
                    .addTypeVariable(Generic.V3.varTypeName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(Generic.V3.varTypeName(), "v3", Modifier.FINAL).build())
                    .returns(ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Trio"),
                            Data.append(tuple.genericArgs(), Generic.V3.varTypeName())))
                    .addStatement("return new $T<>(v1, v2, $L)", ClassName.get(PACKAGE_NAME, "Trio"), "v3")
                    .build());
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("toTrio")
                    .addTypeVariable(Generic.V3.varTypeName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(BiFunction.class),
                                    Generic.V1.varTypeName(),
                                    Generic.V2.varTypeName(),
                                    Generic.V3.varTypeName()),
                            "fn",
                            Modifier.FINAL).build())
                    .returns(ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Trio"),
                            Data.append(tuple.genericArgs(), Generic.V3.varTypeName())))
                    .addStatement("return new $T<>(v1, v2, fn.apply(v1, v2))", ClassName.get(PACKAGE_NAME, "Trio"))
                    .build());
        } else if ("Index".equals(tuple.name().simpleName())) {
            final ClassName streamIndexerClassName = ClassName.get(PACKAGE_NAME, "Index", "StreamIndexer");
            tupleClassBuilder.addType(TypeSpec.classBuilder(streamIndexerClassName)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariable(Generic.A.varTypeName())
                    .addField(FieldSpec.builder(TypeName.INT, "count", Modifier.PRIVATE).build())
                    .addMethod(MethodSpec.methodBuilder("accept")
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(Generic.A.varTypeName(), "a", Modifier.FINAL)
                            .returns(ParameterizedTypeName.get(tuple.name(), Generic.A.varTypeName()))
                            .addStatement("return new Index<>(count++, a)")
                            .build())
                    .build());
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("of")
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                    .addTypeVariable(Generic.A.varTypeName())
                    .returns(ParameterizedTypeName.get(ClassName.get(Stream.class),
                            ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Index"), Generic.A.varTypeName())))
                    .addParameter(ParameterizedTypeName.get(ClassName.get(Stream.class), Generic.A.varTypeName()), "stream", Modifier.FINAL)
                    .addStatement("return stream.map(new $T<A>()::accept)", streamIndexerClassName)
                    .build());
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("i")
                    .returns(TypeName.INT)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return v1")
                    .build());
        } else if (TupleType.Trio.name().equals(tuple.name().simpleName())) {
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("toQuad")
                    .addTypeVariable(Generic.V4.varTypeName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(Generic.V4.varTypeName(), "v4", Modifier.FINAL).build())
                    .returns(ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Quad"),
                            Data.append(tuple.genericArgs(), Generic.V4.varTypeName())))
                    .addStatement("return new $T<>(v1, v2, v3, $L)", ClassName.get(PACKAGE_NAME, "Quad"), "v4")
                    .build());
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("toQuad")
                    .addTypeVariable(Generic.V4.varTypeName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Trio" , "TrioFunction"),
                                    Generic.V1.varTypeName(),
                                    Generic.V2.varTypeName(),
                                    Generic.V3.varTypeName(),
                                    Generic.V4.varTypeName()),
                            "fn",
                            Modifier.FINAL).build())
                    .returns(ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Quad"),
                            Data.append(tuple.genericArgs(), Generic.V4.varTypeName())))
                    .addStatement("return new $T<>(v1, v2, v3, fn.apply(v1, v2, v3))", ClassName.get(PACKAGE_NAME, "Quad"))
                    .build());
        }
    }
    static void customizeHelper(final TypeSpec.Builder tupleClassBuilder, final TupleType tupleType) {
        if (tupleType.equals(TupleType.Pair)) {
            final ClassName streamIndexerClassName = ClassName.get(PACKAGE_NAME, "Index", "StreamIndexer");
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("index")
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                    .addTypeVariable(Generic.A.varTypeName())
                    .returns(ParameterizedTypeName.get(ClassName.get(Stream.class),
                            ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Index"), Generic.A.varTypeName())))
                    .addParameter(ParameterizedTypeName.get(ClassName.get(Stream.class), Generic.A.varTypeName()), "stream", Modifier.FINAL)
                    .addStatement("return stream.map(new $T<A>()::accept)", streamIndexerClassName)
                    .build());
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("index")
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                    .addTypeVariable(Generic.A.varTypeName())
                    .returns(ParameterizedTypeName.get(ClassName.get(Function.class),
                            Generic.A.varTypeName(),
                            ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Index"), Generic.A.varTypeName())))
                    .addStatement("return new $T<A>()::accept", streamIndexerClassName)
                    .build());
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("toTrio")
                    .addTypeVariables(Arrays.asList(Generic.V1.varTypeName(), Generic.V2.varTypeName(), Generic.V3.varTypeName()))
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(Generic.V3.varTypeName(), "v3", Modifier.FINAL).build())
                    .returns(ParameterizedTypeName.get(ClassName.get(Function.class),
                            ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Pair"),
                                    Generic.V1.varTypeName(), Generic.V2.varTypeName()),
                            ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Trio"),
                                    Generic.V1.varTypeName(), Generic.V2.varTypeName(), Generic.V3.varTypeName())))
                    .addStatement("return p -> p.toTrio($L)", "v3")
                    .build());
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("toTrio")
                    .addTypeVariables(Arrays.asList(Generic.V1.varTypeName(), Generic.V2.varTypeName(), Generic.V3.varTypeName()))
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(BiFunction.class),
                                    Generic.V1.varTypeName(),
                                    Generic.V2.varTypeName(),
                                    Generic.V3.varTypeName()),
                            "fn",
                            Modifier.FINAL).build())
                    .returns(ParameterizedTypeName.get(ClassName.get(Function.class),
                            ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Pair"),
                                    Generic.V1.varTypeName(), Generic.V2.varTypeName()),
                            ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Trio"),
                                    Generic.V1.varTypeName(), Generic.V2.varTypeName(), Generic.V3.varTypeName())))
                    .addStatement("return p -> p.toTrio(fn)")
                    .build());
        } else if (tupleType.equals(TupleType.Trio)) {
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("toQuad")
                    .addTypeVariables(Arrays.asList(
                            Generic.V1.varTypeName(),
                            Generic.V2.varTypeName(),
                            Generic.V3.varTypeName(),
                            Generic.V4.varTypeName()))
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(TypeVariableName.get("V4"), "v4", Modifier.FINAL).build())
                    .returns(ParameterizedTypeName.get(ClassName.get(Function.class),
                            ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Trio"),
                                    Generic.V1.varTypeName(), Generic.V2.varTypeName(), Generic.V3.varTypeName()),
                            ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Quad"),
                                    Generic.V1.varTypeName(),
                                    Generic.V2.varTypeName(),
                                    Generic.V3.varTypeName(),
                                    Generic.V4.varTypeName())))
                    .addStatement("return t -> t.toQuad($L)", "v4")
                    .build());
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("toQuad")
                    .addTypeVariables(Arrays.asList(
                            Generic.V1.varTypeName(),
                            Generic.V2.varTypeName(),
                            Generic.V3.varTypeName(),
                            Generic.V4.varTypeName()))
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Trio" , "TrioFunction"),
                                    Generic.V1.varTypeName(),
                                    Generic.V2.varTypeName(),
                                    Generic.V3.varTypeName(),
                                    Generic.V4.varTypeName()),
                            "fn",
                            Modifier.FINAL).build())
                    .returns(ParameterizedTypeName.get(ClassName.get(Function.class),
                            ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Trio"),
                                    Generic.V1.varTypeName(), Generic.V2.varTypeName(), Generic.V3.varTypeName()),
                            ParameterizedTypeName.get(ClassName.get(PACKAGE_NAME, "Quad"),
                                    Generic.V1.varTypeName(),
                                    Generic.V2.varTypeName(),
                                    Generic.V3.varTypeName(),
                                    Generic.V4.varTypeName())))
                    .addStatement("return t -> t.toQuad(fn)")
                    .build());
        }
    }
}
