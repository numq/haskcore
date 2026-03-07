package io.github.numq.haskcore.platform.theme.editor.palette.highlighting

import io.github.numq.haskcore.platform.theme.editor.palette.ColorPalette

interface HighlightingColorPalette : ColorPalette {
    val keywordColor: Int

    val keywordConditionalColor: Int

    val keywordImportColor: Int

    val keywordRepeatColor: Int

    val keywordDirectiveColor: Int

    val keywordExceptionColor: Int

    val keywordDebugColor: Int

    val typeColor: Int

    val constructorColor: Int

    val booleanColor: Int

    val functionColor: Int

    val functionCallColor: Int

    val variableColor: Int

    val variableParameterColor: Int

    val variableMemberColor: Int

    val operatorColor: Int

    val numberColor: Int

    val numberFloatColor: Int

    val stringColor: Int

    val characterColor: Int

    val stringSpecialSymbolColor: Int

    val commentColor: Int

    val commentDocumentationColor: Int

    val punctuationBracketColor: Int

    val punctuationDelimiterColor: Int

    val moduleColor: Int

    val spellColor: Int

    val wildcardColor: Int

    val unknownColor: Int

    val localDefinitionColor: Int

    val localReferenceColor: Int
}