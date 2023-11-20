package com.fashiondigital.politicalspeeches.controller.validation

import com.fashiondigital.politicalspeeches.exception.EvaluationServiceException
import com.fashiondigital.politicalspeeches.model.ErrorCode
import java.net.URL
import java.util.regex.Pattern


object UrlHeaderValidation {
    private val URL_HEADER_PATTERN = Pattern.compile("url\\d+")

    //no need to support file, ftp or jar
    private val SUPPORTED_PROTOCOLS = setOf("http", "https")

    fun extractAndValidateUrlsFromRequest(headers: Map<String, String>): Set<String> {
        val result: MutableSet<String> = HashSet()
        for (key in headers.keys) {
            val m = URL_HEADER_PATTERN.matcher(key)
            if (m.find() && isValidURL(headers[key])) {
                result.add(headers[key]!!)
            }
        }
        if (result.isEmpty()) {
            throw EvaluationServiceException(ErrorCode.URL_PARAM_REQUIRED_ERROR)
        }
        return result
    }

    private fun isValidURL(url: String?): Boolean {
        return try {
            val uri = URL(url).toURI()
            if (!SUPPORTED_PROTOCOLS.contains(uri.scheme)) {
                throw EvaluationServiceException(ErrorCode.UNSUPPORTED_PROTOCOL)
            }
            true
        } catch (e: Exception) {
            when (e) {
                is EvaluationServiceException -> throw EvaluationServiceException(ErrorCode.UNSUPPORTED_PROTOCOL)
                else -> throw EvaluationServiceException(ErrorCode.URL_VALIDATION_ERROR, e)
            }
        }
    }
}
