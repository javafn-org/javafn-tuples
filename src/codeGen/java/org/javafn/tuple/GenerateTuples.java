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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.function.Predicate.not;

public class GenerateTuples {

    public static final String PACKAGE_NAME = "org.javafn.tuples";

    record Tuple(
            List<TypeName> types,
//            int numElements,
            ClassName name
//            ClassName predicate,
//            ClassName consumer,
//            ClassName mapper
    ) {
        public IntStream idx() { return IntStream.range(0, types.size()); }
//        public ParameterizedTypeName parameterizedMapper(final List<TypeVariableName> vars, TypeVariableName retType) {
//            final int nArgs = vars.size();
//            final TypeName[] args = new TypeName[nArgs + 1];
//            for (int i = 0; i < nArgs; i++) {
//                args[i] = vars.get(i);
//            }
//            args[nArgs] = retType;
//            return ParameterizedTypeName.get(mapper, args);
//        }
    }

    public static void main(String[] args) throws IOException {
        final File out = new File("build/generated/main/java");
        if (!out.exists()) {
            if (!out.mkdirs()) {
                throw new IOException("Error: Unable to create output directory " + out.getAbsolutePath());
            }
        }

        final var pair = new Tuple(
                List.of(ClassName.OBJECT, ClassName.OBJECT),
                ClassName.get(PACKAGE_NAME, "PairType"));
//                ClassName.get(BiPredicate.class),
//                ClassName.get(BiConsumer.class),
//                ClassName.get(BiFunction.class));
        genTuple(out, pair);
        genTuple(out, new Tuple(List.of(ClassName.INT, ClassName.OBJECT), ClassName.get(PACKAGE_NAME, "IntObjPair")));
    }

    static void genTuple(final File packageDir, final Tuple tuple) throws IOException {
        final List<TypeVariableName> varTypes = tuple.idx()
                .mapToObj(i -> {
                    final var type = tuple.types.get(i);
                    if (type == ClassName.OBJECT) {
                        return TypeVariableName.get("V" + (i+1));
                    } else {
                        return TypeVariableName.get(type.toString());
                    }
                })
                .toList();
        final TypeVariableName[] genericArgs = tuple.idx()
                .filter(i -> !tuple.types.get(i).isPrimitive())
                .mapToObj(varTypes::get)
                .toArray(TypeVariableName[]::new);
        final List<ParameterSpec> varFields = tuple.idx()
                .mapToObj(i -> ParameterSpec.builder(varTypes.get(i), "v" + (i+1), Modifier.FINAL).build())
                .toList();

        final TypeVariableName zType = TypeVariableName.get("Z");
        final ParameterSpec zField = ParameterSpec.builder(zType, "z").build();

        final var tupleClassBuilder = TypeSpec.recordBuilder(tuple.name);

        // Define the record, its generic arguments, and fields
        tupleClassBuilder
                .addTypeVariables(Arrays.asList(genericArgs))
                .recordConstructor(MethodSpec.constructorBuilder()
                        .addParameters(tuple.idx().mapToObj(i -> ParameterSpec.builder(varTypes.get(i), "v" + (i+1)).build()).toList())
                        .build());
        // Define static factory methods
        tupleClassBuilder
                .addMethod(MethodSpec.methodBuilder("of")
                        .returns(ParameterizedTypeName.get(tuple.name, genericArgs))
                        .addTypeVariables(Arrays.asList(genericArgs))
                        .addParameters(varFields)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addStatement("return new $L<>($L)", tuple.name.simpleName(),
                                varFields.stream().map(ParameterSpec::name).collect(Collectors.joining(", ")))
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
                genericTypes = genericArgs;
            } else {
                argType = zType;
                genericTypes = tuple.idx()
                        .filter(j -> !tuple.types.get(j).isPrimitive())
                        .mapToObj(j -> {
                            if (i == j) return zType;
                            else return varTypes.get(j);
                        })
                        .toArray(TypeVariableName[]::new);
                methodBuilder.addTypeVariable(zType);
            }
            final List<ParameterSpec> argNames = new ArrayList<>(varFields);
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
}
