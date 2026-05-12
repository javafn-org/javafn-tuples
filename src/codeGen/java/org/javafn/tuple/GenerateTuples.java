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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GenerateTuples {

    public static final String PACKAGE_NAME = "org.javafn.tuples";

    static TypeName singleConsumerFor(final TypeName type, final TypeName genericType) {
        if (!type.isPrimitive()) {
            return ParameterizedTypeName.get(ClassName.get(Consumer.class), genericType);
        } else if (type == TypeName.INT) {
            return TypeName.get(IntConsumer.class);
        } else if (type == TypeName.LONG) {
            return TypeName.get(LongConsumer.class);
        }
        throw new IllegalStateException("Single consumer type not implemented for " + type);
    }

    static MethodSpec singleArgMapperFor(
            final int i,
            final Tuple tuple,
            final TypeName type,
            final TypeName typeName,
            final MethodSpec.Builder method) {
        final ParameterSpec fnParam;
        final String fnApplyName;
        if (!type.isPrimitive()) {
            final TypeVariableName zType = TypeVariableName.get("Z");
            method.addTypeVariable(zType);
            fnParam = ParameterSpec.builder(
                    ParameterizedTypeName.get(ClassName.get(Function.class), typeName, zType),
                    "fn", Modifier.FINAL)
                    .build();
            fnApplyName = "apply";
        } else if (type == TypeName.INT) {
            fnParam = ParameterSpec.builder(ClassName.get(IntUnaryOperator.class), "fn", Modifier.FINAL).build();
            fnApplyName = "applyAsInt";
        } else if (type == TypeName.LONG) {
            fnParam = ParameterSpec.builder(ClassName.get(LongUnaryOperator.class), "fn", Modifier.FINAL).build();
            fnApplyName = "applyAsLong";
        } else {
            throw new IllegalStateException("Single function type not implemented for " + type);
        }
        return method
                .addParameter(fnParam)
                .addStatement("return $T.of($L)",
                        tuple.name,
                        tuple.idx().mapToObj(j -> {
                            final String varName = tuple.entryFields.get(j).name();
                            if (i == j) return "fn." + fnApplyName + "(" + varName + ")";
                            else return varName;
                        }).collect(Collectors.joining(", ")))
                .build();
    }

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

        final TypeVariableName zType = TypeVariableName.get("Z");
        final FunctionalInterfaces fi = FunctionalInterfaces.of(tuple);

        final TypeSpec.Builder tupleClassBuilder = TypeSpec.recordBuilder(tuple.name())
                .addModifiers(Modifier.PUBLIC)
                .addTypes(fi.newTypes())
                .addTypeVariables(Arrays.asList(tuple.genericArgs()))
                .recordConstructor(tuple.genConstructor())
                .addMethod(tuple.genStaticFactory())
                .addMethods(tuple.genPredicates(fi))
                .addMethods(tuple.genConsumers(fi));

        genMappers(tuple, tupleClassBuilder, fi, thisType, zType);

        tupleClassBuilder.addMethods(tuple.entries().stream()
                .map(e -> e.genSetter(tuple))
                .toList());

        customize(tupleClassBuilder, tuple);

        JavaFile.builder(PACKAGE_NAME, tupleClassBuilder.build())
                .build()
                .writeTo(packageDir);
    }

    static void customize(final TypeSpec.Builder tupleClassBuilder, final Tuple tuple) {
        if (tuple.entries.size() == 2 && tuple.entries.stream().allMatch(t -> Objects.equals(t, ClassName.OBJECT))) {
            // Tuple type
            final TypeVariableName[] genericArgs = new TypeVariableName[]{
                    TypeVariableName.get("KEY"),
                    TypeVariableName.get("VAL")};
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("from")
                    .addTypeVariables(Arrays.asList(genericArgs))
                    .addParameter(ParameterSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(Entry.class), genericArgs),
                            "e", Modifier.FINAL).build())
                    .returns(ParameterizedTypeName.get(tuple.name, genericArgs))
                            .addStatement("return new $T<>(e.getKey(), e.getValue())", tuple.name)
                    .build());
        }
    }

    static void genMappers(
            final Tuple tuple,
            final TypeSpec.Builder tupleClassBuilder,
            final FunctionalInterfaces fi,
            final TypeName thisType,
            final TypeVariableName zType) {
        // Full map
        tupleClassBuilder.addMethod(MethodSpec.methodBuilder("map")
                .addTypeVariable(zType)
                .returns(zType)
                .addParameter(ParameterSpec.builder(
                        fi.parameterizedMapper(tuple.genericArgs, zType), "fn", Modifier.FINAL)
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return fn.apply($L)", argList(tuple.entryFields))
                .build());
        // Single element map
        tuple.idx().forEach(i -> {
            final TypeName type = tuple.entries.get(i);
            final ParameterSpec field = tuple.entryFields.get(i);
            final TypeName typeName = field.type();

            final TypeName returnType;
            if (thisType instanceof ParameterizedTypeName pType) {
                final TypeVariableName[] genericArgs = tuple.idx()
                        .filter(j -> tuple.entries.get(j).isObj())
                        .mapToObj(j -> {
                            if (i == j) return zType;
                            else return tuple.entryTypes.get(j);
                        })
                        .toArray(TypeVariableName[]::new);
                returnType = ParameterizedTypeName.get(pType.rawType(), genericArgs);
            } else {
                returnType = thisType;
            }
            // Full arg list
            final ParameterSpec fullMapperArg = ParameterSpec.builder(
                            tuple.isFullyPrimitive()
                                    ? fi.primitiveTypeOperators().get(type)
                                    : type.isPrimitive()
                                      ? ParameterizedTypeName.get(fi.primitiveTypeOperators.get(type), tuple.genericArgs)
                                      : fi.parameterizedMapper(tuple.genericArgs, zType),
                            "fn", Modifier.FINAL)
                    .build();
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("map" + (i + 1))
                    .addTypeVariable(zType)
                    .returns(returnType)
                    .addParameter(fullMapperArg)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return $T.of($L)",
                            tuple.name,
                            tuple.idx().mapToObj(j -> {
                                final String varName = tuple.entryFields.get(j).name();
                                if (i == j) return "fn.apply(" + argList(tuple.entryFields) + ")";
                                else return varName;
                            }).collect(Collectors.joining(", ")))
                    .build());

            // Single arg
            tupleClassBuilder.addMethod(
                    singleArgMapperFor(i, tuple, type, typeName,
                            MethodSpec.methodBuilder("map" + (i + 1))
                                    .addModifiers(Modifier.PUBLIC)
                                    .returns(returnType)));

//            if (type.isPrimitive()) {
//                // ToObj Mapper
//                tupleClassBuilder.addMethod(
//                        singleArgMapperFor(index, tuple, type, typeName,
//                                MethodSpec.methodBuilder("map" + (index + 1) + "ToObj")
//                                        .addModifiers(Modifier.PUBLIC)
//                                        .returns(returnType)));
//            }
        });
    }
}
