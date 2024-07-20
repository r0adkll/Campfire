package com.r0adkll.kotlininject.merge.util.ksp

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.r0adkll.kotlininject.merge.util.toClassName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Search through all of the [KSFunctionDeclaration.parameters] for one whos type implements the
 * passed [className]
 */
fun KSFunctionDeclaration.findParameterThatImplements(className: ClassName): KSValueParameter? {
  return parameters.find { parameter ->
    val classDecl = parameter.type.resolve().declaration as? KSClassDeclaration
    if (classDecl != null) {
      return@find classDecl.getAllSuperTypes()
        .any { it.declaration.toClassName() == className }
    }
    false
  }
}

fun KSFunctionDeclaration.findParameterThatIs(className: ClassName): KSValueParameter? {
  return parameters.find { parameter ->
    parameter.type.resolve().toClassName() == className
  }
}
