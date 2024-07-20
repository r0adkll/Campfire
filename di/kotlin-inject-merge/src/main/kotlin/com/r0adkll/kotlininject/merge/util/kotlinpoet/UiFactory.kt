package com.r0adkll.kotlininject.merge.util.kotlinpoet

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.r0adkll.kotlininject.merge.util.ClassNames
import com.r0adkll.kotlininject.merge.util.MemberNames
import com.r0adkll.kotlininject.merge.util.ksp.findActualType
import com.r0adkll.kotlininject.merge.util.ksp.findParameterThatImplements
import com.r0adkll.kotlininject.merge.util.ksp.findParameterThatIs
import com.r0adkll.kotlininject.merge.util.toClassName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock

fun CodeBlock.Builder.addUiFactoryCreateStatement(
  element: KSFunctionDeclaration,
  screen: ClassName,
): CodeBlock.Builder {
  // Since we need to account for stateless screens, i.e. [StaticScreen], we should search for parameters
  // that implement [StaticScreen] rather than assuming positions
  val stateClassParameter = element.findParameterThatImplements(ClassNames.Circuit.UiState)
  val screenClassParameter = element.findParameterThatIs(screen)

  // Validate that a modifier exists, taking the assumption that it is the last parameter
  // due to the nature of standard composable function setups
  element.findParameterThatIs(ClassNames.Modifier)
    ?: throw IllegalStateException("@CircuitInject requires your composable function to have a Modifier parameter")

  when {
    stateClassParameter == null -> if (screenClassParameter != null) {
      addStatement(
        "%M<%T> { _, modifier -> %T(screen, modifier) }",
        MemberNames.CircuitUi,
        ClassNames.Circuit.UiState,
        element.toClassName(),
      )
    } else {
      addStatement(
        "%M<%T> { _, modifier -> %T(modifier) }",
        MemberNames.CircuitUi,
        ClassNames.Circuit.UiState,
        element.toClassName(),
      )
    }
    else -> {
      val stateClassName = stateClassParameter.type.findActualType().toClassName()
      addStatement(
        "%M<%T> { state, modifier -> %T(state, modifier) }",
        MemberNames.CircuitUi,
        stateClassName,
        element.toClassName(),
      )
    }
  }

  return this
}
