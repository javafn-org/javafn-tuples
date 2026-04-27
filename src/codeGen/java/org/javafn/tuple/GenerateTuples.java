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
import java.util.Map.Entry;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GenerateTuples {

    public static final String PACKAGE_NAME = "org.javafn.tuples";

    static String capitalize(final String name) {
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    static TypeName singlePredicateFor(final TypeName type, final TypeName typeName) {
        if (!type.isPrimitive()) {
            return ParameterizedTypeName.get(ClassName.get(Predicate.class), typeName);
        } else if (type == TypeName.INT) {
            return TypeName.get(IntPredicate.class);
        } else if (type == TypeName.LONG) {
            return TypeName.get(LongPredicate.class);
        }
        throw new IllegalStateException("Single predicate type not implemented for " + type);
    }
    static TypeName singleConsumerFor(final TypeName type, final TypeName typeName) {
        if (!type.isPrimitive()) {
            return ParameterizedTypeName.get(ClassName.get(Consumer.class), typeName);
        } else if (type == TypeName.INT) {
            return TypeName.get(IntConsumer.class);
        } else if (type == TypeName.LONG) {
            return TypeName.get(LongConsumer.class);
        }
        throw new IllegalStateException("Single consumer type not implemented for " + type);
    }

    static MethodSpec singleFunctionFor(
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
                            final String varName = tuple.varFields.get(j).name();
                            if (i == j) return "fn." + fnApplyName + "(" + varName + ")";
                            else return varName;
                        }).collect(Collectors.joining(", ")))
                .build();
    }

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
                            return TypeVariableName.get("V" + (i + 1));
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
                    .mapToObj(i -> ParameterSpec.builder(varTypes.get(i), "v" + (i + 1), Modifier.FINAL).build())
                    .toList();
            return new Tuple(name, types, varTypes, varFields, genericArgs);
        }

        public IntStream idx() {
            return IntStream.range(0, types.size());
        }
    }

    record FunctionalInterfaces(
            ClassName fullPredicate,
            ClassName fullConsumer,
            ClassName fullMapper,
            Map<TypeName, ClassName> primitiveTypeOperators
    ) {
        public TypeName parameterizedPredicate(final TypeVariableName[] genericArgs) {
            if (genericArgs.length == 0) {
                return fullPredicate;
            } else {
                return ParameterizedTypeName.get(fullPredicate, genericArgs);
            }
        }

        public TypeName parameterizedConsumer(final TypeVariableName[] genericArgs) {
            if (genericArgs.length == 0) {
                return fullConsumer;
            } else {
                return ParameterizedTypeName.get(fullConsumer, genericArgs);
            }
        }

        public TypeName parameterizedMapper(final TypeVariableName[] genericArgs, final TypeVariableName retType) {
            final int nArgs = genericArgs.length;
            final TypeName[] args = new TypeName[nArgs + 1];
            System.arraycopy(genericArgs, 0, args, 0, nArgs);
            args[nArgs] = retType;
            return ParameterizedTypeName.get(fullMapper, args);
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
        genTuple(out, Tuple.of(ClassName.get(PACKAGE_NAME, "Indexed"), List.of(ClassName.INT, ClassName.OBJECT)));
        genTuple(out, Tuple.of(ClassName.get(PACKAGE_NAME, "IntIntPair"), List.of(ClassName.INT, ClassName.INT)));
        genTuple(out, Tuple.of(ClassName.get(PACKAGE_NAME, "IntLongPair"), List.of(ClassName.INT, ClassName.LONG)));
    }

    static void genTuple(final File packageDir, final Tuple tuple) throws IOException {
        final TypeVariableName zType = TypeVariableName.get("Z");
        final ParameterSpec zField = ParameterSpec.builder(zType, "z").build();

        final TypeSpec.Builder tupleClassBuilder = TypeSpec.recordBuilder(tuple.name);
        final FunctionalInterfaces fi = getFunctionalInterfaceDefinitions(tuple, tupleClassBuilder);
        final TypeName thisType;
        if (tuple.genericArgs.length == 0) {
            thisType = tuple.name;
        } else {
            thisType = ParameterizedTypeName.get(tuple.name, tuple.genericArgs);
        }

        // Define the record, its generic arguments, and fields
        tupleClassBuilder
                .addTypeVariables(Arrays.asList(tuple.genericArgs))
                .recordConstructor(MethodSpec.constructorBuilder()
                        .addParameters(tuple.idx().mapToObj(i -> ParameterSpec.builder(tuple.varTypes.get(i), "v" + (i + 1)).build()).toList())
                        .build());
        // Define static factory methods
        tupleClassBuilder
                .addMethod(MethodSpec.methodBuilder("of")
                        .returns(thisType)
                        .addTypeVariables(Arrays.asList(tuple.genericArgs))
                        .addParameters(tuple.varFields)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addStatement("return new $L$L($L)",
                                tuple.name.simpleName(),
                                tuple.genericArgs.length == 0 ? "" : "<>",
                                argList(tuple.varFields))
                        .build());
        genMatchers(tuple, tupleClassBuilder, fi);
        genPeekers(tuple, tupleClassBuilder, fi, thisType);
        genMappers(tuple, tupleClassBuilder, fi, thisType, zType);

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
            final TypeName returnType = genericTypes.length == 0
                    ? tuple.name
                    : ParameterizedTypeName.get(tuple.name, genericTypes);
            argNames.set(i, zField);
            methodBuilder
                    .returns(returnType)
                    .addParameter(ParameterSpec.builder(argType, "z").addModifiers(Modifier.FINAL).build())
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return $T.of($L)", tuple.name, argList(argNames))
                    .build();
            tupleClassBuilder.addMethod(methodBuilder.build());
        });

        JavaFile.builder(PACKAGE_NAME, tupleClassBuilder.build())
                .build()
                .writeTo(packageDir);
    }

    static void genMatchers(
            final Tuple tuple,
            final TypeSpec.Builder tupleClassBuilder,
            final FunctionalInterfaces fi) {
        // Full matches
        tupleClassBuilder
                .addMethod(MethodSpec.methodBuilder("matches")
                        .returns(ClassName.BOOLEAN)
                        .addParameter(ParameterSpec.builder(
                                fi.parameterizedPredicate(tuple.genericArgs), "fn", Modifier.FINAL)
                                .build())
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("return fn.test($L)", argList(tuple.varFields))
                        .build());
        // Single argument matches
        tuple.idx().forEach(i -> {
            final TypeName type = tuple.types.get(i);
            final ParameterSpec field = tuple.varFields.get(i);
            final TypeName typeName = field.type();
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("matches" + (i+1))
                    .returns(ClassName.BOOLEAN)
                    .addParameter(ParameterSpec.builder(
                            singlePredicateFor(type, typeName),
                            "fn",
                            Modifier.FINAL)
                            .build())
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return fn.test($L)", tuple.varFields.get(i).name())
                    .build());
        });
    }

    static void genPeekers(
            final Tuple tuple,
            final TypeSpec.Builder tupleClassBuilder,
            final FunctionalInterfaces fi,
            final TypeName thisType) {
        // Full peek
        tupleClassBuilder.addMethod(MethodSpec.methodBuilder("peek")
                .returns(thisType)
                .addParameter(ParameterSpec.builder(
                        fi.parameterizedConsumer(tuple.genericArgs),
                        "fn",
                        Modifier.FINAL)
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .addStatement("fn.accept($L)", argList(tuple.varFields))
                .addStatement("return this")
                .build());
        // Full consume is almost the same, so let's include it here
        tupleClassBuilder.addMethod(MethodSpec.methodBuilder("consume")
                .addParameter(ParameterSpec.builder(
                        fi.parameterizedConsumer(tuple.genericArgs),
                        "fn",
                        Modifier.FINAL)
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .addStatement("fn.accept($L)", argList(tuple.varFields))
                .build());
        // Single arg peek
        tuple.idx().forEach(i -> {
            final TypeName type = tuple.types.get(i);
            final ParameterSpec field = tuple.varFields.get(i);
            final TypeName typeName = field.type();
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("peek" + (i+1))
                    .returns(thisType)
                    .addParameter(ParameterSpec.builder(
                            singleConsumerFor(type, typeName),
                            "fn",
                            Modifier.FINAL)
                            .build())
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("fn.accept($L)", tuple.varFields.get(i).name())
                    .addStatement("return this")
                    .build());
        });
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
                .addStatement("return fn.apply($L)", argList(tuple.varFields))
                .build());
        // Single element map
        tuple.idx().forEach(i -> {
            final TypeName type = tuple.types.get(i);
            final ParameterSpec field = tuple.varFields.get(i);
            final TypeName typeName = field.type();

            final TypeName returnType;
            if (thisType instanceof ParameterizedTypeName pType) {
                final TypeVariableName[] genericArgs = tuple.idx()
                        .filter(j -> !tuple.types.get(j).isPrimitive())
                        .mapToObj(j -> {
                            if (i == j) return zType;
                            else return tuple.varTypes.get(j);
                        })
                        .toArray(TypeVariableName[]::new);
                returnType = ParameterizedTypeName.get(pType.rawType(), genericArgs);
            } else {
                returnType = thisType;
            }
            // Full arg list
            tupleClassBuilder.addMethod(MethodSpec.methodBuilder("map" + (i + 1))
                    .addTypeVariable(zType)
                    .returns(returnType)
                    .addParameter(ParameterSpec.builder(
                            type.isPrimitive()
                                    ? fi.primitiveTypeOperators.get(type)
                                    : fi.parameterizedMapper(tuple.genericArgs, zType),
                            "fn", Modifier.FINAL)
                            .build())
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return $T.of($L)",
                            tuple.name,
                            tuple.idx().mapToObj(j -> {
                                final String varName = tuple.varFields.get(j).name();
                                if (i == j) return "fn.apply(" + argList(tuple.varFields) + ")";
                                else return varName;
                            }).collect(Collectors.joining(", ")))
                    .build());

            // Single arg
            tupleClassBuilder.addMethod(
                    singleFunctionFor(i, tuple, type, typeName,
                            MethodSpec.methodBuilder("map" + (i + 1))
                                    .addModifiers(Modifier.PUBLIC)
                                    .returns(returnType)));
        });
    }

    static FunctionalInterfaces getFunctionalInterfaceDefinitions(final Tuple tuple, final TypeSpec.Builder tupleClassBuilder) {
        final List<TypeName> types = tuple.types;
        final int nTypes = types.size();
        if (nTypes < 2 || nTypes > 4) {
            throw new IllegalStateException("Only tuples of length 2, 3, or 4 are supported");
        }
        if (nTypes == 2 && types.stream().allMatch(t -> Objects.equals(t, ClassName.OBJECT))) {
            // This is a pair; let's reuse the Bi* interfaces
            return new FunctionalInterfaces(
                    ClassName.get(BiPredicate.class), ClassName.get(BiConsumer.class),
                    ClassName.get(BiFunction.class), Map.of());
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
        final Map<TypeName, ClassName> operators = types.stream().filter(TypeName::isPrimitive).distinct().map(type -> {
            final ClassName operator = ClassName.get(
                    PACKAGE_NAME,
                    tuple.name.simpleName(),
                    tuple.name.simpleName() + "To" + capitalize(type.toString()) + "Operator");
            tupleClassBuilder.addType(TypeSpec.interfaceBuilder(operator)
                    .addAnnotation(FunctionalInterface.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariables(Arrays.asList(tuple.genericArgs))
                    .addMethod(MethodSpec.methodBuilder("apply")
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addParameters(tuple.varFields)
                            .returns(type)
                            .build())
                    .build());
            return Map.entry(type, operator);
        }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        return new FunctionalInterfaces(predicate, consumer, mapper, operators);
    }

    static String argList(final List<ParameterSpec> varFields) {
        return varFields.stream()
                .map(ParameterSpec::name)
                .collect(Collectors.joining(", "));
    }
}
