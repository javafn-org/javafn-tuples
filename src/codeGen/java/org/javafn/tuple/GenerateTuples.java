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
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GenerateTuples {

    public static final String PACKAGE_NAME = "org.javafn.tuples";

    record Tuple(
            ClassName name,
            List<TypeName> types,
            List<TypeVariableName> varTypes,
            List<ParameterSpec> varFields,
            TypeVariableName[] genericArgs
    ) {
        public static Tuple of(final ClassName name, final List<TypeName> types) {
            final List<TypeVariableName> varTypes = IntStream.range(0, types.size())
                    .mapToObj(i -> {
                        final var type = types.get(i);
                        if (type == ClassName.OBJECT) {
                            return TypeVariableName.get("V" + (i+1));
                        } else {
                            return TypeVariableName.get(type.toString());
                        }
                    })
                    .toList();
            final TypeVariableName[] genericArgs = IntStream.range(0, types.size())
                    .filter(i -> !types.get(i).isPrimitive())
                    .mapToObj(varTypes::get)
                    .toArray(TypeVariableName[]::new);
            final List<ParameterSpec> varFields = IntStream.range(0, types.size())
                    .mapToObj(i -> ParameterSpec.builder(varTypes.get(i), "v" + (i+1), Modifier.FINAL).build())
                    .toList();
            return new Tuple(name, types, varTypes, varFields, genericArgs);
        }
        public IntStream idx() { return IntStream.range(0, types.size()); }
    }

    record FunctionalInterfaces(
            ClassName predicate,
            ClassName consumer,
            ClassName mapper
    ) {
        public ParameterizedTypeName parameterizedMapper(final List<TypeVariableName> vars, TypeVariableName retType) {
            final int nArgs = vars.size();
            final TypeName[] args = new TypeName[nArgs + 1];
            for (int i = 0; i < nArgs; i++) {
                args[i] = vars.get(i);
            }
            args[nArgs] = retType;
            return ParameterizedTypeName.get(mapper, args);
        }
    }

    public static void main(String[] args) throws IOException {
        final File out = new File("build/generated/main/java");
        if (!out.exists()) {
            if (!out.mkdirs()) {
                throw new IOException("Error: Unable to create output directory " + out.getAbsolutePath());
            }
        }

        genTuple(out, Tuple.of(ClassName.get(PACKAGE_NAME, "PairType"), List.of(ClassName.OBJECT, ClassName.OBJECT)));
        genTuple(out, Tuple.of(ClassName.get(PACKAGE_NAME, "TrioType"), List.of(ClassName.OBJECT, ClassName.OBJECT, ClassName.OBJECT)));
        genTuple(out, Tuple.of(ClassName.get(PACKAGE_NAME, "QuadType"), List.of(ClassName.OBJECT, ClassName.OBJECT, ClassName.OBJECT, ClassName.OBJECT)));
        genTuple(out, Tuple.of(ClassName.get(PACKAGE_NAME, "IntObjPair"), List.of(ClassName.INT, ClassName.OBJECT)));
    }

    static void genTuple(final File packageDir, final Tuple tuple) throws IOException {
        final TypeVariableName zType = TypeVariableName.get("Z");
        final ParameterSpec zField = ParameterSpec.builder(zType, "z").build();

        final TypeSpec.Builder tupleClassBuilder = TypeSpec.recordBuilder(tuple.name);
        final FunctionalInterfaces fi = getFunctionalInterfaceDefinitions(tuple, tupleClassBuilder);

        // Define the record, its generic arguments, and fields
        tupleClassBuilder
                .addTypeVariables(Arrays.asList(tuple.genericArgs))
                .recordConstructor(MethodSpec.constructorBuilder()
                        .addParameters(tuple.idx().mapToObj(i -> ParameterSpec.builder(tuple.varTypes.get(i), "v" + (i+1)).build()).toList())
                        .build());
        // Define static factory methods
        tupleClassBuilder
                .addMethod(MethodSpec.methodBuilder("of")
                        .returns(ParameterizedTypeName.get(tuple.name, tuple.genericArgs))
                        .addTypeVariables(Arrays.asList(tuple.genericArgs))
                        .addParameters(tuple.varFields)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addStatement("return new $L<>($L)", tuple.name.simpleName(),
                                tuple.varFields.stream().map(ParameterSpec::name).collect(Collectors.joining(", ")))
                        .build());
//        tupleClassBuilder
//                .addMethod(MethodSpec.methodBuilder("map")
//                        .addTypeVariable(zType)
//                        .returns(zType)
//                        .addParameter(ParameterSpec.builder(
//                                tuple.parameterizedMapper(varTypes, zType), "fn", Modifier.FINAL)
//                                .build())
//                        .addModifiers(Modifier.PUBLIC)
//                        .addStatement("return fn.apply($L)", varFields.stream()
//                                .map(ParameterSpec::name)
//                                .collect(Collectors.joining(", ")))
//                        .build());
        // Generate setters for each field.  Primitive types remain fixed, but generic types can change.
        tuple.idx().forEach(i -> {
            final TypeName setterType = tuple.types.get(i);
            final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("v" + (i + 1));
            final TypeVariableName argType;
            final TypeVariableName[] genericTypes;
            if (setterType.isPrimitive()) {
                argType = TypeVariableName.get(setterType.toString());
                genericTypes = tuple.genericArgs;
            } else {
                argType = zType;
                genericTypes = tuple.idx()
                        .filter(j -> !tuple.types.get(j).isPrimitive())
                        .mapToObj(j -> {
                            if (i == j) return zType;
                            else return tuple.varTypes.get(j);
                        })
                        .toArray(TypeVariableName[]::new);
                methodBuilder.addTypeVariable(zType);
            }
            final List<ParameterSpec> argNames = new ArrayList<>(tuple.varFields);
            argNames.set(i, zField);
            methodBuilder
                    .returns(ParameterizedTypeName.get(tuple.name, genericTypes))
                    .addParameter(ParameterSpec.builder(argType, "z").addModifiers(Modifier.FINAL).build())
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return $T.of($L)",
                            tuple.name,
                            argNames.stream().map(ParameterSpec::name).collect(Collectors.joining(", ")))
                    .build();
            tupleClassBuilder.addMethod(methodBuilder.build());
        });

        JavaFile.builder(PACKAGE_NAME, tupleClassBuilder.build())
                .build()
                .writeTo(packageDir);
    }

    static FunctionalInterfaces getFunctionalInterfaceDefinitions(final Tuple tuple, final TypeSpec.Builder tupleClassBuilder) {
        final List<TypeName> types = tuple.types;
        final int nTypes = types.size();
        if (nTypes < 2 || nTypes > 4) {
            throw new IllegalStateException("Only tuples of length 2, 3, or 4 are supported");
        }
        if (nTypes == 2 && types.stream().allMatch(t -> Objects.equals(t, ClassName.OBJECT))) {
                // This is a pair; let's reuse the Bi* interfaces
            return new FunctionalInterfaces( ClassName.get(BiPredicate.class), ClassName.get(BiConsumer.class), ClassName.get(BiFunction.class));
        }
        final ClassName predicate = ClassName.get(PACKAGE_NAME, tuple.name.simpleName(), tuple.name.simpleName() + "Predicate");
        final ClassName consumer = ClassName.get(PACKAGE_NAME, tuple.name.simpleName(), tuple.name.simpleName() + "Consumer");
        final ClassName mapper = ClassName.get(PACKAGE_NAME, tuple.name.simpleName(), tuple.name.simpleName() + "Function");
        tupleClassBuilder.addType(TypeSpec.interfaceBuilder(predicate)
                    .addAnnotation(FunctionalInterface.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariables(Arrays.asList(tuple.genericArgs))
                    .addMethod(MethodSpec.methodBuilder("test")
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(TypeName.BOOLEAN)
                            .addParameters(tuple.varFields)
                            .build())
                    .build());
        tupleClassBuilder.addType(TypeSpec.interfaceBuilder(consumer)
                    .addAnnotation(FunctionalInterface.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariables(Arrays.asList(tuple.genericArgs))
                    .addMethod(MethodSpec.methodBuilder("accept")
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addParameters(tuple.varFields)
                            .build())
                    .build());
            final TypeVariableName mapReturnType = TypeVariableName.get("R");
            final List<TypeVariableName> mapperGenericArgs = new ArrayList<>(tuple.genericArgs.length + 1);
            mapperGenericArgs.addAll(List.of(tuple.genericArgs));
            mapperGenericArgs.add(mapReturnType);
            tupleClassBuilder.addType(TypeSpec.interfaceBuilder(mapper)
                    .addAnnotation(FunctionalInterface.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariables(mapperGenericArgs)
                    .addMethod(MethodSpec.methodBuilder("apply")
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addParameters(tuple.varFields)
                            .returns(mapReturnType)
                            .build())
                    .build());

        return new FunctionalInterfaces(predicate, consumer, mapper);
    }
}
