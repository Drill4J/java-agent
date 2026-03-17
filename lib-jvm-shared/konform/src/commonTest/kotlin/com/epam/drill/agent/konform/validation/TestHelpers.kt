package com.epam.drill.agent.konform.validation

import com.epam.drill.agent.konform.validation.Invalid
import com.epam.drill.agent.konform.validation.ValidationResult

fun <T> countFieldsWithErrors(validationResult: ValidationResult<T>) = (validationResult as Invalid).internalErrors.size
fun countErrors(validationResult: ValidationResult<*>, vararg properties: Any) = validationResult.get(*properties)?.size
    ?: 0
